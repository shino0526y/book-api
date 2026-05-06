package io.github.shino0526y.book_api.service

import io.github.shino0526y.book_api.api.AuthorBooksResponse
import io.github.shino0526y.book_api.api.AuthorResponse
import io.github.shino0526y.book_api.api.BookSummaryResponse
import io.github.shino0526y.book_api.api.PublicationStatus
import io.github.shino0526y.book_api.api.UpsertAuthorRequest
import io.github.shino0526y.book_api.exception.ResourceNotFoundException
import io.github.shino0526y.book_api.generated.jooq.tables.references.AUTHORS
import io.github.shino0526y.book_api.generated.jooq.tables.references.BOOK_AUTHORS
import io.github.shino0526y.book_api.generated.jooq.tables.references.BOOKS
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class AuthorService(
	private val dsl: DSLContext,
) {
	fun create(request: UpsertAuthorRequest): AuthorResponse {
		val author = dsl.insertInto(AUTHORS)
			.set(AUTHORS.NAME, request.name.trim())
			.set(AUTHORS.BIRTH_DATE, request.birthDate)
			.returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.fetchOne() ?: error("Failed to create author")

		return toAuthorResponse(author)
	}

	fun update(authorId: Long, request: UpsertAuthorRequest): AuthorResponse {
		val author = dsl.update(AUTHORS)
			.set(AUTHORS.NAME, request.name.trim())
			.set(AUTHORS.BIRTH_DATE, request.birthDate)
			.where(AUTHORS.ID.eq(authorId))
			.returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.fetchOne() ?: throw ResourceNotFoundException("author $authorId was not found")

		return toAuthorResponse(author)
	}

	fun getBooks(authorId: Long): AuthorBooksResponse {
		val author = dsl.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.from(AUTHORS)
			.where(AUTHORS.ID.eq(authorId))
			.fetchOne() ?: throw ResourceNotFoundException("author $authorId was not found")

		val books = dsl.selectDistinct(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
			.from(BOOKS)
			.join(BOOK_AUTHORS).on(BOOK_AUTHORS.BOOK_ID.eq(BOOKS.ID))
			.where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
			.orderBy(BOOKS.ID.asc())
			.fetch { record ->
				BookSummaryResponse(
					id = record.get(BOOKS.ID)!!,
					title = record.get(BOOKS.TITLE)!!,
					price = record.get(BOOKS.PRICE)!!,
					publicationStatus = PublicationStatus.valueOf(record.get(BOOKS.PUBLICATION_STATUS)!!),
				)
			}

		return AuthorBooksResponse(
			author = toAuthorResponse(author),
			books = books,
		)
	}

	private fun toAuthorResponse(record: Record): AuthorResponse = AuthorResponse(
		id = record.get(AUTHORS.ID)!!,
		name = record.get(AUTHORS.NAME)!!,
		birthDate = record.get(AUTHORS.BIRTH_DATE)!!,
	)
}
