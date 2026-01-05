package com.example.promptdispenser.data

import kotlinx.coroutines.flow.Flow

class PromptRepository(private val dao: PromptDao) {
    val allLists: Flow<List<PromptListEntity>> = dao.getAllLists()

    suspend fun insert(list: PromptListEntity) = dao.insert(list)
    suspend fun update(list: PromptListEntity) = dao.update(list)
    suspend fun delete(list: PromptListEntity) = dao.delete(list)
}