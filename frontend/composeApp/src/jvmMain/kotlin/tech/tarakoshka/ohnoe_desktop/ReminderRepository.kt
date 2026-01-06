package tech.tarakoshka.ohnoe_desktop

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import tech.tarakoshka.ohnoe_desktop.AppDatabase
import tech.tarakoshka.ohnoedesktop.Reminder

class ReminderRepository(driverFactory: DatabaseDriverFactory) {
    private val driver = driverFactory.createDriver()
    private val database = AppDatabase(driver)
    private val queries = database.reminderQueries

    val reminders: Flow<List<Reminder>> = queries.getAllReminders()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun addReminder(text: String, timestamp: Long) {
        queries.insertReminder(text, timestamp)
    }

    fun complete(id: Long) {
        queries.completeReminder(id)
    }
}