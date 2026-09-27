package com.example.calibretv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.calibretv.ui.components.UserProfilesDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.opds.OpdsFeedContent
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.DrawerItem
import com.example.calibretv.ui.components.TvSideDrawer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryGridScreen(
    repository: BookRepository,
    onBookSelected: (Book) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOpds: () -> Unit,
    onNavigateToReader: () -> Unit,
    onBack: () -> Unit
) {
    var feedContent by remember { mutableStateOf<OpdsFeedContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var showUserProfilesModal by remember { mutableStateOf(false) }

    // Modal state for Book Details
    var showDetailsModal by remember { mutableStateOf(false) }
    var detailsBook by remember { mutableStateOf<Book?>(null) }
    val modalReadFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (showDetailsModal) showDetailsModal = false
        else if (showUserProfilesModal) showUserProfilesModal = false
        else if (isDrawerOpen) isDrawerOpen = false
        else onBack()
    }

    LaunchedEffect(showDetailsModal) {
        if (showDetailsModal) {
            modalReadFocusRequester.requestFocus()
        }
    }

    val config = remember { repository.getServerConfig() }
    val authHeader = remember(config) { CoverLoader.buildBasicAuth(config.username, config.password) }

    // Load full catalog
    LaunchedEffect(activeProfile) {
        isLoading = true
        val result = repository.getFeed()
        feedContent = result
        isLoading = false
    }

    val allBooks = feedContent?.books ?: emptyList()

    // Distinct tags
    val filterTags = remember(allBooks) {
        val extracted = allBooks.flatMap { it.tags.ifEmpty { listOf(it.category) } }
            .distinct()
            .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
        listOf("Todos") + extracted
    }

    val filteredBooks = remember(allBooks, selectedCategory) {
        if (selectedCategory == "Todos" || selectedCategory.isBlank()) allBooks
        else allBooks.filter { b ->
            b.category.equals(selectedCategory, ignoreCase = true) ||
                    b.tags.any { it.equals(selectedCategory, ignoreCase = true) }
        }
    }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTime = sdf.format(Date())
    }

    val COLUMNS = 7

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isDrawerOpen = true }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = AmberWarm,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ESTANTERÍA DE LIBROS",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "• ${filteredBooks.size} libros sincronizados",
                                color = CyanElectric,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Presiona [Izquierda] en el primer libro para abrir el menú lateral",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Clock and profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = currentTime,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val profColor = try {
                        Color(android.graphics.Color.parseColor(activeProfile.avatarColorHex))
                    } catch (e: Exception) {
                        AmberWarm
                    }
                    var isProfileFocused by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .scale(if (isProfileFocused) 1.05f else 1.0f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isProfileFocused) AmberWarm else Color.White.copy(alpha = 0.12f))
                            .border(
                                width = if (isProfileFocused) 2.dp else 0.dp,
                                color = if (isProfileFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .onFocusChanged { isProfileFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    showUserProfilesModal = true
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { showUserProfilesModal = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isProfileFocused) Color(0xFF131315) else profColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isProfileFocused) AmberWarm else Color(0xFF131315),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = activeProfile.name,
                            color = if (isProfileFocused) Color(0xFF131315) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Category Filter Chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(filterTags) { index, tag ->
                        val isFirst = index == 0
                        GridCapsuleChip(
                            title = tag,
                            icon = if (tag == "Todos") Icons.Default.Folder else Icons.Default.Sell,
                            isSelected = selectedCategory == tag,
                            isFirst = isFirst,
                            onLeftAtBoundary = { isDrawerOpen = true },
                            onClick = { selectedCategory = tag }
                        )
                    }
                }
            }

            // Books Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanElectric)
                    }
                } else if (filteredBooks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No hay libros en esta categoría", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(COLUMNS),
                        contentPadding = PaddingValues(bottom = 60.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(filteredBooks) { index, book ->
                            val isLeftEdge = index % COLUMNS == 0
                            GridCoverCard(
                                book = book,
                                authHeader = authHeader,
                                isInteractive = !showDetailsModal && !isDrawerOpen,
                                isLeftEdge = isLeftEdge,
                                onLeftAtBoundary = { isDrawerOpen = true },
                                onSelected = {
                                    detailsBook = book
                                    showDetailsModal = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Side Drawer
        TvSideDrawer(
            isOpen = isDrawerOpen,
            currentSelection = DrawerItem.BIBLIOTECA,
            onClose = { isDrawerOpen = false },
            onItemSelected = { item ->
                when (item) {
                    DrawerItem.HOME -> onNavigateToHome()
                    DrawerItem.BIBLIOTECA -> { /* Already here */ }
                    DrawerItem.USUARIOS -> showUserProfilesModal = true
                    DrawerItem.LECTOR_3D -> onNavigateToReader()
                    DrawerItem.AJUSTES -> onNavigateToSettings()
                    DrawerItem.OPDS -> onNavigateToOpds()
                }
            }
        )

        // Modal de Gestión de Perfiles
        if (showUserProfilesModal) {
            UserProfilesDialog(
                repository = repository,
                activeProfile = activeProfile,
                onProfileChanged = { newProfile ->
                    activeProfile = newProfile
                },
                onDismiss = { showUserProfilesModal = false }
            )
        }

        // Book Details Dialog
        if (showDetailsModal && detailsBook != null) {
            val book = detailsBook!!
            val modalCover = rememberCoverImage(book.coverUrl, authHeader)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { showDetailsModal = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .fillMaxHeight(0.80f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceContainer)
                        .border(1.5.dp, CyanElectric.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable(enabled = false) {}
                        .padding(28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Cover on the left
                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh)
                                .border(1.dp, Color(0xFF333338), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (modalCover != null) {
                                Image(
                                    bitmap = modalCover,
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = AmberWarm,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = book.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Details & Actions on the right
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = book.title,
                                    color = TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${book.author} • ${book.category}",
                                    color = AmberWarm,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (book.tags.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        book.tags.take(4).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(SurfaceContainerHigh, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(text = "#$tag", color = CyanElectric, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Sinopsis:",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = book.summary,
                                    color = TextPrimary.copy(alpha = 0.88f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .verticalScroll(rememberScrollState())
                                )
                            }

                            var isFav by remember(book.id) { mutableStateOf(repository.isFavorite(book.id)) }

                            // Modal Buttons with trapped remote focus
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GridActionCapsule(
                                    title = "Leer en 3D",
                                    icon = Icons.Default.MenuBook,
                                    isPrimary = true,
                                    modifier = Modifier.focusRequester(modalReadFocusRequester),
                                    onClick = {
                                        showDetailsModal = false
                                        onBookSelected(book)
                                    }
                                )
                                GridActionCapsule(
                                    title = if (isFav) "★ En Favoritos" else "☆ Añadir a Favoritos",
                                    icon = Icons.Default.Star,
                                    isPrimary = isFav,
                                    onClick = {
                                        repository.toggleFavorite(book.id)
                                        isFav = repository.isFavorite(book.id)
                                    }
                                )
                                GridActionCapsule(
                                    title = "Cerrar",
                                    icon = Icons.Default.Close,
                                    isPrimary = false,
                                    onClick = { showDetailsModal = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCoverCard(
    book: Book,
    authHeader: String?,
    isInteractive: Boolean = true,
    isLeftEdge: Boolean = false,
    onLeftAtBoundary: () -> Unit,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused && isInteractive) 1.08f else 1.0f)
            .shadow(if (isFocused && isInteractive) 14.dp else 2.dp, RoundedCornerShape(8.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised)
            .border(
                width = if (isFocused && isInteractive) 2.5.dp else 1.dp,
                color = if (isFocused && isInteractive) CyanElectric else Color(0xFF242428),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged {
                if (isInteractive) {
                    isFocused = it.isFocused
                }
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isLeftEdge) {
                                onLeftAtBoundary()
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onSelected()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(enabled = isInteractive)
            .clickable(enabled = isInteractive) { onSelected() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF22222A), Color(0xFF131316))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (coverBmp != null) {
                Image(
                    bitmap = coverBmp,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = book.title,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (book.progressPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xFF09090B).copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${book.progressPercent}%",
                        color = CyanElectric,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Text(
                text = book.title,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0xFF26262A), RoundedCornerShape(1.dp))
            ) {
                if (book.progressPercent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent / 100f)
                            .height(2.5.dp)
                            .background(CyanElectric, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCapsuleChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isFirst: Boolean = false,
    onLeftAtBoundary: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isSelected -> CyanElectric.copy(alpha = 0.20f)
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> AmberWarm
                    isSelected -> CyanElectric
                    else -> Color(0xFF2E2E34)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isFirst) {
                                onLeftAtBoundary()
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isFocused -> Color(0xFF131315)
                isSelected -> CyanElectric
                else -> TextMuted
            },
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            color = when {
                isFocused -> Color(0xFF131315)
                isSelected -> CyanElectric
                else -> TextPrimary
            },
            fontSize = 11.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun GridActionCapsule(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .scale(if (isFocused) 1.06f else 1.0f)
            .shadow(if (isFocused) 10.dp else 2.dp, RoundedCornerShape(20.dp), spotColor = AmberWarm)
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isPrimary -> AmberWarm
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused || isPrimary) Color(0xFF131315) else TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = if (isFocused || isPrimary) Color(0xFF131315) else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
