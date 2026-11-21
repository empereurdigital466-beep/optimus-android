package com.optimus.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChatLine(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { OptimusScreen(onSpeak = { speak(it) }) }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
    }
    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "optimus_reply")
    }
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun OptimusScreen(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val messages = remember {
        mutableStateListOf(ChatLine("Bonjour ! Je suis OPTIMUS. Écris-moi ou appuie sur le micro pour parler.", false))
    }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }

    fun answer(raw: String) {
        val question = raw.trim()
        if (question.isEmpty()) return
        messages.add(ChatLine(question, true))
        input = ""
        val normalized = question.lowercase(Locale.FRENCH)
        val reply = when {
            normalized.contains("bonjour") || normalized.contains("salut") ->
                "Bonjour ! Que puis-je faire pour toi ?"
            normalized.contains("heure") ->
                "Il est " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) + "."
            normalized.contains("date") || normalized.contains("jour") ->
                "Nous sommes le " + SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(Date()) + "."
            normalized.contains("paramètre") || normalized.contains("réglage") ->
                "J'ouvre les paramètres du téléphone."
            normalized.contains("internet") || normalized.contains("navigateur") ->
                "J'ouvre le navigateur."
            else ->
                "J'ai bien reçu ta demande. Je fonctionne actuellement en mode de démarrage. La connexion à une véritable IA en ligne sera ajoutée ensuite."
        }
        messages.add(ChatLine(reply, false))
        onSpeak(reply)
        if (normalized.contains("paramètre") || normalized.contains("réglage")) {
            runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
        } else if (normalized.contains("internet") || normalized.contains("navigateur")) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))) }
        }
    }

    val speechPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeechRecognition(context, onResult = { spoken ->
            listening = false
            answer(spoken)
        }, onFailure = { listening = false })
        else {
            listening = false
            messages.add(ChatLine("L'autorisation du microphone est nécessaire pour utiliser la voix.", false))
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF55D6FF), secondary = Color(0xFFFFC857))) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF101D35), Color(0xFF070B13))))
                .padding(18.dp)
        ) {
            Text("OPTIMUS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("TON ASSISTANT INTELLIGENT", color = Color(0xFF55D6FF), fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(112.dp).background(Color(0xFF173B59), CircleShape), contentAlignment = Alignment.Center) {
                    Text("O", color = Color(0xFF55D6FF), fontSize = 62.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(if (listening) "Je t'écoute…" else "Prêt à t'aider", color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                        Surface(
                            color = if (message.fromUser) Color(0xFF164B69) else Color(0xFF172338),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(message.text, color = Color.White, modifier = Modifier.padding(13.dp))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Écris à Optimus…") },
                    singleLine = true
                )
                IconButton(onClick = { answer(input) }) {
                    Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = Color(0xFF55D6FF))
                }
                IconButton(onClick = {
                    listening = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startSpeechRecognition(context, onResult = { spoken ->
                            listening = false
                            answer(spoken)
                        }, onFailure = { listening = false })
                    } else speechPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "Parler", tint = Color(0xFF55D6FF))
                }
            }
            Text("Version de démarrage • Les commandes sensibles demanderont ton autorisation.", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

private fun startSpeechRecognition(
    context: android.content.Context,
    onResult: (String) -> Unit,
    onFailure: () -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onFailure()
        return
    }
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à OPTIMUS")
    }
    recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) { recognizer.destroy(); onFailure() }
        override fun onResults(results: Bundle?) {
            val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            recognizer.destroy()
            if (!spoken.isNullOrBlank()) onResult(spoken) else onFailure()
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    recognizer.startListening(intent)
}
