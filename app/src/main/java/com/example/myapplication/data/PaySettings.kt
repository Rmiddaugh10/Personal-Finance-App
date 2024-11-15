package com.example.myapplication.data

data class PaySettings(
    val salarySettings: SalarySettings = SalarySettings(),
    val hourlySettings: HourlySettings = HourlySettings(),
    val taxSettings: TaxSettings = TaxSettings(),
    val deductions: List<Deduction> = emptyList()
) {
    companion object {
        val DEFAULT = PaySettings()
    }
}

data class SalarySettings(
    val enabled: Boolean = false,
    val annualSalary: Double = 0.0,
    val payFrequency: PayFrequency = PayFrequency.BI_WEEKLY
)

data class HourlySettings(
    val enabled: Boolean = false,
    val baseRate: Double = 0.0,
    val weekendRate: Double = 0.0,
    val nightDifferential: Double = 0.0,
    val overtimeMultiplier: Double = 1.5,
    val holidayRate: Double = 0.0,
    val nightShiftStart: Int = 18, // 6 PM
    val nightShiftEnd: Int = 6,    // 6 AM
    val payFrequency: PayFrequency = PayFrequency.BI_WEEKLY
)

enum class EmploymentType {
    HOURLY, SALARY
}

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
