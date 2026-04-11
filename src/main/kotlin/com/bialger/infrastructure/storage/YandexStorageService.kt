package com.bialger.infrastructure.storage

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI

/**
 * Infrastructure module for Yandex Object Storage (S3-compatible).
 *
 * Wraps AWS SDK v2 S3Client pointed at the Yandex Cloud endpoint
 * (`https://storage.yandexcloud.net`). The access key and secret come from
 * environment variables YC_KEY / YC_SECRET via application.properties placeholders.
 *
 * Uploaded objects are made publicly readable so that the stored URL can be
 * served directly to clients without signed URLs.
 */
@Singleton
class YandexStorageService(
    @Value("\${yc.key}") private val accessKey: String,
    @Value("\${yc.secret}") private val secretKey: String,
    @Value("\${yc.bucket}") private val bucket: String,
) : StorageService {
    private val log = LoggerFactory.getLogger(YandexStorageService::class.java)

    private val s3: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(URI.create("https://storage.yandexcloud.net"))
            .region(Region.of("ru-central1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            // YOS requires path-style URLs: endpoint/bucket/key (not bucket.endpoint/key)
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
            .build()
    }

    /**
     * Uploads [bytes] under [key] in the configured bucket.
     *
     * @return Public URL of the uploaded object.
     */
    override fun upload(key: String, bytes: ByteArray, contentType: String): String {
        log.info("Uploading {} bytes to YOS: bucket={} key={}", bytes.size, bucket, key)
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .build()
        s3.putObject(request, RequestBody.fromBytes(bytes))
        return publicUrl(key)
    }

    /**
     * Deletes the object identified by [key] from the configured bucket.
     */
    override fun delete(key: String) {
        log.info("Deleting from YOS: bucket={} key={}", bucket, key)
        val request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()
        s3.deleteObject(request)
    }

    private fun publicUrl(key: String) =
        "https://storage.yandexcloud.net/$bucket/$key"
}
