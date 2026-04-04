package com.bialger.graphql

import graphql.GraphQL
import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.core.io.ResourceResolver
import jakarta.inject.Singleton

@Factory
class GraphqlFactory(
    private val resourceResolver: ResourceResolver,
    private val queryResolver: GraphqlQueryResolver,
    private val mutationResolver: GraphqlMutationResolver,
    private val fieldResolver: GraphqlFieldResolver,
    private val catalogResolver: GraphqlCatalogResolver
) {

    @Bean
    @Singleton
    fun graphQL(): GraphQL {
        val typeRegistry = TypeDefinitionRegistry()
        val schemaParser = SchemaParser()
        val schemaStream = resourceResolver.getResourceAsStream("classpath:graphql/schema.graphqls")
            .orElseThrow { IllegalStateException("GraphQL schema not found") }
        schemaStream.bufferedReader().use { reader ->
            typeRegistry.merge(schemaParser.parse(reader))
        }

        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Query") { type ->
                var wiring = type.dataFetcher("patients", queryResolver.patients)
                    .dataFetcher("patient", queryResolver.patient)
                    .dataFetcher("appointments", queryResolver.appointments)
                    .dataFetcher("appointment", queryResolver.appointment)
                GraphqlCatalogResolver.queryToEntity.forEach { (fieldName, entityName) ->
                    wiring = wiring.dataFetcher(fieldName, catalogResolver.listFetcher(entityName))
                }
                wiring
            }
            .type("Mutation") { type ->
                type.dataFetcher("createPatient", mutationResolver.createPatient)
                    .dataFetcher("updatePatient", mutationResolver.updatePatient)
                    .dataFetcher("confirmAppointment", mutationResolver.confirmAppointment)
                    .dataFetcher("markAppointmentArrived", mutationResolver.markAppointmentArrived)
                    .dataFetcher("markAppointmentNoShow", mutationResolver.markAppointmentNoShow)
                    .dataFetcher("cancelAppointment", mutationResolver.cancelAppointment)
            }
            .type("Patient") { type ->
                type.dataFetcher("appointments", fieldResolver.patientAppointments)
            }
            .type("Appointment") { type ->
                type.dataFetcher("patient", fieldResolver.appointmentPatient)
                    .dataFetcher("employee", fieldResolver.appointmentEmployee)
                    .dataFetcher("branch", fieldResolver.appointmentBranch)
                    .dataFetcher("room", fieldResolver.appointmentRoom)
                    .dataFetcher("payments", fieldResolver.appointmentPayments)
            }
            .build()

        val graphQLSchema = SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
        return GraphQL.newGraphQL(graphQLSchema)
            .instrumentation(MaxQueryComplexityInstrumentation(MAX_QUERY_COMPLEXITY))
            .build()
    }

    companion object {
        private const val MAX_QUERY_COMPLEXITY = 120
    }
}
