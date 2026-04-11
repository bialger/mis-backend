package com.bialger.infrastructure

import com.bialger.infrastructure.storage.StorageService
import com.bialger.infrastructure.storage.YandexStorageService
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.env.Environment
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

/**
 * In-test stub for [StorageService] that replaces [YandexStorageService].
 *
 * Activated only in the `test` Micronaut environment so real S3 calls are never made
 * during test runs. Stores the last uploaded key/bytes in memory for assertion support.
 */
@Singleton
@Replaces(YandexStorageService::class)
@Requires(env = [Environment.TEST])
class StorageServiceStub : StorageService {

    val uploads: MutableList<Upload> = mutableListOf()
    val deletions: MutableList<String> = mutableListOf()

    data class Upload(val key: String, val bytes: ByteArray, val contentType: String)

    override fun upload(key: String, bytes: ByteArray, contentType: String): String {
        uploads += Upload(key, bytes, contentType)
        return "https://storage.yandexcloud.net/test-bucket/$key"
    }

    override fun delete(key: String) {
        deletions += key
    }

    fun reset() {
        uploads.clear()
        deletions.clear()
    }
}
