// WalletEntities.kt
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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

@Entity(tableName = "salary_tax_settings")
data class SalaryTaxSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val federalWithholding: Boolean = false,
    val stateTaxEnabled: Boolean = false,
    val stateWithholdingPercentage: Double = 0.0,
    val cityTaxEnabled: Boolean = false,
    val cityWithholdingPercentage: Double = 0.0,
    val medicareTaxEnabled: Boolean = false,
    val socialSecurityTaxEnabled: Boolean = false
)

@Entity(tableName = "hourly_tax_settings")
data class HourlyTaxSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val federalWithholding: Boolean = false,
    val stateTaxEnabled: Boolean = false,
    val stateWithholdingPercentage: Double = 0.0,
    val cityTaxEnabled: Boolean = false,
    val cityWithholdingPercentage: Double = 0.0,
    val medicareTaxEnabled: Boolean = false,
    val socialSecurityTaxEnabled: Boolean = false
)

@Entity(tableName = "salary_deductions")
data class SalaryDeductionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val frequency: String,
    val type: String,
    val taxable: Boolean = false
)

@Entity(tableName = "hourly_deductions")
data class HourlyDeductionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val frequency: String,
    val type: String,
    val taxable: Boolean = false
)

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
    val baseRate: Double?,
    val salaryEnabled: Boolean = false,  // Add these new fields
    val hourlyEnabled: Boolean = false,
    val annualSalary: Double = 0.0
)

@Entity(tableName = "pay_rates")
data class PayRateEntity(
    @PrimaryKey
    val employeeId: String,
    val baseRate: Double,
    val weekendRate: Double,
    val nightDifferential: Double,
    val overtimeMultiplier: Double,
    val nightShiftStart: Int = 18,  // Add these new fields
    val nightShiftEnd: Int = 6
)

data class PayPeriodWithShifts(
    @Embedded val payPeriod: PayPeriodEntity,
    @Relation(
        parentColumn = "employeeId",
        entityColumn = "employeeId"
    )
    val shifts: List<WorkShiftEntity>
)

@Entity(
    tableName = "payment_calculations",
    foreignKeys = [ForeignKey(
        entity = PayPeriodEntity::class,
        parentColumns = ["id"],
        childColumns = ["payPeriodId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["payPeriodId"], name = "index_payment_calculations_payPeriodId")]
)
data class PaymentCalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payPeriodId: Long = 0,
    val regularHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val weekendHours: Double = 0.0,
    val nightHours: Double = 0.0,
    val grossAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val calculatedAt: LocalDateTime = LocalDateTime.now()
)


@Entity(
    tableName = "payment_deductions",
    foreignKeys = [ForeignKey(
        entity = PaymentCalculationEntity::class,
        parentColumns = ["id"],
        childColumns = ["paymentCalculationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["paymentCalculationId"], name = "idx_payment_deductions_calculation_id")]
)
data class PaymentDeductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentCalculationId: Long = 0,
    val name: String,
    val amount: Double
)


data class PaymentCalculationWithDeductions(
    @Embedded val calculation: PaymentCalculationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "paymentCalculationId"
    )
    val deductions: List<PaymentDeductionEntity>
)

@Dao
interface SalaryTaxSettingsDao {
    @Query("SELECT * FROM salary_tax_settings ORDER BY id DESC LIMIT 1")
    fun getTaxSettings(): Flow<SalaryTaxSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxSettings(settings: SalaryTaxSettingsEntity)
}

@Dao
interface HourlyTaxSettingsDao {
    @Query("SELECT * FROM hourly_tax_settings ORDER BY id DESC LIMIT 1")
    fun getTaxSettings(): Flow<HourlyTaxSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxSettings(settings: HourlyTaxSettingsEntity)
}

@Dao
interface SalaryDeductionsDao {
    @Query("SELECT * FROM salary_deductions")
    fun getAllDeductions(): Flow<List<SalaryDeductionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeduction(deduction: SalaryDeductionEntity)

    @Delete
    suspend fun deleteDeduction(deduction: SalaryDeductionEntity)
}

@Dao
interface HourlyDeductionsDao {
    @Query("SELECT * FROM hourly_deductions")
    fun getAllDeductions(): Flow<List<HourlyDeductionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeduction(deduction: HourlyDeductionEntity)

    @Delete
    suspend fun deleteDeduction(deduction: HourlyDeductionEntity)
}

// Extension functions for conversion between domain and entity models
fun SalaryTaxSettingsEntity.toDomainModel() = TaxSettings(
    federalWithholding = federalWithholding,
    stateTaxEnabled = stateTaxEnabled,
    stateWithholdingPercentage = stateWithholdingPercentage,
    cityTaxEnabled = cityTaxEnabled,
    cityWithholdingPercentage = cityWithholdingPercentage,
    medicareTaxEnabled = medicareTaxEnabled,
    socialSecurityTaxEnabled = socialSecurityTaxEnabled
)

fun HourlyTaxSettingsEntity.toDomainModel() = TaxSettings(
    federalWithholding = federalWithholding,
    stateTaxEnabled = stateTaxEnabled,
    stateWithholdingPercentage = stateWithholdingPercentage,
    cityTaxEnabled = cityTaxEnabled,
    cityWithholdingPercentage = cityWithholdingPercentage,
    medicareTaxEnabled = medicareTaxEnabled,
    socialSecurityTaxEnabled = socialSecurityTaxEnabled
)

fun SalaryDeductionEntity.toDomainModel() = Deduction(
    name = name,
    amount = amount,
    frequency = DeductionFrequency.valueOf(frequency),
    type = DeductionType.valueOf(type),
    taxable = taxable
)

fun HourlyDeductionEntity.toDomainModel() = Deduction(
    name = name,
    amount = amount,
    frequency = DeductionFrequency.valueOf(frequency),
    type = DeductionType.valueOf(type),
    taxable = taxable
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
interface PaymentCalculationDao {
    @Transaction
    @Query("""
        SELECT * FROM payment_calculations 
        WHERE payPeriodId = :payPeriodId
    """)
    fun getCalculationWithDeductions(payPeriodId: Long): Flow<PaymentCalculationWithDeductions>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: PaymentCalculationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeductions(deductions: List<PaymentDeductionEntity>)

    @Transaction
    suspend fun insertCalculationWithDeductions(
        calculation: PaymentCalculationEntity,
        deductions: List<PaymentDeductionEntity>
    ) {
        val calculationId = insertCalculation(calculation)
        insertDeductions(deductions.map { it.copy(paymentCalculationId = calculationId) })
    }

    @Query("DELETE FROM payment_calculations WHERE payPeriodId = :payPeriodId")
    suspend fun deleteCalculationsForPayPeriod(payPeriodId: Long)
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
        shiftDao: ShiftDao
    ) {
        val currentPeriod = getPayPeriod(employeeId)
        currentPeriod?.let { period ->
            // Use shiftDao to get shifts
            val shifts = shiftDao.getShiftsForPayPeriod(
                employeeId,
                period.startDate.toEpochDay(),
                period.endDate.toEpochDay()
            ).first()

            // Convert WorkShiftEntity to WorkShift
            val domainShifts = shifts.map { it.toDomainModel() }

            updatePayPeriod(period.copy(
                baseRate = when {
                    paySettings.salarySettings.enabled ->
                        paySettings.salarySettings.annualSalary / 52.0
                    paySettings.hourlySettings.enabled ->
                        paySettings.hourlySettings.baseRate
                    else -> 0.0
                }
            ))
        }
    }

    private fun PayPeriodEntity.getDaysInPeriod(): Int =
        (endDate.toEpochDay() - startDate.toEpochDay()).toInt() + 1
}