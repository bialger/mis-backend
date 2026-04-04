package com.bialger.graphql

import com.bialger.domain.patient.enums.GenderType
import com.bialger.domain.patient.enums.LocalityType
import com.bialger.graphql.model.AppointmentGql
import com.bialger.graphql.model.PatientGql
import com.bialger.graphql.model.PatientUpsertInput
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class GraphqlMutationResolver(
    private val facade: GraphqlFacadeService
) {

    val createPatient: DataFetcher<PatientGql> = DataFetcher { env ->
        facade.createPatient(readPatientInput(env, "input"))
    }

    val updatePatient: DataFetcher<PatientGql> = DataFetcher { env ->
        val id = uuidArg(env, "id")
        facade.updatePatient(id, readPatientInput(env, "input"))
    }

    val confirmAppointment: DataFetcher<AppointmentGql> = DataFetcher { env ->
        facade.confirmAppointment(uuidArg(env, "appointmentId"))
    }

    val markAppointmentArrived: DataFetcher<AppointmentGql> = DataFetcher { env ->
        facade.markAppointmentArrived(uuidArg(env, "appointmentId"))
    }

    val markAppointmentNoShow: DataFetcher<AppointmentGql> = DataFetcher { env ->
        facade.markAppointmentNoShow(uuidArg(env, "appointmentId"))
    }

    val cancelAppointment: DataFetcher<AppointmentGql> = DataFetcher { env ->
        val id = uuidArg(env, "appointmentId")
        val reason = env.getArgument<String?>("reason")
        facade.cancelAppointment(id, reason)
    }

    private fun readPatientInput(env: DataFetchingEnvironment, argName: String): PatientUpsertInput {
        @Suppress("UNCHECKED_CAST")
        val payload = env.getArgument<Map<String, Any?>>(argName)
            ?: throw IllegalArgumentException("$argName is required")

        return PatientUpsertInput(
            organizationId = requiredString(payload, "organizationId"),
            cardNumber = requiredString(payload, "cardNumber"),
            fullName = requiredString(payload, "fullName"),
            gender = payload["gender"]?.let(::toGender),
            birthDate = optionalString(payload, "birthDate"),
            phone = optionalString(payload, "phone"),
            email = optionalString(payload, "email"),
            registrationAddress = optionalString(payload, "registrationAddress"),
            residenceAddress = optionalString(payload, "residenceAddress"),
            localityType = payload["localityType"]?.let(::toLocalityType),
            citizenship = optionalString(payload, "citizenship"),
            identityDocument = optionalString(payload, "identityDocument"),
            omsPolicy = optionalString(payload, "omsPolicy"),
            snils = optionalString(payload, "snils"),
            insuranceOrganization = optionalString(payload, "insuranceOrganization"),
            contactPerson = optionalString(payload, "contactPerson"),
            guardian = optionalString(payload, "guardian"),
            profession = optionalString(payload, "profession"),
            workplace = optionalString(payload, "workplace")
        )
    }

    private fun uuidArg(env: DataFetchingEnvironment, name: String): UUID {
        val raw = env.getArgument<String?>(name) ?: throw IllegalArgumentException("$name is required")
        return runCatching { UUID.fromString(raw) }
            .getOrElse { throw IllegalArgumentException("Invalid UUID: $raw") }
    }

    private fun requiredString(payload: Map<String, Any?>, key: String): String =
        optionalString(payload, key)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$key is required")

    private fun optionalString(payload: Map<String, Any?>, key: String): String? =
        payload[key]?.toString()

    private fun toGender(raw: Any): GenderType {
        val value = raw.toString()
        return runCatching { GenderType.valueOf(value) }
            .getOrElse { throw IllegalArgumentException("Invalid gender: $value") }
    }

    private fun toLocalityType(raw: Any): LocalityType {
        val value = raw.toString()
        return runCatching { LocalityType.valueOf(value) }
            .getOrElse { throw IllegalArgumentException("Invalid localityType: $value") }
    }
}
