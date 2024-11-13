package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.converter.DateConverter


class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }
    }

    @TypeConverter
    fun toIntList(list: List<Int>?): String? {
        return list?.joinToString(",")
    }
}

@Database(
    entities = [
        ExpenseData::class,
        BudgetCategory::class,
        MonthlyBudget::class,
        BudgetComparison::class,
        PinnedCategory::class,
        WalletBalanceEntity::class,
        WorkShiftEntity::class,
        PayPeriodEntity::class,
        PayRateEntity::class,

    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(DateConverter::class, Converters::class, WalletTypeConverters::class)

abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun budgetComparisonDao(): BudgetComparisonDao
    abstract fun pinnedCategoryDao(): PinnedCategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun shiftDao(): ShiftDao
    abstract fun payPeriodDao(): PayPeriodDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Keep existing migration
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Keep existing migration
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary table with correct schema
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `source` TEXT
                    )
                """
                )

                // Copy existing data
                database.execSQL(
                    """
                    INSERT INTO expenses_temp (
                        id, amount, category, description, 
                        year, month, day, source
                    )
                    SELECT 
                        id, amount, category, description,
                        year, month, day, 
                        CASE 
                            WHEN source IS NULL THEN NULL 
                            ELSE source 
                        END as source
                    FROM expenses
                """
                )

                // Drop old table
                database.execSQL("DROP TABLE expenses")

                // Rename temp table to final
                database.execSQL("ALTER TABLE expenses_temp RENAME TO expenses")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary tables with correct schema
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        day INTEGER NOT NULL,
                        source TEXT
                    )
                """
                )

                // Copy data
                database.execSQL(
                    """
                    INSERT INTO expenses_temp 
                    SELECT id, amount, category, description, year, month, day, source 
                    FROM expenses
                """
                )

                // Drop old table and rename new one
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_temp RENAME TO expenses")

                // Recalculate budget comparisons
                database.execSQL(
                    """
                    UPDATE budget_comparison 
                    SET actualAmount = ABS(actualAmount),
                        budgetedAmount = ABS(budgetedAmount),
                        difference = budgetedAmount - actualAmount,
                        percentageUsed = CASE 
                            WHEN budgetedAmount != 0 
                            THEN (ABS(actualAmount) / ABS(budgetedAmount)) * 100
                            ELSE CASE 
                                WHEN actualAmount != 0 THEN 100
                                ELSE 0
                            END
                        END
                    WHERE 1=1
                """
                )
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the pinned_categories table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pinned_categories` (
                        `categoryName` TEXT NOT NULL PRIMARY KEY
                    )
                """.trimIndent()
                )
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create wallet balance table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `wallet_balance` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                cashAmount REAL NOT NULL,
                lastUpdated TEXT NOT NULL
            )
        """.trimIndent())

                // Create work shifts table with correct column types
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `work_shifts` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId TEXT NOT NULL,
                date INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                breakDurationMinutes INTEGER NOT NULL,
                isWeekend INTEGER NOT NULL,
                isNightShift INTEGER NOT NULL,
                actualHoursWorked REAL,
                status TEXT NOT NULL
            )
        """.trimIndent())

                // Create pay periods table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `pay_periods` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                endDate INTEGER NOT NULL,
                payDate INTEGER NOT NULL,
                isSalary INTEGER NOT NULL,
                baseRate REAL
            )
        """.trimIndent())

                // Create pay rates table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `pay_rates` (
                employeeId TEXT PRIMARY KEY NOT NULL,
                baseRate REAL NOT NULL,
                weekendRate REAL NOT NULL,
                nightDifferential REAL NOT NULL,
                overtimeMultiplier REAL NOT NULL
            )
        """.trimIndent())
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create payment calculations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS payment_calculations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        payPeriodId INTEGER NOT NULL,
                        regularHours REAL NOT NULL DEFAULT 0.0,
                        overtimeHours REAL NOT NULL DEFAULT 0.0,
                        weekendHours REAL NOT NULL DEFAULT 0.0,
                        nightHours REAL NOT NULL DEFAULT 0.0,
                        grossAmount REAL NOT NULL,
                        netAmount REAL NOT NULL,
                        calculatedAt TEXT NOT NULL,
                        FOREIGN KEY(payPeriodId) REFERENCES pay_periods(id) 
                        ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create payment deductions table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS payment_deductions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        paymentCalculationId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        FOREIGN KEY(paymentCalculationId) 
                        REFERENCES payment_calculations(id) 
                        ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices for better query performance
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_payment_calculations_pay_period_id ON payment_calculations(payPeriodId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_payment_deductions_calculation_id ON payment_deductions(paymentCalculationId)"
                )
            }
        }
    }
}
