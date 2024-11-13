// WalletEntities.kt
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "wallet_balance")
data class WalletBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cashAmount: Double,
    val lastUpdated: LocalDateTime
)

@Entity(tableName = "work_shifts")
data class WorkShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val date: Long, // Store as epoch days
    val startTime: Int, // Store as seconds of day
    val endTime: Int, // Store as seconds of day
    val breakDurationMinutes: Long,
    val isWeekend: Boolean,
    val isNightShift: Boolean,
    val actualHoursWorked: Double?,
    val status: String
)
// Extension functions for conversion
fun WorkShiftEntity.toDomainModel(): WorkShift {
    return WorkShift(
        id = id,
        employeeId = employeeId,
        date = LocalDate.ofEpochDay(date),
        startTime = LocalTime.ofSecondOfDay(startTime.toLong()),
        endTime = LocalTime.ofSecondOfDay(endTime.toLong()),
        breakDuration = Duration.ofMinutes(breakDurationMinutes),
        isWeekend = isWeekend,
        isNightShift = isNightShift,
        actualHoursWorked = actualHoursWorked,
        status = status
    )
}

fun WorkShift.toEntity(): WorkShiftEntity {
    return WorkShiftEntity(
        id = id,
        employeeId = employeeId,
        date = date.toEpochDay(),
        startTime = startTime.toSecondOfDay(),
        endTime = endTime.toSecondOfDay(),
        breakDurationMinutes = breakDuration.toMinutes(),
        isWeekend = isWeekend,
        isNightShift = isNightShift,
        actualHoursWorked = actualHoursWorked,
        status = status
    )
}

@Entity(tableName = "pay_periods")
data class PayPeriodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val payDate: LocalDate,
    val isSalary: Boolean,
    val baseRate: Double?
)

@Entity(tableName = "pay_rates")
data class PayRateEntity(
    @PrimaryKey
    val employeeId: String,
    val baseRate: Double,
    val weekendRate: Double,
    val nightDifferential: Double,
    val overtimeMultiplier: Double
)

data class PayPeriodWithShifts(
    @Embedded val payPeriod: PayPeriodEntity,
    @Relation(
        parentColumn = "employeeId",
        entityColumn = "employeeId"
    )
    val shifts: List<WorkShiftEntity>
)

// WalletDao.kt
@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_balance ORDER BY lastUpdated DESC LIMIT 1")
    fun getLatestBalance(): Flow<WalletBalanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: WalletBalanceEntity)

    @Query("SELECT * FROM wallet_balance ORDER BY lastUpdated DESC")
    fun getBalanceHistory(): Flow<List<WalletBalanceEntity>>

    @Update
    suspend fun updateBalance(balance: WalletBalanceEntity)

    // Alternative method that uses Insert with REPLACE strategy
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBalanceWithReplace(balance: WalletBalanceEntity)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM work_shifts WHERE date >= :date ORDER BY date ASC")
    fun getUpcomingShifts(date: Long): Flow<List<WorkShiftEntity>>

    @Query("""
        SELECT * FROM work_shifts 
        WHERE date BETWEEN :startDate AND :endDate 
        AND employeeId = :employeeId
        ORDER BY date ASC
    """)
    fun getShiftsForPayPeriod(
        employeeId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<WorkShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: WorkShiftEntity)

    @Update
    suspend fun updateShift(shift: WorkShiftEntity)

    @Delete
    suspend fun deleteShift(shift: WorkShiftEntity)
}

@Dao
interface PayPeriodDao {
    @Query("SELECT * FROM pay_periods WHERE payDate >= :date ORDER BY payDate ASC LIMIT 5")
    fun getUpcomingPayPeriods(date: LocalDate): Flow<List<PayPeriodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayPeriod(payPeriod: PayPeriodEntity)

    @Query("SELECT * FROM pay_rates WHERE employeeId = :employeeId")
    suspend fun getPayRate(employeeId: String): PayRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePayRate(payRate: PayRateEntity)

    @Query("SELECT * FROM pay_periods WHERE employeeId = :employeeId ORDER BY startDate DESC LIMIT 1")
    suspend fun getPayPeriod(employeeId: String): PayPeriodEntity?

    @Update
    suspend fun updatePayPeriod(payPeriod: PayPeriodEntity)

    @Transaction
    suspend fun updateEmploymentType(employeeId: String, isSalary: Boolean) {
        getPayPeriod(employeeId)?.let { payPeriod ->
            updatePayPeriod(payPeriod.copy(isSalary = isSalary))
        }
    }
    @Transaction
    @Query("""
    SELECT * FROM pay_periods pp
    WHERE pp.employeeId = :employeeId 
    AND pp.payDate >= :fromDate
    ORDER BY pp.payDate ASC
""")
    fun getPayPeriodsWithShifts(
        employeeId: String,
        fromDate: LocalDate
    ): Flow<List<PayPeriodEntity>>

    @Transaction
    suspend fun updatePayPeriodCalculations(
        employeeId: String,
        paySettings: PaySettings,
        shiftDao: ShiftDao  // Pass ShiftDao as parameter
    ) {
        val currentPeriod = getPayPeriod(employeeId)
        currentPeriod?.let { period ->
            // Use shiftDao to get shifts
            val shifts = shiftDao.getShiftsForPayPeriod(
                employeeId,
                period.startDate.toEpochDay(),
                period.endDate.toEpochDay()
            ).first()

            val calculation = when (paySettings.employmentType) {
                EmploymentType.SALARY -> calculateSalaryPay(period, paySettings)
                EmploymentType.HOURLY -> calculateHourlyPay(shifts, paySettings)
            }

            updatePayPeriod(period.copy(
                baseRate = calculation.grossPay / period.getDaysInPeriod()
            ))
        }
    }

    private fun PayPeriodEntity.getDaysInPeriod(): Int =
        (endDate.toEpochDay() - startDate.toEpochDay()).toInt() + 1

    private fun calculateSalaryPay(
        period: PayPeriodEntity,
        settings: PaySettings
    ): PaycheckCalculation {
        val annualSalary = settings.payRates.basePay
        val payPerPeriod = when (settings.payRates.payFrequency) {
            PayFrequency.WEEKLY -> annualSalary / 52
            PayFrequency.BI_WEEKLY -> annualSalary / 26
            PayFrequency.SEMI_MONTHLY -> annualSalary / 24
            PayFrequency.MONTHLY -> annualSalary / 12
        }

        val deductions = calculateDeductions(payPerPeriod, settings)
        val netPay = payPerPeriod - deductions.values.sum()

        return PaycheckCalculation(
            grossPay = payPerPeriod,
            netPay = netPay,
            regularHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            nightHours = 0.0,
            deductions = deductions
        )
    }

    private fun calculateHourlyPay(
        shifts: List<WorkShiftEntity>,
        settings: PaySettings
    ): PaycheckCalculation {
        var regularHours = 0.0
        var overtimeHours = 0.0
        var weekendHours = 0.0
        var nightHours = 0.0

        shifts.forEach { shift ->
            val hours = calculateShiftHours(shift)
            when {
                shift.isWeekend -> weekendHours += hours
                shift.isNightShift -> nightHours += hours
                else -> {
                    if (regularHours + hours > 40) {
                        overtimeHours += (regularHours + hours - 40)
                        regularHours = 40.0
                    } else {
                        regularHours += hours
                    }
                }
            }
        }

        val rates = settings.payRates
        val grossPay = (regularHours * rates.basePay) +
                (overtimeHours * rates.basePay * rates.overtimeMultiplier) +
                (weekendHours * rates.weekendRate) +
                (nightHours * (rates.basePay + rates.nightDifferential))

        val deductions = calculateDeductions(grossPay, settings)
        val netPay = grossPay - deductions.values.sum()

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

    private fun calculateShiftHours(shift: WorkShiftEntity): Double {
        val startTime = LocalTime.ofSecondOfDay(shift.startTime.toLong())
        val endTime = LocalTime.ofSecondOfDay(shift.endTime.toLong())
        val duration = Duration.between(startTime, endTime)
        return (duration.toMinutes() - shift.breakDurationMinutes) / 60.0
    }

    private fun calculateDeductions(
        grossPay: Double,
        settings: PaySettings
    ): Map<String, Double> {
        val deductions = mutableMapOf<String, Double>()
        val taxSettings = settings.taxSettings

        if (taxSettings.federalWithholding) {
            deductions["Federal Tax"] = grossPay * 0.22 // Example rate
        }
        if (taxSettings.stateTaxEnabled) {
            deductions["State Tax"] = grossPay * (taxSettings.stateWithholdingPercentage / 100)
        }
        if (taxSettings.cityTaxEnabled) {
            deductions["City Tax"] = grossPay * (taxSettings.cityWithholdingPercentage / 100)
        }

        settings.deductions.forEach { deduction ->
            val amount = when (deduction.frequency) {
                DeductionFrequency.PER_PAYCHECK -> deduction.amount
                DeductionFrequency.MONTHLY -> deduction.amount / 2
                DeductionFrequency.ANNUAL -> deduction.amount / 24
            }
            deductions[deduction.name] = amount
        }

        return deductions
    }
}