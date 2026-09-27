package com.example.calibretv.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsController(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    // Índice de la oración actualmente siendo leída
    private val _currentSentenceIndex = MutableStateFlow(-1)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex

    private val _currentSentenceText = MutableStateFlow("")
    val currentSentenceText: StateFlow<String> = _currentSentenceText

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var sentences = listOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Detectar idioma del sistema, con fallback a español
            val locale = if (tts.isLanguageAvailable(Locale("es", "ES")) >= TextToSpeech.LANG_AVAILABLE) {
                Locale("es", "ES")
            } else {
                Locale.getDefault()
            }
            tts.language = locale
            isReady = true
        }
    }

    /**
     * Divide el texto de la página en oraciones y las encola en el TTS.
     * Cada oración dispara una actualización del índice resaltado.
     */
    fun readPage(text: String, speedRate: Float = 1.0f) {
        if (!isReady) return
        stop()
        sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) return
        tts.setSpeechRate(speedRate)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                _currentSentenceIndex.value = idx
                _currentSentenceText.value = sentences.getOrNull(idx) ?: ""
                _isPlaying.value = true
            }
            override fun onDone(utteranceId: String) {
                val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                if (idx >= sentences.size - 1) {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = -1
                    _currentSentenceText.value = ""
                }
            }
            override fun onError(utteranceId: String) {
                _isPlaying.value = false
                _currentSentenceIndex.value = -1
                _currentSentenceText.value = ""
            }
        })

        sentences.forEachIndexed { idx, sentence ->
            val params = android.os.Bundle()
            tts.speak(sentence, TextToSpeech.QUEUE_ADD, params, "sentence_$idx")
        }
    }

    fun stop() {
        tts.stop()
        _isPlaying.value = false
        _currentSentenceIndex.value = -1
        _currentSentenceText.value = ""
    }

    fun destroy() {
        tts.stop()
        tts.shutdown()
    }
}
