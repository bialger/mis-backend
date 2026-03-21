package com.bialger.domain.mvc

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@Introspected
data class SpecialtyMvcForm(
    val name: String = "",
    val description: String = ""
)

@Serdeable
@Introspected
data class PatientTagTypeMvcForm(
    val code: String = "",
    val name: String = "",
    val icon: String = "",
    val description: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class MedicalServiceMvcForm(
    val name: String = "",
    val price: String = "",
    val costPrice: String = "",
    val branchId: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class ClinicalTemplateMvcForm(
    val name: String = "",
    val type: String = "",
    val specialtyId: String = "",
    val employeeId: String = "",
    val content: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class LaboratoryMvcForm(
    val name: String = "",
    val integrationType: String = "",
    val config: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class InventoryCategoryMvcForm(
    val name: String = "",
    val description: String = ""
)

@Serdeable
@Introspected
data class SalaryRecordMvcForm(
    val employeeId: String = "",
    val branchId: String = "",
    val periodStart: String = "",
    val periodEnd: String = "",
    val amount: String = "",
    val hoursWorked: String = "",
    val shiftsCount: String = ""
)

@Serdeable
@Introspected
data class IntegrationMvcForm(
    val type: String = "",
    val name: String = "",
    val config: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class SystemSettingMvcForm(
    val branchId: String = "",
    val key: String = "",
    val value: String = "",
    val description: String = ""
)

@Serdeable
@Introspected
data class OrganizationMvcForm(
    val name: String = "",
    val codeOkpo: String = "",
    val codeOkud: String = "",
    val address: String = ""
)

@Serdeable
@Introspected
data class BranchMvcForm(
    val organizationId: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class RoomMvcForm(
    val branchId: String = "",
    val name: String = "",
    val description: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class RoleMvcForm(
    val name: String = "",
    val displayName: String = "",
    val description: String = ""
)

@Serdeable
@Introspected
data class PermissionMvcForm(
    val code: String = "",
    val name: String = "",
    val description: String = ""
)

@Serdeable
@Introspected
data class EmployeeMvcForm(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val isActive: String = "",
    val specialtyIds: List<String>? = null,
    val branchIds: List<String>? = null,
    val roleId: String = ""
)

@Serdeable
@Introspected
data class LabTestMvcForm(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val laboratoryId: String = "",
    val isActive: String = ""
)

@Serdeable
@Introspected
data class InventoryItemMvcForm(
    val categoryId: String = "",
    val branchId: String = "",
    val roomId: String = "",
    val name: String = "",
    val unit: String = "",
    val quantity: String = "",
    val minQuantity: String = "",
    val costPrice: String = ""
)

@Serdeable
@Introspected
data class TimeSlotMvcForm(
    val employeeId: String = "",
    val roomId: String = "",
    val branchId: String = "",
    val slotDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val isAvailable: String = ""
)

@Serdeable
@Introspected
data class PatientMvcForm(
    val organizationId: String = "",
    val cardNumber: String = "",
    val fullName: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val phone: String = "",
    val email: String = "",
    val registrationAddress: String = "",
    val residenceAddress: String = "",
    val localityType: String = "",
    val citizenship: String = "",
    val identityDocument: String = "",
    val omsPolicy: String = "",
    val snils: String = "",
    val insuranceOrganization: String = "",
    val contactPerson: String = "",
    val guardian: String = "",
    val profession: String = "",
    val workplace: String = ""
)

@Serdeable
@Introspected
data class AppointmentMvcForm(
    val patientId: String = "",
    val employeeId: String = "",
    val timeSlotId: String = "",
    val branchId: String = "",
    val roomId: String = "",
    val status: String = "",
    val source: String = "",
    val notes: String = "",
    val createdBy: String = ""
)

@Serdeable
@Introspected
data class PaymentMvcForm(
    val appointmentId: String = "",
    val amount: String = "",
    val paymentMethod: String = "",
    val paymentStatus: String = "",
    val notes: String = "",
    val createdBy: String = ""
)
