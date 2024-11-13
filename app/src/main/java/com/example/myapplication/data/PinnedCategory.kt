// PinnedCategory.kt
package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_categories")
data class PinnedCategory(
    @PrimaryKey val categoryName: String
)

