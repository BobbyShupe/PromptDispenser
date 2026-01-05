package com.example.promptdispenser.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompt_lists ORDER BY name")
    fun getAllLists(): Flow<List<PromptListEntity>>

    @Insert
    suspend fun insert(list: PromptListEntity)

    @Update
    suspend fun update(list: PromptListEntity)

    @Delete
    suspend fun delete(list: PromptListEntity)
}