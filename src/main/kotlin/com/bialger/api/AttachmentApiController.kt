package com.bialger.api

import com.bialger.api.dto.AttachmentRestDto
import com.bialger.domain.attachment.entity.AttachmentEntity
import com.bialger.domain.attachment.enums.FileType
import com.bialger.domain.attachment.repository.AttachmentRepository
import com.bialger.infrastructure.storage.StorageService
import com.bialger.infrastructure.storage.YandexStorageService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Part
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Controller("/api/attachments")
@Tag(name = "Attachments", description = "Patient file attachments stored in Yandex Object Storage")
open class AttachmentApiController(
    private val attachmentRepository: AttachmentRepository,
    private val storageService: StorageService
) {
    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10 MB

        private val ALLOWED_CONTENT_TYPES = mapOf(
            "application/pdf" to FileType.PDF,
            "image/jpeg" to FileType.IMAGE,
            "image/png" to FileType.IMAGE,
            "image/gif" to FileType.IMAGE,
            "image/webp" to FileType.IMAGE,
            "text/plain" to FileType.TEXT,
            "text/csv" to FileType.TEXT,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to FileType.DOCUMENT,
            "application/msword" to FileType.DOCUMENT,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to FileType.DOCUMENT,
            "application/vnd.ms-excel" to FileType.DOCUMENT,
            "application/zip" to FileType.COMPRESSED,
            "application/x-zip-compressed" to FileType.COMPRESSED
        )
    }

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List attachments for a patient")
    @ApiResponses(
        ApiResponse(
            responseCode = "200", description = "List of attachments",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AttachmentRestDto::class))]
        ),
        ApiResponse(responseCode = "400", description = "Missing patientId")
    )
    fun list(@QueryValue patientId: UUID): List<AttachmentRestDto> =
        attachmentRepository.findByPatientId(patientId).map { it.toDto() }

    @Get("/{id}/download")
    @Operation(summary = "Download attachment file proxied through the backend")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "File bytes with original filename"),
        ApiResponse(responseCode = "404", description = "Attachment not found")
    )
    open fun download(@PathVariable id: UUID): HttpResponse<ByteArray> {
        val entity = attachmentRepository.findById(id)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Attachment not found") }

        val key = extractStorageKey(entity.filePath)
        val fetched = storageService.download(key)

        val safeFileName = URLEncoder.encode(entity.fileName, StandardCharsets.UTF_8)
            .replace("+", "%20")
        val disposition = "attachment; filename=\"${entity.fileName}\"; filename*=UTF-8''$safeFileName"

        return HttpResponse.ok(fetched.bytes)
            .header("Content-Disposition", disposition)
            .header("Content-Type", fetched.contentType)
            .header("Content-Length", fetched.bytes.size.toString())
            .header("Cache-Control", "private, max-age=3600")
    }

    /**
     * Extracts the S3 object key from a full public URL.
     * Supports both YandexStorageService URLs and the test-stub's "test-bucket" URL.
     */
    private fun extractStorageKey(filePath: String): String {
        if (storageService is YandexStorageService) {
            return storageService.keyFromUrl(filePath)
        }
        // Fallback for test stub: https://storage.yandexcloud.net/test-bucket/<key>
        val marker = ".net/"
        val afterNet = filePath.substringAfter(marker)
        return afterNet.substringAfter("/")   // skip bucket name
    }

    @Post("/upload", produces = [MediaType.APPLICATION_JSON])
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload a file attachment for a patient")
    @ApiResponses(
        ApiResponse(
            responseCode = "200", description = "Created attachment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AttachmentRestDto::class))]
        ),
        ApiResponse(responseCode = "400", description = "Validation error (size, type, missing params)")
    )
    open fun upload(
        @Part file: CompletedFileUpload,
        @Part patientId: String,
        @Part uploadedBy: String,
        @Part appointmentId: String?,
        @Part description: String?
    ): HttpResponse<AttachmentRestDto> {
        val fileBytes = file.bytes
        val fileSize = fileBytes.size.toLong()
        if (fileSize == 0L) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty")
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "File exceeds the 10 MB limit (got ${fileSize} bytes)")
        }

        val contentType = file.contentType.map { it.name }.orElse("application/octet-stream")
        val fileType = ALLOWED_CONTENT_TYPES[contentType]
            ?: throw HttpStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported content type: $contentType. Allowed: ${ALLOWED_CONTENT_TYPES.keys.joinToString()}"
            )

        val patientUuid = runCatching { UUID.fromString(patientId) }.getOrElse {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid patientId UUID: $patientId")
        }
        val uploadedByUuid = runCatching { UUID.fromString(uploadedBy) }.getOrElse {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid uploadedBy UUID: $uploadedBy")
        }
        val appointmentUuid = appointmentId?.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrElse {
                throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid appointmentId UUID: $it")
            }
        }

        val originalFileName = file.filename ?: "upload"
        // S3 key uses only UUIDs — original filename is stored in DB, never exposed in the URL.
        // This prevents special-character issues, path traversal, and metadata leakage.
        val objectKey = "attachments/${patientUuid}/${UUID.randomUUID()}"
        val publicUrl = storageService.upload(objectKey, fileBytes, contentType)

        val entity = AttachmentEntity(
            id = UUID.randomUUID(),
            patientId = patientUuid,
            appointmentId = appointmentUuid,
            fileName = originalFileName,
            fileType = fileType,
            filePath = publicUrl,
            fileSize = fileSize,
            description = description?.takeIf { it.isNotBlank() },
            uploadedBy = uploadedByUuid,
            uploadedAt = Instant.now()
        )
        attachmentRepository.save(entity)

        return HttpResponse.ok(entity.toDto())
    }

    private fun AttachmentEntity.toDto() = AttachmentRestDto(
        id = id.toString(),
        patientId = patientId.toString(),
        appointmentId = appointmentId?.toString(),
        medicalRecordId = medicalRecordId?.toString(),
        fileName = fileName,
        fileType = fileType.name,
        filePath = filePath,
        fileSize = fileSize,
        description = description,
        uploadedBy = uploadedBy.toString(),
        uploadedAt = uploadedAt?.toString()
    )
}
