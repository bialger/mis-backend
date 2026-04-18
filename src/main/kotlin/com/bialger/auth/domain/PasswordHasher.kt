package com.bialger.auth.domain

import jakarta.inject.Singleton
import org.mindrot.jbcrypt.BCrypt

@Singleton
class PasswordHasher {

    fun hash(plainPassword: String): String = BCrypt.hashpw(plainPassword, BCrypt.gensalt())

    fun matches(plainPassword: String, storedHash: String): Boolean =
        BCrypt.checkpw(plainPassword, storedHash)
}
