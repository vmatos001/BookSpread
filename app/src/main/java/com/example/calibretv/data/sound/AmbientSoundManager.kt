package com.example.calibretv.data.sound

import android.content.Context
import android.media.MediaPlayer
import com.example.calibretv.R
import com.example.calibretv.data.model.AmbientSound

class AmbientSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: AmbientSound = AmbientSound.NONE

    private fun getResId(sound: AmbientSound): Int? = when (sound) {
        AmbientSound.NONE -> null
        AmbientSound.RAIN -> R.raw.ambient_rain
        AmbientSound.FIREPLACE -> R.raw.ambient_fireplace
        AmbientSound.OCEAN -> R.raw.ambient_ocean
        AmbientSound.CAFE -> R.raw.ambient_cafe
        AmbientSound.FOREST -> R.raw.ambient_forest
    }

    fun play(sound: AmbientSound, volume: Float = 0.4f) {
        if (sound == currentSound && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.setVolume(volume, volume)
            return
        }
        stop()
        if (sound == AmbientSound.NONE) return
        val resId = getResId(sound) ?: return
        try {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            currentSound = sound
        } catch (_: Exception) {}
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentSound = AmbientSound.NONE
    }

    fun release() = stop()
}
