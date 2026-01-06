package tech.tarakoshka.ohnoe_desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import tech.tarakoshka.ohnoedesktop.Reminder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.concurrent.timer
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    val driverFactory = DatabaseDriverFactory()
    val repository = remember { ReminderRepository(driverFactory) }

    val reminders by repository.reminders.collectAsState(initial = emptyList())

    Window(onCloseRequest = ::exitApplication, title = "Ohnoe") {
        MaterialTheme {
            Row(modifier = Modifier.padding(16.dp)) {

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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Reminder content") },
                        shape = RectangleShape,
                        minLines = 3,
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
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
                        shape = RectangleShape
                    )
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
                        shape = RectangleShape
                    )
                    Button(enabled = dateValidation.first && timeValidation.first && text.isNotBlank(), onClick = {
                        repository.addReminder(
                            text, SimpleDateFormat("ddMMyyyyhhmm").parse(date + time).toInstant().toEpochMilli()
                        )
                    }, shape = RectangleShape) {
                        Text("Add reminder")
                    }
                }

                VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reminders) { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
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
                }
            }
        }
    }
}