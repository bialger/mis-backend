package com.bialger.db.converter

import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.model.runtime.convert.AttributeConverter
import jakarta.inject.Singleton
import org.postgresql.util.PGobject

@Singleton
class JsonbConverter : AttributeConverter<String?, Any> {
    override fun convertToPersistedValue(entityValue: String?, context: ConversionContext): Any? {
        if (entityValue == null) return null
        return PGobject().apply {
            setType("jsonb")
            setValue(entityValue)
        }
    }

    override fun convertToEntityValue(persistedValue: Any?, context: ConversionContext): String? = when (persistedValue) {
        is PGobject -> persistedValue.value
        is String -> persistedValue
        null -> null
        else -> persistedValue.toString()
    }
}
