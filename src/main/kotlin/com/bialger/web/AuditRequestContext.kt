package com.bialger.web

import io.micronaut.runtime.http.scope.RequestScope
import java.util.UUID

@RequestScope
class AuditRequestContext {
    var ipAddress: String? = null
    var userAgent: String? = null
    var actorId: UUID? = null
}
