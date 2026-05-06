package io.github.shino0526y.book_api.exception

open class ApiException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : ApiException(message)

class RequestValidationException(message: String) : ApiException(message)

class StateConflictException(message: String) : ApiException(message)
