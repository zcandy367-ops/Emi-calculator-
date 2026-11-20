package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SavedEmiSessionEntity
import com.example.domain.CurrencyOption
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle
import java.text.DateFormat
import java.util.Date

private enum class HistorySortOrder {
    NEWEST,
    LOWEST_EMI,
    LARGEST_LOAN
}

@Composable
fun HistoryScreen(
    sessions: List<SavedEmiSessionEntity>,
    useDeviceLocale: Boolean = false,
    onLoadSession: (SavedEmiSessionEntity) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onClearAll: () -> Unit,
    onGoToCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPresetFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOrder by rememberSaveable { mutableStateOf(HistorySortOrder.NEWEST) }

    val filteredSessions = remember(sessions, searchQuery, selectedPresetFilter, sortOrder) {
        val trimmed = searchQuery.trim().lowercase()
        val matched = sessions.filter { item ->
            val matchesPreset = selectedPresetFilter == null || item.presetName == selectedPresetFilter
            val matchesQuery = trimmed.isEmpty() ||
                item.label.lowercase().contains(trimmed) ||
                item.currencyCode.lowercase().contains(trimmed)
            matchesPreset && matchesQuery
        }
        when (sortOrder) {
            HistorySortOrder.NEWEST -> matched.sortedByDescending { it.createdAtMillis }
            HistorySortOrder.LOWEST_EMI -> matched.sortedBy { it.monthlyEmi }
            HistorySortOrder.LARGEST_LOAN -> matched.sortedByDescending { it.principal }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (sessions.isEmpty()) {
            EmptyHistoryState(onGoToCalculator = onGoToCalculator)
        } else {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize()
                    .testTag("history_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "history_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.history_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredSessions.size} of ${sessions.size} ${if (sessions.size == 1) "session" else "sessions"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("history_clear_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.history_clear_all),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.history_search_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = LocalEmiColors.current.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = listOf(
                                null to stringResource(R.string.history_filter_all),
                                LoanPreset.HOME.name to stringResource(R.string.preset_home),
                                LoanPreset.CAR.name to stringResource(R.string.preset_car),
                                LoanPreset.PERSONAL.name to stringResource(R.string.preset_personal),
                                LoanPreset.EDUCATION.name to stringResource(R.string.preset_education)
                            )
                            filterOptions.forEach { (presetKey, label) ->
                                val selected = selectedPresetFilter == presetKey
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedPresetFilter = presetKey },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("history_filter_${presetKey?.lowercase() ?: "all"}")
                                )
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = sortOrder == HistorySortOrder.NEWEST,
                                onClick = { sortOrder = HistorySortOrder.NEWEST },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                modifier = Modifier.testTag("history_sort_newest")
                            ) {
                                Text(
                                    text = stringResource(R.string.history_sort_newest),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                            SegmentedButton(
                                selected = sortOrder == HistorySortOrder.LOWEST_EMI,
                                onClick = { sortOrder = HistorySortOrder.LOWEST_EMI },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                modifier = Modifier.testTag("history_sort_emi")
                            ) {
                                Text(
                                    text = stringResource(R.string.history_sort_emi),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                            SegmentedButton(
                                selected = sortOrder == HistorySortOrder.LARGEST_LOAN,
                                onClick = { sortOrder = HistorySortOrder.LARGEST_LOAN },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                modifier = Modifier.testTag("history_sort_principal")
                            ) {
                                Text(
                                    text = stringResource(R.string.history_sort_principal),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (filteredSessions.isEmpty()) {
                    item(key = "history_no_match") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, LocalEmiColors.current.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.history_no_match),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedPresetFilter = null
                                    }
                                ) {
                                    Text(stringResource(R.string.history_filter_all))
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredSessions,
                        key = { it.id }
                    ) { session ->
                        SavedSessionCard(
                            session = session,
                            useDeviceLocale = useDeviceLocale,
                            onLoad = { onLoadSession(session) },
                            onDelete = { onDeleteSession(session.id) }
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                    modifier = Modifier.testTag("history_clear_confirm_button")
                ) {
                    Text(
                        text = stringResource(R.string.history_clear_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.save_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun SavedSessionCard(
    session: SavedEmiSessionEntity,
    useDeviceLocale: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val emiColors = LocalEmiColors.current
    val currency = CurrencyOption.fromCode(session.currencyCode)
    val presetIcon = presetIconFor(session.presetName)
    val dateLabel = remember(session.createdAtMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(session.createdAtMillis))
    }
    val tenureMonths = if (session.tenureUnitName == TenureUnit.YEARS.name) {
        session.tenureValue * 12
    } else {
        session.tenureValue
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_session_card_${session.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = presetIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.history_delete_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Highlighted EMI + Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.monthly_emi),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(session.monthlyEmi, currency, useDeviceLocale = useDeviceLocale),
                        style = MaterialTheme.typography.headlineSmall.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = onLoad,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("load_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.history_load_action),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            HorizontalDivider(color = emiColors.cardBorder)

            // Compact parameters grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionSpecMetric(
                    label = stringResource(R.string.principal_label),
                    value = CurrencyFormatter.formatCompact(session.principal, currency, useDeviceLocale = useDeviceLocale),
                    modifier = Modifier.weight(1f)
                )
                SessionSpecMetric(
                    label = "Rate",
                    value = CurrencyFormatter.formatPercent(session.annualRatePercent, 2, currency, useDeviceLocale),
                    modifier = Modifier.weight(0.8f)
                )
                SessionSpecMetric(
                    label = "Tenure",
                    value = CurrencyFormatter.formatTenureMonths(tenureMonths),
                    modifier = Modifier.weight(0.8f)
                )
                SessionSpecMetric(
                    label = stringResource(R.string.interest_label),
                    value = CurrencyFormatter.formatCompact(session.totalInterest, currency, useDeviceLocale = useDeviceLocale),
                    valueColor = emiColors.interestColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (session.prepaymentFrequencyName != PrepaymentFrequency.NONE.name && session.interestSaved > 0.0) {
                Surface(
                    color = emiColors.positiveBadgeBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = emiColors.positiveBadgeFg,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Prepay saved ${CurrencyFormatter.formatCompact(session.interestSaved, currency, useDeviceLocale = useDeviceLocale)}",
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                                color = emiColors.positiveBadgeFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (session.monthsSaved > 0) {
                            Text(
                                text = "-${session.monthsSaved} mos",
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                                color = emiColors.positiveBadgeFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSpecMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyHistoryState(
    onGoToCalculator: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("history_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Bookmarks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGoToCalculator,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("history_empty_cta_button")
        ) {
            Text(text = stringResource(R.string.history_go_calculator))
        }
    }
}

private fun presetIconFor(presetName: String): ImageVector {
    return when (presetName) {
        LoanPreset.CAR.name -> Icons.Filled.DirectionsCar
        LoanPreset.PERSONAL.name -> Icons.Filled.Wallet
        LoanPreset.EDUCATION.name -> Icons.Filled.School
        else -> Icons.Filled.AccountBalance
    }
}
