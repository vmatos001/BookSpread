package com.example.calibretv.data.sound

import android.content.Context
import android.media.SoundPool
import com.example.calibretv.R

class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .build()

    private val pageSounds = listOf(
        soundPool.load(context, R.raw.page_turn_1, 1),
        soundPool.load(context, R.raw.page_turn_2, 1),
        soundPool.load(context, R.raw.page_turn_3, 1)
    )
    private var lastSoundIdx = -1

    /** Reproduce un sonido de paso de página aleatorio (nunca el mismo dos veces seguidas) */
    fun playPageTurn(volume: Float = 0.6f) {
        var idx: Int
        do { idx = (0..2).random() } while (idx == lastSoundIdx)
        lastSoundIdx = idx
        soundPool.play(pageSounds[idx], volume, volume, 1, 0, 1.0f)
    }

    fun release() = soundPool.release()
}
