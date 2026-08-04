package app.eob.me.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognizerManager(context: Context) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun setTranscriptBase(text: String) {
        _transcript.value = text
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _errorMessage.value = "speech_unavailable"
            return
        }
        destroyRecognizerOnly()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(recognitionListener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        _listening.value = true
        _errorMessage.value = ""
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _listening.value = false
    }

    fun destroy() {
        destroyRecognizerOnly()
        _listening.value = false
    }

    private fun destroyRecognizerOnly() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _listening.value = false
        }

        override fun onError(error: Int) {
            _listening.value = false
            if (error != SpeechRecognizer.ERROR_CLIENT) {
                _errorMessage.value = "speech_error_$error"
            }
        }

        override fun onResults(results: Bundle?) {
            _listening.value = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) {
                appendRecognizedText(text)
            }
            destroyRecognizerOnly()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (partial.isNotBlank()) {
                _transcript.value = mergeWithPartial(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private var committedPrefix: String = ""

    fun beginDictationSession(existingAnswers: String) {
        committedPrefix = existingAnswers.trim()
        _transcript.value = committedPrefix
    }

    private fun appendRecognizedText(fragment: String) {
        committedPrefix = joinSegments(committedPrefix, fragment)
        _transcript.value = committedPrefix
    }

    private fun mergeWithPartial(partial: String): String {
        return if (committedPrefix.isBlank()) partial else "$committedPrefix $partial"
    }

    private fun joinSegments(left: String, right: String): String {
        if (left.isBlank()) return right.trim()
        if (right.isBlank()) return left.trim()
        return "${left.trim()} ${right.trim()}"
    }
}
