package io.github.shino0526y.book_api.exception

import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiErrorResponse(
	val message: String,
	val details: List<String> = emptyList(),
)

@RestControllerAdvice
class ApiExceptionHandler {
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleMethodArgumentNotValid(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
		val details = exception.bindingResult.allErrors.mapNotNull { error ->
			when (error) {
				is FieldError -> "${error.field}: ${error.defaultMessage}"
				else -> error.defaultMessage
			}
		}

		return ResponseEntity.badRequest().body(ApiErrorResponse(message = "validation failed", details = details))
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleHttpMessageNotReadable(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.badRequest().body(ApiErrorResponse(message = "invalid request body"))

	@ExceptionHandler(RequestValidationException::class)
	fun handleRequestValidation(exception: RequestValidationException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.badRequest().body(ApiErrorResponse(message = exception.message ?: "invalid request"))

	@ExceptionHandler(ResourceNotFoundException::class)
	fun handleNotFound(exception: ResourceNotFoundException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiErrorResponse(message = exception.message ?: "resource not found"))

	@ExceptionHandler(StateConflictException::class)
	fun handleConflict(exception: StateConflictException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiErrorResponse(message = exception.message ?: "conflict"))

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrityViolation(exception: DataIntegrityViolationException): ResponseEntity<ApiErrorResponse> {
		val message = NestedExceptionUtils.getMostSpecificCause(exception).message ?: "data integrity violation"
		return ResponseEntity.badRequest().body(ApiErrorResponse(message = message))
	}
}
