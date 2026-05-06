package io.github.shino0526y.book_api.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate

data class UpsertAuthorRequest(
	@field:NotBlank
	val name: String,
	@field:PastOrPresent
	val birthDate: LocalDate,
)

data class AuthorResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
)

data class AuthorBooksResponse(
	val author: AuthorResponse,
	val books: List<BookSummaryResponse>,
)
