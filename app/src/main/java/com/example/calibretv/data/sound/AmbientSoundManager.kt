package com.example.calibretv.data.sound

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.calibretv.R
import com.example.calibretv.data.model.AmbientSound

class AmbientSoundManager(private val context: Context) {

    private val TAG = "AmbientSoundManager"
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
        try {
            if (sound == currentSound && isPlayerActive()) {
                setVolume(volume)
                return
            }
            stop()
            if (sound == AmbientSound.NONE) return
            val resId = getResId(sound) ?: return

            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            currentSound = sound
        } catch (e: Throwable) {
            Log.e(TAG, "Error playing ambient sound $sound", e)
            mediaPlayer = null
            currentSound = AmbientSound.NONE
        }
    }

    private fun isPlayerActive(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Throwable) {
            false
        }
    }

    fun setVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (_: Throwable) {}
    }

    fun stop() {
        try {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (_: Throwable) {}
                try {
                    player.release()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        mediaPlayer = null
        currentSound = AmbientSound.NONE
    }

    fun release() = stop()
}
