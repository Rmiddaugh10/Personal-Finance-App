package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCategoryDao {

    @Query("SELECT * FROM budget_category ORDER BY categoryName")
    fun getAllCategoriesFlow(): Flow<List<BudgetCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: BudgetCategory)

    @Update
    suspend fun updateCategory(category: BudgetCategory)

    @Delete
    suspend fun deleteCategory(category: BudgetCategory)

    @Query("SELECT * FROM budget_category WHERE categoryName = :name")
    suspend fun getCategoryByName(name: String): BudgetCategory?

    @Query("SELECT * FROM budget_category")
    suspend fun getAllCategories(): List<BudgetCategory>

    @Query("""
        INSERT INTO budget_category (categoryName, budgetedAmount)
        SELECT DISTINCT 
            category as categoryName,
            0.0 as budgetedAmount
        FROM expenses
        WHERE category NOT IN (SELECT categoryName FROM budget_category)
    """)
    suspend fun createMissingCategories()

    @Transaction
    suspend fun ensureHistoricalBudgets(expenseDao: ExpenseDao) {
        // First, ensure we have categories for all expenses
        createMissingCategories()

        // Get all unique year/month combinations from expenses
        val periods = expenseDao.getAllExpensePeriods()

        // Create monthly budgets for all periods
        periods.forEach { period ->
            getAllCategories().forEach { category ->
                // You might want to set default budgets based on some logic
                val monthlyBudget = MonthlyBudget(
                    year = period.year,
                    month = period.month,
                    categoryName = category.categoryName,
                    budgetedAmount = category.budgetedAmount
                )
                insertOrUpdateMonthlyBudget(monthlyBudget)
            }
        }
    }

    @Query("""
        INSERT OR REPLACE INTO monthly_budgets 
        (year, month, categoryName, budgetedAmount) 
        VALUES (:year, :month, :categoryName, :budgetedAmount)
    """)
    suspend fun insertOrUpdateMonthlyBudget(
        year: Int,
        month: Int,
        categoryName: String,
        budgetedAmount: Double
    )

    @Transaction
    suspend fun insertOrUpdateMonthlyBudget(monthlyBudget: MonthlyBudget) {
        insertOrUpdateMonthlyBudget(
            year = monthlyBudget.year,
            month = monthlyBudget.month,
            categoryName = monthlyBudget.categoryName,
            budgetedAmount = monthlyBudget.budgetedAmount
        )
    }
}


