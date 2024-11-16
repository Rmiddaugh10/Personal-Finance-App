package com.example.myapplication.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.example.myapplication.data.datastore.PreferencesKeys
import com.example.myapplication.utils.PaycheckCalculator
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs


class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val budgetComparisonDao: BudgetComparisonDao,
    private val pinnedCategoryDao: PinnedCategoryDao,
    private val paymentCalculationDao: PaymentCalculationDao,
    private val walletDao: WalletDao,
    private val shiftDao: ShiftDao,
    private val payPeriodDao: PayPeriodDao,
    private val salaryTaxSettingsDao: SalaryTaxSettingsDao,
    private val hourlyTaxSettingsDao: HourlyTaxSettingsDao,
    private val salaryDeductionsDao: SalaryDeductionsDao,
    private val hourlyDeductionsDao: HourlyDeductionsDao,
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>,
    private val scope: CoroutineScope,
) {

    private val gson = Gson()

    suspend fun savePaySettings(settings: PaySettings) {
        try {
            val settingsJson = gson.toJson(settings)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PAY_SETTINGS] = settingsJson
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error saving pay settings", e)
            throw e
        }
    }

    suspend fun getPaySettings(): PaySettings {
        return try {
            val preferences = dataStore.data.first()
            val baseSettings = preferences[PreferencesKeys.PAY_SETTINGS]?.let { json ->
                gson.fromJson(json, PaySettings::class.java)
            } ?: PaySettings.DEFAULT

            // Get tax settings and deductions from the database
            baseSettings.copy(
                salaryTaxSettings = getSalaryTaxSettings(),
                hourlyTaxSettings = getHourlyTaxSettings(),
                salaryDeductions = getSalaryDeductions().first(),
                hourlyDeductions = getHourlyDeductions().first()
            )
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error loading pay settings", e)
            PaySettings.DEFAULT
        }
    }



    private var categoryPatterns: List<CategoryPattern>? = null

    suspend fun suggestCategory(description: String): String {
        // Lazy load patterns if not already loaded
        if (categoryPatterns == null) {
            categoryPatterns = expenseDao.getCategoryPatterns()
        }

        // Try exact match first
        categoryPatterns?.find { it.description == description }?.let {
            return it.category
        }

        // Try partial matches using keywords
        val words = description.split(" ")
            .filter { it.length > 3 } // Skip small words
            .sortedByDescending { it.length } // Try longer words first

        for (word in words) {
            val pattern = "%$word%"
            expenseDao.getMostFrequentCategoryForPattern(pattern)?.let {
                return it
            }
        }

        return "Uncategorized"
    }

    suspend fun updateExpenseWithCategory(expense: ExpenseData, suggestedCategory: String) {
        val finalExpense = expense.copy(category = suggestedCategory)
        insertExpense(finalExpense)
    }


    // Flow of budget comparisons
    val budgetComparisons = budgetComparisonDao.getAllBudgetComparisons()
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    suspend fun insertCategory(category: BudgetCategory) {
        budgetCategoryDao.insertCategory(category)

        // Create monthly budgets for the current year
        val currentYear = LocalDate.now().year
        for (month in 1..12) {
            val monthlyBudget = MonthlyBudget(
                year = currentYear,
                month = month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount
            )
            monthlyBudgetDao.insertMonthlyBudget(monthlyBudget)
        }

        // Calculate initial comparisons
        for (month in 1..12) {
            budgetComparisonDao.calculateAndStoreBudgetComparisons(
                expenseDao = expenseDao,
                budgetCategoryDao = budgetCategoryDao,
                year = currentYear,
                month = month
            )
        }
    }

    // Add these methods
    fun getPinnedCategories(): Flow<List<PinnedCategory>> =
        pinnedCategoryDao.getPinnedCategories()
            .catch { e ->
                Log.e("Repository", "Error getting pinned categories", e)
                emit(emptyList())
            }

    suspend fun togglePinnedCategory(categoryName: String) {
        if (pinnedCategoryDao.isCategoryPinned(categoryName)) {
            // If category is already pinned, delete it
            pinnedCategoryDao.deletePinnedCategory(categoryName)
            Log.d("Repository", "Unpinning category: $categoryName")
        } else {
            // If category is not pinned, add it
            pinnedCategoryDao.insertPinnedCategory(PinnedCategory(categoryName))
            Log.d("Repository", "Pinning category: $categoryName")
        }
    }


    suspend fun getCategoryByName(name: String): BudgetCategory? =
        budgetCategoryDao.getCategoryByName(name)

    fun getAllCategoriesFlow(): Flow<List<BudgetCategory>> =
        budgetCategoryDao.getAllCategoriesFlow()

    suspend fun updateCategory(category: BudgetCategory) {
        // First cleanup old data
        monthlyBudgetDao.deleteBudgetsByCategory(category.categoryName)
        budgetComparisonDao.deleteBudgetByCategory(category.categoryName)

        // Update the category
        budgetCategoryDao.updateCategory(category)

        // Create new monthly budgets for the current year
        val currentYear = LocalDate.now().year
        for (month in 1..12) {
            val monthlyBudget = MonthlyBudget(
                year = currentYear,
                month = month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount
            )
            monthlyBudgetDao.insertMonthlyBudget(monthlyBudget)
        }

        // Recalculate comparisons for each month
        for (month in 1..12) {
            budgetComparisonDao.calculateAndStoreBudgetComparisons(
                expenseDao = expenseDao,
                budgetCategoryDao = budgetCategoryDao,
                year = currentYear,
                month = month
            )
        }
    }

    suspend fun deleteCategory(category: BudgetCategory) {
        budgetCategoryDao.deleteCategory(category)
        budgetComparisonDao.deleteBudgetByCategory(category.categoryName)
        monthlyBudgetDao.deleteBudgetsByCategory(category.categoryName)
    }

    // Expenses
    fun getAllSources(): Flow<List<String>> = expenseDao.getAllSources()

    fun getAllExpensesFlow(): Flow<List<ExpenseData>> =
        expenseDao.getAllExpensesFlow()

    suspend fun insertExpense(expense: ExpenseData) {
        expenseDao.insertExpense(expense)
        // Cleanup old comparisons for this month before recalculating
        budgetComparisonDao.cleanupOldComparisons(expense.year, expense.month)
        calculateBudgetComparisons(expense.year, expense.month)
    }

    suspend fun deleteExpense(expense: ExpenseData) {
        expenseDao.deleteExpense(expense.id)
        calculateBudgetComparisons(expense.year, expense.month)
    }

    suspend fun updateExpenseCategory(expenseId: Int, newCategory: String) {
        // Get the current expense
        val expense = expenseDao.getExpenseById(expenseId) ?: return

        // Get the old category
        val oldCategory = expense.category

        // Update the expense category
        expenseDao.updateCategory(expenseId, newCategory)

        // Delete existing budget comparisons for both old and new categories
        budgetComparisonDao.deleteComparisonForPeriod(
            year = expense.year,
            month = expense.month,
            categoryName = oldCategory
        )
        budgetComparisonDao.deleteComparisonForPeriod(
            year = expense.year,
            month = expense.month,
            categoryName = newCategory
        )

        // Get all expenses for the month after the update
        val monthlyExpenses = expenseDao.getExpensesByYearAndMonth(expense.year, expense.month)

        // Get all budget categories
        val categories = budgetCategoryDao.getAllCategories()

        // Recalculate budget comparisons
        categories.forEach { category ->
            val totalExpenses = monthlyExpenses
                .filter { it.category == category.categoryName }
                .sumOf { it.amount }

            val budgetComparison = BudgetComparison(
                year = expense.year,
                month = expense.month,
                categoryName = category.categoryName,
                budgetedAmount = category.budgetedAmount,
                actualAmount = totalExpenses,
                difference = category.budgetedAmount - totalExpenses,
                percentageUsed = if (category.budgetedAmount != 0.0)
                    (abs(totalExpenses) / category.budgetedAmount) * 100
                else 0.0
            )

            budgetComparisonDao.insertBudgetComparison(budgetComparison)
        }
    }


    suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
        budgetComparisonDao.deleteAllComparisons()
    }

    // Budget Comparisons
    suspend fun calculateBudgetComparisons(year: Int, month: Int) {
        budgetComparisonDao.calculateAndStoreBudgetComparisons(
            expenseDao = expenseDao,
            budgetCategoryDao = budgetCategoryDao,
            year = year,
            month = month
        )
    }

    fun getAllBudgetComparisons(
        categories: List<String>,
        years: List<Int>,
        months: List<Int>
    ): Flow<List<BudgetComparison>> {
        return budgetComparisonDao.getBudgetComparisonsByFilters(
            years = if (years.isEmpty()) null else years,
            months = if (months.isEmpty()) null else months,
            categories = if (categories.isEmpty()) null else categories
        )
    }

    suspend fun updateMonthlyBudget(year: Int, month: Int, category: String, amount: Double) {
        if (monthlyBudgetDao.budgetExists(year, month, category)) {
            monthlyBudgetDao.updateBudgetAmount(year, month, category, amount)
        } else {
            monthlyBudgetDao.insertMonthlyBudget(
                MonthlyBudget(
                    year = year,
                    month = month,
                    categoryName = category,
                    budgetedAmount = amount
                )
            )
        }
    }

    // Wallet finance stuff

    // Tax Settings Methods
    private suspend fun getSalaryTaxSettings(): TaxSettings {
        return salaryTaxSettingsDao.getTaxSettings().first()?.toDomainModel() ?: TaxSettings()
    }

    private suspend fun getHourlyTaxSettings(): TaxSettings {
        return hourlyTaxSettingsDao.getTaxSettings().first()?.toDomainModel() ?: TaxSettings()
    }

    suspend fun updateSalaryTaxSettings(settings: TaxSettings) {
        salaryTaxSettingsDao.insertTaxSettings(
            SalaryTaxSettingsEntity(
                federalWithholding = settings.federalWithholding,
                stateTaxEnabled = settings.stateTaxEnabled,
                stateWithholdingPercentage = settings.stateWithholdingPercentage,
                cityTaxEnabled = settings.cityTaxEnabled,
                cityWithholdingPercentage = settings.cityWithholdingPercentage,
                medicareTaxEnabled = settings.medicareTaxEnabled,
                socialSecurityTaxEnabled = settings.socialSecurityTaxEnabled
            )
        )
    }

    suspend fun updateHourlyTaxSettings(settings: TaxSettings) {
        hourlyTaxSettingsDao.insertTaxSettings(
            HourlyTaxSettingsEntity(
                federalWithholding = settings.federalWithholding,
                stateTaxEnabled = settings.stateTaxEnabled,
                stateWithholdingPercentage = settings.stateWithholdingPercentage,
                cityTaxEnabled = settings.cityTaxEnabled,
                cityWithholdingPercentage = settings.cityWithholdingPercentage,
                medicareTaxEnabled = settings.medicareTaxEnabled,
                socialSecurityTaxEnabled = settings.socialSecurityTaxEnabled
            )
        )
    }

    // Deductions Methods
    fun getSalaryDeductions(): Flow<List<Deduction>> =
        salaryDeductionsDao.getAllDeductions()
            .map { entities -> entities.map { it.toDomainModel() } }

    fun getHourlyDeductions(): Flow<List<Deduction>> =
        hourlyDeductionsDao.getAllDeductions()
            .map { entities -> entities.map { it.toDomainModel() } }

    suspend fun addSalaryDeduction(deduction: Deduction) {
        salaryDeductionsDao.insertDeduction(
            SalaryDeductionEntity(
                name = deduction.name,
                amount = deduction.amount,
                frequency = deduction.frequency.name,
                type = deduction.type.name,
                taxable = deduction.taxable
            )
        )
    }

    suspend fun addHourlyDeduction(deduction: Deduction) {
        hourlyDeductionsDao.insertDeduction(
            HourlyDeductionEntity(
                name = deduction.name,
                amount = deduction.amount,
                frequency = deduction.frequency.name,
                type = deduction.type.name,
                taxable = deduction.taxable
            )
        )
    }

    suspend fun removeSalaryDeduction(deduction: Deduction) {
        salaryDeductionsDao.deleteDeduction(
            SalaryDeductionEntity(
                name = deduction.name,
                amount = deduction.amount,
                frequency = deduction.frequency.name,
                type = deduction.type.name,
                taxable = deduction.taxable
            )
        )
    }

    suspend fun removeHourlyDeduction(deduction: Deduction) {
        hourlyDeductionsDao.deleteDeduction(
            HourlyDeductionEntity(
                name = deduction.name,
                amount = deduction.amount,
                frequency = deduction.frequency.name,
                type = deduction.type.name,
                taxable = deduction.taxable
            )
        )
    }




    suspend fun transactionExists(
        year: Int,
        month: Int,
        day: Int,
        amount: Double,
        description: String
    ): Boolean {
        return expenseDao.transactionExists(year, month, day, amount, description)
    }

    fun getPaymentCalculations(payPeriodId: Long): Flow<PaymentCalculationWithDeductions> {
        return paymentCalculationDao.getCalculationWithDeductions(payPeriodId)
    }

    suspend fun savePaymentCalculation(
        payPeriodId: Long,
        calculation: PaycheckCalculation
    ) {
        // Delete existing calculations for this pay period
        paymentCalculationDao.deleteCalculationsForPayPeriod(payPeriodId)

        // Create new calculation entity
        val calculationEntity = PaymentCalculationEntity(
            payPeriodId = payPeriodId,
            regularHours = calculation.regularHours,
            overtimeHours = calculation.overtimeHours,
            weekendHours = calculation.weekendHours,
            nightHours = calculation.nightHours,
            grossAmount = calculation.grossPay,
            netAmount = calculation.netPay,
            calculatedAt = LocalDateTime.now()
        )

        // Create deduction entities
        val deductionEntities = calculation.deductions.map { (name, amount) ->
            PaymentDeductionEntity(
                paymentCalculationId = 0, // This will be set by insertCalculationWithDeductions
                name = name,
                amount = amount
            )
        }

        // Save both calculation and deductions
        paymentCalculationDao.insertCalculationWithDeductions(
            calculationEntity,
            deductionEntities
        )
    }

    // Pay period related methods
    fun getUpcomingPayPeriods(): Flow<List<PayPeriod>> =
        payPeriodDao.getUpcomingPayPeriods(LocalDate.now())
            .map { entities ->
                entities.map { entity ->
                    PayPeriod(
                        startDate = entity.startDate,
                        endDate = entity.endDate,
                        payDate = entity.payDate
                    )
                }
            }


    // Helper method to get current pay period
    private suspend fun getCurrentPayPeriod(): PayPeriodEntity? {
        val today = LocalDate.now()
        return payPeriodDao.getUpcomingPayPeriods(today)
            .first()
            .firstOrNull { period ->
                today in period.startDate..period.endDate
            }
    }

    fun getShiftsForPayPeriod(
        employeeId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkShift>> {
        return shiftDao.getShiftsForPayPeriod(
            employeeId = employeeId,
            startDate = startDate.toEpochDay(),
            endDate = endDate.toEpochDay()
        ).map { shifts -> shifts.map { it.toDomainModel() } }
    }

    suspend fun insertShift(
        employeeId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        breakDurationMinutes: Long,
        isWeekend: Boolean,
        actualHoursWorked: Double?,
        status: String
    ) {
        val settings = getPaySettings()
        val calculatedIsNightShift = isNightShiftPeriod(
            startTime,
            endTime,
            settings.hourlySettings.nightShiftStart,
            settings.hourlySettings.nightShiftEnd
        )

        val shift = WorkShift(
            employeeId = employeeId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            breakDuration = Duration.ofMinutes(breakDurationMinutes),
            isWeekend = isWeekend,
            isNightShift = calculatedIsNightShift,
            actualHoursWorked = actualHoursWorked,
            status = status
        )
        shiftDao.insertShift(shift.toEntity())
    }

    suspend fun deleteShift(shift: WorkShift) {
        shiftDao.deleteShift(shift.toEntity())

        // Get current settings
        val currentSettings = getPaySettings()
        val payPeriod = getCurrentPayPeriod()

        // Update calculations
        payPeriod?.let { period ->
            payPeriodDao.updatePayPeriodCalculations(
                employeeId = "current_user",
                paySettings = currentSettings,
                shiftDao = shiftDao  // Pass the shiftDao
            )
        }
    }

    private fun isNightShiftPeriod(
        startTime: LocalTime,
        endTime: LocalTime,
        nightShiftStart: Int,
        nightShiftEnd: Int
    ): Boolean {
        val nightStart = LocalTime.of(nightShiftStart, 0)
        val nightEnd = LocalTime.of(nightShiftEnd, 0)

        return if (nightShiftStart > nightShiftEnd) {
            // Overnight shift (e.g., 18-6)
            startTime >= nightStart || endTime <= nightEnd
        } else {
            // Same day shift
            startTime >= nightStart && endTime <= nightEnd
        }
    }

    suspend fun updateWalletBalance(balance: WalletBalance) {
        walletDao.updateBalanceWithReplace(
            WalletBalanceEntity(
                cashAmount = balance.cashAmount,
                lastUpdated = balance.lastUpdated
            )
        )
    }

    // Shift Management
    fun getUpcomingShifts(): Flow<List<WorkShift>> {
        return shiftDao.getUpcomingShifts(LocalDate.now().toEpochDay())
            .map { shifts ->
                shifts.map { it.toDomainModel() }
            }
    }

    suspend fun updateWorkShift(shift: WorkShift) {
        shiftDao.updateShift(shift.toEntity())
    }

    // Paycheck Calculation
    suspend fun calculatePaychecks(): List<PaycheckCalculation> {
        val settings = getPaySettings()
        val payPeriods = payPeriodDao.getUpcomingPayPeriods(LocalDate.now()).first()

        return payPeriods.map { period ->
            val periodShifts = getShiftsForPayPeriod(
                employeeId = "current_user",
                startDate = period.startDate,
                endDate = period.endDate
            ).first()

            val calculation = PaycheckCalculator.calculatePaycheck(
                shifts = periodShifts,
                settings = settings
            )

            // Save the calculation
            savePaymentCalculation(period.id, calculation)

            calculation
        }
    }

    suspend fun getWalletBalance(): WalletBalance? {
        return try {
            walletDao.getLatestBalance().first()?.let { entity ->
                WalletBalance(
                    cashAmount = entity.cashAmount,
                    lastUpdated = entity.lastUpdated
                )
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting wallet balance", e)
            null
        }
    }

    fun generatePayPeriodDates(
        startDate: LocalDate = LocalDate.now(),
        count: Int = 3
    ): List<PayPeriod> {
        return (0 until count).map { i ->
            val periodStart = startDate.plusMonths(i.toLong()).withDayOfMonth(1)
            val periodEnd = periodStart.plusMonths(1).minusDays(1)
            val payDate = periodEnd.plusDays(5) // Pay 5 days after period end

            PayPeriod(
                startDate = periodStart,
                endDate = periodEnd,
                payDate = payDate
            )
        }
    }

}
