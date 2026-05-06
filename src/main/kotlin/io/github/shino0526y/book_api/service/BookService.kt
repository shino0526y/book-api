package io.github.shino0526y.book_api.service

import io.github.shino0526y.book_api.api.BookAuthorResponse
import io.github.shino0526y.book_api.api.BookResponse
import io.github.shino0526y.book_api.api.PublicationStatus
import io.github.shino0526y.book_api.api.UpsertBookRequest
import io.github.shino0526y.book_api.exception.RequestValidationException
import io.github.shino0526y.book_api.exception.ResourceNotFoundException
import io.github.shino0526y.book_api.exception.StateConflictException
import io.github.shino0526y.book_api.generated.jooq.tables.references.AUTHORS
import io.github.shino0526y.book_api.generated.jooq.tables.references.BOOK_AUTHORS
import io.github.shino0526y.book_api.generated.jooq.tables.references.BOOKS
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
	private val dsl: DSLContext,
) {
	@Transactional
	fun create(request: UpsertBookRequest): BookResponse {
		val authorIds = normalizeAuthorIds(request.authorIds)
		validateAuthorsExist(authorIds)

		val book = dsl.insertInto(BOOKS)
			.set(BOOKS.TITLE, request.title.trim())
			.set(BOOKS.PRICE, request.price)
			.set(BOOKS.PUBLICATION_STATUS, request.publicationStatus.name)
			.returning(BOOKS.ID)
			.fetchOne() ?: error("Failed to create book")

		val bookId = book.get(BOOKS.ID)!!
		replaceAuthors(bookId, authorIds)

		return get(bookId)
	}

	@Transactional
	fun update(bookId: Long, request: UpsertBookRequest): BookResponse {
		val currentStatus = dsl.select(BOOKS.PUBLICATION_STATUS)
			.from(BOOKS)
			.where(BOOKS.ID.eq(bookId))
			.fetchOne(BOOKS.PUBLICATION_STATUS)
			?: throw ResourceNotFoundException("book $bookId was not found")

		if (PublicationStatus.valueOf(currentStatus) == PublicationStatus.PUBLISHED && request.publicationStatus == PublicationStatus.UNPUBLISHED) {
			throw StateConflictException("published books cannot be changed back to unpublished")
		}

		val authorIds = normalizeAuthorIds(request.authorIds)
		validateAuthorsExist(authorIds)

		dsl.update(BOOKS)
			.set(BOOKS.TITLE, request.title.trim())
			.set(BOOKS.PRICE, request.price)
			.set(BOOKS.PUBLICATION_STATUS, request.publicationStatus.name)
			.where(BOOKS.ID.eq(bookId))
			.execute()

		replaceAuthors(bookId, authorIds)

		return get(bookId)
	}

	fun get(bookId: Long): BookResponse {
		val book = dsl.select(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
			.from(BOOKS)
			.where(BOOKS.ID.eq(bookId))
			.fetchOne() ?: throw ResourceNotFoundException("book $bookId was not found")

		val authors = dsl.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.from(AUTHORS)
			.join(BOOK_AUTHORS).on(BOOK_AUTHORS.AUTHOR_ID.eq(AUTHORS.ID))
			.where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
			.orderBy(AUTHORS.ID.asc())
			.fetch { record ->
				BookAuthorResponse(
					id = record.get(AUTHORS.ID)!!,
					name = record.get(AUTHORS.NAME)!!,
					birthDate = record.get(AUTHORS.BIRTH_DATE)!!,
				)
			}

		return BookResponse(
			id = book.get(BOOKS.ID)!!,
			title = book.get(BOOKS.TITLE)!!,
			price = book.get(BOOKS.PRICE)!!,
			publicationStatus = PublicationStatus.valueOf(book.get(BOOKS.PUBLICATION_STATUS)!!),
			authors = authors,
		)
	}

	private fun normalizeAuthorIds(authorIds: List<Long>): List<Long> {
		if (authorIds.isEmpty()) {
			throw RequestValidationException("book must have at least one author")
		}

		if (authorIds.any { it <= 0 }) {
			throw RequestValidationException("authorIds must contain positive ids")
		}

		return authorIds.distinct()
	}

	private fun validateAuthorsExist(authorIds: List<Long>) {
		val existingAuthorIds = dsl.select(AUTHORS.ID)
			.from(AUTHORS)
			.where(AUTHORS.ID.`in`(authorIds))
			.fetchSet(AUTHORS.ID)
			.filterNotNull()
			.toSet()

		val missingAuthorIds = authorIds.filterNot(existingAuthorIds::contains)
		if (missingAuthorIds.isNotEmpty()) {
			throw RequestValidationException("authorIds contains unknown authors: $missingAuthorIds")
		}
	}

	private fun replaceAuthors(bookId: Long, authorIds: List<Long>) {
		dsl.deleteFrom(BOOK_AUTHORS)
			.where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
			.execute()

		authorIds.forEach { authorId ->
			dsl.insertInto(BOOK_AUTHORS)
				.set(BOOK_AUTHORS.BOOK_ID, bookId)
				.set(BOOK_AUTHORS.AUTHOR_ID, authorId)
				.execute()
		}
	}
}
