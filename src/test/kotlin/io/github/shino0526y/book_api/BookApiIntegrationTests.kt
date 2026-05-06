package io.github.shino0526y.book_api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.shino0526y.book_api.generated.jooq.tables.references.AUTHORS
import io.github.shino0526y.book_api.generated.jooq.tables.references.BOOK_AUTHORS
import io.github.shino0526y.book_api.generated.jooq.tables.references.BOOKS
import java.math.BigDecimal
import java.time.LocalDate
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
class BookApiIntegrationTests {
	private val objectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()

	lateinit var mockMvc: MockMvc

	@Autowired
	lateinit var dsl: DSLContext

	@Autowired
	lateinit var webApplicationContext: WebApplicationContext

	@BeforeEach
	fun cleanDatabase() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
		dsl.deleteFrom(BOOKS).execute()
		dsl.deleteFrom(AUTHORS).execute()
	}

	@Test
	fun `author can be created and updated`() {
		val createResponse = mockMvc.perform(
			post("/api/authors")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					jsonBody(
						mapOf(
							"name" to "Ursula K. Le Guin",
							"birthDate" to "1929-10-21",
						),
					),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.name").value("Ursula K. Le Guin"))
			.andExpect(jsonPath("$.birthDate").value("1929-10-21"))
			.andReturn()
			.response
			.contentAsString

		val authorId = readJson(createResponse)["id"].asLong()

		mockMvc.perform(
			put("/api/authors/$authorId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					jsonBody(
						mapOf(
							"name" to "Ursula Le Guin",
							"birthDate" to "1929-10-21",
						),
					),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(authorId))
			.andExpect(jsonPath("$.name").value("Ursula Le Guin"))
	}

	@Test
	fun `book can be created updated and listed by author`() {
		val authorId1 = insertAuthor("Neil Gaiman", LocalDate.of(1960, 11, 10))
		val authorId2 = insertAuthor("Terry Pratchett", LocalDate.of(1948, 4, 28))

		val createResponse = mockMvc.perform(
			post("/api/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					jsonBody(
						mapOf(
							"title" to "Good Omens",
							"price" to BigDecimal("1800.00"),
							"authorIds" to listOf(authorId1, authorId2),
							"publicationStatus" to "UNPUBLISHED",
						),
					),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.title").value("Good Omens"))
			.andExpect(jsonPath("$.authors.length()").value(2))
			.andReturn()
			.response
			.contentAsString

		val bookId = readJson(createResponse)["id"].asLong()

		mockMvc.perform(
			put("/api/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					jsonBody(
						mapOf(
							"title" to "Good Omens Revised",
							"price" to BigDecimal("2200.00"),
							"authorIds" to listOf(authorId1),
							"publicationStatus" to "PUBLISHED",
						),
					),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.title").value("Good Omens Revised"))
			.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
			.andExpect(jsonPath("$.authors.length()").value(1))

		mockMvc.perform(get("/api/authors/$authorId1/books"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.author.id").value(authorId1))
			.andExpect(jsonPath("$.books.length()").value(1))
			.andExpect(jsonPath("$.books[0].title").value("Good Omens Revised"))
			.andExpect(jsonPath("$.books[0].publicationStatus").value("PUBLISHED"))

		mockMvc.perform(get("/api/authors/$authorId2/books"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.author.id").value(authorId2))
			.andExpect(jsonPath("$.books.length()").value(0))
	}

	@Test
	fun `published book cannot be reverted to unpublished`() {
		val authorId = insertAuthor("Mary Shelley", LocalDate.of(1797, 8, 30))
		val bookId = insertBookWithAuthors(
			title = "Frankenstein",
			price = BigDecimal("1500.00"),
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)

		mockMvc.perform(
			put("/api/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					jsonBody(
						mapOf(
							"title" to "Frankenstein",
							"price" to BigDecimal("1500.00"),
							"authorIds" to listOf(authorId),
							"publicationStatus" to "UNPUBLISHED",
						),
					),
				),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.message").value("published books cannot be changed back to unpublished"))
	}

	private fun insertAuthor(name: String, birthDate: LocalDate): Long = dsl.insertInto(AUTHORS)
		.set(AUTHORS.NAME, name)
		.set(AUTHORS.BIRTH_DATE, birthDate)
		.returningResult(AUTHORS.ID)
		.fetchOne(AUTHORS.ID)!!

	private fun insertBookWithAuthors(
		title: String,
		price: BigDecimal,
		publicationStatus: String,
		authorIds: List<Long>,
	): Long = dsl.transactionResult { configuration ->
		val tx = DSL.using(configuration)
		val bookId = tx.insertInto(BOOKS)
			.set(BOOKS.TITLE, title)
			.set(BOOKS.PRICE, price)
			.set(BOOKS.PUBLICATION_STATUS, publicationStatus)
			.returningResult(BOOKS.ID)
			.fetchOne(BOOKS.ID)!!

		authorIds.forEach { authorId ->
			tx.insertInto(BOOK_AUTHORS)
				.set(BOOK_AUTHORS.BOOK_ID, bookId)
				.set(BOOK_AUTHORS.AUTHOR_ID, authorId)
				.execute()
		}

		bookId
	}

	private fun jsonBody(value: Any): String = objectMapper.writeValueAsString(value)

	private fun readJson(body: String): JsonNode = objectMapper.readTree(body)
}
