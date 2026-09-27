package com.example.calibretv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.Book
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.screens.HomeScreen
import com.example.calibretv.ui.screens.LibraryGridScreen
import com.example.calibretv.ui.screens.ReaderScreen
import com.example.calibretv.ui.screens.ServerScreen
import com.example.calibretv.ui.screens.SettingsScreen
import com.example.calibretv.ui.screens.SplashScreen
import com.example.calibretv.ui.screens.SetupWizardScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val repository = remember { BookRepository(context) }

    // Start with branded Splash screen
    val backStack = rememberNavBackStack(SplashNavKey)

    fun navigateToReaderForLastBook() {
        val lastBook = repository.getLastOpenedBook() ?: repository.getCachedBooks().firstOrNull()
        if (lastBook != null && backStack.lastOrNull() !is ReaderNavKey) {
            backStack.add(
                ReaderNavKey(
                    bookId = lastBook.id,
                    bookTitle = lastBook.title,
                    bookAuthor = lastBook.author,
                    epubUrl = lastBook.epubUrl
                )
            )
        } else if (lastBook == null && backStack.lastOrNull() !is HomeNavKey) {
            backStack.add(HomeNavKey)
        }
    }

    fun navigateToTab(tab: TvNavTab) {
        when (tab) {
            TvNavTab.HOME -> {
                if (backStack.lastOrNull() !is HomeNavKey) {
                    backStack.add(HomeNavKey)
                }
            }
            TvNavTab.BIBLIOTECA -> {
                if (backStack.lastOrNull() !is LibraryNavKey) {
                    backStack.add(LibraryNavKey())
                }
            }
            TvNavTab.LECTOR_3D -> navigateToReaderForLastBook()
            TvNavTab.AJUSTES -> {
                if (backStack.lastOrNull() !is SettingsNavKey) {
                    backStack.add(SettingsNavKey)
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SplashNavKey> {
                SplashScreen(
                    repository = repository,
                    onNavigateNext = { isSetupCompleted ->
                        if (isSetupCompleted) {
                            backStack.add(HomeNavKey)
                        } else {
                            backStack.add(SetupWizardNavKey)
                        }
                    }
                )
            }
            entry<SetupWizardNavKey> {
                SetupWizardScreen(
                    repository = repository,
                    onSetupFinished = {
                        backStack.add(HomeNavKey)
                    }
                )
            }
            entry<HomeNavKey> {
                HomeScreen(
                    repository = repository,
                    onBookSelected = { book ->
                        backStack.add(
                            ReaderNavKey(
                                bookId = book.id,
                                bookTitle = book.title,
                                bookAuthor = book.author,
                                epubUrl = book.epubUrl
                            )
                        )
                    },
                    onNavigateToLibrary = {
                        if (backStack.lastOrNull() !is LibraryNavKey) {
                            backStack.add(LibraryNavKey())
                        }
                    },
                    onNavigateToSettings = {
                        if (backStack.lastOrNull() !is SettingsNavKey) {
                            backStack.add(SettingsNavKey)
                        }
                    },
                    onNavigateToOpds = {
                        if (backStack.lastOrNull() !is ServerNavKey) {
                            backStack.add(ServerNavKey)
                        }
                    },
                    onNavigateToReader = ::navigateToReaderForLastBook
                )
            }
            entry<LibraryNavKey> {
                LibraryGridScreen(
                    repository = repository,
                    onBookSelected = { book ->
                        backStack.add(
                            ReaderNavKey(
                                bookId = book.id,
                                bookTitle = book.title,
                                bookAuthor = book.author,
                                epubUrl = book.epubUrl
                            )
                        )
                    },
                    onNavigateToHome = {
                        if (backStack.lastOrNull() !is HomeNavKey) {
                            backStack.add(HomeNavKey)
                        }
                    },
                    onNavigateToSettings = {
                        if (backStack.lastOrNull() !is SettingsNavKey) {
                            backStack.add(SettingsNavKey)
                        }
                    },
                    onNavigateToOpds = {
                        if (backStack.lastOrNull() !is ServerNavKey) {
                            backStack.add(ServerNavKey)
                        }
                    },
                    onNavigateToReader = ::navigateToReaderForLastBook,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<ServerNavKey> {
                ServerScreen(
                    repository = repository,
                    onConnected = {
                        backStack.add(HomeNavKey)
                    },
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onTabSelected = ::navigateToTab
                )
            }
            entry<SettingsNavKey> {
                SettingsScreen(
                    repository = repository,
                    onTabSelected = ::navigateToTab,
                    onOpenOpds = {
                        backStack.add(ServerNavKey)
                    },
                    onSaved = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<ReaderNavKey> { key ->
                val book = remember(key) {
                    Book(
                        id = key.bookId,
                        title = key.bookTitle,
                        author = key.bookAuthor,
                        epubUrl = key.epubUrl
                    )
                }
                ReaderScreen(
                    book = book,
                    repository = repository,
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onTabSelected = ::navigateToTab
                )
            }
        }
    )
}
