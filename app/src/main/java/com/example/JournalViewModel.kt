package com.example

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: JournalRepository
    
    // UI state flows
    val allEntries: StateFlow<List<JournalEntry>>
    val searchQuery = MutableStateFlow("")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = JournalRepository(database.journalDao())
        
        // Combine allEntries flow with search query to allow robust search filtering
        allEntries = repository.allEntries
            .combine(searchQuery) { entries, query ->
                if (query.isBlank()) {
                    entries
                } else {
                    entries.filter {
                        it.title.contains(query, ignoreCase = true) || 
                        it.content.contains(query, ignoreCase = true)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun saveEntry(
        id: Int = 0,
        title: String,
        content: String,
        alarmTime: Long? = null,
        isAlarmActive: Boolean = false
    ) {
        viewModelScope.launch {
            val entry = JournalEntry(
                id = id,
                title = title.trim(),
                content = content.trim(),
                timestamp = System.currentTimeMillis(),
                alarmTime = alarmTime,
                isAlarmActive = isAlarmActive
            )
            
            // Insert or update
            val savedId = repository.insertEntry(entry).toInt()
            
            // If alarm is set and active, schedule it
            if (alarmTime != null && isAlarmActive) {
                // Use savedId if it's a new entry (savedId > 0) or the existing id
                val actualId = if (id == 0) savedId else id
                scheduleAlarm(getApplication(), actualId, title, content, alarmTime)
            } else {
                // If alarm is disabled or removed, cancel it
                cancelAlarm(getApplication(), if (id == 0) savedId else id)
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            // Cancel alarm first
            cancelAlarm(getApplication(), entry.id)
            repository.deleteEntry(entry)
        }
    }

    fun toggleAlarm(entry: JournalEntry, active: Boolean) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(isAlarmActive = active)
            repository.insertEntry(updatedEntry)
            if (active && entry.alarmTime != null) {
                scheduleAlarm(getApplication(), entry.id, entry.title, entry.content, entry.alarmTime)
            } else {
                cancelAlarm(getApplication(), entry.id)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        entryId: Int,
        title: String,
        content: String,
        triggerTimeMs: Long
    ) {
        if (triggerTimeMs < System.currentTimeMillis()) {
            Log.w("JournalViewModel", "Cannot schedule alarm in the past.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(AlarmReceiver.EXTRA_ENTRY_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ENTRY_CONTENT, content)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                    Log.d("JournalViewModel", "Scheduled exact alarm for ID: $entryId at $triggerTimeMs")
                } else {
                    // Fallback to non-exact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                    Log.d("JournalViewModel", "Fallback: Scheduled non-exact alarm for ID: $entryId")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
                Log.d("JournalViewModel", "Scheduled exact alarm (Pre-S) for ID: $entryId")
            }
        } catch (e: SecurityException) {
            Log.e("JournalViewModel", "SecurityException scheduling exact alarm: ${e.message}")
            // Fallback to standard set
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e("JournalViewModel", "Failed completely to schedule alarm: ${ex.message}")
            }
        }
    }

    private fun cancelAlarm(context: Context, entryId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("JournalViewModel", "Cancelled alarm for ID: $entryId")
        }
    }
}
