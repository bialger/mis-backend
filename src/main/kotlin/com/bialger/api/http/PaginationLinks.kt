package com.bialger.api.http

import io.micronaut.data.model.Page
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse

/**
 * RFC 5988 Link header for Micronaut [Page] (HATEOAS-style pagination).
 */
object PaginationLinks {

    fun appendToResponse(
        request: HttpRequest<*>,
        page: Page<*>,
        response: MutableHttpResponse<*>
    ) {
        build(request, page)?.let { response.header("Link", it) }
    }

    fun build(request: HttpRequest<*>, page: Page<*>): String? {
        val parts = mutableListOf<String>()
        val path = request.path
        val query = linkedMapOf<String, MutableList<String>>()
        request.parameters.forEach { name, values ->
            if (name != "page" && name != "size") {
                query[name] = values.toMutableList()
            }
        }
        val pageNum = page.pageable.number
        val size = page.size
        val totalPages = page.totalPages
        if (pageNum + 1 < totalPages) {
            parts += "<${absoluteLink(request, path, query, pageNum + 1, size)}>; rel=\"next\""
        }
        if (pageNum > 0) {
            parts += "<${absoluteLink(request, path, query, pageNum - 1, size)}>; rel=\"prev\""
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun absoluteLink(
        request: HttpRequest<*>,
        path: String,
        baseQuery: Map<String, MutableList<String>>,
        page: Int,
        size: Int
    ): String {
        val scheme = request.uri.scheme ?: "http"
        val host = request.uri.host ?: "localhost"
        val port = request.uri.port
        val portPart = if (port > 0 && port != 80 && port != 443) ":$port" else ""
        val sb = StringBuilder()
        sb.append(scheme).append("://").append(host).append(portPart).append(path).append('?')
        val q = LinkedHashMap<String, MutableList<String>>()
        baseQuery.forEach { (k, v) -> q[k] = v.toMutableList() }
        q["page"] = mutableListOf(page.toString())
        q["size"] = mutableListOf(size.toString())
        sb.append(
            q.entries.joinToString("&") { (k, vals) ->
                vals.joinToString("&") { v -> "${encode(k)}=${encode(v)}" }
            }
        )
        return sb.toString()
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, Charsets.UTF_8)
}
