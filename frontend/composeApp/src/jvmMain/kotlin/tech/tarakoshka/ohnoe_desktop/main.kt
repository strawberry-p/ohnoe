package tech.tarakoshka.ohnoe_desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamPanel
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.builder.sendNotification
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.json.Json
import tech.tarakoshka.ohnoe_desktop.theme.AppTheme
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.WeakHashMap
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

const val TWITTER = "http://localhost:8080"

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

sealed interface Data<out T> {
    data object Initial : Data<Nothing>
    data object Loading : Data<Nothing>
    data class Success<out T>(val data: T) : Data<T>
    data class Error(val str: String) : Data<Nothing>
}

fun String.formatDate(): String {
    val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val tomorrow = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    return replace(today, "Today").replace(tomorrow, "Tomorrow")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalNotificationsApi::class)
fun main() = application {
    val driverFactory = DatabaseDriverFactory()
    val repository = remember { ReminderRepository(driverFactory) }
    val integrationRepo = remember { IntegrationRepository(driverFactory) }

    val client: Client? = Client.builder().apiKey(System.getenv("GEMINI_TOKEN")).build()

    val reminders by repository.reminders.collectAsState(initial = emptyList())
    val integrations by integrationRepo.integrations.collectAsState(initial = emptyList())

    var popupContent: @Composable (() -> Unit)? by remember { mutableStateOf(null) }
    var windowJob by remember { mutableStateOf<Job?>(null) }
    var confirmationFor by remember { mutableStateOf<Long?>(null) }

    Window(onCloseRequest = ::exitApplication, title = "Ohnoe") {
        AppTheme {
            confirmationFor?.let { id ->
                var loading by remember { mutableStateOf(false) }
                Dialog(onDismissRequest = { if (!loading) confirmationFor = null }) {
                    Card(shape = RectangleShape) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val webcam = remember { Webcam.getDefault() }
                            val panel = remember { WebcamPanel(webcam) }

                            DisposableEffect(Unit) {
                                panel.isFillArea = true
                                panel.start()
                                onDispose {
                                    panel.stop()
                                }
                            }

                            SwingPanel(
                                factory = { panel },
                                modifier = Modifier.fillMaxWidth().aspectRatio(1.25f)
                            )
                            var error by remember { mutableStateOf<String?>(null) }
                            val scope = rememberCoroutineScope()
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    scope.launch {
                                        loading = true
                                        error = null
                                        val stream = ByteArrayOutputStream()
                                        ImageIO.write(webcam.image, "jpg", stream)
                                        val imageBytes = stream.toByteArray()
                                        try {
                                            val response = httpClient.post("http://localhost:8080/is_done") {
                                                setBody(
                                                    MultiPartFormDataContent(
                                                        formData {
                                                            append("task", reminders.find { it.id == confirmationFor }!!.text)
                                                            append("image.jpg", imageBytes, Headers.build {
                                                                append(HttpHeaders.ContentType, "image/jpeg")
                                                                append(HttpHeaders.ContentDisposition, "filename=\"image.jpg\"")
                                                            })
                                                        }
                                                    ))
                                            }
                                            when (response.bodyAsText()) {
                                                "Task done!" -> {
                                                    repository.complete(id)
                                                    sendNotification(
                                                        title = "Ohnoe! Task completed",
                                                        message = reminders.find { it.id == confirmationFor }!!.text,
                                                    )
                                                    confirmationFor = null
                                                }
                                                else -> {
                                                    error = "Task not done"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            "Error: ${e.message}"
                                        }
                                        loading = false
                                    }
                                }, shape = RectangleShape) {
                                    Text("Confirm")
                                }
                                if (loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            val scope = rememberCoroutineScope()
            scope.launch {
                repository.fiveMin.collect {
                    it.forEach { r ->
                        sendNotification(
                            title = "Ohnoe! Task in 5min",
                            message = r.text,
                        )
                        if (windowJob == null) {
                            windowJob = launch {
                                delay(Duration.ofSeconds(5))
                                popupContent = {
                                    val timer = flow {
                                        while (true) {
                                            emit((r.time - Clock.System.now().toEpochMilliseconds()) / 1000)
                                            delay(1000L)
                                        }
                                    }
                                    val secondsLeft by timer.collectAsState(0L)
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Red).padding(32.dp)) {
                                        Text("WARNING!\nYour computer has been blocked for not completing \"${r.text}\".\nDO THE TASK NOW TO AVOID DATA LOSS.\n\nTime left:\n${max(secondsLeft, 0L)} seconds", fontSize = 48.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                windowJob = null
                            }
                        }
                    }
                }
            }
            scope.launch(Dispatchers.IO) {
                repository.missed.collect {
                    it.forEach { r ->
                        val messages = r.messages.split(";")
                        sendNotification(
                            title = "Task Failed",
                            message = messages.random(),
                        )
                        if (integrations.filter { it.active == 1L }.map { it.name }.contains("Twitter")) {
                            val resp = httpClient.post("$TWITTER/post") {
                                contentType(ContentType.Application.FormUrlEncoded)
                                setBody(
                                    FormDataContent(
                                        Parameters.build {
                                            append("text", messages.random())
                                        })
                                )
                            }
                            print(resp.bodyAsText())
                        }
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
                            Text(
                                "Ohnoe", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                            Column {
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
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(onClick = {
                                        date = SimpleDateFormat("ddMMyyyy").format(Date())
                                    }, shape = RectangleShape, enabled = state !is Data.Loading) {
                                        Text("Today")
                                    }
                                    OutlinedButton(onClick = {
                                        date = SimpleDateFormat("ddMMyyyy").format(
                                            Date.from(
                                                Date().toInstant().plus(
                                                    Duration.ofHours(24)
                                                )
                                            )
                                        )
                                    }, shape = RectangleShape, enabled = state !is Data.Loading) {
                                        Text("Tomorrow")
                                    }
                                }
                            }
                        }
                        item {
                            Column {
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
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(onClick = {
                                        time = LocalDateTime.now().plusMinutes(1)
                                            .format(DateTimeFormatter.ofPattern("HHmm"))
                                    }, shape = RectangleShape, enabled = state !is Data.Loading) {
                                        Text("+1 min")
                                    }
                                    OutlinedButton(onClick = {
                                        time = LocalDateTime.now().plusMinutes(15)
                                            .format(DateTimeFormatter.ofPattern("HHmm"))
                                    }, shape = RectangleShape, enabled = state !is Data.Loading) {
                                        Text("+15 min")
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(onClick = { settingsOpen = !settingsOpen }, shape = RectangleShape) {
                                    Text("API")
                                }
                                Button(
                                    enabled = dateValidation.first && date.length == 8 && timeValidation.first && time.length == 4 && text.isNotBlank() && state !is Data.Loading,
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            state = Data.Loading
                                            val resp = client?.models?.generateContent(
                                                "gemini-2.5-flash",
                                                "Write three completely independent derogatory, witty, and mocking sentences in first person about how you didn't manage to complete this task separated by semicolon with no extra spacing: \"$text\". If the text provided is not a humanly possible task, answer \"INCORRECT\"",
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
                        item {
                            AnimatedVisibility(settingsOpen) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    integrations.forEach { i ->
                                        val uri = LocalUriHandler.current
                                        Card(onClick =
                                            {
                                                if (i.active != 1L) {
                                                    try {
                                                        uri.openUri(
                                                            when (i.name) {
                                                                "Twitter" -> "$TWITTER/auth"
                                                                else -> "https://google.com"
                                                            }
                                                        )
                                                        integrationRepo.toggle(i.name)
                                                    } catch (_: Throwable) {
                                                    }
                                                } else integrationRepo.toggle(i.name)
                                            }, shape = RectangleShape
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    i.name,
                                                    color = if (i.active == 1L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                                )
                                                if (i.active == 1L) Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { doneExpanded = !doneExpanded },
                                shape = RectangleShape
                            ) {
                                Row {
                                    Text(
                                        if (doneExpanded) "Close" else "See completed",
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                        if (doneExpanded) items(
                            reminders.filter { it.completed == 1L || it.completed == 2L },
                            key = { it.id }) { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                    .alpha(0.7f).animateItem(), horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = r.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (r.completed == 2L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(r.time), ZoneOffset.systemDefault()
                                    ).format(
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                    ).formatDate(), style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        items(reminders.filter { it.completed == 0L || it.completed == -1L }, key = { it.id }) { r ->
                            Column(modifier = Modifier.animateItem()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = r.text,
                                        style = MaterialTheme.typography.displaySmall,
                                        modifier = Modifier.padding(end = 8.dp).weight(1f),
                                        lineHeight = 26.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            textAlign = TextAlign.End, text = LocalDateTime.ofInstant(
                                                java.time.Instant.ofEpochMilli(r.time), ZoneOffset.systemDefault()
                                            ).format(
                                                DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm")
                                            ).formatDate(), style = MaterialTheme.typography.bodyMedium
                                        )
                                        Button(
                                            onClick = {
                                                confirmationFor = r.id
                                            },
                                            shape = RectangleShape,
                                            modifier = Modifier.size(48.dp),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    popupContent?.let {
        val windowState = rememberWindowState(placement = WindowPlacement.Fullscreen)
        Window(state = windowState, onCloseRequest = { windowJob?.cancel(); windowJob = null; popupContent = null; }, title = "ATTENTION!") {
            it()
        }
    }
}