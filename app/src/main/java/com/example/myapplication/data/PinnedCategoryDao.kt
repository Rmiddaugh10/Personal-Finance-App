// PinnedCategoryDao.kt
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedCategoryDao {
    @Query("SELECT * FROM pinned_categories")
    fun getPinnedCategories(): Flow<List<PinnedCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedCategory(category: PinnedCategory)

    @Query("DELETE FROM pinned_categories WHERE categoryName = :categoryName")
    suspend fun deletePinnedCategory(categoryName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_categories WHERE categoryName = :categoryName)")
    suspend fun isCategoryPinned(categoryName: String): Boolean
}