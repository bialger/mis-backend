package com.bialger.graphql

import graphql.schema.DataFetcher
import io.micronaut.context.BeanContext
import io.micronaut.data.repository.CrudRepository
import jakarta.inject.Singleton
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Singleton
class GraphqlCatalogResolver(
    private val beanContext: BeanContext,
    private val facade: GraphqlFacadeService
) {

    fun listFetcher(entityName: String): DataFetcher<List<Any>> = DataFetcher {
        listEntityIdMaps(entityName)
    }

    private fun listEntityIdMaps(entityName: String): List<Any> {
        when (entityName) {
            "Patient" -> return facade.listAllPatients()
            "Appointment" -> return facade.listAllAppointments()
            "Payment" -> return facade.listAllPayments()
            "Employee" -> return facade.listAllEmployees()
            "Branch" -> return facade.listAllBranches()
            "Room" -> return facade.listAllRooms()
        }
        val repository = repositoryBySimpleName["${entityName}Repository"]
            ?: throw IllegalArgumentException("Repository not found for entity: $entityName")
        @Suppress("UNCHECKED_CAST")
        val all = (repository as CrudRepository<Any, Any>).findAll()
        return all.mapNotNull { row ->
            val id = extractId(row) ?: return@mapNotNull null
            mapOf("id" to id)
        }
    }

    private fun extractId(row: Any): String? {
        @Suppress("UNCHECKED_CAST")
        val idProperty = row::class.memberProperties
            .firstOrNull { it.name == "id" } as? KProperty1<Any, *>
        return idProperty?.get(row)?.toString()
    }

    private val repositoryBySimpleName: Map<String, Any> by lazy {
        beanContext.getBeansOfType(CrudRepository::class.java)
            .associateBy { bean ->
                bean.javaClass.interfaces
                    .firstOrNull { it.simpleName.endsWith("Repository") }
                    ?.simpleName
                    ?: bean.javaClass.simpleName
            }
    }

    companion object {
        val queryToEntity: Map<String, String> = listOf(
            "Appointment",
            "AppointmentService",
            "Attachment",
            "AuditLog",
            "Branch",
            "Diagnosis",
            "Employee",
            "EmployeeBranch",
            "EmployeePermission",
            "EmployeeRole",
            "EmployeeSpecialty",
            "InsertSheet",
            "Integration",
            "InventoryAccess",
            "InventoryCategory",
            "InventoryItem",
            "InventoryOperation",
            "Laboratory",
            "LabOrder",
            "LabOrderItem",
            "LabResult",
            "LabTest",
            "MedicalRecord",
            "Notification",
            "Organization",
            "Patient",
            "PatientConsent",
            "PatientTag",
            "PatientTagType",
            "Payment",
            "Permission",
            "Post",
            "Prescription",
            "Role",
            "RolePermission",
            "Room",
            "SalaryRecord",
            "Service",
            "ServiceInventoryItem",
            "Specialty",
            "SystemSetting",
            "Template",
            "TimeSlot"
        ).associateBy({ "all${it}Entities" }, { it })
    }
}
