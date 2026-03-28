package com.bialger.api.error

import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.core.util.CollectionUtils
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.response.ErrorContext
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor
import io.micronaut.validation.exceptions.ConstraintExceptionHandler
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ElementKind
@Produces
@Singleton
@Replaces(ConstraintExceptionHandler::class)
@Requires(classes = [ExceptionHandler::class])
class ConstraintViolationExceptionHandler @Inject constructor(
    private val responseProcessor: ErrorResponseProcessor<Any>
) : ExceptionHandler<ConstraintViolationException, HttpResponse<*>> {

    override fun handle(
        request: HttpRequest<*>,
        exception: ConstraintViolationException
    ): HttpResponse<*> {
        if (request.isApiRequest()) {
            val violations = exception.constraintViolations.map { v ->
                ApiErrorResponse.ViolationItem(
                    path = v.propertyPath?.toString() ?: "",
                    message = v.message
                )
            }
            return HttpResponse.badRequest(
                ApiErrorResponse(
                    error = "validation_failed",
                    message = "Validation failed",
                    violations = violations
                )
            )
        }
        val violations = exception.constraintViolations
        val response: MutableHttpResponse<Any> = HttpResponse.badRequest()
        val contextBuilder = ErrorContext.builder(request).cause(exception)
        return if (CollectionUtils.isEmpty(violations)) {
            responseProcessor.processResponse(
                contextBuilder.errorMessage(
                    exception.message ?: HttpStatus.BAD_REQUEST.reason
                ).build(),
                response
            )
        } else {
            responseProcessor.processResponse(
                contextBuilder.errorMessages(
                    violations.map { buildMessage(it) }.sorted()
                ).build(),
                response
            )
        }
    }

    private fun buildMessage(violation: ConstraintViolation<*>): String {
        val propertyPath = violation.propertyPath
        val message = StringBuilder()
        val iterator = propertyPath.iterator()
        var firstNode = true
        while (iterator.hasNext()) {
            val node = iterator.next()
            if (node.kind == ElementKind.METHOD || node.kind == ElementKind.CONSTRUCTOR) {
                continue
            }
            if (node.isInIterable) {
                message.append('[')
                if (node.key != null) {
                    message.append(node.key)
                } else if (node.index != null) {
                    message.append(node.index)
                }
                message.append(']')
            }
            if (node.kind != ElementKind.CONTAINER_ELEMENT && node.name != null) {
                if (!firstNode) {
                    message.append('.')
                }
                message.append(node.name)
            }
            firstNode = false
        }
        message.append(": ").append(violation.message)
        return message.toString()
    }
}
