package com.example.calibretv.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.R
import com.example.calibretv.data.BookRepository
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    repository: BookRepository,
    onNavigateNext: (isSetupCompleted: Boolean) -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        // Keep splash screen visible for branding and initialization
        delay(1400)
        val isCompleted = repository.isSetupCompleted()
        onNavigateNext(isCompleted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E2028),
                        BackgroundDark,
                        Color(0xFF090A0D)
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // App Icon with glow and rounded border
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(32.dp), spotColor = AccentGold)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, AccentGold.copy(alpha = 0.7f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "BookSpread Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "BookSpread",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The 3D Dual-Page Reader for Android TV",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // TV loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = AccentGold,
                strokeWidth = 2.5.dp
            )
        }
    }
}
