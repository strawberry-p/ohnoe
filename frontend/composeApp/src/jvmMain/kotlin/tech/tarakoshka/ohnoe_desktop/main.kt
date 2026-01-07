package tech.tarakoshka.ohnoe_desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.builder.notification
import io.github.kdroidfilter.knotify.builder.sendNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.tarakoshka.ohnoe_desktop.theme.AppTheme
import tech.tarakoshka.ohnoedesktop.Reminder
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Clock

sealed interface Data<out T> {
    data object Initial : Data<Nothing>
    data object Loading : Data<Nothing>
    data class Success<out T>(val data: T) : Data<T>
    data class Error(val str: String) : Data<Nothing>
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalNotificationsApi::class)
fun main() = application {
    val driverFactory = DatabaseDriverFactory()
    val repository = remember { ReminderRepository(driverFactory) }
    val integrationRepo = remember { IntegrationRepository(driverFactory) }

    val client: Client? = Client.builder().apiKey(System.getenv("GEMINI_TOKEN")).build()

    val reminders by repository.reminders.collectAsState(initial = emptyList())
    val integrations by integrationRepo.integrations.collectAsState(initial = emptyList())

    Window(onCloseRequest = ::exitApplication, title = "Ohnoe") {
        AppTheme {
            val scope = rememberCoroutineScope()
            scope.launch {
                repository.notify.collect {
                    it.forEach { r ->
                        sendNotification(
                            title = "Ohnoe! Task Approaching",
                            message = r.text,
                        )
                    }
                }
            }
            scope.launch {
                repository.missed.collect {
                    it.forEach { r ->
                        sendNotification(
                            title = "Task Failed",
                            message = r.messages.split(";").random(),
                        )
                    }
                }
            }
            var settingsOpen by remember { mutableStateOf(false) }
            Surface(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    var text by remember { mutableStateOf("") }
                    var date by remember { mutableStateOf("") }
                    val dateValidation by derivedStateOf {
                        if (date.length >= 2 && date.slice(0..1).toInt() > 31) return@derivedStateOf Pair(
                            false, "Invalid day"
                        )
                        if (date.length >= 4 && date.slice(2..3).toInt() > 12) return@derivedStateOf Pair(
                            false, "Invalid month"
                        )
                        if (date.length == 8) {
                            val parsed = SimpleDateFormat("ddMMyyyy").parse(date)
                            if (parsed == null) return@derivedStateOf Pair(false, "Invalid date")
                            val ldt = LocalDateTime.ofInstant(parsed.toInstant(), ZoneId.systemDefault()).withHour(23)
                                .withMinute(59)
                            if (ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < Clock.System.now()
                                    .toEpochMilliseconds()
                            ) return@derivedStateOf Pair(false, "Date in the past")
                        }
                        return@derivedStateOf Pair(true, "")
                    }
                    var time by remember { mutableStateOf("") }
                    val timeValidation by derivedStateOf {
                        if (time.length >= 2 && time.slice(0..1).toInt() > 24) return@derivedStateOf Pair(
                            false, "Invalid hour"
                        )
                        if (time.length == 4 && time.slice(2..3).toInt() > 59) return@derivedStateOf Pair(
                            false, "Invalid minute"
                        )
                        if (time.length == 4 && date.length == 8 && dateValidation.first) {
                            val parsed = SimpleDateFormat("ddMMyyyy").parse(date)
                            val ldt = LocalDateTime.ofInstant(parsed.toInstant(), ZoneId.systemDefault())
                            val tldt = ldt.withHour(time.slice(0..1).toInt()).withMinute(time.slice(2..3).toInt())
                            if (tldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < Clock.System.now()
                                    .toEpochMilliseconds()
                            ) {
                                return@derivedStateOf Pair(false, "Time in the past")
                            }
                        }
                        return@derivedStateOf Pair(true, "")
                    }
                    var state: Data<GenerateContentResponse> by remember { mutableStateOf(Data.Initial) }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.widthIn(max = 256.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                label = { Text("Reminder content") },
                                shape = RectangleShape,
                                minLines = 3,
                                enabled = state !is Data.Loading
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = date,
                                onValueChange = { newVal: String ->
                                    if (newVal.all(Char::isDigit) && newVal.length <= 8) {
                                        date = newVal
                                    }
                                },
                                isError = !dateValidation.first,
                                supportingText = {
                                    AnimatedVisibility(!dateValidation.first) {
                                        Text(dateValidation.second)
                                    }
                                },
                                visualTransformation = DateTransformation(),
                                placeholder = { Text("dd/mm/yyyy") },
                                label = { Text("Date") },
                                singleLine = true,
                                shape = RectangleShape,
                                enabled = state !is Data.Loading
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = time,
                                onValueChange = { newVal: String ->
                                    if (newVal.all(Char::isDigit) && newVal.length <= 4) {
                                        time = newVal
                                    }
                                },
                                isError = !timeValidation.first,
                                supportingText = {
                                    AnimatedVisibility(!timeValidation.first) {
                                        Text(timeValidation.second)
                                    }
                                },
                                visualTransformation = TimeTransformation(),
                                placeholder = { Text("hh:mm") },
                                label = { Text("Time") },
                                singleLine = true,
                                shape = RectangleShape,
                                enabled = state !is Data.Loading
                            )
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(onClick = { settingsOpen = !settingsOpen }, shape = RectangleShape) {
                                    Text("*")
                                }
                                Button(
                                    enabled = dateValidation.first && date.length == 8 && timeValidation.first && time.length == 4 && text.isNotBlank() && state !is Data.Loading,
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            state = Data.Loading
                                            val resp = client?.models?.generateContent(
                                                "gemini-2.5-flash",
                                                "Write several distinct witty and mocking sentences in first person about how you didn't manage to complete this task separated by semicolon: \"$text\". If the text provided is not a humanly possible task, answer \"INCORRECT\"",
                                                null
                                            )
                                            if (resp?.text()?.uppercase()?.contains("INCORRECT")?.equals(true)
                                                    ?: false
                                            ) {
                                                state = Data.Error("Not a real task")
                                            } else {
                                                resp?.let {
                                                    state = Data.Success(it)
                                                    it.text()?.let {
                                                        repository.addReminder(
                                                            text,
                                                            it,
                                                            SimpleDateFormat("ddMMyyyyhhmm").parse(date + time)
                                                                .toInstant().toEpochMilli()
                                                        )
                                                    }
                                                    text = ""
                                                    date = ""
                                                    time = ""
                                                } ?: {
                                                    state = Data.Error("Failed to generate response")
                                                }
                                            }
                                        }
                                    },
                                    shape = RectangleShape,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Add reminder")
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (state) {
                                    is Data.Error -> Text(
                                        (state as Data.Error).str, color = MaterialTheme.colorScheme.error
                                    )

                                    Data.Initial -> {}
                                    Data.Loading -> CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary
                                    )

                                    is Data.Success<*> -> {}
                                }
                            }
                        }
                        if (settingsOpen) {
                            item {
                                Column {
                                    integrations.forEach { i ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable(onClick = {
                                                integrationRepo.toggle(i.name)
                                            }).padding(start = 16.dp)
                                        ) {
                                            Text(i.name, modifier = Modifier.weight(1f))
                                            Checkbox(checked = i.active != 0L, onCheckedChange = {
                                                integrationRepo.toggle(i.name)
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    var doneExpanded by remember { mutableStateOf(false) }
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), onClick = { doneExpanded = !doneExpanded }) {
                                Row {
                                    Text(
                                        if (doneExpanded) "Close" else "See completed",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        if (doneExpanded) items(reminders.filter { it.completed != 0L }, key = { it.id }) { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                    .alpha(0.7f).animateItem(), horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = r.text, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(r.time), ZoneOffset.systemDefault()
                                    ).format(
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                    ), style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        items(reminders.filter { it.completed == 0L }, key = { it.id }) { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).animateItem(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = r.text, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(r.time), ZoneOffset.systemDefault()
                                    ).format(
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                    ), style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = {
                                    repository.complete(r.id)
                                }, shape = RectangleShape) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}