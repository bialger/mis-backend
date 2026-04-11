package com.bialger.infrastructure.storage

/**
 * Abstraction for object (S3-compatible) storage.
 * Production implementation: [YandexStorageService].
 * Test implementation: a simple in-memory stub via @Replaces.
 */
interface StorageService {
    /**
     * Uploads [bytes] under [key] and returns the public URL of the stored object.
     */
    fun upload(key: String, bytes: ByteArray, contentType: String): String

    /**
     * Deletes the object identified by [key].
     */
    fun delete(key: String)
}
