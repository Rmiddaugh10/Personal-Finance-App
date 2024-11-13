package com.example.myapplication.utils

import com.example.myapplication.data.PayRate
import com.example.myapplication.data.PaycheckCalculation
import com.example.myapplication.data.TaxInfo
import com.example.myapplication.data.WorkShift
import java.time.Duration

object PaycheckCalculator {
    fun calculatePaycheck(
        shifts: List<WorkShift>,
        payRate: PayRate,
        taxInfo: TaxInfo
    ): PaycheckCalculation {
        var regularHours = 0.0
        var overtimeHours = 0.0
        var weekendHours = 0.0
        var nightHours = 0.0

        // Calculate hours by type
        shifts.forEach { shift ->
            val shiftHours = calculateShiftHours(shift)

            when {
                shift.isWeekend -> weekendHours += shiftHours
                shift.isNightShift -> nightHours += shiftHours
                else -> {
                    if (regularHours + shiftHours > 40) {
                        overtimeHours += (regularHours + shiftHours - 40)
                        regularHours = 40.0
                    } else {
                        regularHours += shiftHours
                    }
                }
            }
        }

        // Calculate gross pay
        val regularPay = regularHours * payRate.baseRate
        val overtimePay = overtimeHours * payRate.baseRate * payRate.overtimeMultiplier
        val weekendPay = weekendHours * payRate.weekendRate
        val nightPay = nightHours * (payRate.baseRate + payRate.nightDifferential)

        val grossPay = regularPay + overtimePay + weekendPay + nightPay

        // Calculate deductions
        val deductions = mutableMapOf<String, Double>()

        // Standard deductions
        deductions["Federal Tax"] = grossPay * taxInfo.federalWithholding
        deductions["State Tax"] = grossPay * taxInfo.stateWithholding
        deductions["Social Security"] = grossPay * taxInfo.socialSecurity
        deductions["Medicare"] = grossPay * taxInfo.medicare

        // Additional deductions
        taxInfo.additionalDeductions.forEach { (name, rate) ->
            deductions[name] = grossPay * rate
        }

        val totalDeductions = deductions.values.sum()
        val netPay = grossPay - totalDeductions

        return PaycheckCalculation(
            grossPay = grossPay,
            netPay = netPay,
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            weekendHours = weekendHours,
            nightHours = nightHours,
            deductions = deductions
        )
    }

    private fun calculateShiftHours(shift: WorkShift): Double {
        // Get the duration between start and end times
        val shiftDuration = Duration.between(shift.startTime, shift.endTime)

        // Convert to minutes and subtract break duration
        val totalMinutes = shiftDuration.toMinutes() - shift.breakDuration.toMinutes()

        // Convert to hours
        return totalMinutes / 60.0
    }
}
