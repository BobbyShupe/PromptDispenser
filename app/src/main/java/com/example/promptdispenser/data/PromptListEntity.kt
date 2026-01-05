package com.example.promptdispenser.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "prompt_lists")
data class PromptListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val allPrompts: List<String>,
    val usedPrompts: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()  // New: timestamp when created
)