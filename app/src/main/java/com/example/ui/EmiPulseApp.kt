package com.example.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ThemeMode
import com.example.domain.CurrencyOption
import com.example.ui.components.CurrencyFormatter
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.EligibilityScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.EmiPulseTheme
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle
import kotlinx.coroutines.launch

enum class EmiTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CALCULATOR(
        labelRes = R.string.nav_calculator,
        selectedIcon = Icons.Filled.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_calculator"
    ),
    SCHEDULE(
        labelRes = R.string.nav_schedule,
        selectedIcon = Icons.Filled.TableChart,
        unselectedIcon = Icons.Outlined.TableChart,
        testTag = "nav_schedule"
    ),
    COMPARE(
        labelRes = R.string.nav_compare,
        selectedIcon = Icons.AutoMirrored.Filled.CompareArrows,
        unselectedIcon = Icons.AutoMirrored.Outlined.CompareArrows,
        testTag = "nav_compare"
    ),
    ELIGIBILITY(
        labelRes = R.string.nav_eligibility,
        selectedIcon = Icons.Filled.VerifiedUser,
        unselectedIcon = Icons.Outlined.VerifiedUser,
        testTag = "nav_eligibility"
    ),
    HISTORY(
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiPulseApp(viewModel: EmiViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSessions by viewModel.savedSessions.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(EmiTab.CALCULATOR) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveLabelDraft by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val isDarkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val activity = context as? ComponentActivity
    DisposableEffect(isDarkTheme, activity) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            },
            navigationBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            }
        )
        onDispose {}
    }

    if (currentTab != EmiTab.CALCULATOR) {
        BackHandler {
            currentTab = EmiTab.CALCULATOR
        }
    }

    EmiPulseTheme(darkTheme = isDarkTheme) {
        val emiColors = LocalEmiColors.current

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.tagline),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                        actions = {
                            Surface(
                                onClick = { showCurrencyDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, emiColors.cardBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .padding(end = 4.dp)
                                    .testTag("currency_selector_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = uiState.currency.symbol,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = uiState.currency.code,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            val themeContentDesc = stringResource(
                                if (isDarkTheme) R.string.cd_switch_to_light else R.string.cd_switch_to_dark
                            )
                            val themeStateText = stringResource(
                                if (isDarkTheme) R.string.theme_option_dark else R.string.theme_option_light
                            )
                            Surface(
                                onClick = { viewModel.toggleLightDark(isDarkTheme) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, emiColors.cardBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .padding(end = 6.dp)
                                    .semantics {
                                        role = Role.Switch
                                        toggleableState = ToggleableState(isDarkTheme)
                                        stateDescription = themeStateText
                                    }
                                    .testTag("theme_toggle_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Crossfade(
                                        targetState = isDarkTheme,
                                        label = "themeIconCrossfade"
                                    ) { dark ->
                                        Icon(
                                            imageVector = if (dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                            contentDescription = themeContentDesc,
                                            tint = if (dark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = stringResource(
                                            if (isDarkTheme) R.string.theme_mode_dark else R.string.theme_mode_light
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = emiColors.cardBorder.copy(alpha = 0.65f))
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        Column {
                            HorizontalDivider(color = emiColors.cardBorder.copy(alpha = 0.65f))
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                EmiTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            TabIconWithOptionalBadge(
                                                tab = tab,
                                                selected = selected,
                                                savedCount = savedSessions.size
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            EmiTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        TabIconWithOptionalBadge(
                                            tab = tab,
                                            selected = selected,
                                            savedCount = savedSessions.size
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Crossfade(targetState = currentTab, label = "tabCrossfade") { tab ->
                            when (tab) {
                                EmiTab.CALCULATOR -> CalculatorScreen(
                                    uiState = uiState,
                                    onSelectPreset = viewModel::selectPreset,
                                    onPrincipalTextChange = viewModel::updatePrincipalText,
                                    onPrincipalSliderChange = viewModel::updatePrincipalFromSlider,
                                    onNudgePrincipal = viewModel::nudgePrincipal,
                                    onToggleIncludeInterest = viewModel::toggleIncludeInterest,
                                    onRateTextChange = viewModel::updateRateText,
                                    onRateSliderChange = viewModel::updateRateFromSlider,
                                    onNudgeRate = viewModel::nudgeRate,
                                    onTenureTextChange = viewModel::updateTenureText,
                                    onTenureSliderChange = viewModel::updateTenureFromSlider,
                                    onSwitchTenureUnit = viewModel::switchTenureUnit,
                                    onSelectPrepaymentFreq = viewModel::selectPrepaymentFrequency,
                                    onExtraPaymentTextChange = viewModel::updateExtraPaymentText,
                                    onSaveSessionClick = {
                                        saveLabelDraft = viewModel.defaultSessionLabel()
                                        showSaveDialog = true
                                    }
                                )
                                EmiTab.SCHEDULE -> ScheduleScreen(
                                    uiState = uiState,
                                    onGranularityChange = viewModel::setScheduleGranularity,
                                    onShowMessage = { msg ->
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                )
                                EmiTab.COMPARE -> CompareScreen(
                                    uiState = uiState,
                                    onCopyCurrentToSlot = viewModel::copyCurrentToCompareSlot,
                                    onSwapLoans = viewModel::swapCompareLoans,
                                    onToggleCompareInterest = viewModel::toggleCompareInterest,
                                    onUpdateCompareField = viewModel::updateCompareField,
                                    onShowMessage = { msg ->
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                )
                                EmiTab.ELIGIBILITY -> EligibilityScreen(
                                    uiState = uiState,
                                    onIncomeTextChange = viewModel::updateEligibilityIncomeText,
                                    onIncomeSliderChange = viewModel::updateEligibilityIncomeSlider,
                                    onNudgeIncome = viewModel::nudgeEligibilityIncome,
                                    onLiabilitiesTextChange = viewModel::updateEligibilityLiabilitiesText,
                                    onLiabilitiesSliderChange = viewModel::updateEligibilityLiabilitiesSlider,
                                    onNudgeLiabilities = viewModel::nudgeEligibilityLiabilities,
                                    onSelectDtiPreset = viewModel::selectDtiPreset,
                                    onDtiSliderChange = viewModel::updateEligibilityDtiSlider,
                                    onToggleInterest = viewModel::toggleEligibilityInterest,
                                    onRateTextChange = viewModel::updateEligibilityRateText,
                                    onRateSliderChange = viewModel::updateEligibilityRateSlider,
                                    onNudgeRate = viewModel::nudgeEligibilityRate,
                                    onTenureTextChange = viewModel::updateEligibilityTenureText,
                                    onTenureSliderChange = viewModel::updateEligibilityTenureSlider,
                                    onSwitchTenureUnit = viewModel::switchEligibilityTenureUnit,
                                    onApplyToCalculator = {
                                        viewModel.applyEligibilityToCalculator()
                                        currentTab = EmiTab.CALCULATOR
                                    }
                                )
                                EmiTab.HISTORY -> HistoryScreen(
                                    sessions = savedSessions,
                                    useDeviceLocale = uiState.useDeviceLocaleSeparators,
                                    onLoadSession = { session ->
                                        viewModel.loadSavedSession(session)
                                        currentTab = EmiTab.CALCULATOR
                                    },
                                    onDeleteSession = viewModel::deleteSavedSession,
                                    onClearAll = viewModel::clearAllSavedSessions,
                                    onGoToCalculator = { currentTab = EmiTab.CALCULATOR }
                                )
                            }
                        }
                    }
                }
            }

            if (showCurrencyDialog) {
                CurrencySelectionDialog(
                    selected = uiState.currency,
                    themeMode = uiState.themeMode,
                    onSelectThemeMode = viewModel::setThemeMode,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators,
                    onToggleDeviceLocale = viewModel::setUseDeviceLocaleSeparators,
                    onSelect = {
                        viewModel.selectCurrency(it)
                        showCurrencyDialog = false
                    },
                    onDismiss = { showCurrencyDialog = false }
                )
            }

            if (showSaveDialog) {
                SaveSessionDialog(
                    label = saveLabelDraft,
                    onLabelChange = { saveLabelDraft = it },
                    onConfirm = {
                        viewModel.saveCurrentSession(saveLabelDraft)
                        showSaveDialog = false
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val res = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.saved_snackbar_message),
                                actionLabel = context.getString(R.string.saved_snackbar_action),
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                currentTab = EmiTab.HISTORY
                            }
                        }
                    },
                    onDismiss = { showSaveDialog = false }
                )
            }
        }
    }
}

@Composable
private fun TabIconWithOptionalBadge(
    tab: EmiTab,
    selected: Boolean,
    savedCount: Int
) {
    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
    val desc = stringResource(tab.labelRes)
    if (tab == EmiTab.HISTORY && savedCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = if (savedCount > 99) "99+" else savedCount.toString())
                }
            }
        ) {
            Icon(imageVector = iconVector, contentDescription = desc)
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = desc)
    }
}

@Composable
private fun SaveSessionDialog(
    label: String,
    onLabelChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.save_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                singleLine = true,
                label = { Text(stringResource(R.string.save_dialog_label_hint)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_session_label_input")
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("save_session_confirm_button")
            ) {
                Text(stringResource(R.string.save_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save_dialog_cancel))
            }
        }
    )
}

@Composable
private fun CurrencySelectionDialog(
    selected: CurrencyOption,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    useDeviceLocale: Boolean,
    onToggleDeviceLocale: (Boolean) -> Unit,
    onSelect: (CurrencyOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.currency_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.theme_appearance_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf(
                            ThemeMode.SYSTEM to R.string.theme_mode_auto,
                            ThemeMode.LIGHT to R.string.theme_mode_light,
                            ThemeMode.DARK to R.string.theme_mode_dark
                        )
                        modes.forEachIndexed { index, (mode, labelRes) ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                                modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.currency_device_locale_toggle),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.currency_device_locale_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDeviceLocale,
                            onCheckedChange = onToggleDeviceLocale,
                            modifier = Modifier.testTag("device_locale_separators_switch")
                        )
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(CurrencyOption.entries) { option ->
                        val isSelected = option == selected
                        val preview = CurrencyFormatter.format(
                            amount = 12345.67,
                            currency = option,
                            showDecimals = true,
                            useDeviceLocale = useDeviceLocale
                        )
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { onSelect(option) }
                                .testTag("currency_option_${option.code.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = option.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(42.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = option.code,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = option.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = preview,
                                        style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.currency_dialog_close))
            }
        }
    )
}
