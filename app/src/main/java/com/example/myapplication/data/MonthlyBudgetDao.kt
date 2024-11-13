package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// MonthlyBudgetDao.kt
@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month")
    fun getBudgetsByMonth(year: Int, month: Int): Flow<List<MonthlyBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudget(budget: MonthlyBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudgets(budgets: List<MonthlyBudget>)

    @Query("DELETE FROM monthly_budgets WHERE categoryName = :categoryName")
    suspend fun deleteBudgetsByCategory(categoryName: String)

    @Query("""
        INSERT INTO monthly_budgets (year, month, categoryName, budgetedAmount) 
        SELECT :year, :month, categoryName, budgetedAmount 
        FROM budget_category
    """)
    suspend fun generateMonthlyBudgetFromTemplate(year: Int, month: Int)

    @Transaction
    suspend fun generateYearlyBudget(year: Int) {
        for (month in 1..12) {
            generateMonthlyBudgetFromTemplate(year, month)
        }
    }

    // Get monthly budget vs actual comparison with proper handling of expense amounts
    @Query("""
    SELECT 
        0 as id,
        mb.year,
        mb.month,
        mb.categoryName,
        ABS(mb.budgetedAmount) as budgetedAmount,
        ABS(COALESCE(SUM(e.amount), 0.0)) as actualAmount,
        ABS(mb.budgetedAmount) - ABS(COALESCE(SUM(e.amount), 0.0)) as difference,
        CASE 
            WHEN ABS(mb.budgetedAmount) > 0 
            THEN (ABS(COALESCE(SUM(e.amount), 0.0)) / ABS(mb.budgetedAmount)) * 100 
            ELSE 0.0 
        END as percentageUsed
    FROM monthly_budgets mb
    LEFT JOIN expenses e ON 
        mb.year = e.year AND
        mb.month = e.month AND
        mb.categoryName = e.category
    WHERE mb.year = :year AND mb.month = :month
    GROUP BY mb.categoryName, mb.year, mb.month, mb.budgetedAmount
    """)
    fun getBudgetVsActualByMonth(year: Int, month: Int): Flow<List<BudgetComparison>>

    // Get yearly budget vs actual comparison
    @Query("""
    SELECT 
        0 as id,
        mb.year,
        -1 as month,
        mb.categoryName,
        ABS(SUM(mb.budgetedAmount)) as budgetedAmount,
        ABS(COALESCE(SUM(e.amount), 0.0)) as actualAmount,
        ABS(SUM(mb.budgetedAmount)) - ABS(COALESCE(SUM(e.amount), 0.0)) as difference,
        CASE 
            WHEN ABS(SUM(mb.budgetedAmount)) > 0 
            THEN (ABS(COALESCE(SUM(e.amount), 0.0)) / ABS(SUM(mb.budgetedAmount))) * 100 
            ELSE 0.0 
        END as percentageUsed
    FROM monthly_budgets mb
    LEFT JOIN expenses e ON 
        mb.year = e.year AND
        mb.categoryName = e.category
    WHERE mb.year = :year
    GROUP BY mb.categoryName, mb.year
    """)
    fun getBudgetVsActualByYear(year: Int): Flow<List<BudgetComparison>>

    // Add method to check if budget exists
    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM monthly_budgets 
            WHERE year = :year 
            AND month = :month 
            AND categoryName = :categoryName
        )
    """)
    suspend fun budgetExists(year: Int, month: Int, categoryName: String): Boolean

    // Add method to update existing budget
    @Query("""
        UPDATE monthly_budgets 
        SET budgetedAmount = :amount 
        WHERE year = :year 
        AND month = :month 
        AND categoryName = :categoryName
    """)
    suspend fun updateBudgetAmount(year: Int, month: Int, categoryName: String, amount: Double)

    // Add cleanup method
    @Query("DELETE FROM monthly_budgets WHERE year = :year AND month = :month")
    suspend fun clearBudgetsByMonth(year: Int, month: Int)
}