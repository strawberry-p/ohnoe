package tech.tarakoshka.ohnoe_desktop

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tech.tarakoshka.ohnoe_desktop.AppDatabase
import tech.tarakoshka.ohnoedesktop.Reminder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ReminderRepository(driverFactory: DatabaseDriverFactory) {
    private val driver = driverFactory.createDriver()
    private val database = AppDatabase(driver)
    private val queries = database.reminderQueries

    val reminders: Flow<List<Reminder>> = queries.getAllReminders().asFlow().mapToList(Dispatchers.IO)

    val notify = flow {
        while (true) {
            emit(getForNotification(5))
            delay(4.minutes)
        }
    }

    val missed = flow {
        while (true) {
            emit(getForNotificationSec(1))
            delay(2.seconds)
        }
    }

    fun addReminder(text: String, messages: String, timestamp: Long) {
        queries.insertReminder(text, messages, timestamp)
    }

    suspend fun getForNotification(notifyBeforeMin: Long): List<Reminder> {
        return queries.getInRange(Clock.System.now().toEpochMilliseconds(), 1000 * 60 * notifyBeforeMin).executeAsList()
    }

    suspend fun getForNotificationSec(seconds: Long): List<Reminder> {
        val lst = queries.getInRange(Clock.System.now().toEpochMilliseconds(), 1000 * seconds).executeAsList()
        lst.forEach { complete(it.id) }
        return lst
    }


    fun complete(id: Long) {
        queries.completeReminder(id)
    }

    fun miss(id: Long) {
        queries
    }
}