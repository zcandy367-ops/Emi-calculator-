package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CurrencyOption
import com.example.domain.DtiPreset
import com.example.domain.DtiStatus
import com.example.domain.EligibilityResult
import com.example.domain.TenureUnit
import com.example.ui.EmiUiState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.heroAtmosphere

@Composable
fun EligibilityScreen(
    uiState: EmiUiState,
    onIncomeTextChange: (String) -> Unit,
    onIncomeSliderChange: (Double) -> Unit,
    onNudgeIncome: (Double) -> Unit,
    onLiabilitiesTextChange: (String) -> Unit,
    onLiabilitiesSliderChange: (Double) -> Unit,
    onNudgeLiabilities: (Double) -> Unit,
    onSelectDtiPreset: (DtiPreset) -> Unit,
    onDtiSliderChange: (Double) -> Unit,
    onToggleInterest: (Boolean) -> Unit,
    onRateTextChange: (String) -> Unit,
    onRateSliderChange: (Double) -> Unit,
    onNudgeRate: (Double) -> Unit,
    onTenureTextChange: (String) -> Unit,
    onTenureSliderChange: (Int) -> Unit,
    onSwitchTenureUnit: (TenureUnit) -> Unit,
    onApplyToCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareEligibilityTitle = stringResource(R.string.share_eligibility_chooser)
    val shareEligibilitySummary: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "EMI Pulse Loan Eligibility")
            putExtra(Intent.EXTRA_TEXT, buildEligibilityShareText(uiState))
        }
        context.startActivity(Intent.createChooser(shareIntent, shareEligibilityTitle))
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
                        EligibilityHeroCard(
                            result = uiState.eligibilityResult,
                            currency = uiState.currency,
                            useDeviceLocale = uiState.useDeviceLocaleSeparators,
                            onApplyToCalculator = onApplyToCalculator,
                            onShareClick = shareEligibilitySummary
                        )
                        DtiIncomeAllocationCard(
                            result = uiState.eligibilityResult,
                            currency = uiState.currency,
                            useDeviceLocale = uiState.useDeviceLocaleSeparators
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        EligibilityInputsSection(
                            uiState = uiState,
                            onIncomeTextChange = onIncomeTextChange,
                            onIncomeSliderChange = onIncomeSliderChange,
                            onNudgeIncome = onNudgeIncome,
                            onLiabilitiesTextChange = onLiabilitiesTextChange,
                            onLiabilitiesSliderChange = onLiabilitiesSliderChange,
                            onNudgeLiabilities = onNudgeLiabilities,
                            onSelectDtiPreset = onSelectDtiPreset,
                            onDtiSliderChange = onDtiSliderChange,
                            onToggleInterest = onToggleInterest,
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
                EligibilityHeroCard(
                    result = uiState.eligibilityResult,
                    currency = uiState.currency,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators,
                    onApplyToCalculator = onApplyToCalculator,
                    onShareClick = shareEligibilitySummary
                )
                DtiIncomeAllocationCard(
                    result = uiState.eligibilityResult,
                    currency = uiState.currency,
                    useDeviceLocale = uiState.useDeviceLocaleSeparators
                )
                EligibilityInputsSection(
                    uiState = uiState,
                    onIncomeTextChange = onIncomeTextChange,
                    onIncomeSliderChange = onIncomeSliderChange,
                    onNudgeIncome = onNudgeIncome,
                    onLiabilitiesTextChange = onLiabilitiesTextChange,
                    onLiabilitiesSliderChange = onLiabilitiesSliderChange,
                    onNudgeLiabilities = onNudgeLiabilities,
                    onSelectDtiPreset = onSelectDtiPreset,
                    onDtiSliderChange = onDtiSliderChange,
                    onToggleInterest = onToggleInterest,
                    onRateTextChange = onRateTextChange,
                    onRateSliderChange = onRateSliderChange,
                    onNudgeRate = onNudgeRate,
                    onTenureTextChange = onTenureTextChange,
                    onTenureSliderChange = onTenureSliderChange,
                    onSwitchTenureUnit = onSwitchTenureUnit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EligibilityHeroCard(
    result: EligibilityResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean,
    onApplyToCalculator: () -> Unit,
    onShareClick: () -> Unit
) {
    val emiColors = LocalEmiColors.current

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eligibility_hero_card")
    ) {
        Box(
            modifier = Modifier
                .heroAtmosphere(emiColors.heroStart, emiColors.heroEnd)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.eligibility_max_loan),
                            style = MaterialTheme.typography.labelLarge,
                            color = emiColors.heroMuted
                        )
                        Text(
                            text = CurrencyFormatter.format(
                                amount = result.maxLoanAmount,
                                currency = currency,
                                useDeviceLocale = useDeviceLocale
                            ),
                            style = MaterialTheme.typography.displaySmall.merge(TabularNumberStyle),
                            color = emiColors.heroOnSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("eligibility_max_loan_value")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(emiColors.heroPillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (result.status == DtiStatus.EXCEEDED) {
                                Icons.Filled.WarningAmber
                            } else {
                                Icons.Filled.VerifiedUser
                            },
                            contentDescription = null,
                            tint = if (result.status == DtiStatus.EXCEEDED) {
                                emiColors.heroInterestAccent
                            } else {
                                emiColors.heroPrincipalAccent
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
                    EligibilityMetricItem(
                        label = stringResource(R.string.eligibility_affordable_emi),
                        value = CurrencyFormatter.formatCompact(
                            result.availableMonthlyEmi,
                            currency,
                            useDeviceLocale
                        ),
                        color = emiColors.heroPrincipalAccent,
                        mutedColor = emiColors.heroMuted,
                        modifier = Modifier.weight(1f)
                    )
                    EligibilityMetricItem(
                        label = stringResource(R.string.eligibility_dti_cap),
                        value = CurrencyFormatter.formatPercent(
                            result.maxDtiPercent,
                            0,
                            currency,
                            useDeviceLocale
                        ),
                        color = emiColors.heroOnSurface,
                        mutedColor = emiColors.heroMuted,
                        modifier = Modifier.weight(1f)
                    )
                    EligibilityMetricItem(
                        label = stringResource(R.string.eligibility_post_dti),
                        value = CurrencyFormatter.formatPercent(
                            result.postLoanDtiPercent,
                            1,
                            currency,
                            useDeviceLocale
                        ),
                        color = if (result.status == DtiStatus.EXCEEDED) {
                            emiColors.heroHighlightText
                        } else {
                            emiColors.heroOnSurface
                        },
                        mutedColor = emiColors.heroMuted,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (result.maxLoanAmount > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.eligibility_total_interest_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = emiColors.heroMuted
                            )
                            Text(
                                text = CurrencyFormatter.format(
                                    amount = result.totalInterest,
                                    currency = currency,
                                    useDeviceLocale = useDeviceLocale
                                ),
                                style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                                color = emiColors.heroHighlightText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onShareClick,
                                shape = RoundedCornerShape(10.dp),
                                color = emiColors.heroPillBg.copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, emiColors.heroPillBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("eligibility_share_button")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = stringResource(R.string.share_summary),
                                        tint = emiColors.heroOnSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = onApplyToCalculator,
                                shape = RoundedCornerShape(10.dp),
                                color = emiColors.heroPillBg,
                                border = BorderStroke(1.dp, emiColors.heroPillBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("eligibility_apply_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.eligibility_apply_cta),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = emiColors.heroOnSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = emiColors.heroOnSurface,
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
}

@Composable
private fun EligibilityMetricItem(
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
private fun DtiIncomeAllocationCard(
    result: EligibilityResult,
    currency: CurrencyOption,
    useDeviceLocale: Boolean
) {
    val emiColors = LocalEmiColors.current
    val totalIncome = result.monthlyIncome.coerceAtLeast(1.0)
    val debtFraction = (result.existingLiabilities / totalIncome).toFloat().coerceIn(0f, 1f)
    val availableFraction = (result.availableMonthlyEmi / totalIncome).toFloat()
        .coerceIn(0f, (1f - debtFraction).coerceAtLeast(0f))

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, emiColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eligibility_dti_meter_card")
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
                Text(
                    text = stringResource(R.string.eligibility_dti_meter_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = CurrencyFormatter.formatCompact(
                        result.monthlyIncome,
                        currency,
                        useDeviceLocale
                    ),
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Stacked income allocation bar
            val remainingFraction = (1f - debtFraction - availableFraction).coerceAtLeast(0f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (debtFraction > 0.001f) {
                    Box(
                        modifier = Modifier
                            .weight(debtFraction)
                            .fillMaxHeight()
                            .background(emiColors.interestColor)
                    )
                }
                if (availableFraction > 0.001f) {
                    Box(
                        modifier = Modifier
                            .weight(availableFraction)
                            .fillMaxHeight()
                            .background(emiColors.principalColor)
                    )
                }
                if (remainingFraction > 0.001f) {
                    Spacer(modifier = Modifier.weight(remainingFraction))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DtiLegendChip(
                    color = emiColors.interestColor,
                    label = stringResource(R.string.eligibility_existing_debt),
                    value = CurrencyFormatter.formatCompact(
                        result.existingLiabilities,
                        currency,
                        useDeviceLocale
                    )
                )
                DtiLegendChip(
                    color = emiColors.principalColor,
                    label = stringResource(R.string.eligibility_new_emi),
                    value = CurrencyFormatter.formatCompact(
                        result.availableMonthlyEmi,
                        currency,
                        useDeviceLocale
                    )
                )
                DtiLegendChip(
                    color = MaterialTheme.colorScheme.outline,
                    label = stringResource(R.string.eligibility_free_income),
                    value = CurrencyFormatter.formatCompact(
                        result.remainingIncome,
                        currency,
                        useDeviceLocale
                    )
                )
            }

            if (result.status == DtiStatus.EXCEEDED && result.monthlyIncome > 0.0) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.eligibility_exceeded_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DtiLegendChip(
    color: Color,
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EligibilityInputsSection(
    uiState: EmiUiState,
    onIncomeTextChange: (String) -> Unit,
    onIncomeSliderChange: (Double) -> Unit,
    onNudgeIncome: (Double) -> Unit,
    onLiabilitiesTextChange: (String) -> Unit,
    onLiabilitiesSliderChange: (Double) -> Unit,
    onNudgeLiabilities: (Double) -> Unit,
    onSelectDtiPreset: (DtiPreset) -> Unit,
    onDtiSliderChange: (Double) -> Unit,
    onToggleInterest: (Boolean) -> Unit,
    onRateTextChange: (String) -> Unit,
    onRateSliderChange: (Double) -> Unit,
    onNudgeRate: (Double) -> Unit,
    onTenureTextChange: (String) -> Unit,
    onTenureSliderChange: (Int) -> Unit,
    onSwitchTenureUnit: (TenureUnit) -> Unit
) {
    val emiColors = LocalEmiColors.current
    val scaleFactor = if (uiState.currency == CurrencyOption.INR) 20.0 else 1.0
    val minIncome = 1_000.0 * scaleFactor
    val maxIncome = 50_000.0 * scaleFactor
    val incomeNudge = 500.0 * scaleFactor

    val maxLiabilities = (uiState.eligibilityInput.monthlyIncome.coerceAtLeast(minIncome))
    val liabilityNudge = 200.0 * scaleFactor

    // Card 1: Monthly Income
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
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.eligibility_monthly_income),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = CurrencyFormatter.format(
                        uiState.eligibilityInput.monthlyIncome,
                        uiState.currency,
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
                    value = uiState.eligibilityStrings.incomeText,
                    onValueChange = onIncomeTextChange,
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
                        .testTag("eligibility_income_input")
                )
                FilledTonalIconButton(
                    onClick = { onNudgeIncome(-incomeNudge) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("eligibility_income_minus")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease monthly income")
                }
                FilledTonalIconButton(
                    onClick = { onNudgeIncome(incomeNudge) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("eligibility_income_plus")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase monthly income")
                }
            }

            Slider(
                value = uiState.eligibilityInput.monthlyIncome.coerceIn(minIncome, maxIncome).toFloat(),
                onValueChange = { onIncomeSliderChange(it.toDouble()) },
                valueRange = minIncome.toFloat()..maxIncome.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("eligibility_income_slider")
            )
        }
    }

    // Card 2: Existing Liabilities
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
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = emiColors.interestColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.eligibility_liabilities),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = CurrencyFormatter.format(
                        uiState.eligibilityInput.existingLiabilities,
                        uiState.currency,
                        useDeviceLocale = uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.eligibilityStrings.liabilitiesText,
                    onValueChange = onLiabilitiesTextChange,
                    singleLine = true,
                    prefix = {
                        Text(
                            text = uiState.currency.symbol,
                            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                            color = emiColors.interestColor
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
                        .testTag("eligibility_liabilities_input")
                )
                FilledTonalIconButton(
                    onClick = { onNudgeLiabilities(-liabilityNudge) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("eligibility_liabilities_minus")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease existing liabilities")
                }
                FilledTonalIconButton(
                    onClick = { onNudgeLiabilities(liabilityNudge) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("eligibility_liabilities_plus")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase existing liabilities")
                }
            }

            Slider(
                value = uiState.eligibilityInput.existingLiabilities
                    .coerceIn(0.0, maxLiabilities)
                    .toFloat(),
                onValueChange = { onLiabilitiesSliderChange(it.toDouble()) },
                valueRange = 0f..maxLiabilities.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.testTag("eligibility_liabilities_slider")
            )
        }
    }

    // Card 3: Standard Debt-to-Income (DTI) Ratio
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
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.eligibility_dti_ratio_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = CurrencyFormatter.formatPercent(
                        uiState.eligibilityInput.maxDtiPercent,
                        0,
                        uiState.currency,
                        uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    DtiPreset.CONSERVATIVE to stringResource(R.string.eligibility_dti_36),
                    DtiPreset.STANDARD to stringResource(R.string.eligibility_dti_43),
                    DtiPreset.FLEXIBLE to stringResource(R.string.eligibility_dti_50)
                )
                presets.forEach { (preset, label) ->
                    val isSelected = kotlin.math.abs(
                        uiState.eligibilityInput.maxDtiPercent - preset.dtiPercent
                    ) < 0.5
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDtiPreset(preset) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("dti_preset_${preset.dtiPercent.toInt()}")
                    )
                }
            }

            Slider(
                value = uiState.eligibilityInput.maxDtiPercent.coerceIn(20.0, 60.0).toFloat(),
                onValueChange = { onDtiSliderChange(it.toDouble()) },
                valueRange = 20f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("eligibility_dti_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatPercent(
                        20.0,
                        0,
                        uiState.currency,
                        uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatPercent(
                        60.0,
                        0,
                        uiState.currency,
                        uiState.useDeviceLocaleSeparators
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Card 4: Loan Tenure & Interest Toggle
    val isYears = uiState.eligibilityInput.tenureUnit == TenureUnit.YEARS
    val maxTenure = if (isYears) 30 else 360

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
                        text = stringResource(R.string.eligibility_terms_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (uiState.eligibilityIncludeInterest) {
                            CurrencyFormatter.formatPercent(
                                uiState.eligibilityInput.annualRatePercent,
                                2,
                                uiState.currency,
                                uiState.useDeviceLocaleSeparators
                            )
                        } else {
                            stringResource(R.string.interest_toggle_free_badge)
                        },
                        style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                        color = if (uiState.eligibilityIncludeInterest) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            emiColors.principalColor
                        }
                    )
                    Switch(
                        checked = uiState.eligibilityIncludeInterest,
                        onCheckedChange = onToggleInterest,
                        modifier = Modifier.testTag("eligibility_interest_toggle_switch")
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.eligibilityIncludeInterest,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.eligibilityStrings.rateText,
                            onValueChange = onRateTextChange,
                            singleLine = true,
                            label = { Text(stringResource(R.string.interest_rate)) },
                            suffix = { Text("% p.a.") },
                            textStyle = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = emiColors.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eligibility_rate_input")
                        )
                        FilledTonalIconButton(
                            onClick = { onNudgeRate(-0.25) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease rate")
                        }
                        FilledTonalIconButton(
                            onClick = { onNudgeRate(0.25) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase rate")
                        }
                    }

                    Slider(
                        value = uiState.eligibilityInput.annualRatePercent.coerceIn(0.5, 24.0).toFloat(),
                        onValueChange = { onRateSliderChange(it.toDouble()) },
                        valueRange = 0.5f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.testTag("eligibility_rate_slider")
                    )
                }
            }

            HorizontalDivider(color = emiColors.cardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.loan_tenure),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = isYears,
                        onClick = { onSwitchTenureUnit(TenureUnit.YEARS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.years_unit), style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = !isYears,
                        onClick = { onSwitchTenureUnit(TenureUnit.MONTHS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.months_unit), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            OutlinedTextField(
                value = uiState.eligibilityStrings.tenureText,
                onValueChange = onTenureTextChange,
                singleLine = true,
                suffix = {
                    Text(
                        text = if (isYears) {
                            "${uiState.eligibilityInput.totalMonths} months"
                        } else {
                            CurrencyFormatter.formatTenureMonths(uiState.eligibilityInput.totalMonths)
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
                    .testTag("eligibility_tenure_input")
            )

            Slider(
                value = uiState.eligibilityInput.tenureValue.coerceIn(1, maxTenure).toFloat(),
                onValueChange = { onTenureSliderChange(it.toInt()) },
                valueRange = 1f..maxTenure.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("eligibility_tenure_slider")
            )

            val quickTenures = if (isYears) listOf(5, 10, 15, 20, 30) else listOf(6, 12, 24, 36, 60)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTenures.forEach { option ->
                    val selected = uiState.eligibilityInput.tenureValue == option
                    FilterChip(
                        selected = selected,
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
                            .testTag("eligibility_quick_tenure_$option")
                    )
                }
            }
        }
    }
}

private fun buildEligibilityShareText(uiState: EmiUiState): String {
    val currency = uiState.currency
    val useLocale = uiState.useDeviceLocaleSeparators
    val res = uiState.eligibilityResult
    val inp = uiState.eligibilityInput
    val rateLabel = if (uiState.eligibilityIncludeInterest && inp.annualRatePercent > 0.0) {
        "${CurrencyFormatter.formatPercent(inp.annualRatePercent, 2, currency, useLocale)} p.a."
    } else {
        "0% (Interest-Free)"
    }
    return buildString {
        appendLine("EMI Pulse — Loan Eligibility Estimate")
        appendLine("Max Affordable Loan: ${CurrencyFormatter.format(res.maxLoanAmount, currency, useDeviceLocale = useLocale)}")
        appendLine("Max New Monthly EMI: ${CurrencyFormatter.format(res.availableMonthlyEmi, currency, useDeviceLocale = useLocale)}")
        appendLine("Monthly Income: ${CurrencyFormatter.format(res.monthlyIncome, currency, useDeviceLocale = useLocale)}")
        appendLine("Existing Liabilities: ${CurrencyFormatter.format(res.existingLiabilities, currency, useDeviceLocale = useLocale)}")
        appendLine("DTI Limit: ${CurrencyFormatter.formatPercent(res.maxDtiPercent, 0, currency, useLocale)} (Post-Loan: ${CurrencyFormatter.formatPercent(res.postLoanDtiPercent, 1, currency, useLocale)})")
        appendLine("Terms: $rateLabel over ${CurrencyFormatter.formatTenureMonths(inp.totalMonths)}")
    }.trim()
}
