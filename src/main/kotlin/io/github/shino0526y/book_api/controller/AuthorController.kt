package io.github.shino0526y.book_api.controller

import io.github.shino0526y.book_api.api.AuthorBooksResponse
import io.github.shino0526y.book_api.api.AuthorResponse
import io.github.shino0526y.book_api.api.UpsertAuthorRequest
import io.github.shino0526y.book_api.service.AuthorService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/authors")
class AuthorController(
	private val authorService: AuthorService,
) {
	@PostMapping
	fun create(@Valid @RequestBody request: UpsertAuthorRequest): ResponseEntity<AuthorResponse> {
		val author = authorService.create(request)
		return ResponseEntity.created(URI.create("/api/authors/${author.id}")).body(author)
	}

	@PutMapping("/{authorId}")
	fun update(
		@PathVariable authorId: Long,
		@Valid @RequestBody request: UpsertAuthorRequest,
	): AuthorResponse = authorService.update(authorId, request)

	@GetMapping("/{authorId}/books")
	fun getBooks(@PathVariable authorId: Long): AuthorBooksResponse = authorService.getBooks(authorId)
}
