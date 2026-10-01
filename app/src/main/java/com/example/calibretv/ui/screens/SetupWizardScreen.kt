package com.example.calibretv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.R
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    repository: BookRepository,
    onSetupFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: OPDS Server, 2: First Profile
    val scope = rememberCoroutineScope()

    // Step 1: Server Config
    val currentConfig = remember { repository.getServerConfig() }
    var serverUrl by remember { mutableStateOf(currentConfig.serverUrl.ifBlank { "http://" }) }
    var username by remember { mutableStateOf(currentConfig.username) }
    var password by remember { mutableStateOf(currentConfig.password) }

    var isConnecting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    // Step 2: First Profile
    var profileName by remember { mutableStateOf("Mi Perfil") }
    val presetColors = listOf("#FFA000", "#38BDF8", "#4CAF50", "#AB47BC", "#FF5722", "#E91E63")
    var selectedColor by remember { mutableStateOf("#FFA000") }

    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(step) {
        delay(200)
        try {
            initialFocus.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF13151D),
                        BackgroundDark,
                        Color(0xFF090A0D)
                    )
                )
            )
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainer)
                .border(1.5.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Steps Progress
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "BookSpread",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "BIENVENIDO A BOOKSPREAD",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (step == 1) "Paso 1 de 2: Configurar Servidor OPDS" else "Paso 2 de 2: Crear tu Perfil",
                                fontSize = 13.sp,
                                color = AmberWarm,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Step badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (step >= 1) AmberWarm else Color(0xFF383842))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (step >= 2) AmberWarm else Color(0xFF383842))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF282834))
                )
            }

            // Body depending on Step
            if (step == 1) {
                // STEP 1: OPDS SERVER CONFIG
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Conecta BookSpread a tu biblioteca de Calibre o Calibre-Web para sincronizar tus libros con soporte EPUB:",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("URL del Servidor OPDS:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                placeholder = { Text("http://192.168.1.X:8083/opds", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(initialFocus),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanElectric,
                                    unfocusedBorderColor = Color(0xFF383842),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Usuario (opcional):", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        placeholder = { Text("admin", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanElectric,
                                            unfocusedBorderColor = Color(0xFF383842),
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Contraseña (opcional):", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        placeholder = { Text("••••", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanElectric,
                                            unfocusedBorderColor = Color(0xFF383842),
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Right column: Feedback status card
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerHigh)
                                .border(1.dp, Color(0xFF333340), RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("ESTADO DE LA CONEXIÓN", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                            if (isConnecting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AmberWarm, strokeWidth = 2.dp)
                                    Text("Probando y sincronizando catálogo...", fontSize = 12.sp, color = TextPrimary)
                                }
                            } else if (testResult != null) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (testSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (testSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = testResult!!,
                                        fontSize = 12.sp,
                                        color = if (testSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        lineHeight = 16.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Ingresa la dirección IP local de tu servidor Calibre-Web y pulsa 'Probar y Sincronizar'.",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // STEP 2: FIRST USER PROFILE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "BookSpread soporta perfiles de lectura familiares. Cada usuario tiene su propia lista de favoritos y páginas leídas.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar Preview
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(selectedColor))
                        } catch (_: Exception) {
                            AmberWarm
                        }
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profileName.take(1).uppercase().ifBlank { "U" },
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF131315)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Nombre de tu Perfil:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                            var isNameFieldFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isNameFieldFocused) Color(0xFF1E2A35) else SurfaceContainerHigh)
                                    .border(
                                        width = if (isNameFieldFocused) 2.5.dp else 1.dp,
                                        color = if (isNameFieldFocused) CyanElectric else Color(0xFF4A4A58),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isNameFieldFocused = it.isFocused }
                            ) {
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    placeholder = {
                                        Text(
                                            "Ej: Laura, Niños...",
                                            color = if (isNameFieldFocused) TextMuted else Color(0xFF808090),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = CyanElectric
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp)
                                        .focusRequester(initialFocus)
                                )
                            }

                            Text("Color de Avatar:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                presetColors.forEach { hex ->
                                    val isColorSelected = selectedColor == hex
                                    var isColorFocused by remember { mutableStateOf(false) }
                                    val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { AmberWarm }

                                    Box(
                                        modifier = Modifier
                                            .size(if (isColorFocused) 40.dp else 34.dp)
                                            .shadow(
                                                elevation = if (isColorFocused) 10.dp else 0.dp,
                                                shape = CircleShape,
                                                spotColor = parsedColor
                                            )
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(
                                                width = when {
                                                    isColorFocused -> 3.dp
                                                    isColorSelected -> 2.5.dp
                                                    else -> 0.dp
                                                },
                                                color = when {
                                                    isColorFocused -> Color.White
                                                    isColorSelected -> Color.White
                                                    else -> Color.Transparent
                                                },
                                                shape = CircleShape
                                            )
                                            .onFocusChanged { isColorFocused = it.isFocused }
                                            .onKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyDown &&
                                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                ) {
                                                    selectedColor = hex
                                                    true
                                                } else false
                                            }
                                            .focusable()
                                            .clickable { selectedColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isColorSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF131315),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step == 1) {
                    // Step 1 buttons
                    var isTestFocused by remember { mutableStateOf(false) }
                    var isSkipFocused by remember { mutableStateOf(false) }
                    var isNextFocused by remember { mutableStateOf(false) }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Test Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isTestFocused) AmberWarm else CyanElectric)
                                .onFocusChanged { isTestFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        scope.launch {
                                            isConnecting = true
                                            testResult = null
                                            val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                            val scanResult = repository.scanServerLibrary(cfg)
                                            isConnecting = false
                                            if (scanResult.isSuccess) {
                                                val feed = scanResult.getOrNull()!!
                                                if (feed.books.isNotEmpty()) {
                                                    testSuccess = true
                                                    testResult = "¡Conexión exitosa! Sincronizados ${feed.books.size} libros EPUB."
                                                } else {
                                                    testSuccess = false
                                                    testResult = "Se conectó al servidor pero no se encontraron libros con formato EPUB."
                                                }
                                            } else {
                                                testSuccess = false
                                                val err = scanResult.exceptionOrNull()?.message ?: "Error de conexión"
                                                testResult = "Fallo al conectar: $err"
                                            }
                                        }
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    scope.launch {
                                        isConnecting = true
                                        testResult = null
                                        val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                        val scanResult = repository.scanServerLibrary(cfg)
                                        isConnecting = false
                                        if (scanResult.isSuccess) {
                                            val feed = scanResult.getOrNull()!!
                                            if (feed.books.isNotEmpty()) {
                                                testSuccess = true
                                                testResult = "¡Conexión exitosa! Sincronizados ${feed.books.size} libros EPUB."
                                            } else {
                                                testSuccess = false
                                                testResult = "Se conectó al servidor pero no se encontraron libros con formato EPUB."
                                            }
                                        } else {
                                            testSuccess = false
                                            val err = scanResult.exceptionOrNull()?.message ?: "Error de conexión"
                                            testResult = "Fallo al conectar: $err"
                                        }
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Probar y Sincronizar Catálogo",
                                color = Color(0xFF131315),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Skip / Configure Later
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSkipFocused) AmberWarm else SurfaceRaised)
                                .border(1.dp, if (isSkipFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                                .onFocusChanged { isSkipFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        step = 2
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable { step = 2 }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Configurar Más Tarde",
                                color = if (isSkipFocused) Color(0xFF131315) else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Next Step Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNextFocused) AmberWarm else SurfaceContainerHigh)
                            .border(1.dp, if (isNextFocused) Color.White else CyanElectric, RoundedCornerShape(12.dp))
                            .onFocusChanged { isNextFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                    repository.saveServerConfig(cfg)
                                    step = 2
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable {
                                val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                repository.saveServerConfig(cfg)
                                step = 2
                            }
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Siguiente >",
                            color = if (isNextFocused) Color(0xFF131315) else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Step 2 buttons
                    var isBackFocused by remember { mutableStateOf(false) }
                    var isFinishFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isBackFocused) AmberWarm else SurfaceRaised)
                            .border(1.dp, if (isBackFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    step = 1
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { step = 1 }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "< Atrás",
                            color = if (isBackFocused) Color(0xFF131315) else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFinishFocused) CyanElectric else AmberWarm)
                            .onFocusChanged { isFinishFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    repository.createProfile(profileName.trim().ifBlank { "Mi Perfil" }, selectedColor)
                                    repository.setSetupCompleted(true)
                                    onSetupFinished()
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable {
                                repository.createProfile(profileName.trim().ifBlank { "Mi Perfil" }, selectedColor)
                                repository.setSetupCompleted(true)
                                onSetupFinished()
                            }
                            .padding(horizontal = 26.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Comenzar a Disfrutar BookSpread",
                            color = Color(0xFF131315),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
