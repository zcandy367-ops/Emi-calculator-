package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CurrencyOption
import com.example.domain.EmiResult
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.ui.EmiUiState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.EmiDonutChart
import com.example.ui.components.LoanBreakdownPieChart
import com.example.ui.components.PieSlice
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.heroAtmosphere

@Composable
fun CalculatorScreen(
    uiState: EmiUiState,
    onSelectPreset: (LoanPreset) -> Unit,
    onPrincipalTextChange: (String) -> Unit,
    onPrincipalSliderChange: (Double) -> Unit,
    onNudgePrincipal: (Double) -> Unit,
    onToggleIncludeInterest: (Boolean) -> Unit,
    onRateTextChange: (String) -> Unit,
    onRateSliderChange: (Double) -> Unit,
    onNudgeRate: (Double) -> Unit,
    onTenureTextChange: (String) -> Unit,
    onTenureSliderChange: (Int) -> Unit,
    onSwitchTenureUnit: (TenureUnit) -> Unit,
    onSelectPrepaymentFreq: (PrepaymentFrequency) -> Unit,
    onExtraPaymentTextChange: (String) -> Unit,
    onSaveSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.share_quote_chooser)
    val shareLoanSummary: () -> Unit = {
        val text = buildLoanShareText(uiState)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "EMI Pulse Loan Quote")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, shareChooserTitle))
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PresetChipRow(
                selectedPreset = uiState.selectedPreset,
                onSelectPreset = onSelectPreset
            )

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HeroSummaryCard(
                            result = uiState.emiResult,
                            currency = uiState.currency,
                            useDeviceLocale = uiState.useDeviceLocaleSeparators,
                            onSaveClick = onSaveSessionClick,
                            onShareClick = shareLoanSummary
                        )
                        LoanBreakdownPieCard(
                            result = uiState.emiResult,
                            currency = uiState.currency,
                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                        )
                        PrepaymentSimulatorCard(
                            uiState = uiState,
                            onSelectPrepaymentFreq = onSelectPrepaymentFreq,
                            onExtraPaymentTextChange = onExtraPaymentTextChange
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LoanInputsSection(
                            uiState = uiState,
                            onPrincipalTextChange = onPrincipalTextChange,
                            onPrincipalSliderChange = onPrincipalSliderChange,
                            onNudgePrincipal = onNudgePrincipal,
                            onToggleIncludeInterest = onToggleIncludeInterest,
                            onRateTextChange = onRateTextChange,
                            onRateSliderChange = onRateSliderChange,
                            onNudgeRate = onNudgeRate,
                            onTenureTextChange = onTenureTextChange,
                            onTenureSliderChange = onTenureSliderChange,
                            onSwitchTenureUnit = onSwitchTenureUnit
                        )
                    }
                }
            } else {
                HeroSummaryCard(
                    result = uiState.emiResult,
                    currency = uiState.currency,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators,
                    onSaveClick = onSaveSessionClick,
                    onShareClick = shareLoanSummary
                )
                LoanBreakdownPieCard(
                    result = uiState.emiResult,
                    currency = uiState.currency,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators
                )
                LoanInputsSection(
                    uiState = uiState,
                    onPrincipalTextChange = onPrincipalTextChange,
                    onPrincipalSliderChange = onPrincipalSliderChange,
                    onNudgePrincipal = onNudgePrincipal,
                    onToggleIncludeInterest = onToggleIncludeInterest,
                    onRateTextChange = onRateTextChange,
                    onRateSliderChange = onRateSliderChange,
                    onNudgeRate = onNudgeRate,
                    onTenureTextChange = onTenureTextChange,
                    onTenureSliderChange = onTenureSliderChange,
                    onSwitchTenureUnit = onSwitchTenureUnit
                )
                PrepaymentSimulatorCard(
                    uiState = uiState,
                    onSelectPrepaymentFreq = onSelectPrepaymentFreq,
                    onExtraPaymentTextChange = onExtraPaymentTextChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PresetChipRow(
    selectedPreset: LoanPreset,
    onSelectPreset: (LoanPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PresetItem(
            preset = LoanPreset.HOME,
            label = stringResource(R.string.preset_home),
            icon = Icons.Filled.AccountBalance,
            selected = selectedPreset == LoanPreset.HOME,
            testTag = "preset_home_chip",
            onClick = { onSelectPreset(LoanPreset.HOME) }
        )
        PresetItem(
            preset = LoanPreset.CAR,
            label = stringResource(R.string.preset_car),
            icon = Icons.Filled.DirectionsCar,
            selected = selectedPreset == LoanPreset.CAR,
            testTag = "preset_car_chip",
            onClick = { onSelectPreset(LoanPreset.CAR) }
        )
        PresetItem(
            preset = LoanPreset.PERSONAL,
            label = stringResource(R.string.preset_personal),
            icon = Icons.Filled.Wallet,
            selected = selectedPreset == LoanPreset.PERSONAL,
            testTag = "preset_personal_chip",
            onClick = { onSelectPreset(LoanPreset.PERSONAL) }
        )
        PresetItem(
            preset = LoanPreset.EDUCATION,
            label = stringResource(R.string.preset_education),
            icon = Icons.Filled.School,
            selected = selectedPreset == LoanPreset.EDUCATION,
            testTag = "preset_education_chip",
            onClick = { onSelectPreset(LoanPreset.EDUCATION) }
        )
    }
}

@Composable
private fun PresetItem(
    preset: LoanPreset,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = LocalEmiColors.current.cardBorder
        )
    )
}

@Composable
private fun HeroSummaryCard(
    result: EmiResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val emiColors = LocalEmiColors.current

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_summary_card")
    ) {
        Box(
            modifier = Modifier
                .heroAtmosphere(emiColors.heroStart, emiColors.heroEnd)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.monthly_emi),
                            style = MaterialTheme.typography.labelLarge,
                            color = emiColors.heroMuted
                        )
                        Text(
                            text = CurrencyFormatter.format(
                                amount = result.monthlyEmi,
                                currency = currency,
                                useDeviceLocale = useDeviceLocale
                            ),
                            style = MaterialTheme.typography.displaySmall.merge(TabularNumberStyle),
                            color = emiColors.heroOnSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("hero_monthly_emi_value")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroLegendDot(
                                color = emiColors.heroPrincipalAccent,
                                text = "Principal",
                                textColor = emiColors.heroOnSurface.copy(alpha = 0.88f)
                            )
                            HeroLegendDot(
                                color = emiColors.heroInterestAccent,
                                text = "Interest",
                                textColor = emiColors.heroOnSurface.copy(alpha = 0.88f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onSaveClick,
                                shape = RoundedCornerShape(10.dp),
                                color = emiColors.heroPillBg,
                                border = BorderStroke(1.dp, emiColors.heroPillBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_session_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkAdd,
                                        contentDescription = null,
                                        tint = emiColors.heroOnSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.save_session),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = emiColors.heroOnSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Surface(
                                onClick = onShareClick,
                                shape = RoundedCornerShape(10.dp),
                                color = emiColors.heroPillBg.copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, emiColors.heroPillBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_summary_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        tint = emiColors.heroOnSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.share_summary),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = emiColors.heroOnSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    EmiDonutChart(
                        principalRatio = result.principalRatio,
                        interestRatio = result.interestRatio,
                        size = 104.dp,
                        strokeWidth = 12.dp,
                        principalColor = emiColors.heroPrincipalAccent,
                        interestColor = emiColors.heroInterestAccent,
                        trackColor = emiColors.heroTrackColor,
                        centerTextColor = emiColors.heroOnSurface,
                        centerSubColor = emiColors.heroMuted
                    )
                }

                // 3-Column Metrics Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(emiColors.heroPillBg.copy(alpha = 0.65f))
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroMetricItem(
                        label = stringResource(R.string.principal_label),
                        value = CurrencyFormatter.formatCompact(result.principal, currency, useDeviceLocale),
                        color = emiColors.heroOnSurface,
                        mutedColor = emiColors.heroMuted,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricItem(
                        label = stringResource(R.string.interest_label),
                        value = CurrencyFormatter.formatCompact(result.effectiveTotalInterest, currency, useDeviceLocale),
                        color = emiColors.heroHighlightText,
                        mutedColor = emiColors.heroMuted,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricItem(
                        label = stringResource(R.string.total_payable_label),
                        value = CurrencyFormatter.formatCompact(result.effectiveTotalPayable, currency, useDeviceLocale),
                        color = emiColors.heroOnSurface,
                        mutedColor = emiColors.heroMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLegendDot(color: Color, text: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun HeroMetricItem(
    label: String,
    value: String,
    color: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoanInputsSection(
    uiState: EmiUiState,
    onPrincipalTextChange: (String) -> Unit,
    onPrincipalSliderChange: (Double) -> Unit,
    onNudgePrincipal: (Double) -> Unit,
    onToggleIncludeInterest: (Boolean) -> Unit,
    onRateTextChange: (String) -> Unit,
    onRateSliderChange: (Double) -> Unit,
    onNudgeRate: (Double) -> Unit,
    onTenureTextChange: (String) -> Unit,
    onTenureSliderChange: (Int) -> Unit,
    onSwitchTenureUnit: (TenureUnit) -> Unit
) {
    val emiColors = LocalEmiColors.current
    val preset = uiState.selectedPreset
    val maxPrincipal = preset.scaledMaxPrincipal(uiState.currency)
    val minPrincipal = preset.scaledMinPrincipal(uiState.currency)
    val sliderPrincipal = uiState.loanInput.principal.coerceIn(minPrincipal, maxPrincipal).toFloat()
    val principalNudge = if (uiState.currency == CurrencyOption.INR) 100_000.0 else 10_000.0

    // Card 1: Principal Amount
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.loan_amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = CurrencyFormatter.format(
                        amount = uiState.loanInput.principal,
                        currency = uiState.currency,
                        useDeviceLocale = uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputStrings.principalText,
                    onValueChange = onPrincipalTextChange,
                    singleLine = true,
                    prefix = {
                        Text(
                            text = uiState.currency.symbol,
                            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = emiColors.cardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("principal_input")
                )

                FilledTonalIconButton(
                    onClick = { onNudgePrincipal(-principalNudge) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("principal_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease loan amount"
                    )
                }
                FilledTonalIconButton(
                    onClick = { onNudgePrincipal(principalNudge) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("principal_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase loan amount"
                    )
                }
            }

            Slider(
                value = sliderPrincipal,
                onValueChange = { onPrincipalSliderChange(it.toDouble()) },
                valueRange = minPrincipal.toFloat()..maxPrincipal.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("principal_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatCompact(
                        minPrincipal,
                        uiState.currency,
                        uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatCompact(
                        maxPrincipal,
                        uiState.currency,
                        uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Card 2: Interest Rate with Interest Toggle
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Percent,
                        contentDescription = null,
                        tint = emiColors.interestColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.interest_rate),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (uiState.includeInterest) {
                            CurrencyFormatter.formatPercent(
                                percent = uiState.loanInput.annualRatePercent,
                                decimals = 2,
                                currency = uiState.currency,
                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                            )
                        } else {
                            stringResource(R.string.interest_toggle_free_badge)
                        },
                        style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                        color = if (uiState.includeInterest) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            emiColors.principalColor
                        }
                    )
                    Switch(
                        checked = uiState.includeInterest,
                        onCheckedChange = onToggleIncludeInterest,
                        modifier = Modifier.testTag("interest_toggle_switch")
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.includeInterest,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.inputStrings.rateText,
                            onValueChange = onRateTextChange,
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "% p.a.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            textStyle = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = emiColors.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rate_input")
                        )

                        FilledTonalIconButton(
                            onClick = { onNudgeRate(-0.25) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("rate_minus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Decrease interest rate"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { onNudgeRate(0.25) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("rate_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Increase interest rate"
                            )
                        }
                    }

                    Slider(
                        value = uiState.loanInput.annualRatePercent.coerceIn(0.5, 24.0).toFloat(),
                        onValueChange = { onRateSliderChange(it.toDouble()) },
                        valueRange = 0.5f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("rate_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = CurrencyFormatter.formatPercent(
                                percent = 0.5,
                                decimals = 1,
                                currency = uiState.currency,
                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatPercent(
                                percent = 24.0,
                                decimals = 0,
                                currency = uiState.currency,
                                useDeviceLocale = uiState.useDeviceLocaleSeparators
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!uiState.includeInterest) {
                Surface(
                    color = emiColors.positiveBadgeBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.interest_toggle_free_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = emiColors.positiveBadgeFg,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Card 3: Tenure (Years / Months)
    val isYears = uiState.loanInput.tenureUnit == TenureUnit.YEARS
    val maxSliderTenure = if (isYears) preset.maxYears.coerceAtLeast(30) else preset.maxYears.coerceAtLeast(30) * 12

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.loan_tenure),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = isYears,
                        onClick = { onSwitchTenureUnit(TenureUnit.YEARS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("tenure_years_toggle")
                    ) {
                        Text(
                            text = stringResource(R.string.years_unit),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    SegmentedButton(
                        selected = !isYears,
                        onClick = { onSwitchTenureUnit(TenureUnit.MONTHS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("tenure_months_toggle")
                    ) {
                        Text(
                            text = stringResource(R.string.months_unit),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.inputStrings.tenureText,
                onValueChange = onTenureTextChange,
                singleLine = true,
                suffix = {
                    Text(
                        text = if (isYears) {
                            "${uiState.loanInput.totalMonths} months"
                        } else {
                            CurrencyFormatter.formatTenureMonths(uiState.loanInput.totalMonths)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = emiColors.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tenure_input")
            )

            Slider(
                value = uiState.loanInput.tenureValue.coerceIn(1, maxSliderTenure).toFloat(),
                onValueChange = { onTenureSliderChange(it.toInt()) },
                valueRange = 1f..maxSliderTenure.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("tenure_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isYears) "1 Yr" else "1 Mo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isYears) "$maxSliderTenure Yrs" else "$maxSliderTenure Mos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val quickOptions = if (isYears) listOf(5, 10, 15, 20, 30) else listOf(6, 12, 24, 36, 60)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_tenure_row")
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickOptions.forEach { option ->
                    val isSelected = uiState.loanInput.tenureValue == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTenureSliderChange(option) },
                        label = {
                            Text(
                                text = if (isYears) "${option}Y" else "${option}M",
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_tenure_$option")
                    )
                }
            }
        }
    }
}

@Composable
private fun PrepaymentSimulatorCard(
    uiState: EmiUiState,
    onSelectPrepaymentFreq: (PrepaymentFrequency) -> Unit,
    onExtraPaymentTextChange: (String) -> Unit
) {
    val emiColors = LocalEmiColors.current
    val freq = uiState.loanInput.prepaymentFrequency
    val result = uiState.emiResult

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prepayment_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(emiColors.prepayColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = emiColors.prepayColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.prepayment_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.prepayment_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = freq == PrepaymentFrequency.NONE,
                    onClick = { onSelectPrepaymentFreq(PrepaymentFrequency.NONE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    modifier = Modifier.testTag("prepay_off_button")
                ) {
                    Text(
                        text = stringResource(R.string.prepayment_none),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
                SegmentedButton(
                    selected = freq == PrepaymentFrequency.MONTHLY,
                    onClick = { onSelectPrepaymentFreq(PrepaymentFrequency.MONTHLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    modifier = Modifier.testTag("prepay_monthly_button")
                ) {
                    Text(
                        text = stringResource(R.string.prepayment_monthly),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SegmentedButton(
                    selected = freq == PrepaymentFrequency.YEARLY,
                    onClick = { onSelectPrepaymentFreq(PrepaymentFrequency.YEARLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    modifier = Modifier.testTag("prepay_yearly_button")
                ) {
                    Text(
                        text = stringResource(R.string.prepayment_yearly),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = freq != PrepaymentFrequency.NONE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.inputStrings.extraPaymentText,
                        onValueChange = onExtraPaymentTextChange,
                        singleLine = true,
                        label = { Text(stringResource(R.string.extra_payment_amount)) },
                        prefix = {
                            Text(
                                text = uiState.currency.symbol,
                                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                color = emiColors.prepayColor
                            )
                        },
                        textStyle = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = emiColors.prepayColor,
                            unfocusedBorderColor = emiColors.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_payment_input")
                    )

                    val quickPrepayOptions = remember(uiState.currency, freq) {
                        val base = if (uiState.currency == CurrencyOption.INR) {
                            listOf(2000, 5000, 10000, 25000)
                        } else {
                            listOf(100, 250, 500, 1000)
                        }
                        if (freq == PrepaymentFrequency.YEARLY) base.map { it * 10 } else base
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_prepay_row")
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrepayOptions.forEach { option ->
                            val selected = kotlin.math.abs(uiState.loanInput.extraPayment - option.toDouble()) < 0.5
                            FilterChip(
                                selected = selected,
                                onClick = { onExtraPaymentTextChange(option.toString()) },
                                label = {
                                    Text(
                                        text = CurrencyFormatter.formatCompact(
                                            amount = option.toDouble(),
                                            currency = uiState.currency,
                                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                                        ),
                                        style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = emiColors.prepayColor.copy(alpha = 0.16f),
                                    selectedLabelColor = emiColors.prepayColor
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("quick_prepay_$option")
                            )
                        }
                    }

                    Surface(
                        color = emiColors.positiveBadgeBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.interest_saved),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = emiColors.positiveBadgeFg.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = CurrencyFormatter.format(
                                        amount = result.interestSaved,
                                        currency = uiState.currency,
                                        useDeviceLocale = uiState.useDeviceLocaleSeparators
                                    ),
                                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                    color = emiColors.positiveBadgeFg,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("interest_saved_value")
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = stringResource(R.string.tenure_reduced),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = emiColors.positiveBadgeFg.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = if (result.monthsSaved > 0) {
                                        "${result.monthsSaved} mos (${CurrencyFormatter.formatTenureMonths(result.effectiveMonths)})"
                                    } else {
                                        "0 mos"
                                    },
                                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                    color = emiColors.positiveBadgeFg,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("tenure_saved_value")
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
private fun LoanBreakdownPieCard(
    result: EmiResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current
    var selectedSlice by remember { mutableStateOf<PieSlice?>(null) }

    val principalPct = (result.principalRatio * 100.0).coerceIn(0.0, 100.0)
    val interestPct = (result.interestRatio * 100.0).coerceIn(0.0, 100.0)
    val tenureLabel = CurrencyFormatter.formatTenureMonths(result.effectiveMonths)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_breakdown_pie_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.loan_breakdown_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (result.principal > 0.0) {
                            stringResource(R.string.loan_breakdown_subtitle, tenureLabel)
                        } else {
                            stringResource(R.string.loan_breakdown_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (result.principal > 0.0) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackVertically = maxWidth < 330.dp
                    if (stackVertically) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LoanBreakdownPieChart(
                                principalRatio = result.principalRatio,
                                interestRatio = result.interestRatio,
                                selectedSlice = selectedSlice,
                                onSelectSlice = { selectedSlice = it },
                                size = 148.dp,
                                modifier = Modifier.testTag("loan_breakdown_pie_chart")
                            )
                            PieBreakdownLegendColumn(
                                principalAmount = CurrencyFormatter.format(
                                    amount = result.principal,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                interestAmount = CurrencyFormatter.format(
                                    amount = result.effectiveTotalInterest,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                principalPercent = CurrencyFormatter.formatPercent(
                                    percent = principalPct,
                                    decimals = 1,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                interestPercent = CurrencyFormatter.formatPercent(
                                    percent = interestPct,
                                    decimals = 1,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                selectedSlice = selectedSlice,
                                onSelectSlice = { selectedSlice = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LoanBreakdownPieChart(
                                principalRatio = result.principalRatio,
                                interestRatio = result.interestRatio,
                                selectedSlice = selectedSlice,
                                onSelectSlice = { selectedSlice = it },
                                size = 144.dp,
                                modifier = Modifier.testTag("loan_breakdown_pie_chart")
                            )
                            PieBreakdownLegendColumn(
                                principalAmount = CurrencyFormatter.format(
                                    amount = result.principal,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                interestAmount = CurrencyFormatter.format(
                                    amount = result.effectiveTotalInterest,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                principalPercent = CurrencyFormatter.formatPercent(
                                    percent = principalPct,
                                    decimals = 1,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                interestPercent = CurrencyFormatter.formatPercent(
                                    percent = interestPct,
                                    decimals = 1,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                selectedSlice = selectedSlice,
                                onSelectSlice = { selectedSlice = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = emiColors.cardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.total_payable_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (result.interestSaved > 0.01) {
                            Surface(
                                color = emiColors.positiveBadgeBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Saved ${CurrencyFormatter.formatCompact(result.interestSaved, currency, useDeviceLocale)}",
                                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                    color = emiColors.positiveBadgeFg,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = CurrencyFormatter.format(
                            amount = result.effectiveTotalPayable,
                            currency = currency,
                            useDeviceLocale = useDeviceLocale
                        ),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("pie_total_payable_value")
                    )
                }
            }
        }
    }
}

@Composable
private fun PieBreakdownLegendColumn(
    principalAmount: String,
    interestAmount: String,
    principalPercent: String,
    interestPercent: String,
    selectedSlice: PieSlice?,
    onSelectSlice: (PieSlice?) -> Unit,
    modifier: Modifier = Modifier
) {
    val emiColors = LocalEmiColors.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PieLegendItemCard(
            dotColor = emiColors.principalColor,
            label = stringResource(R.string.loan_breakdown_principal_paid),
            amount = principalAmount,
            percent = principalPercent,
            selected = selectedSlice == PieSlice.PRINCIPAL,
            onClick = {
                onSelectSlice(if (selectedSlice == PieSlice.PRINCIPAL) null else PieSlice.PRINCIPAL)
            },
            modifier = Modifier.testTag("pie_principal_legend")
        )
        PieLegendItemCard(
            dotColor = emiColors.interestColor,
            label = stringResource(R.string.loan_breakdown_interest_paid),
            amount = interestAmount,
            percent = interestPercent,
            selected = selectedSlice == PieSlice.INTEREST,
            onClick = {
                onSelectSlice(if (selectedSlice == PieSlice.INTEREST) null else PieSlice.INTEREST)
            },
            modifier = Modifier.testTag("pie_interest_legend")
        )
    }
}

@Composable
private fun PieLegendItemCard(
    dotColor: Color,
    label: String,
    amount: String,
    percent: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emiColors = LocalEmiColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            dotColor.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) dotColor else emiColors.cardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = percent,
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
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

private fun buildLoanShareText(uiState: EmiUiState): String {
    val currency = uiState.currency
    val useLocale = uiState.useDeviceLocaleSeparators
    val res = uiState.emiResult
    val input = uiState.loanInput
    val presetName = when (uiState.selectedPreset) {
        LoanPreset.HOME -> "Home Loan"
        LoanPreset.CAR -> "Car Loan"
        LoanPreset.PERSONAL -> "Personal Loan"
        LoanPreset.EDUCATION -> "Education Loan"
    }
    val rateLabel = if (uiState.includeInterest && input.annualRatePercent > 0.0) {
        "${CurrencyFormatter.formatPercent(input.annualRatePercent, 2, currency, useLocale)} p.a."
    } else {
        "0% (Interest-Free)"
    }
    return buildString {
        appendLine("EMI Pulse — $presetName Quote")
        appendLine("Monthly EMI: ${CurrencyFormatter.format(res.monthlyEmi, currency, useDeviceLocale = useLocale)}")
        appendLine("Loan Amount: ${CurrencyFormatter.format(res.principal, currency, useDeviceLocale = useLocale)}")
        appendLine("Interest Rate: $rateLabel")
        appendLine("Tenure: ${CurrencyFormatter.formatTenureMonths(res.effectiveMonths)}")
        appendLine("Total Interest: ${CurrencyFormatter.format(res.effectiveTotalInterest, currency, useDeviceLocale = useLocale)}")
        appendLine("Total Payable: ${CurrencyFormatter.format(res.effectiveTotalPayable, currency, useDeviceLocale = useLocale)}")
        if (res.interestSaved > 0.01) {
            appendLine("Prepayment Savings: ${CurrencyFormatter.format(res.interestSaved, currency, useDeviceLocale = useLocale)} (${res.monthsSaved} mos saved)")
        }
    }.trim()
}
