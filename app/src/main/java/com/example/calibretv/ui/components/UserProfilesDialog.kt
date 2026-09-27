package com.example.calibretv.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.UserProfile
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
fun UserProfilesDialog(
    repository: BookRepository,
    activeProfile: UserProfile,
    onProfileChanged: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var profiles by remember { mutableStateOf(repository.getProfiles()) }
    var currentActive by remember { mutableStateOf(activeProfile) }
    var showCreateField by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#FFA000") }

    val presetColors = listOf("#FFA000", "#38BDF8", "#4CAF50", "#AB47BC", "#FF5722", "#E91E63")

    val initialFocusRequester = remember { FocusRequester() }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer)
                .border(1.5.dp, CyanElectric.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarm),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF131315),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "GESTIONAR USUARIOS",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Current active badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberWarm.copy(alpha = 0.15f))
                                .border(1.dp, AmberWarm, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Activo: ", color = TextMuted, fontSize = 12.sp)
                            Text(currentActive.name, color = AmberWarm, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Cada usuario tiene su propia lista de favoritos y progreso de lectura independiente.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Middle: Profile Cards Row
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Selecciona un perfil:",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (profiles.isEmpty()) {
                        Text(
                            text = "No hay perfiles creados aún. Pulsa 'Crear Nuevo Usuario' abajo para crear uno.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(profiles) { profile ->
                                val isActive = profile.id == currentActive.id
                                val shouldFocus = if (profiles.any { it.id == currentActive.id }) isActive else profile == profiles.firstOrNull()
                                var isFocused by remember { mutableStateOf(false) }

                                val cardModifier = Modifier
                                    .width(140.dp)
                                    .scale(if (isFocused) 1.08f else 1.0f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isActive) CyanElectric.copy(alpha = 0.20f) else SurfaceRaised)
                                    .border(
                                        width = if (isFocused) 2.5.dp else if (isActive) 1.5.dp else 1.dp,
                                        color = if (isFocused) AmberWarm else if (isActive) CyanElectric else Color(0xFF2E2E36),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .then(if (shouldFocus) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            repository.saveActiveProfile(profile)
                                            currentActive = profile
                                            onProfileChanged(profile)
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        repository.saveActiveProfile(profile)
                                        currentActive = profile
                                        onProfileChanged(profile)
                                    }
                                    .padding(vertical = 16.dp, horizontal = 12.dp)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = cardModifier
                                ) {
                                val profileColor = try {
                                    Color(android.graphics.Color.parseColor(profile.avatarColorHex))
                                } catch (_: Exception) {
                                    AmberWarm
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(profileColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        color = Color(0xFF131315),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Text(
                                    text = profile.name,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                if (isActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AmberWarm)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF131315),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text("ACTIVO", color = Color(0xFF131315), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                } else {
                                    Text(
                                        text = "Cambiar",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                    // Create New User Form / Toggle
                    if (showCreateField) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh)
                                .padding(14.dp)
                        ) {
                            Text("Nombre del nuevo usuario:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var isNameFieldFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNameFieldFocused) Color(0xFF1E2A35) else SurfaceContainerHigh)
                                        .border(
                                            width = if (isNameFieldFocused) 2.dp else 1.dp,
                                            color = if (isNameFieldFocused) CyanElectric else Color(0xFF4A4A58),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .onFocusChanged { isNameFieldFocused = it.isFocused }
                                ) {
                                    OutlinedTextField(
                                        value = newUserName,
                                        onValueChange = { newUserName = it },
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
                                    )
                                }

                                // Color selector — círculos grandes con foco D-Pad visible
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    presetColors.forEach { hex ->
                                        val isColorSelected = selectedColorHex == hex
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
                                                        selectedColorHex = hex
                                                        true
                                                    } else false
                                                }
                                                .focusable()
                                                .clickable { selectedColorHex = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isColorSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                var isSaveFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSaveFocused) AmberWarm else CyanElectric)
                                        .onFocusChanged { isSaveFocused = it.isFocused }
                                        .onKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                            ) {
                                                if (newUserName.isNotBlank()) {
                                                    val created = repository.createProfile(newUserName.trim(), selectedColorHex)
                                                    profiles = repository.getProfiles()
                                                    currentActive = created
                                                    onProfileChanged(created)
                                                    showCreateField = false
                                                    newUserName = ""
                                                }
                                                true
                                            } else false
                                        }
                                        .focusable()
                                        .clickable {
                                            if (newUserName.isNotBlank()) {
                                                val created = repository.createProfile(newUserName.trim(), selectedColorHex)
                                                profiles = repository.getProfiles()
                                                currentActive = created
                                                onProfileChanged(created)
                                                showCreateField = false
                                                newUserName = ""
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text("Guardar", color = Color(0xFF131315), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isAddFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAddFocused) AmberWarm else SurfaceContainerHigh)
                            .border(1.dp, if (isAddFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                            .onFocusChanged { isAddFocused = it.isFocused }
                            .then(if (profiles.isEmpty()) Modifier.focusRequester(initialFocusRequester) else Modifier)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    showCreateField = !showCreateField
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { showCreateField = !showCreateField }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isAddFocused) Color(0xFF131315) else AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showCreateField) "Cancelar" else "Crear Nuevo Usuario",
                            color = if (isAddFocused) Color(0xFF131315) else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var isCloseFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCloseFocused) AmberWarm else SurfaceRaised)
                            .border(1.dp, if (isCloseFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    onDismiss()
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { onDismiss() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCloseFocused) Color(0xFF131315) else TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Listo",
                            color = if (isCloseFocused) Color(0xFF131315) else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
