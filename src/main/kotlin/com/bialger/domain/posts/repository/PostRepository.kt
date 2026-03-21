package com.bialger.domain.posts.repository

import com.bialger.domain.posts.entity.PostEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PostRepository : CrudRepository<PostEntity, Long> {

    @Query("SELECT * FROM crm_post ORDER BY created_at DESC")
    fun findAllOrdered(): List<PostEntity>
}
