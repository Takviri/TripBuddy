package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TripBuddyDatabase
import com.example.data.TripRepository
import com.example.ui.TripBuddyViewModel
import com.example.ui.TripTab
import com.example.ui.components.AddLocationSheet
import com.example.ui.components.MapsSearchSheet
import com.example.ui.components.MemoryDetailSheet
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.theme.TripBuddyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TripBuddyDatabase.getInstance(applicationContext)
        val repository = TripRepository(database.tripVisitDao())

        setContent {
            TripBuddyTheme {
                val viewModel: TripBuddyViewModel = viewModel(
                    factory = TripBuddyViewModel.Factory(repository)
                )
                TripBuddyApp(viewModel = viewModel)
            }
        }
    }
}

private data class NavTabItem(
    val tab: TripTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun TripBuddyApp(viewModel: TripBuddyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navItems = remember {
        listOf(
            NavTabItem(
                tab = TripTab.BERANDA,
                labelRes = R.string.tab_home,
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_tab_beranda"
            ),
            NavTabItem(
                tab = TripTab.KALENDER,
                labelRes = R.string.tab_calendar,
                selectedIcon = Icons.Rounded.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth,
                testTag = "nav_tab_kalender"
            ),
            NavTabItem(
                tab = TripTab.KENANGAN,
                labelRes = R.string.tab_memories,
                selectedIcon = Icons.Rounded.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_tab_kenangan"
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.selectedTab == item.tab
                            val labelText = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = labelText
                                    )
                                },
                                label = { Text(labelText) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        navItems.forEach { item ->
                            val selected = uiState.selectedTab == item.tab
                            val labelText = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = labelText
                                    )
                                },
                                label = { Text(labelText) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Crossfade(
                        targetState = uiState.selectedTab,
                        label = "TripTabTransition"
                    ) { currentTab ->
                        when (currentTab) {
                            TripTab.BERANDA -> HomeScreen(
                                uiState = uiState,
                                onOpenCalendar = { viewModel.selectTab(TripTab.KALENDER) },
                                onOpenMemories = { viewModel.selectTab(TripTab.KENANGAN) },
                                onOpenAddToday = {
                                    viewModel.selectTab(TripTab.KALENDER)
                                    viewModel.openAddLocationSheet(uiState.todayEpochDay)
                                },
                                onOpenMapsForVisit = { visit -> viewModel.openMapsSearchSheet(visit) },
                                onToggleComplete = { visit ->
                                    viewModel.toggleManualCompleted(visit)
                                    showMessage("\"${visit.locationName}\" dipindahkan ke Kenangan")
                                }
                            )

                            TripTab.KALENDER -> CalendarScreen(
                                uiState = uiState,
                                onBackToHome = { viewModel.selectTab(TripTab.BERANDA) },
                                onPreviousMonth = viewModel::goToPreviousMonth,
                                onNextMonth = viewModel::goToNextMonth,
                                onJumpToToday = viewModel::jumpToToday,
                                onDateClick = viewModel::onCalendarDateClicked,
                                onAddLocationForSelectedDate = {
                                    viewModel.openAddLocationSheet(uiState.selectedDateEpochDay)
                                },
                                onOpenMapsForVisit = { visit -> viewModel.openMapsSearchSheet(visit) },
                                onToggleComplete = { visit ->
                                    viewModel.toggleManualCompleted(visit)
                                    val msg = if (!visit.isCompletedManually) {
                                        "\"${visit.locationName}\" ditandai selesai"
                                    } else {
                                        "\"${visit.locationName}\" dikembalikan ke rencana"
                                    }
                                    showMessage(msg)
                                },
                                onEditVisit = { visit -> viewModel.openEditLocationSheet(visit) },
                                onDeleteVisit = { visit ->
                                    viewModel.deleteVisit(visit)
                                    showMessage("\"${visit.locationName}\" dihapus")
                                }
                            )

                            TripTab.KENANGAN -> MemoriesScreen(
                                uiState = uiState,
                                onBackToHome = { viewModel.selectTab(TripTab.BERANDA) },
                                onSearchQueryChange = viewModel::setMemorySearchQuery,
                                onFilterChange = viewModel::setMemoryFilter,
                                onMemoryClick = viewModel::openMemoryDetailSheet,
                                onOpenMapsForVisit = { visit -> viewModel.openMapsSearchSheet(visit) }
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.activeAddEditDateEpochDay?.let { epochDay ->
        AddLocationSheet(
            epochDay = epochDay,
            editingVisit = uiState.editingVisit,
            onDismiss = viewModel::dismissAddEditSheet,
            onSave = { locationName, tripTitle, hour, minute, planNotes ->
                viewModel.saveLocationVisit(
                    locationName = locationName,
                    tripTitle = tripTitle,
                    epochDay = epochDay,
                    hour = hour,
                    minute = minute,
                    planNotes = planNotes
                )
                showMessage("Lokasi \"$locationName\" disimpan")
            }
        )
    }

    uiState.activeMapsVisit?.let { visit ->
        MapsSearchSheet(
            visit = visit,
            onDismiss = viewModel::dismissMapsSearchSheet,
            onUpdateLocationName = { updatedName ->
                viewModel.updateLocationNameFromMapsSheet(visit, updatedName)
            },
            onShowMessage = showMessage
        )
    }

    uiState.activeMemoryDetailVisit?.let { visit ->
        MemoryDetailSheet(
            visit = visit,
            todayEpochDay = uiState.todayEpochDay,
            onDismiss = viewModel::dismissMemoryDetailSheet,
            onSaveMemoryNotes = { notes ->
                viewModel.saveMemoryNotes(visit, notes)
                showMessage("Catatan kenangan disimpan")
            },
            onOpenMaps = { viewModel.openMapsSearchSheet(visit) },
            onRevertCompleted = {
                viewModel.toggleManualCompleted(visit)
                showMessage("\"${visit.locationName}\" dikembalikan ke rencana")
            },
            onDelete = {
                viewModel.deleteVisit(visit)
                showMessage("\"${visit.locationName}\" dihapus dari kenangan")
            }
        )
    }
}
