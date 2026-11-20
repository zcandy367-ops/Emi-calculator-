package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import com.example.data.local.SavedEmiSessionEntity
import com.example.data.local.SavedSessionRepository
import com.example.domain.ComparisonResult
import com.example.domain.CurrencyOption
import com.example.domain.DtiPreset
import com.example.domain.EligibilityInput
import com.example.domain.EligibilityResult
import com.example.domain.EmiCalculatorEngine
import com.example.domain.EmiResult
import com.example.domain.LoanInput
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.ui.components.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScheduleGranularity {
    YEARLY,
    MONTHLY
}

data class LoanFieldStrings(
    val principalText: String,
    val rateText: String,
    val tenureText: String,
    val extraPaymentText: String = "0",
    val feeText: String = "0.5"
)

data class EligibilityFieldStrings(
    val incomeText: String = "8500",
    val liabilitiesText: String = "1200",
    val rateText: String = "7.5",
    val tenureText: String = "20"
)

data class EmiUiState(
    val currency: CurrencyOption = CurrencyOption.USD,
    val useDeviceLocaleSeparators: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedPreset: LoanPreset = LoanPreset.HOME,
    val includeInterest: Boolean = true,
    val lastNonZeroRatePercent: Double = 7.5,
    val loanInput: LoanInput = LoanInput(),
    val inputStrings: LoanFieldStrings = LoanFieldStrings(
        principalText = "250000",
        rateText = "7.5",
        tenureText = "20",
        extraPaymentText = "0"
    ),
    val emiResult: EmiResult = EmiCalculatorEngine.calculate(LoanInput()),
    val scheduleGranularity: ScheduleGranularity = ScheduleGranularity.YEARLY,
    val loanA: LoanInput = LoanInput(
        principal = 250000.0,
        annualRatePercent = 7.25,
        tenureValue = 20,
        tenureUnit = TenureUnit.YEARS,
        processingFeePercent = 0.5
    ),
    val loanAStrings: LoanFieldStrings = LoanFieldStrings(
        principalText = "250000",
        rateText = "7.25",
        tenureText = "20",
        feeText = "0.5"
    ),
    val loanB: LoanInput = LoanInput(
        principal = 250000.0,
        annualRatePercent = 7.75,
        tenureValue = 15,
        tenureUnit = TenureUnit.YEARS,
        processingFeePercent = 0.25
    ),
    val loanBStrings: LoanFieldStrings = LoanFieldStrings(
        principalText = "250000",
        rateText = "7.75",
        tenureText = "15",
        feeText = "0.25"
    ),
    val comparisonResult: ComparisonResult = EmiCalculatorEngine.compare(
        LoanInput(principal = 250000.0, annualRatePercent = 7.25, tenureValue = 20, processingFeePercent = 0.5),
        LoanInput(principal = 250000.0, annualRatePercent = 7.75, tenureValue = 15, processingFeePercent = 0.25)
    ),
    val loanAIncludeInterest: Boolean = true,
    val loanASavedRatePercent: Double = 7.25,
    val loanBIncludeInterest: Boolean = true,
    val loanBSavedRatePercent: Double = 7.75,
    val eligibilityIncludeInterest: Boolean = true,
    val eligibilitySavedRatePercent: Double = 7.5,
    val eligibilityInput: EligibilityInput = EligibilityInput(),
    val eligibilityStrings: EligibilityFieldStrings = EligibilityFieldStrings(),
    val eligibilityResult: EligibilityResult = EmiCalculatorEngine.calculateEligibility(EligibilityInput())
)

class EmiViewModel(
    private val prefsRepo: UserPreferencesRepository,
    private val savedSessionRepo: SavedSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmiUiState())
    val uiState: StateFlow<EmiUiState> = _uiState.asStateFlow()

    val savedSessions: StateFlow<List<SavedEmiSessionEntity>> = savedSessionRepo.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var hasAppliedInitialCurrencyScale = false

    init {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    if (!hasAppliedInitialCurrencyScale) {
                        hasAppliedInitialCurrencyScale = true
                        if (prefs.currency == CurrencyOption.INR && current.currency != CurrencyOption.INR) {
                            val preset = current.selectedPreset
                            val scaledPrincipal = preset.scaledDefaultPrincipal(prefs.currency)
                            val updatedInput = current.loanInput.copy(principal = scaledPrincipal)
                            val updatedLoanA = current.loanA.copy(principal = scaledPrincipal)
                            val updatedLoanB = current.loanB.copy(principal = scaledPrincipal)
                            val scaledIncome = current.eligibilityInput.monthlyIncome * 20.0
                            val scaledLiabilities = current.eligibilityInput.existingLiabilities * 20.0
                            val updatedEligibility = current.eligibilityInput.copy(
                                monthlyIncome = scaledIncome,
                                existingLiabilities = scaledLiabilities
                            )
                            return@update current.copy(
                                currency = prefs.currency,
                                useDeviceLocaleSeparators = prefs.useDeviceLocaleSeparators,
                                themeMode = prefs.themeMode,
                                loanInput = updatedInput,
                                inputStrings = current.inputStrings.copy(
                                    principalText = CurrencyFormatter.formatPlainNumber(scaledPrincipal)
                                ),
                                emiResult = EmiCalculatorEngine.calculate(updatedInput),
                                loanA = updatedLoanA,
                                loanAStrings = current.loanAStrings.copy(
                                    principalText = CurrencyFormatter.formatPlainNumber(scaledPrincipal)
                                ),
                                loanB = updatedLoanB,
                                loanBStrings = current.loanBStrings.copy(
                                    principalText = CurrencyFormatter.formatPlainNumber(scaledPrincipal)
                                ),
                                comparisonResult = EmiCalculatorEngine.compare(updatedLoanA, updatedLoanB),
                                eligibilityInput = updatedEligibility,
                                eligibilityStrings = current.eligibilityStrings.copy(
                                    incomeText = CurrencyFormatter.formatPlainNumber(scaledIncome),
                                    liabilitiesText = CurrencyFormatter.formatPlainNumber(scaledLiabilities)
                                ),
                                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updatedEligibility)
                            )
                        }
                    }
                    current.copy(
                        currency = prefs.currency,
                        useDeviceLocaleSeparators = prefs.useDeviceLocaleSeparators,
                        themeMode = prefs.themeMode
                    )
                }
            }
        }
    }

    fun selectCurrency(currency: CurrencyOption) {
        viewModelScope.launch {
            prefsRepo.setCurrency(currency)
        }
        _uiState.update { it.copy(currency = currency) }
    }

    fun setUseDeviceLocaleSeparators(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setUseDeviceLocaleSeparators(enabled)
        }
        _uiState.update { it.copy(useDeviceLocaleSeparators = enabled) }
    }

    fun toggleLightDark(isCurrentlyDark: Boolean) {
        val next = if (isCurrentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    fun cycleThemeMode() {
        val next = when (_uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun selectPreset(preset: LoanPreset) {
        _uiState.update { current ->
            val principal = preset.scaledDefaultPrincipal(current.currency)
            val tenureValue = if (current.loanInput.tenureUnit == TenureUnit.YEARS) {
                preset.defaultYears
            } else {
                preset.defaultYears * 12
            }
            val updated = current.loanInput.copy(
                principal = principal,
                annualRatePercent = preset.defaultRatePercent,
                tenureValue = tenureValue
            )
            current.copy(
                selectedPreset = preset,
                includeInterest = true,
                lastNonZeroRatePercent = preset.defaultRatePercent,
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    principalText = CurrencyFormatter.formatPlainNumber(principal),
                    rateText = CurrencyFormatter.formatPlainNumber(preset.defaultRatePercent, 2),
                    tenureText = tenureValue.toString()
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun toggleIncludeInterest(enabled: Boolean) {
        _uiState.update { current ->
            if (enabled == current.includeInterest) return@update current
            val targetRate = if (enabled) {
                current.lastNonZeroRatePercent.takeIf { it > 0.0 } ?: current.selectedPreset.defaultRatePercent
            } else {
                0.0
            }
            val savedRate = if (!enabled && current.loanInput.annualRatePercent > 0.0) {
                current.loanInput.annualRatePercent
            } else {
                current.lastNonZeroRatePercent
            }
            val updated = current.loanInput.copy(annualRatePercent = targetRate)
            current.copy(
                includeInterest = enabled,
                lastNonZeroRatePercent = savedRate,
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    rateText = CurrencyFormatter.formatPlainNumber(targetRate, 2)
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updatePrincipalText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = false)
        val parsed = sanitized.toDoubleOrNull()
        _uiState.update { current ->
            val newPrincipal = parsed?.coerceIn(0.0, 1_000_000_000.0) ?: 0.0
            val updated = current.loanInput.copy(principal = newPrincipal)
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(principalText = sanitized),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updatePrincipalFromSlider(value: Double) {
        _uiState.update { current ->
            val step = if (current.currency == CurrencyOption.INR) 10_000.0 else 1_000.0
            val snapped = (kotlin.math.round(value / step) * step).coerceAtLeast(0.0)
            val updated = current.loanInput.copy(principal = snapped)
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    principalText = CurrencyFormatter.formatPlainNumber(snapped)
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun nudgePrincipal(delta: Double) {
        _uiState.update { current ->
            val next = (current.loanInput.principal + delta).coerceIn(0.0, 1_000_000_000.0)
            val updated = current.loanInput.copy(principal = next)
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    principalText = CurrencyFormatter.formatPlainNumber(next)
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updateRateText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = true)
        val parsed = sanitized.toDoubleOrNull()
        _uiState.update { current ->
            val rate = parsed?.coerceIn(0.0, 50.0) ?: 0.0
            val updated = current.loanInput.copy(annualRatePercent = rate)
            current.copy(
                includeInterest = rate > 0.0 || current.includeInterest,
                lastNonZeroRatePercent = if (rate > 0.0) rate else current.lastNonZeroRatePercent,
                loanInput = updated,
                inputStrings = current.inputStrings.copy(rateText = sanitized),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updateRateFromSlider(value: Double) {
        _uiState.update { current ->
            val snapped = (kotlin.math.round(value * 20.0) / 20.0).coerceIn(0.5, 30.0)
            val updated = current.loanInput.copy(annualRatePercent = snapped)
            current.copy(
                includeInterest = true,
                lastNonZeroRatePercent = snapped,
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    rateText = CurrencyFormatter.formatPlainNumber(snapped, 2)
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun nudgeRate(delta: Double) {
        _uiState.update { current ->
            val next = (kotlin.math.round((current.loanInput.annualRatePercent + delta) * 100.0) / 100.0)
                .coerceIn(0.0, 50.0)
            val updated = current.loanInput.copy(annualRatePercent = next)
            current.copy(
                includeInterest = next > 0.0,
                lastNonZeroRatePercent = if (next > 0.0) next else current.lastNonZeroRatePercent,
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    rateText = CurrencyFormatter.formatPlainNumber(next, 2)
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updateTenureText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = false)
        val parsed = sanitized.toIntOrNull()
        _uiState.update { current ->
            val maxVal = if (current.loanInput.tenureUnit == TenureUnit.YEARS) 40 else 480
            val tenure = (parsed ?: 1).coerceIn(1, maxVal)
            val updated = current.loanInput.copy(tenureValue = tenure)
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(tenureText = sanitized),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updateTenureFromSlider(value: Int) {
        _uiState.update { current ->
            val maxVal = if (current.loanInput.tenureUnit == TenureUnit.YEARS) 40 else 480
            val clamped = value.coerceIn(1, maxVal)
            val updated = current.loanInput.copy(tenureValue = clamped)
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(tenureText = clamped.toString()),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun switchTenureUnit(unit: TenureUnit) {
        _uiState.update { current ->
            if (current.loanInput.tenureUnit == unit) return@update current
            val convertedValue = when (unit) {
                TenureUnit.YEARS -> (current.loanInput.tenureValue / 12.0)
                    .let { kotlin.math.round(it).toInt() }
                    .coerceIn(1, 40)
                TenureUnit.MONTHS -> (current.loanInput.tenureValue * 12).coerceIn(1, 480)
            }
            val updated = current.loanInput.copy(
                tenureValue = convertedValue,
                tenureUnit = unit
            )
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(tenureText = convertedValue.toString()),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun selectPrepaymentFrequency(frequency: PrepaymentFrequency) {
        _uiState.update { current ->
            val defaultExtra = if (frequency != PrepaymentFrequency.NONE && current.loanInput.extraPayment <= 0.0) {
                when (frequency) {
                    PrepaymentFrequency.MONTHLY -> if (current.currency == CurrencyOption.INR) 5000.0 else 200.0
                    PrepaymentFrequency.YEARLY -> if (current.currency == CurrencyOption.INR) 50000.0 else 2500.0
                    PrepaymentFrequency.NONE -> 0.0
                }
            } else {
                current.loanInput.extraPayment
            }
            val updated = current.loanInput.copy(
                prepaymentFrequency = frequency,
                extraPayment = if (frequency == PrepaymentFrequency.NONE) 0.0 else defaultExtra
            )
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(
                    extraPaymentText = CurrencyFormatter.formatPlainNumber(updated.extraPayment)
                ),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun updateExtraPaymentText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = false)
        val parsed = sanitized.toDoubleOrNull() ?: 0.0
        _uiState.update { current ->
            val updated = current.loanInput.copy(extraPayment = parsed.coerceIn(0.0, 100_000_000.0))
            current.copy(
                loanInput = updated,
                inputStrings = current.inputStrings.copy(extraPaymentText = sanitized),
                emiResult = EmiCalculatorEngine.calculate(updated)
            )
        }
    }

    fun defaultSessionLabel(): String {
        val current = _uiState.value
        val presetLabel = when (current.selectedPreset) {
            LoanPreset.HOME -> "Home Loan"
            LoanPreset.CAR -> "Car Loan"
            LoanPreset.PERSONAL -> "Personal Loan"
            LoanPreset.EDUCATION -> "Education Loan"
        }
        val compactAmt = CurrencyFormatter.formatCompact(
            amount = current.loanInput.principal,
            currency = current.currency,
            useDeviceLocale = current.useDeviceLocaleSeparators
        )
        return "$presetLabel • $compactAmt"
    }

    fun saveCurrentSession(customLabel: String) {
        val current = _uiState.value
        val trimmedLabel = customLabel.trim().ifEmpty { defaultSessionLabel() }
        val input = current.loanInput
        val result = current.emiResult
        val entity = SavedEmiSessionEntity(
            label = trimmedLabel,
            presetName = current.selectedPreset.name,
            currencyCode = current.currency.code,
            principal = input.principal,
            annualRatePercent = input.annualRatePercent,
            tenureValue = input.tenureValue,
            tenureUnitName = input.tenureUnit.name,
            extraPayment = input.extraPayment,
            prepaymentFrequencyName = input.prepaymentFrequency.name,
            monthlyEmi = result.monthlyEmi,
            totalInterest = result.effectiveTotalInterest,
            totalPayable = result.effectiveTotalPayable,
            interestSaved = result.interestSaved,
            monthsSaved = result.monthsSaved
        )
        viewModelScope.launch {
            savedSessionRepo.saveSession(entity)
        }
    }

    fun loadSavedSession(session: SavedEmiSessionEntity) {
        val targetCurrency = CurrencyOption.fromCode(session.currencyCode)
        val targetPreset = LoanPreset.entries.firstOrNull { it.name == session.presetName } ?: LoanPreset.HOME
        val targetUnit = TenureUnit.entries.firstOrNull { it.name == session.tenureUnitName } ?: TenureUnit.YEARS
        val targetFreq = PrepaymentFrequency.entries.firstOrNull { it.name == session.prepaymentFrequencyName }
            ?: PrepaymentFrequency.NONE

        viewModelScope.launch {
            prefsRepo.setCurrency(targetCurrency)
        }

        val loadedInput = LoanInput(
            principal = session.principal,
            annualRatePercent = session.annualRatePercent,
            tenureValue = session.tenureValue,
            tenureUnit = targetUnit,
            extraPayment = session.extraPayment,
            prepaymentFrequency = targetFreq
        )

        _uiState.update { current ->
            current.copy(
                currency = targetCurrency,
                selectedPreset = targetPreset,
                includeInterest = session.annualRatePercent > 0.0,
                lastNonZeroRatePercent = if (session.annualRatePercent > 0.0) {
                    session.annualRatePercent
                } else {
                    current.lastNonZeroRatePercent
                },
                loanInput = loadedInput,
                inputStrings = LoanFieldStrings(
                    principalText = CurrencyFormatter.formatPlainNumber(session.principal),
                    rateText = CurrencyFormatter.formatPlainNumber(session.annualRatePercent, 2),
                    tenureText = session.tenureValue.toString(),
                    extraPaymentText = CurrencyFormatter.formatPlainNumber(session.extraPayment)
                ),
                emiResult = EmiCalculatorEngine.calculate(loadedInput)
            )
        }
    }

    fun deleteSavedSession(id: Long) {
        viewModelScope.launch {
            savedSessionRepo.deleteSession(id)
        }
    }

    fun clearAllSavedSessions() {
        viewModelScope.launch {
            savedSessionRepo.clearAll()
        }
    }

    fun setScheduleGranularity(granularity: ScheduleGranularity) {
        _uiState.update { it.copy(scheduleGranularity = granularity) }
    }

    fun copyCurrentToCompareSlot(isLoanA: Boolean) {
        _uiState.update { current ->
            val src = current.loanInput
            val srcStrings = LoanFieldStrings(
                principalText = CurrencyFormatter.formatPlainNumber(src.principal),
                rateText = CurrencyFormatter.formatPlainNumber(src.annualRatePercent, 2),
                tenureText = src.tenureValue.toString(),
                feeText = if (isLoanA) current.loanAStrings.feeText else current.loanBStrings.feeText
            )
            val fee = if (isLoanA) current.loanA.processingFeePercent else current.loanB.processingFeePercent
            val copied = src.copy(
                prepaymentFrequency = PrepaymentFrequency.NONE,
                extraPayment = 0.0,
                processingFeePercent = fee
            )
            val nextA = if (isLoanA) copied else current.loanA
            val nextB = if (!isLoanA) copied else current.loanB
            current.copy(
                loanA = nextA,
                loanB = nextB,
                loanAStrings = if (isLoanA) srcStrings else current.loanAStrings,
                loanBStrings = if (!isLoanA) srcStrings else current.loanBStrings,
                loanAIncludeInterest = if (isLoanA) copied.annualRatePercent > 0.0 else current.loanAIncludeInterest,
                loanASavedRatePercent = if (isLoanA && copied.annualRatePercent > 0.0) copied.annualRatePercent else current.loanASavedRatePercent,
                loanBIncludeInterest = if (!isLoanA) copied.annualRatePercent > 0.0 else current.loanBIncludeInterest,
                loanBSavedRatePercent = if (!isLoanA && copied.annualRatePercent > 0.0) copied.annualRatePercent else current.loanBSavedRatePercent,
                comparisonResult = EmiCalculatorEngine.compare(nextA, nextB)
            )
        }
    }

    fun swapCompareLoans() {
        _uiState.update { current ->
            val nextA = current.loanB
            val nextB = current.loanA
            current.copy(
                loanA = nextA,
                loanB = nextB,
                loanAStrings = current.loanBStrings,
                loanBStrings = current.loanAStrings,
                loanAIncludeInterest = current.loanBIncludeInterest,
                loanASavedRatePercent = current.loanBSavedRatePercent,
                loanBIncludeInterest = current.loanAIncludeInterest,
                loanBSavedRatePercent = current.loanASavedRatePercent,
                comparisonResult = EmiCalculatorEngine.compare(nextA, nextB)
            )
        }
    }

    fun toggleCompareInterest(isLoanA: Boolean, enabled: Boolean) {
        _uiState.update { current ->
            val target = if (isLoanA) current.loanA else current.loanB
            val targetStrings = if (isLoanA) current.loanAStrings else current.loanBStrings
            val currentEnabled = if (isLoanA) current.loanAIncludeInterest else current.loanBIncludeInterest
            val savedRate = if (isLoanA) current.loanASavedRatePercent else current.loanBSavedRatePercent
            if (enabled == currentEnabled) return@update current

            val nextSavedRate = if (!enabled && target.annualRatePercent > 0.0) target.annualRatePercent else savedRate
            val targetRate = if (enabled) nextSavedRate.takeIf { it > 0.0 } ?: 7.5 else 0.0
            val updatedLoan = target.copy(annualRatePercent = targetRate)
            val updatedStrings = targetStrings.copy(
                rateText = CurrencyFormatter.formatPlainNumber(targetRate, 2)
            )
            val nextA = if (isLoanA) updatedLoan else current.loanA
            val nextB = if (!isLoanA) updatedLoan else current.loanB
            current.copy(
                loanA = nextA,
                loanB = nextB,
                loanAStrings = if (isLoanA) updatedStrings else current.loanAStrings,
                loanBStrings = if (!isLoanA) updatedStrings else current.loanBStrings,
                loanAIncludeInterest = if (isLoanA) enabled else current.loanAIncludeInterest,
                loanASavedRatePercent = if (isLoanA) nextSavedRate else current.loanASavedRatePercent,
                loanBIncludeInterest = if (!isLoanA) enabled else current.loanBIncludeInterest,
                loanBSavedRatePercent = if (!isLoanA) nextSavedRate else current.loanBSavedRatePercent,
                comparisonResult = EmiCalculatorEngine.compare(nextA, nextB)
            )
        }
    }

    fun updateCompareField(
        isLoanA: Boolean,
        principalRaw: String? = null,
        rateRaw: String? = null,
        tenureRaw: String? = null,
        feeRaw: String? = null,
        tenureUnit: TenureUnit? = null
    ) {
        _uiState.update { current ->
            val target = if (isLoanA) current.loanA else current.loanB
            val targetStrings = if (isLoanA) current.loanAStrings else current.loanBStrings

            val pStr = principalRaw?.let { sanitizeDecimalInput(it, false) } ?: targetStrings.principalText
            val rStr = rateRaw?.let { sanitizeDecimalInput(it, true) } ?: targetStrings.rateText
            val tStr = tenureRaw?.let { sanitizeDecimalInput(it, false) } ?: targetStrings.tenureText
            val fStr = feeRaw?.let { sanitizeDecimalInput(it, true) } ?: targetStrings.feeText

            val nextUnit = tenureUnit ?: target.tenureUnit
            val convertedTenure = if (tenureUnit != null && tenureUnit != target.tenureUnit) {
                when (tenureUnit) {
                    TenureUnit.YEARS -> kotlin.math.round(target.tenureValue / 12.0).toInt().coerceIn(1, 40)
                    TenureUnit.MONTHS -> (target.tenureValue * 12).coerceIn(1, 480)
                }
            } else {
                (tStr.toIntOrNull() ?: 1).coerceIn(1, if (nextUnit == TenureUnit.YEARS) 40 else 480)
            }

            val finalTenureText = if (tenureUnit != null && tenureUnit != target.tenureUnit) {
                convertedTenure.toString()
            } else {
                tStr
            }

            val newRate = (rStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 50.0)
            val updatedLoan = target.copy(
                principal = (pStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 1_000_000_000.0),
                annualRatePercent = newRate,
                tenureValue = convertedTenure,
                tenureUnit = nextUnit,
                processingFeePercent = (fStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 15.0)
            )
            val updatedStrings = targetStrings.copy(
                principalText = pStr,
                rateText = rStr,
                tenureText = finalTenureText,
                feeText = fStr
            )

            val nextA = if (isLoanA) updatedLoan else current.loanA
            val nextB = if (!isLoanA) updatedLoan else current.loanB

            current.copy(
                loanA = nextA,
                loanB = nextB,
                loanAStrings = if (isLoanA) updatedStrings else current.loanAStrings,
                loanBStrings = if (!isLoanA) updatedStrings else current.loanBStrings,
                loanAIncludeInterest = if (isLoanA && rateRaw != null) newRate > 0.0 else current.loanAIncludeInterest,
                loanASavedRatePercent = if (isLoanA && newRate > 0.0) newRate else current.loanASavedRatePercent,
                loanBIncludeInterest = if (!isLoanA && rateRaw != null) newRate > 0.0 else current.loanBIncludeInterest,
                loanBSavedRatePercent = if (!isLoanA && newRate > 0.0) newRate else current.loanBSavedRatePercent,
                comparisonResult = EmiCalculatorEngine.compare(nextA, nextB)
            )
        }
    }

    fun updateEligibilityIncomeText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = false)
        val parsed = sanitized.toDoubleOrNull() ?: 0.0
        _uiState.update { current ->
            val updated = current.eligibilityInput.copy(monthlyIncome = parsed.coerceIn(0.0, 100_000_000.0))
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(incomeText = sanitized),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityIncomeSlider(value: Double) {
        _uiState.update { current ->
            val step = if (current.currency == CurrencyOption.INR) 5_000.0 else 250.0
            val snapped = (kotlin.math.round(value / step) * step).coerceAtLeast(0.0)
            val updated = current.eligibilityInput.copy(monthlyIncome = snapped)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    incomeText = CurrencyFormatter.formatPlainNumber(snapped)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun nudgeEligibilityIncome(delta: Double) {
        _uiState.update { current ->
            val next = (current.eligibilityInput.monthlyIncome + delta).coerceIn(0.0, 100_000_000.0)
            val updated = current.eligibilityInput.copy(monthlyIncome = next)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    incomeText = CurrencyFormatter.formatPlainNumber(next)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityLiabilitiesText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = false)
        val parsed = sanitized.toDoubleOrNull() ?: 0.0
        _uiState.update { current ->
            val updated = current.eligibilityInput.copy(existingLiabilities = parsed.coerceIn(0.0, 100_000_000.0))
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(liabilitiesText = sanitized),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityLiabilitiesSlider(value: Double) {
        _uiState.update { current ->
            val step = if (current.currency == CurrencyOption.INR) 2_000.0 else 100.0
            val snapped = (kotlin.math.round(value / step) * step).coerceAtLeast(0.0)
            val updated = current.eligibilityInput.copy(existingLiabilities = snapped)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    liabilitiesText = CurrencyFormatter.formatPlainNumber(snapped)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun nudgeEligibilityLiabilities(delta: Double) {
        _uiState.update { current ->
            val next = (current.eligibilityInput.existingLiabilities + delta).coerceIn(0.0, 100_000_000.0)
            val updated = current.eligibilityInput.copy(existingLiabilities = next)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    liabilitiesText = CurrencyFormatter.formatPlainNumber(next)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun selectDtiPreset(preset: DtiPreset) {
        _uiState.update { current ->
            val updated = current.eligibilityInput.copy(maxDtiPercent = preset.dtiPercent)
            current.copy(
                eligibilityInput = updated,
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityDtiSlider(dtiPercent: Double) {
        _uiState.update { current ->
            val snapped = kotlin.math.round(dtiPercent).coerceIn(20.0, 60.0)
            val updated = current.eligibilityInput.copy(maxDtiPercent = snapped)
            current.copy(
                eligibilityInput = updated,
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun toggleEligibilityInterest(enabled: Boolean) {
        _uiState.update { current ->
            if (enabled == current.eligibilityIncludeInterest) return@update current
            val targetRate = if (enabled) {
                current.eligibilitySavedRatePercent.takeIf { it > 0.0 } ?: 7.5
            } else {
                0.0
            }
            val savedRate = if (!enabled && current.eligibilityInput.annualRatePercent > 0.0) {
                current.eligibilityInput.annualRatePercent
            } else {
                current.eligibilitySavedRatePercent
            }
            val updated = current.eligibilityInput.copy(annualRatePercent = targetRate)
            current.copy(
                eligibilityIncludeInterest = enabled,
                eligibilitySavedRatePercent = savedRate,
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    rateText = CurrencyFormatter.formatPlainNumber(targetRate, 2)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityRateText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = true)
        val parsed = sanitized.toDoubleOrNull() ?: 0.0
        _uiState.update { current ->
            val rate = parsed.coerceIn(0.0, 50.0)
            val updated = current.eligibilityInput.copy(annualRatePercent = rate)
            current.copy(
                eligibilityIncludeInterest = rate > 0.0 || current.eligibilityIncludeInterest,
                eligibilitySavedRatePercent = if (rate > 0.0) rate else current.eligibilitySavedRatePercent,
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(rateText = sanitized),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityRateSlider(value: Double) {
        _uiState.update { current ->
            val snapped = (kotlin.math.round(value * 20.0) / 20.0).coerceIn(0.5, 30.0)
            val updated = current.eligibilityInput.copy(annualRatePercent = snapped)
            current.copy(
                eligibilityIncludeInterest = true,
                eligibilitySavedRatePercent = snapped,
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    rateText = CurrencyFormatter.formatPlainNumber(snapped, 2)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun nudgeEligibilityRate(delta: Double) {
        _uiState.update { current ->
            val next = (kotlin.math.round((current.eligibilityInput.annualRatePercent + delta) * 100.0) / 100.0)
                .coerceIn(0.0, 50.0)
            val updated = current.eligibilityInput.copy(annualRatePercent = next)
            current.copy(
                eligibilityIncludeInterest = next > 0.0,
                eligibilitySavedRatePercent = if (next > 0.0) next else current.eligibilitySavedRatePercent,
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(
                    rateText = CurrencyFormatter.formatPlainNumber(next, 2)
                ),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityTenureText(raw: String) {
        val sanitized = sanitizeDecimalInput(raw, allowDecimal = false)
        val parsed = sanitized.toIntOrNull()
        _uiState.update { current ->
            val maxVal = if (current.eligibilityInput.tenureUnit == TenureUnit.YEARS) 40 else 480
            val tenure = (parsed ?: 1).coerceIn(1, maxVal)
            val updated = current.eligibilityInput.copy(tenureValue = tenure)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(tenureText = sanitized),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun updateEligibilityTenureSlider(value: Int) {
        _uiState.update { current ->
            val maxVal = if (current.eligibilityInput.tenureUnit == TenureUnit.YEARS) 40 else 480
            val clamped = value.coerceIn(1, maxVal)
            val updated = current.eligibilityInput.copy(tenureValue = clamped)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(tenureText = clamped.toString()),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun switchEligibilityTenureUnit(unit: TenureUnit) {
        _uiState.update { current ->
            if (current.eligibilityInput.tenureUnit == unit) return@update current
            val converted = when (unit) {
                TenureUnit.YEARS -> kotlin.math.round(current.eligibilityInput.tenureValue / 12.0).toInt().coerceIn(1, 40)
                TenureUnit.MONTHS -> (current.eligibilityInput.tenureValue * 12).coerceIn(1, 480)
            }
            val updated = current.eligibilityInput.copy(tenureValue = converted, tenureUnit = unit)
            current.copy(
                eligibilityInput = updated,
                eligibilityStrings = current.eligibilityStrings.copy(tenureText = converted.toString()),
                eligibilityResult = EmiCalculatorEngine.calculateEligibility(updated)
            )
        }
    }

    fun applyEligibilityToCalculator() {
        _uiState.update { current ->
            val maxLoan = kotlin.math.round(current.eligibilityResult.maxLoanAmount).coerceAtLeast(0.0)
            if (maxLoan <= 0.0) return@update current
            val elig = current.eligibilityInput
            val updatedLoan = current.loanInput.copy(
                principal = maxLoan,
                annualRatePercent = elig.annualRatePercent,
                tenureValue = elig.tenureValue,
                tenureUnit = elig.tenureUnit,
                prepaymentFrequency = PrepaymentFrequency.NONE,
                extraPayment = 0.0
            )
            current.copy(
                includeInterest = elig.annualRatePercent > 0.0,
                lastNonZeroRatePercent = if (elig.annualRatePercent > 0.0) {
                    elig.annualRatePercent
                } else {
                    current.lastNonZeroRatePercent
                },
                loanInput = updatedLoan,
                inputStrings = LoanFieldStrings(
                    principalText = CurrencyFormatter.formatPlainNumber(maxLoan),
                    rateText = CurrencyFormatter.formatPlainNumber(elig.annualRatePercent, 2),
                    tenureText = elig.tenureValue.toString(),
                    extraPaymentText = "0"
                ),
                emiResult = EmiCalculatorEngine.calculate(updatedLoan)
            )
        }
    }

    private fun sanitizeDecimalInput(raw: String, allowDecimal: Boolean): String {
        val builder = StringBuilder()
        var dotSeen = false
        for (ch in raw) {
            if (ch.isDigit()) {
                builder.append(ch)
            } else if (allowDecimal && ch == '.' && !dotSeen) {
                dotSeen = true
                if (builder.isEmpty()) builder.append('0')
                builder.append('.')
            }
        }
        return builder.toString().take(12)
    }

    companion object {
        fun factory(
            prefsRepo: UserPreferencesRepository,
            savedSessionRepo: SavedSessionRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EmiViewModel(prefsRepo, savedSessionRepo) as T
                }
            }
    }
}
