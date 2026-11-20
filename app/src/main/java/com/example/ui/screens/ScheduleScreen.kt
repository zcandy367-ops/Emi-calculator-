package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.export.AmortizationPdfExporter
import com.example.export.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.R
import com.example.domain.AmortizationMonth
import com.example.domain.AmortizationYear
import com.example.domain.CurrencyOption
import com.example.ui.EmiUiState
import com.example.ui.ScheduleGranularity
import com.example.ui.components.AmortizationStackedChart
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.LoanBreakdownPieChart
import com.example.ui.components.PieSlice
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle

@Composable
fun ScheduleScreen(
    uiState: EmiUiState,
    onGranularityChange: (ScheduleGranularity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val result = uiState.emiResult
    val emiColors = LocalEmiColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showPieSummary by rememberSaveable { mutableStateOf(false) }
    var selectedPieSlice by remember { mutableStateOf<PieSlice?>(null) }

    val savedSuccessMsg = stringResource(R.string.export_pdf_saved_msg)
    val exportErrorMsg = stringResource(R.string.export_pdf_error_msg)
    val csvSavedMsg = stringResource(R.string.export_csv_saved_msg)
    val csvErrorMsg = stringResource(R.string.export_csv_error_msg)

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            AmortizationPdfExporter.writePdf(
                                outputStream = stream,
                                preset = uiState.selectedPreset,
                                input = uiState.loanInput,
                                result = uiState.emiResult,
                                currency = uiState.currency,
                                granularity = uiState.scheduleGranularity,
                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                            )
                        } ?: error("Cannot open output stream")
                    }.isSuccess
                }
                isExporting = false
                showExportDialog = false
                onShowMessage(if (success) savedSuccessMsg else exportErrorMsg)
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            CsvExporter.writeScheduleCsv(
                                outputStream = stream,
                                preset = uiState.selectedPreset,
                                input = uiState.loanInput,
                                result = uiState.emiResult,
                                currency = uiState.currency,
                                granularity = uiState.scheduleGranularity
                            )
                        } ?: error("Cannot open output stream")
                    }.isSuccess
                }
                isExporting = false
                showCsvDialog = false
                onShowMessage(if (success) csvSavedMsg else csvErrorMsg)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (result.monthlySchedule.isEmpty()) {
            EmptyScheduleState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize()
                    .testTag("schedule_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header_controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column {
                                Text(
                                    text = stringResource(R.string.schedule_chart_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${result.effectiveMonths} installments • ${CurrencyFormatter.formatTenureMonths(result.effectiveMonths)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showExportDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("export_pdf_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.export_pdf_button),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { showCsvDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("export_csv_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TableView,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.export_csv_full_button),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.scheduleGranularity == ScheduleGranularity.YEARLY,
                                onClick = { onGranularityChange(ScheduleGranularity.YEARLY) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("schedule_yearly_toggle")
                            ) {
                                Text(
                                    text = stringResource(R.string.schedule_yearly),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            SegmentedButton(
                                selected = uiState.scheduleGranularity == ScheduleGranularity.MONTHLY,
                                onClick = { onGranularityChange(ScheduleGranularity.MONTHLY) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("schedule_monthly_toggle")
                            ) {
                                Text(
                                    text = stringResource(R.string.schedule_monthly),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, emiColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = !showPieSummary,
                                        onClick = { showPieSummary = false },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        modifier = Modifier.testTag("schedule_chart_trend_tab")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.schedule_chart_trend),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    SegmentedButton(
                                        selected = showPieSummary,
                                        onClick = { showPieSummary = true },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        modifier = Modifier.testTag("schedule_chart_pie_tab")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.schedule_chart_pie),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                if (!showPieSummary) {
                                    AmortizationStackedChart(
                                        yearlySchedule = result.yearlySchedule,
                                        currency = uiState.currency,
                                        useDeviceLocale = uiState.useDeviceLocaleSeparators
                                    )
                                } else {
                                    SchedulePieSummaryContent(
                                        principalRatio = result.principalRatio,
                                        interestRatio = result.interestRatio,
                                        principalAmount = CurrencyFormatter.format(
                                            amount = result.principal,
                                            currency = uiState.currency,
                                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                                        ),
                                        interestAmount = CurrencyFormatter.format(
                                            amount = result.effectiveTotalInterest,
                                            currency = uiState.currency,
                                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                                        ),
                                        totalPayableAmount = CurrencyFormatter.format(
                                            amount = result.effectiveTotalPayable,
                                            currency = uiState.currency,
                                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                                        ),
                                        savedCompactAmount = if (result.interestSaved > 0.01) {
                                            CurrencyFormatter.formatCompact(
                                                amount = result.interestSaved,
                                                currency = uiState.currency,
                                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                                            )
                                        } else null,
                                        principalPercent = CurrencyFormatter.formatPercent(
                                            percent = result.principalRatio * 100.0,
                                            decimals = 1,
                                            currency = uiState.currency,
                                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                                        ),
                                        interestPercent = CurrencyFormatter.formatPercent(
                                            percent = result.interestRatio * 100.0,
                                            decimals = 1,
                                            currency = uiState.currency,
                                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                                        ),
                                        selectedSlice = selectedPieSlice,
                                        onSelectSlice = { selectedPieSlice = it }
                                    )
                                }

                                val halfwayMonth = remember(result.monthlySchedule, result.principal) {
                                    if (result.principal <= 0.0) null
                                    else result.monthlySchedule.firstOrNull {
                                        it.closingBalance <= result.principal * 0.5
                                    }
                                }
                                if (halfwayMonth != null) {
                                    Surface(
                                        color = emiColors.positiveBadgeBg.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Flag,
                                                contentDescription = null,
                                                tint = emiColors.positiveBadgeFg,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.schedule_milestone_halfway,
                                                    halfwayMonth.yearIndex,
                                                    halfwayMonth.monthIndex
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = emiColors.positiveBadgeFg,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Table column header
                        ScheduleTableHeader()
                    }
                }

                if (uiState.scheduleGranularity == ScheduleGranularity.YEARLY) {
                    items(
                        items = result.yearlySchedule,
                        key = { "year_${it.yearIndex}" }
                    ) { row ->
                        YearlyScheduleRow(
                            year = row,
                            totalPrincipal = result.principal,
                            currency = uiState.currency,
                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                        )
                    }
                } else {
                    items(
                        items = result.monthlySchedule,
                        key = { "month_${it.monthIndex}" }
                    ) { row ->
                        MonthlyScheduleRow(
                            month = row,
                            totalPrincipal = result.principal,
                            currency = uiState.currency,
                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showExportDialog) {
        ExportPdfDialog(
            granularity = uiState.scheduleGranularity,
            yearlyCount = result.yearlySchedule.size,
            monthlyCount = result.monthlySchedule.size,
            isExporting = isExporting,
            onShare = {
                isExporting = true
                coroutineScope.launch {
                    val uriResult = withContext(Dispatchers.IO) {
                        runCatching {
                            AmortizationPdfExporter.exportToCacheUri(
                                context = context,
                                preset = uiState.selectedPreset,
                                input = uiState.loanInput,
                                result = uiState.emiResult,
                                currency = uiState.currency,
                                granularity = uiState.scheduleGranularity,
                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                            )
                        }
                    }
                    isExporting = false
                    showExportDialog = false
                    uriResult.onSuccess { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "EMI Pulse Amortization Schedule")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, context.getString(R.string.export_pdf_share))
                        )
                    }.onFailure {
                        onShowMessage(exportErrorMsg)
                    }
                }
            },
            onSaveToDevice = {
                val suggestedName = AmortizationPdfExporter.defaultFileName(
                    uiState.selectedPreset,
                    uiState.scheduleGranularity
                )
                createPdfLauncher.launch(suggestedName)
            },
            onPrint = {
                isExporting = true
                coroutineScope.launch {
                    val fileResult = withContext(Dispatchers.IO) {
                        runCatching {
                            AmortizationPdfExporter.exportToCacheFile(
                                context = context,
                                preset = uiState.selectedPreset,
                                input = uiState.loanInput,
                                result = uiState.emiResult,
                                currency = uiState.currency,
                                granularity = uiState.scheduleGranularity,
                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                            )
                        }
                    }
                    isExporting = false
                    showExportDialog = false
                    fileResult.onSuccess { pdfFile ->
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        if (printManager != null) {
                            val jobName = "EMI Pulse Amortization Schedule"
                            printManager.print(
                                jobName,
                                AmortizationPdfExporter.createPrintAdapter(context, pdfFile, jobName),
                                null
                            )
                        } else {
                            onShowMessage(exportErrorMsg)
                        }
                    }.onFailure {
                        onShowMessage(exportErrorMsg)
                    }
                }
            },
            onDismiss = {
                if (!isExporting) showExportDialog = false
            }
        )
    }

    if (showCsvDialog) {
        ExportCsvScheduleDialog(
            granularity = uiState.scheduleGranularity,
            yearlyCount = result.yearlySchedule.size,
            monthlyCount = result.monthlySchedule.size,
            isExporting = isExporting,
            onShare = {
                isExporting = true
                coroutineScope.launch {
                    val uriResult = withContext(Dispatchers.IO) {
                        runCatching {
                            CsvExporter.exportScheduleToCacheUri(
                                context = context,
                                preset = uiState.selectedPreset,
                                input = uiState.loanInput,
                                result = uiState.emiResult,
                                currency = uiState.currency,
                                granularity = uiState.scheduleGranularity
                            )
                        }
                    }
                    isExporting = false
                    showCsvDialog = false
                    uriResult.onSuccess { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "EMI Pulse Amortization CSV")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, context.getString(R.string.export_csv_share))
                        )
                    }.onFailure {
                        onShowMessage(csvErrorMsg)
                    }
                }
            },
            onSaveToDevice = {
                val suggestedName = CsvExporter.defaultScheduleFileName(
                    uiState.selectedPreset,
                    uiState.scheduleGranularity
                )
                createCsvLauncher.launch(suggestedName)
            },
            onDismiss = {
                if (!isExporting) showCsvDialog = false
            }
        )
    }
}

@Composable
private fun EmptyScheduleState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.schedule_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScheduleTableHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.schedule_col_period),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = stringResource(R.string.schedule_col_principal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = stringResource(R.string.schedule_col_interest),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = stringResource(R.string.schedule_col_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun YearlyScheduleRow(
    year: AmortizationYear,
    totalPrincipal: Double,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current
    val principalPaid = year.totalPrincipalPaid + year.totalExtraPaid
    val paidFraction = if (totalPrincipal > 0) {
        ((totalPrincipal - year.endingBalance) / totalPrincipal).toFloat().coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(0.8f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Yr ${year.yearIndex}",
                            style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = CurrencyFormatter.formatCompact(principalPaid, currency, useDeviceLocale),
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = emiColors.principalColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = CurrencyFormatter.formatCompact(year.totalInterestPaid, currency, useDeviceLocale),
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = emiColors.interestColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = CurrencyFormatter.formatCompact(year.endingBalance, currency, useDeviceLocale),
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(paidFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(emiColors.principalColor)
                )
            }
        }
    }
}

@Composable
private fun MonthlyScheduleRow(
    month: AmortizationMonth,
    totalPrincipal: Double,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current
    val principalPaid = month.principalPaid + month.extraPaid

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.8f)) {
                    Text(
                        text = "M${month.monthIndex}",
                        style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Yr ${month.yearIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = CurrencyFormatter.format(principalPaid, currency, useDeviceLocale = useDeviceLocale),
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = emiColors.principalColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = CurrencyFormatter.format(month.interestPaid, currency, useDeviceLocale = useDeviceLocale),
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = emiColors.interestColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = CurrencyFormatter.formatCompact(month.closingBalance, currency, useDeviceLocale),
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.2f)
                )
            }

            if (month.extraPaid > 0.01) {
                HorizontalDivider(color = emiColors.cardBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(emiColors.prepayColor.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Extra Prepayment",
                        style = MaterialTheme.typography.labelSmall,
                        color = emiColors.prepayColor
                    )
                    Text(
                        text = CurrencyFormatter.format(month.extraPaid, currency, useDeviceLocale = useDeviceLocale),
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                        color = emiColors.prepayColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportPdfDialog(
    granularity: ScheduleGranularity,
    yearlyCount: Int,
    monthlyCount: Int,
    isExporting: Boolean,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val scopeDescription = if (granularity == ScheduleGranularity.YEARLY) {
        "Yearly summary ($yearlyCount rows)"
    } else {
        "Monthly breakdown ($monthlyCount rows)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.export_pdf_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${stringResource(R.string.export_pdf_subtitle)} • $scopeDescription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    ExportActionTile(
                        icon = Icons.Filled.Share,
                        title = stringResource(R.string.export_pdf_share),
                        subtitle = stringResource(R.string.export_pdf_share_desc),
                        testTag = "export_share_pdf_option",
                        onClick = onShare
                    )
                    ExportActionTile(
                        icon = Icons.Filled.Download,
                        title = stringResource(R.string.export_pdf_save),
                        subtitle = stringResource(R.string.export_pdf_save_desc),
                        testTag = "export_save_pdf_option",
                        onClick = onSaveToDevice
                    )
                    ExportActionTile(
                        icon = Icons.Filled.Print,
                        title = stringResource(R.string.export_pdf_print),
                        subtitle = stringResource(R.string.export_pdf_print_desc),
                        testTag = "export_print_pdf_option",
                        onClick = onPrint
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text(stringResource(R.string.save_dialog_cancel))
            }
        }
    )
}

@Composable
private fun ExportCsvScheduleDialog(
    granularity: ScheduleGranularity,
    yearlyCount: Int,
    monthlyCount: Int,
    isExporting: Boolean,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val scopeDescription = if (granularity == ScheduleGranularity.YEARLY) {
        "Yearly summary ($yearlyCount rows)"
    } else {
        "Monthly breakdown ($monthlyCount rows)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.export_csv_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${stringResource(R.string.export_csv_schedule_subtitle)} • $scopeDescription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    ExportActionTile(
                        icon = Icons.Filled.Share,
                        title = stringResource(R.string.export_csv_share),
                        subtitle = stringResource(R.string.export_csv_share_desc),
                        testTag = "export_share_csv_option",
                        onClick = onShare
                    )
                    ExportActionTile(
                        icon = Icons.Filled.Download,
                        title = stringResource(R.string.export_csv_save),
                        subtitle = stringResource(R.string.export_csv_save_desc),
                        testTag = "export_save_csv_option",
                        onClick = onSaveToDevice
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text(stringResource(R.string.save_dialog_cancel))
            }
        }
    )
}

@Composable
private fun ExportActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    val emiColors = LocalEmiColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SchedulePieSummaryContent(
    principalRatio: Float,
    interestRatio: Float,
    principalAmount: String,
    interestAmount: String,
    totalPayableAmount: String,
    savedCompactAmount: String?,
    principalPercent: String,
    interestPercent: String,
    selectedSlice: PieSlice?,
    onSelectSlice: (PieSlice?) -> Unit
) {
    val emiColors = LocalEmiColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LoanBreakdownPieChart(
                principalRatio = principalRatio,
                interestRatio = interestRatio,
                selectedSlice = selectedSlice,
                onSelectSlice = onSelectSlice,
                size = 132.dp,
                modifier = Modifier.testTag("schedule_pie_chart")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchedulePieStatCard(
                    dotColor = emiColors.principalColor,
                    label = stringResource(R.string.loan_breakdown_principal_paid),
                    amount = principalAmount,
                    percent = principalPercent,
                    selected = selectedSlice == PieSlice.PRINCIPAL,
                    onClick = {
                        onSelectSlice(if (selectedSlice == PieSlice.PRINCIPAL) null else PieSlice.PRINCIPAL)
                    }
                )
                SchedulePieStatCard(
                    dotColor = emiColors.interestColor,
                    label = stringResource(R.string.loan_breakdown_interest_paid),
                    amount = interestAmount,
                    percent = interestPercent,
                    selected = selectedSlice == PieSlice.INTEREST,
                    onClick = {
                        onSelectSlice(if (selectedSlice == PieSlice.INTEREST) null else PieSlice.INTEREST)
                    }
                )
            }
        }

        HorizontalDivider(color = emiColors.cardBorder)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.total_payable_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (savedCompactAmount != null) {
                    Surface(
                        color = emiColors.positiveBadgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Saved $savedCompactAmount",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = emiColors.positiveBadgeFg,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = totalPayableAmount,
                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SchedulePieStatCard(
    dotColor: androidx.compose.ui.graphics.Color,
    label: String,
    amount: String,
    percent: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val emiColors = LocalEmiColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            dotColor.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) dotColor else emiColors.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = percent,
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = dotColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
