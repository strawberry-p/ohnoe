package tech.tarakoshka.ohnoe_desktop

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import tech.tarakoshka.ohnoedesktop.Reminder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class ReminderRepository(driverFactory: DatabaseDriverFactory) {
    private val driver = driverFactory.createDriver()
    private val database = AppDatabase(driver)
    private val queries = database.reminderQueries

    val reminders: Flow<List<Reminder>> = queries.getAllReminders().asFlow().mapToList(Dispatchers.IO)

    val fiveMin = flow {
        while (true) {
            emit(queries.getForNotif(Clock.System.now().toEpochMilliseconds(), 1000 * 60 * 5).executeAsList().also {
                it.forEach { queries.setSeenNotif(it.id) }
            })
            delay(2.seconds)
        }
    }

    val missed = flow {
        while (true) {
            emit(
                queries.getForMiss(Clock.System.now().toEpochMilliseconds()).executeAsList().also {
                    it.forEach { queries.missReminder(it.id) }
                }
            )
            delay(1.seconds)
        }
    }.flowOn(Dispatchers.IO)

    fun addReminder(text: String, messages: String, timestamp: Long) {
        queries.insertReminder(text, messages, timestamp)
    }

    fun complete(id: Long) {
        queries.completeReminder(id)
    }
}