package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ComparisonResult
import com.example.domain.CurrencyOption
import com.example.domain.LoanInput
import com.example.domain.TenureUnit
import com.example.export.CsvExporter
import com.example.ui.EmiUiState
import com.example.ui.LoanFieldStrings
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.heroAtmosphere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun CompareScreen(
    uiState: EmiUiState,
    onCopyCurrentToSlot: (Boolean) -> Unit,
    onSwapLoans: () -> Unit,
    onToggleCompareInterest: (isLoanA: Boolean, enabled: Boolean) -> Unit,
    onUpdateCompareField: (
        isLoanA: Boolean,
        principalRaw: String?,
        rateRaw: String?,
        tenureRaw: String?,
        feeRaw: String?,
        tenureUnit: TenureUnit?
    ) -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shareCompareTitle = stringResource(R.string.share_compare_chooser)
    val csvSavedMsg = stringResource(R.string.export_csv_saved_msg)
    val csvErrorMsg = stringResource(R.string.export_csv_error_msg)

    var showCsvDialog by remember { mutableStateOf(false) }
    var isExportingCsv by remember { mutableStateOf(false) }

    val createCompareCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            isExportingCsv = true
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            CsvExporter.writeComparisonCsv(
                                outputStream = stream,
                                loanA = uiState.loanA,
                                loanB = uiState.loanB,
                                comparison = uiState.comparisonResult,
                                currency = uiState.currency
                            )
                        } ?: error("Cannot open output stream")
                    }.isSuccess
                }
                isExportingCsv = false
                showCsvDialog = false
                onShowMessage(if (success) csvSavedMsg else csvErrorMsg)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 740.dp

        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VerdictBanner(
                comparison = uiState.comparisonResult,
                currency = uiState.currency,
                useDeviceLocale = uiState.useDeviceLocaleSeparators
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { showCsvDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("compare_export_csv_button")
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

                FilledTonalButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "EMI Pulse Loan Comparison")
                            putExtra(Intent.EXTRA_TEXT, buildCompareShareText(uiState))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, shareCompareTitle))
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("compare_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.share_summary),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onSwapLoans,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("compare_swap_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.compare_swap_loans),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CompareLoanCard(
                        title = stringResource(R.string.compare_loan_a),
                        isLoanA = true,
                        isWinner = uiState.comparisonResult.cheaperSide == -1,
                        loan = uiState.loanA,
                        strings = uiState.loanAStrings,
                        includeInterest = uiState.loanAIncludeInterest,
                        monthlyEmi = uiState.comparisonResult.resultA.monthlyEmi,
                        currency = uiState.currency,
                        useDeviceLocale = uiState.useDeviceLocaleSeparators,
                        onCopyCurrent = { onCopyCurrentToSlot(true) },
                        onToggleInterest = { onToggleCompareInterest(true, it) },
                        onUpdateField = { p, r, t, f, unit ->
                            onUpdateCompareField(true, p, r, t, f, unit)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CompareLoanCard(
                        title = stringResource(R.string.compare_loan_b),
                        isLoanA = false,
                        isWinner = uiState.comparisonResult.cheaperSide == 1,
                        loan = uiState.loanB,
                        strings = uiState.loanBStrings,
                        includeInterest = uiState.loanBIncludeInterest,
                        monthlyEmi = uiState.comparisonResult.resultB.monthlyEmi,
                        currency = uiState.currency,
                        useDeviceLocale = uiState.useDeviceLocaleSeparators,
                        onCopyCurrent = { onCopyCurrentToSlot(false) },
                        onToggleInterest = { onToggleCompareInterest(false, it) },
                        onUpdateField = { p, r, t, f, unit ->
                            onUpdateCompareField(false, p, r, t, f, unit)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                CompareLoanCard(
                    title = stringResource(R.string.compare_loan_a),
                    isLoanA = true,
                    isWinner = uiState.comparisonResult.cheaperSide == -1,
                    loan = uiState.loanA,
                    strings = uiState.loanAStrings,
                    includeInterest = uiState.loanAIncludeInterest,
                    monthlyEmi = uiState.comparisonResult.resultA.monthlyEmi,
                    currency = uiState.currency,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators,
                    onCopyCurrent = { onCopyCurrentToSlot(true) },
                    onToggleInterest = { onToggleCompareInterest(true, it) },
                    onUpdateField = { p, r, t, f, unit ->
                        onUpdateCompareField(true, p, r, t, f, unit)
                    }
                )
                CompareLoanCard(
                    title = stringResource(R.string.compare_loan_b),
                    isLoanA = false,
                    isWinner = uiState.comparisonResult.cheaperSide == 1,
                    loan = uiState.loanB,
                    strings = uiState.loanBStrings,
                    includeInterest = uiState.loanBIncludeInterest,
                    monthlyEmi = uiState.comparisonResult.resultB.monthlyEmi,
                    currency = uiState.currency,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators,
                    onCopyCurrent = { onCopyCurrentToSlot(false) },
                    onToggleInterest = { onToggleCompareInterest(false, it) },
                    onUpdateField = { p, r, t, f, unit ->
                        onUpdateCompareField(false, p, r, t, f, unit)
                    }
                )
            }

            ComparisonTableCard(
                loanA = uiState.loanA,
                loanB = uiState.loanB,
                comparison = uiState.comparisonResult,
                currency = uiState.currency,
                useDeviceLocale = uiState.useDeviceLocaleSeparators
            )

            SideBySideBreakdownCard(
                comparison = uiState.comparisonResult,
                currency = uiState.currency,
                useDeviceLocale = uiState.useDeviceLocaleSeparators
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showCsvDialog) {
        ExportCompareCsvDialog(
            isExporting = isExportingCsv,
            onShare = {
                isExportingCsv = true
                coroutineScope.launch {
                    val uriResult = withContext(Dispatchers.IO) {
                        runCatching {
                            CsvExporter.exportComparisonToCacheUri(
                                context = context,
                                loanA = uiState.loanA,
                                loanB = uiState.loanB,
                                comparison = uiState.comparisonResult,
                                currency = uiState.currency
                            )
                        }
                    }
                    isExportingCsv = false
                    showCsvDialog = false
                    uriResult.onSuccess { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "EMI Pulse Loan Comparison CSV")
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
                createCompareCsvLauncher.launch(CsvExporter.defaultComparisonFileName())
            },
            onDismiss = {
                if (!isExportingCsv) showCsvDialog = false
            }
        )
    }
}

@Composable
private fun VerdictBanner(
    comparison: ComparisonResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current
    val headline = when (comparison.cheaperSide) {
        -1 -> stringResource(
            R.string.compare_verdict_cheaper,
            stringResource(R.string.compare_loan_a),
            CurrencyFormatter.format(comparison.totalSavings, currency, useDeviceLocale = useDeviceLocale)
        )
        1 -> stringResource(
            R.string.compare_verdict_cheaper,
            stringResource(R.string.compare_loan_b),
            CurrencyFormatter.format(comparison.totalSavings, currency, useDeviceLocale = useDeviceLocale)
        )
        else -> stringResource(R.string.compare_verdict_equal)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compare_verdict_banner")
    ) {
        Box(
            modifier = Modifier
                .heroAtmosphere(emiColors.heroStart, emiColors.heroEnd)
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(emiColors.heroPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (comparison.cheaperSide == 0) {
                            Icons.AutoMirrored.Filled.CompareArrows
                        } else {
                            Icons.Filled.EmojiEvents
                        },
                        contentDescription = null,
                        tint = emiColors.heroHighlightText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = emiColors.heroOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.compare_emi_diff,
                            CurrencyFormatter.format(comparison.monthlyEmiDiff, currency, useDeviceLocale = useDeviceLocale)
                        ),
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        color = emiColors.heroMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareLoanCard(
    title: String,
    isLoanA: Boolean,
    isWinner: Boolean,
    loan: LoanInput,
    strings: LoanFieldStrings,
    includeInterest: Boolean,
    monthlyEmi: Double,
    currency: CurrencyOption,
    useDeviceLocale: Boolean,
    onCopyCurrent: () -> Unit,
    onToggleInterest: (Boolean) -> Unit,
    onUpdateField: (String?, String?, String?, String?, TenureUnit?) -> Unit,
    modifier: Modifier = Modifier
) {
    val emiColors = LocalEmiColors.current
    val tagPrefix = if (isLoanA) "compare_a" else "compare_b"
    val borderColor = if (isWinner) MaterialTheme.colorScheme.primary else emiColors.cardBorder

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isWinner) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (isWinner) {
                        Surface(
                            color = emiColors.positiveBadgeBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Best Value",
                                style = MaterialTheme.typography.labelSmall,
                                color = emiColors.positiveBadgeFg,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onCopyCurrent,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${tagPrefix}_use_current_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.compare_use_current),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Live EMI readout pill
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.monthly_emi),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(monthlyEmi, currency, useDeviceLocale = useDeviceLocale),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = strings.principalText,
                onValueChange = { onUpdateField(it, null, null, null, null) },
                singleLine = true,
                label = { Text(stringResource(R.string.loan_amount)) },
                prefix = { Text(currency.symbol) },
                textStyle = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = emiColors.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_principal_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.interest_toggle_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!includeInterest) {
                        Surface(
                            color = emiColors.positiveBadgeBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.interest_toggle_free_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = emiColors.positiveBadgeFg,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Switch(
                    checked = includeInterest,
                    onCheckedChange = onToggleInterest,
                    modifier = Modifier.testTag("${tagPrefix}_interest_toggle")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (includeInterest) strings.rateText else "0",
                    onValueChange = { onUpdateField(null, it, null, null, null) },
                    enabled = includeInterest,
                    singleLine = true,
                    label = { Text("Rate (%)") },
                    suffix = { Text("%") },
                    textStyle = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = emiColors.cardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_rate_input")
                )

                OutlinedTextField(
                    value = strings.feeText,
                    onValueChange = { onUpdateField(null, null, null, it, null) },
                    singleLine = true,
                    label = { Text("Fee (%)") },
                    suffix = { Text("%") },
                    textStyle = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = emiColors.cardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_fee_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = strings.tenureText,
                    onValueChange = { onUpdateField(null, null, it, null, null) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.loan_tenure)) },
                    textStyle = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = emiColors.cardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_tenure_input")
                )

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = loan.tenureUnit == TenureUnit.YEARS,
                        onClick = { onUpdateField(null, null, null, null, TenureUnit.YEARS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Yr", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = loan.tenureUnit == TenureUnit.MONTHS,
                        onClick = { onUpdateField(null, null, null, null, TenureUnit.MONTHS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Mo", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonTableCard(
    loanA: LoanInput,
    loanB: LoanInput,
    comparison: ComparisonResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current
    val resA = comparison.resultA
    val resB = comparison.resultB

    val emiWinner = compareWinner(resA.monthlyEmi, resB.monthlyEmi)
    val interestWinner = compareWinner(resA.baselineTotalInterest, resB.baselineTotalInterest)
    val feeWinner = compareWinner(comparison.feeA, comparison.feeB)
    val totalWinner = compareWinner(comparison.totalCostA, comparison.totalCostB)

    val interestPctA = if (resA.principal > 0) (resA.baselineTotalInterest / resA.principal) * 100.0 else 0.0
    val interestPctB = if (resB.principal > 0) (resB.baselineTotalInterest / resB.principal) * 100.0 else 0.0

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comparison_table_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.compare_table_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Table Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.compare_col_metric),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = stringResource(R.string.compare_loan_a),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.compare_loan_b),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.compare_col_diff),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }

            ComparisonTableRow(
                metric = stringResource(R.string.principal_label),
                valueA = CurrencyFormatter.formatCompact(loanA.principal, currency, useDeviceLocale),
                valueB = CurrencyFormatter.formatCompact(loanB.principal, currency, useDeviceLocale),
                diff = formatDiffCompact(loanA.principal - loanB.principal, currency, useDeviceLocale)
            )
            ComparisonTableRow(
                metric = "Rate (% p.a.)",
                valueA = CurrencyFormatter.formatPercent(loanA.annualRatePercent, 2, currency, useDeviceLocale),
                valueB = CurrencyFormatter.formatPercent(loanB.annualRatePercent, 2, currency, useDeviceLocale),
                diff = formatRateDiff(loanA.annualRatePercent - loanB.annualRatePercent, currency, useDeviceLocale)
            )
            ComparisonTableRow(
                metric = "Tenure",
                valueA = CurrencyFormatter.formatTenureMonths(loanA.totalMonths),
                valueB = CurrencyFormatter.formatTenureMonths(loanB.totalMonths),
                diff = formatMonthsDiff(loanA.totalMonths - loanB.totalMonths)
            )
            ComparisonTableRow(
                metric = stringResource(R.string.monthly_emi),
                valueA = CurrencyFormatter.format(resA.monthlyEmi, currency, useDeviceLocale = useDeviceLocale),
                valueB = CurrencyFormatter.format(resB.monthlyEmi, currency, useDeviceLocale = useDeviceLocale),
                diff = CurrencyFormatter.formatCompact(comparison.monthlyEmiDiff, currency, useDeviceLocale),
                winnerSide = emiWinner,
                emphasize = true
            )
            ComparisonTableRow(
                metric = stringResource(R.string.interest_label),
                valueA = CurrencyFormatter.formatCompact(resA.baselineTotalInterest, currency, useDeviceLocale),
                valueB = CurrencyFormatter.formatCompact(resB.baselineTotalInterest, currency, useDeviceLocale),
                diff = CurrencyFormatter.formatCompact(
                    kotlin.math.abs(resA.baselineTotalInterest - resB.baselineTotalInterest),
                    currency,
                    useDeviceLocale
                ),
                winnerSide = interestWinner
            )
            ComparisonTableRow(
                metric = "Int / Prin %",
                valueA = CurrencyFormatter.formatPercent(interestPctA, 1, currency, useDeviceLocale),
                valueB = CurrencyFormatter.formatPercent(interestPctB, 1, currency, useDeviceLocale),
                diff = CurrencyFormatter.formatPercent(kotlin.math.abs(interestPctA - interestPctB), 1, currency, useDeviceLocale)
            )
            ComparisonTableRow(
                metric = stringResource(R.string.compare_fee_label),
                valueA = CurrencyFormatter.formatCompact(comparison.feeA, currency, useDeviceLocale),
                valueB = CurrencyFormatter.formatCompact(comparison.feeB, currency, useDeviceLocale),
                diff = CurrencyFormatter.formatCompact(kotlin.math.abs(comparison.feeA - comparison.feeB), currency, useDeviceLocale),
                winnerSide = feeWinner
            )
            ComparisonTableRow(
                metric = "Total Payable",
                valueA = CurrencyFormatter.formatCompact(comparison.totalCostA, currency, useDeviceLocale),
                valueB = CurrencyFormatter.formatCompact(comparison.totalCostB, currency, useDeviceLocale),
                diff = CurrencyFormatter.formatCompact(comparison.totalSavings, currency, useDeviceLocale),
                winnerSide = totalWinner,
                emphasize = true,
                showDivider = false
            )
        }
    }
}

@Composable
private fun ComparisonTableRow(
    metric: String,
    valueA: String,
    valueB: String,
    diff: String,
    winnerSide: Int = 0, // -1 = A better, 1 = B better, 0 = neutral
    emphasize: Boolean = false,
    showDivider: Boolean = true
) {
    val emiColors = LocalEmiColors.current
    val defaultText = MaterialTheme.colorScheme.onSurface
    val winnerText = emiColors.principalColor
    val baseStyle = if (emphasize) {
        MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
                color = if (emphasize) defaultText else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = valueA,
                style = baseStyle,
                color = if (winnerSide == -1) winnerText else defaultText,
                fontWeight = if (winnerSide == -1 || emphasize) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueB,
                style = baseStyle,
                color = if (winnerSide == 1) winnerText else defaultText,
                fontWeight = if (winnerSide == 1 || emphasize) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = diff,
                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.9f)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = emiColors.cardBorder.copy(alpha = 0.6f))
        }
    }
}

private fun compareWinner(valA: Double, valB: Double): Int = when {
    kotlin.math.abs(valA - valB) < 0.5 -> 0
    valA < valB -> -1
    else -> 1
}

private fun formatDiffCompact(delta: Double, currency: CurrencyOption, useDeviceLocale: Boolean): String {
    return if (kotlin.math.abs(delta) < 1.0) "—" else CurrencyFormatter.formatCompact(kotlin.math.abs(delta), currency, useDeviceLocale)
}

private fun formatRateDiff(delta: Double, currency: CurrencyOption, useDeviceLocale: Boolean): String {
    return if (kotlin.math.abs(delta) < 0.01) "—" else CurrencyFormatter.formatPercent(kotlin.math.abs(delta), 2, currency, useDeviceLocale)
}

private fun formatMonthsDiff(deltaMonths: Int): String {
    return if (deltaMonths == 0) "—" else CurrencyFormatter.formatTenureMonths(kotlin.math.abs(deltaMonths))
}

@Composable
private fun SideBySideBreakdownCard(
    comparison: ComparisonResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.compare_breakdown_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompareBarLegendDot(
                        color = emiColors.principalColor,
                        label = "A"
                    )
                    CompareBarLegendDot(
                        color = emiColors.prepayColor,
                        label = "B"
                    )
                }
            }

            ComparisonMetricBar(
                label = stringResource(R.string.monthly_emi),
                valueA = comparison.resultA.monthlyEmi,
                valueB = comparison.resultB.monthlyEmi,
                currency = currency,
                useDeviceLocale = useDeviceLocale,
                barColorA = emiColors.principalColor,
                barColorB = emiColors.prepayColor
            )
            ComparisonMetricBar(
                label = stringResource(R.string.interest_label),
                valueA = comparison.resultA.baselineTotalInterest,
                valueB = comparison.resultB.baselineTotalInterest,
                currency = currency,
                useDeviceLocale = useDeviceLocale,
                barColorA = emiColors.interestColor,
                barColorB = emiColors.prepayColor
            )
            ComparisonMetricBar(
                label = stringResource(R.string.compare_fee_label),
                valueA = comparison.feeA,
                valueB = comparison.feeB,
                currency = currency,
                useDeviceLocale = useDeviceLocale,
                barColorA = emiColors.principalColor,
                barColorB = emiColors.prepayColor
            )
            ComparisonMetricBar(
                label = stringResource(R.string.compare_total_cost),
                valueA = comparison.totalCostA,
                valueB = comparison.totalCostB,
                currency = currency,
                useDeviceLocale = useDeviceLocale,
                barColorA = emiColors.principalColor,
                barColorB = emiColors.prepayColor
            )
        }
    }
}

@Composable
private fun CompareBarLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ComparisonMetricBar(
    label: String,
    valueA: Double,
    valueB: Double,
    currency: CurrencyOption,
    useDeviceLocale: Boolean,
    barColorA: Color,
    barColorB: Color
) {
    val maxVal = max(valueA, valueB).coerceAtLeast(1.0)
    val fracA = (valueA / maxVal).toFloat().coerceIn(0.05f, 1f)
    val fracB = (valueB / maxVal).toFloat().coerceIn(0.05f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "A",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (valueA <= 0.0) 0f else fracA)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(barColorA)
                )
            }
            Text(
                text = CurrencyFormatter.format(valueA, currency, useDeviceLocale = useDeviceLocale),
                style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "B",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (valueB <= 0.0) 0f else fracB)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(barColorB)
                )
            }
            Text(
                text = CurrencyFormatter.format(valueB, currency, useDeviceLocale = useDeviceLocale),
                style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildCompareShareText(uiState: EmiUiState): String {
    val currency = uiState.currency
    val useLocale = uiState.useDeviceLocaleSeparators
    val cmp = uiState.comparisonResult
    val verdict = when (cmp.cheaperSide) {
        -1 -> "Loan A saves ${CurrencyFormatter.format(cmp.totalSavings, currency, useDeviceLocale = useLocale)} overall"
        1 -> "Loan B saves ${CurrencyFormatter.format(cmp.totalSavings, currency, useDeviceLocale = useLocale)} overall"
        else -> "Both loans cost the same overall"
    }
    return buildString {
        appendLine("EMI Pulse — Loan A vs Loan B")
        appendLine("Verdict: $verdict")
        appendLine("Loan A: EMI ${CurrencyFormatter.format(cmp.resultA.monthlyEmi, currency, useDeviceLocale = useLocale)} | Total ${CurrencyFormatter.format(cmp.totalCostA, currency, useDeviceLocale = useLocale)} (${CurrencyFormatter.formatPercent(uiState.loanA.annualRatePercent, 2, currency, useLocale)}, ${CurrencyFormatter.formatTenureMonths(uiState.loanA.totalMonths)})")
        appendLine("Loan B: EMI ${CurrencyFormatter.format(cmp.resultB.monthlyEmi, currency, useDeviceLocale = useLocale)} | Total ${CurrencyFormatter.format(cmp.totalCostB, currency, useDeviceLocale = useLocale)} (${CurrencyFormatter.formatPercent(uiState.loanB.annualRatePercent, 2, currency, useLocale)}, ${CurrencyFormatter.formatTenureMonths(uiState.loanB.totalMonths)})")
    }.trim()
}

@Composable
private fun ExportCompareCsvDialog(
    isExporting: Boolean,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    text = stringResource(R.string.export_csv_compare_subtitle),
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
                    CompareExportTile(
                        icon = Icons.Filled.Share,
                        title = stringResource(R.string.export_csv_share),
                        subtitle = stringResource(R.string.export_csv_share_desc),
                        testTag = "compare_share_csv_option",
                        onClick = onShare
                    )
                    CompareExportTile(
                        icon = Icons.Filled.Download,
                        title = stringResource(R.string.export_csv_save),
                        subtitle = stringResource(R.string.export_csv_save_desc),
                        testTag = "compare_save_csv_option",
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
private fun CompareExportTile(
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
