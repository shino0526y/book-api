package io.github.shino0526y.book_api.controller

import io.github.shino0526y.book_api.api.BookResponse
import io.github.shino0526y.book_api.api.UpsertBookRequest
import io.github.shino0526y.book_api.service.BookService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/books")
class BookController(
	private val bookService: BookService,
) {
	@PostMapping
	fun create(@Valid @RequestBody request: UpsertBookRequest): ResponseEntity<BookResponse> {
		val book = bookService.create(request)
		return ResponseEntity.created(URI.create("/api/books/${book.id}")).body(book)
	}

	@PutMapping("/{bookId}")
	fun update(
		@PathVariable bookId: Long,
		@Valid @RequestBody request: UpsertBookRequest,
	): BookResponse = bookService.update(bookId, request)
}
