package io.github.shino0526y.book_api.api

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class UpsertBookRequest(
	@field:NotBlank
	val title: String,
	@field:DecimalMin(value = "0.00")
	val price: BigDecimal,
	@field:Size(min = 1)
	val authorIds: List<Long>,
	val publicationStatus: PublicationStatus,
)

data class BookAuthorResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
)

data class BookResponse(
	val id: Long,
	val title: String,
	val price: BigDecimal,
	val publicationStatus: PublicationStatus,
	val authors: List<BookAuthorResponse>,
)

data class BookSummaryResponse(
	val id: Long,
	val title: String,
	val price: BigDecimal,
	val publicationStatus: PublicationStatus,
)
