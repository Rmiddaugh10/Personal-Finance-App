package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ExpenseRepository(
            expenseDao = database.expenseDao(),
            budgetCategoryDao = database.budgetCategoryDao(),
            monthlyBudgetDao = database.monthlyBudgetDao(),
            budgetComparisonDao = database.budgetComparisonDao(),
            pinnedCategoryDao = database.pinnedCategoryDao(),
            walletDao = database.walletDao(),
            shiftDao = database.shiftDao(),
            payPeriodDao = database.payPeriodDao(),
            paymentCalculationDao = database.paymentCalculationDao(),
            salaryTaxSettingsDao = database.salaryTaxSettingsDao(),
            hourlyTaxSettingsDao = database.hourlyTaxSettingsDao(),
            salaryDeductionsDao = database.salaryDeductionsDao(),
            hourlyDeductionsDao = database.hourlyDeductionsDao(),
            dataStore = dataStore,
            scope = applicationScope
        )
    }
}

