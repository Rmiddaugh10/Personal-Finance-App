package com.example.myapplication.data

data class PaySettings(
    val employmentType: EmploymentType = EmploymentType.HOURLY,
    val payRates: PayRates = PayRates(),
    val taxSettings: TaxSettings = TaxSettings(),
    val deductions: List<Deduction> = emptyList()
) {
    companion object {
        val DEFAULT = PaySettings()
    }
}

enum class EmploymentType {
    HOURLY, SALARY
}

data class PayRates(
    val basePay: Double = 0.0,
    val payFrequency: PayFrequency = PayFrequency.BI_WEEKLY,
    val weekendRate: Double = 0.0,
    val nightDifferential: Double = 0.0,
    val overtimeMultiplier: Double = 1.5,
    val holidayRate: Double = 0.0
)

enum class PayFrequency {
    WEEKLY, BI_WEEKLY, SEMI_MONTHLY, MONTHLY
}

data class TaxSettings(
    val federalWithholding: Boolean = true,
    val stateTaxEnabled: Boolean = true,
    val stateWithholdingPercentage: Double = 0.0,
    val cityTaxEnabled: Boolean = false,
    val cityWithholdingPercentage: Double = 0.0,
    val medicareTaxEnabled: Boolean = true,
    val socialSecurityTaxEnabled: Boolean = true
)

data class Deduction(
    val name: String,
    val amount: Double,
    val frequency: DeductionFrequency,
    val type: DeductionType,
    val taxable: Boolean = false
)

enum class DeductionFrequency {
    PER_PAYCHECK, MONTHLY, ANNUAL
}

enum class DeductionType {
    RETIREMENT, INSURANCE, LEGAL, OTHER
}

data class Benefit(
    val name: String,
    val employeeContribution: Double = 0.0,
    val employerContribution: Double = 0.0,
    val pretax: Boolean = true
)
