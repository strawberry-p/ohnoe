package tech.tarakoshka.ohnoe_desktop

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tech.tarakoshka.ohnoedesktop.Integration
import tech.tarakoshka.ohnoedesktop.Reminder

class IntegrationRepository(driverFactory: DatabaseDriverFactory) {
    private val driver = driverFactory.createDriver()
    private val database = AppDatabase(driver)
    private val queries = database.integrationQueries

    private val scope = CoroutineScope(Dispatchers.IO)

    val integrations: Flow<List<Integration>> = queries.getAll().asFlow().mapToList(Dispatchers.IO)

    init {
        scope.launch {
            if (queries.getAll().executeAsList().isEmpty()) {
                queries.add("Twitter")
                queries.add("Bluesky")
                queries.add("Slack")
            }
        }
    }

    fun toggle(name: String) {
        scope.launch {
            val integration = queries.get(name).asFlow().mapToOne(Dispatchers.IO).first()
            queries.toggle(if (integration.active == 0L) 1L else 0L, name)
        }
    }
}
