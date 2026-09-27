package com.example.calibretv.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.calibretv.R
import com.example.calibretv.data.model.ReadingFont

object FontProvider {
    val openDyslexicFamily = FontFamily(
        Font(R.font.open_dyslexic_regular, FontWeight.Normal)
    )

    fun getFontFamily(readingFont: ReadingFont): FontFamily = when (readingFont) {
        ReadingFont.SERIF_SYSTEM -> FontFamily.Serif
        ReadingFont.OPEN_DYSLEXIC -> openDyslexicFamily
        ReadingFont.GEORGIA_LIKE -> FontFamily.Serif  // Mejora futura con asset
        ReadingFont.MONOSPACE -> FontFamily.Monospace
    }
}
