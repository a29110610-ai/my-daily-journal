package com.example

import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    suspend fun getEntryById(id: Int): JournalEntry? {
        return journalDao.getEntryById(id)
    }

    suspend fun insertEntry(entry: JournalEntry): Long {
        return journalDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: JournalEntry) {
        journalDao.deleteEntry(entry)
    }

    suspend fun deleteEntryById(id: Int) {
        journalDao.deleteEntryById(id)
    }
}
