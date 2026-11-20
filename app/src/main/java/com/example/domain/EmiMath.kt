package com.example.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class TenureUnit {
    YEARS,
    MONTHS
}

enum class PrepaymentFrequency {
    NONE,
    MONTHLY,
    YEARLY
}

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val locale: Locale,
    val fractionDigits: Int = 2,
    val usesIndianGrouping: Boolean = false
) {
    INR("INR", "₹", "Indian Rupee", Locale.Builder().setLanguage("en").setRegion("IN").build(), 2, usesIndianGrouping = true),
    USD("USD", "$", "US Dollar", Locale.US, 2),
    EUR("EUR", "€", "Euro (DE/EU)", Locale.GERMANY, 2),
    GBP("GBP", "£", "British Pound", Locale.UK, 2),
    JPY("JPY", "¥", "Japanese Yen", Locale.JAPAN, 0),
    CHF("CHF", "CHF", "Swiss Franc", Locale.Builder().setLanguage("de").setRegion("CH").build(), 2),
    BRL("BRL", "R$", "Brazilian Real", Locale.Builder().setLanguage("pt").setRegion("BR").build(), 2),
    AED("AED", "AED", "UAE Dirham", Locale.Builder().setLanguage("en").setRegion("AE").build(), 2),
    SGD("SGD", "S$", "Singapore Dollar", Locale.Builder().setLanguage("en").setRegion("SG").build(), 2),
    CAD("CAD", "C$", "Canadian Dollar", Locale.CANADA, 2),
    AUD("AUD", "A$", "Australian Dollar", Locale.Builder().setLanguage("en").setRegion("AU").build(), 2);

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: USD
    }
}

enum class LoanPreset(
    val defaultPrincipal: Double,
    val defaultRatePercent: Double,
    val defaultYears: Int,
    val maxPrincipal: Double,
    val minPrincipal: Double,
    val maxYears: Int
) {
    HOME(
        defaultPrincipal = 250000.0,
        defaultRatePercent = 7.5,
        defaultYears = 20,
        maxPrincipal = 2000000.0,
        minPrincipal = 10000.0,
        maxYears = 30
    ),
    CAR(
        defaultPrincipal = 35000.0,
        defaultRatePercent = 8.9,
        defaultYears = 5,
        maxPrincipal = 250000.0,
        minPrincipal = 2000.0,
        maxYears = 10
    ),
    PERSONAL(
        defaultPrincipal = 15000.0,
        defaultRatePercent = 11.5,
        defaultYears = 3,
        maxPrincipal = 150000.0,
        minPrincipal = 1000.0,
        maxYears = 7
    ),
    EDUCATION(
        defaultPrincipal = 50000.0,
        defaultRatePercent = 8.25,
        defaultYears = 10,
        maxPrincipal = 300000.0,
        minPrincipal = 2000.0,
        maxYears = 15
    );

    fun scaledDefaultPrincipal(currency: CurrencyOption): Double {
        val factor = if (currency == CurrencyOption.INR) 20.0 else 1.0
        return defaultPrincipal * factor
    }

    fun scaledMaxPrincipal(currency: CurrencyOption): Double {
        val factor = if (currency == CurrencyOption.INR) 20.0 else 1.0
        return maxPrincipal * factor
    }

    fun scaledMinPrincipal(currency: CurrencyOption): Double {
        val factor = if (currency == CurrencyOption.INR) 20.0 else 1.0
        return minPrincipal * factor
    }
}

data class LoanInput(
    val principal: Double = 250000.0,
    val annualRatePercent: Double = 7.5,
    val tenureValue: Int = 20,
    val tenureUnit: TenureUnit = TenureUnit.YEARS,
    val extraPayment: Double = 0.0,
    val prepaymentFrequency: PrepaymentFrequency = PrepaymentFrequency.NONE,
    val processingFeePercent: Double = 0.0
) {
    val totalMonths: Int
        get() = when (tenureUnit) {
            TenureUnit.YEARS -> (tenureValue * 12).coerceIn(1, 480)
            TenureUnit.MONTHS -> tenureValue.coerceIn(1, 480)
        }
}

data class AmortizationMonth(
    val monthIndex: Int,
    val yearIndex: Int,
    val openingBalance: Double,
    val emiPaid: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val extraPaid: Double,
    val closingBalance: Double,
    val cumulativePrincipal: Double,
    val cumulativeInterest: Double
)

data class AmortizationYear(
    val yearIndex: Int,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val totalExtraPaid: Double,
    val endingBalance: Double
)

data class EmiResult(
    val principal: Double,
    val monthlyEmi: Double,
    val baselineTotalInterest: Double,
    val baselineTotalPayable: Double,
    val effectiveTotalInterest: Double,
    val effectiveTotalPayable: Double,
    val interestSaved: Double,
    val originalMonths: Int,
    val effectiveMonths: Int,
    val monthsSaved: Int,
    val principalRatio: Float,
    val interestRatio: Float,
    val monthlySchedule: List<AmortizationMonth>,
    val yearlySchedule: List<AmortizationYear>
)

data class ComparisonResult(
    val resultA: EmiResult,
    val resultB: EmiResult,
    val feeA: Double,
    val feeB: Double,
    val totalCostA: Double,
    val totalCostB: Double,
    val cheaperSide: Int, // -1 = A cheaper, 1 = B cheaper, 0 = equal
    val totalSavings: Double,
    val monthlyEmiDiff: Double
)

enum class DtiPreset(val label: String, val dtiPercent: Double) {
    CONSERVATIVE("Conservative 36%", 36.0),
    STANDARD("Standard 43%", 43.0),
    FLEXIBLE("Flexible 50%", 50.0)
}

enum class DtiStatus {
    HEALTHY,
    MODERATE,
    EXCEEDED
}

data class EligibilityInput(
    val monthlyIncome: Double = 8500.0,
    val existingLiabilities: Double = 1200.0,
    val annualRatePercent: Double = 7.5,
    val tenureValue: Int = 20,
    val tenureUnit: TenureUnit = TenureUnit.YEARS,
    val maxDtiPercent: Double = 43.0
) {
    val totalMonths: Int
        get() = when (tenureUnit) {
            TenureUnit.YEARS -> (tenureValue * 12).coerceIn(1, 480)
            TenureUnit.MONTHS -> tenureValue.coerceIn(1, 480)
        }
}

data class EligibilityResult(
    val monthlyIncome: Double,
    val existingLiabilities: Double,
    val maxDtiPercent: Double,
    val totalDebtBudget: Double,
    val availableMonthlyEmi: Double,
    val maxLoanAmount: Double,
    val totalPayable: Double,
    val totalInterest: Double,
    val existingDtiPercent: Double,
    val postLoanDtiPercent: Double,
    val remainingIncome: Double,
    val effectiveMonths: Int,
    val status: DtiStatus
)

object EmiCalculatorEngine {

    fun calculate(input: LoanInput): EmiResult {
        val principal = max(0.0, input.principal)
        val months = input.totalMonths
        val monthlyRate = max(0.0, input.annualRatePercent) / 12.0 / 100.0

        if (principal <= 0.0) {
            return EmiResult(
                principal = 0.0,
                monthlyEmi = 0.0,
                baselineTotalInterest = 0.0,
                baselineTotalPayable = 0.0,
                effectiveTotalInterest = 0.0,
                effectiveTotalPayable = 0.0,
                interestSaved = 0.0,
                originalMonths = months,
                effectiveMonths = 0,
                monthsSaved = 0,
                principalRatio = 1f,
                interestRatio = 0f,
                monthlySchedule = emptyList(),
                yearlySchedule = emptyList()
            )
        }

        val monthlyEmi = computeMonthlyEmi(principal, monthlyRate, months)
        val baselineTotalPayable = monthlyEmi * months
        val baselineTotalInterest = max(0.0, baselineTotalPayable - principal)

        val monthlySchedule = mutableListOf<AmortizationMonth>()
        var balance = principal
        var cumulativePrincipal = 0.0
        var cumulativeInterest = 0.0

        val extraAmount = max(0.0, input.extraPayment)
        val frequency = input.prepaymentFrequency

        var monthIndex = 1
        // Safety cap at months + 1 so floating-point rounding never loops indefinitely
        while (balance > 0.01 && monthIndex <= months) {
            val interestForMonth = balance * monthlyRate
            val scheduledPrincipal = if (monthIndex == months && frequency == PrepaymentFrequency.NONE) {
                balance
            } else {
                min(balance, max(0.0, monthlyEmi - interestForMonth))
            }

            val actualEmi = if (scheduledPrincipal >= balance - 0.01) {
                balance + interestForMonth
            } else {
                monthlyEmi
            }

            val remainingAfterRegular = max(0.0, balance - scheduledPrincipal)
            val scheduledExtra = when (frequency) {
                PrepaymentFrequency.NONE -> 0.0
                PrepaymentFrequency.MONTHLY -> extraAmount
                PrepaymentFrequency.YEARLY -> if (monthIndex % 12 == 0) extraAmount else 0.0
            }
            val actualExtra = min(remainingAfterRegular, scheduledExtra)
            val closingBalance = max(0.0, remainingAfterRegular - actualExtra)

            cumulativePrincipal += scheduledPrincipal + actualExtra
            cumulativeInterest += interestForMonth

            monthlySchedule.add(
                AmortizationMonth(
                    monthIndex = monthIndex,
                    yearIndex = (monthIndex - 1) / 12 + 1,
                    openingBalance = balance,
                    emiPaid = actualEmi,
                    principalPaid = scheduledPrincipal,
                    interestPaid = interestForMonth,
                    extraPaid = actualExtra,
                    closingBalance = closingBalance,
                    cumulativePrincipal = cumulativePrincipal,
                    cumulativeInterest = cumulativeInterest
                )
            )

            balance = closingBalance
            monthIndex++
        }

        val effectiveTotalInterest = cumulativeInterest
        val effectiveTotalPayable = principal + effectiveTotalInterest
        val effectiveMonths = monthlySchedule.size
        val interestSaved = max(0.0, baselineTotalInterest - effectiveTotalInterest)
        val monthsSaved = max(0, months - effectiveMonths)

        val totalForRatio = effectiveTotalPayable.coerceAtLeast(1.0)
        val principalRatio = (principal / totalForRatio).toFloat().coerceIn(0f, 1f)
        val interestRatio = (1f - principalRatio).coerceIn(0f, 1f)

        val yearlySchedule = monthlySchedule
            .groupBy { it.yearIndex }
            .entries
            .sortedBy { it.key }
            .map { (year, rows) ->
                AmortizationYear(
                    yearIndex = year,
                    totalPrincipalPaid = rows.sumOf { it.principalPaid },
                    totalInterestPaid = rows.sumOf { it.interestPaid },
                    totalExtraPaid = rows.sumOf { it.extraPaid },
                    endingBalance = rows.last().closingBalance
                )
            }

        return EmiResult(
            principal = principal,
            monthlyEmi = monthlyEmi,
            baselineTotalInterest = baselineTotalInterest,
            baselineTotalPayable = baselineTotalPayable,
            effectiveTotalInterest = effectiveTotalInterest,
            effectiveTotalPayable = effectiveTotalPayable,
            interestSaved = interestSaved,
            originalMonths = months,
            effectiveMonths = effectiveMonths,
            monthsSaved = monthsSaved,
            principalRatio = principalRatio,
            interestRatio = interestRatio,
            monthlySchedule = monthlySchedule,
            yearlySchedule = yearlySchedule
        )
    }

    fun compare(loanA: LoanInput, loanB: LoanInput): ComparisonResult {
        val resA = calculate(loanA.copy(prepaymentFrequency = PrepaymentFrequency.NONE, extraPayment = 0.0))
        val resB = calculate(loanB.copy(prepaymentFrequency = PrepaymentFrequency.NONE, extraPayment = 0.0))
        val feeA = max(0.0, loanA.principal) * (max(0.0, loanA.processingFeePercent) / 100.0)
        val feeB = max(0.0, loanB.principal) * (max(0.0, loanB.processingFeePercent) / 100.0)
        val totalCostA = resA.baselineTotalPayable + feeA
        val totalCostB = resB.baselineTotalPayable + feeB
        val diff = totalCostA - totalCostB
        val cheaperSide = when {
            kotlin.math.abs(diff) < 1.0 -> 0
            diff < 0 -> -1
            else -> 1
        }
        return ComparisonResult(
            resultA = resA,
            resultB = resB,
            feeA = feeA,
            feeB = feeB,
            totalCostA = totalCostA,
            totalCostB = totalCostB,
            cheaperSide = cheaperSide,
            totalSavings = kotlin.math.abs(diff),
            monthlyEmiDiff = kotlin.math.abs(resA.monthlyEmi - resB.monthlyEmi)
        )
    }

    fun calculateEligibility(input: EligibilityInput): EligibilityResult {
        val income = max(0.0, input.monthlyIncome)
        val liabilities = max(0.0, input.existingLiabilities)
        val dtiCap = input.maxDtiPercent.coerceIn(10.0, 80.0)
        val months = input.totalMonths
        val monthlyRate = max(0.0, input.annualRatePercent) / 12.0 / 100.0

        val totalDebtBudget = income * (dtiCap / 100.0)
        val availableMonthlyEmi = max(0.0, totalDebtBudget - liabilities)
        val existingDtiPercent = if (income > 0.0) (liabilities / income) * 100.0 else 0.0

        val maxLoanAmount = if (availableMonthlyEmi <= 0.0 || months <= 0) {
            0.0
        } else if (monthlyRate <= 0.0) {
            availableMonthlyEmi * months
        } else {
            val factor = (1.0 + monthlyRate).pow(months.toDouble())
            availableMonthlyEmi * (factor - 1.0) / (monthlyRate * factor)
        }

        val totalPayable = if (maxLoanAmount > 0.0) availableMonthlyEmi * months else 0.0
        val totalInterest = max(0.0, totalPayable - maxLoanAmount)
        val postLoanDtiPercent = if (income > 0.0) {
            ((liabilities + availableMonthlyEmi) / income) * 100.0
        } else 0.0
        val remainingIncome = max(0.0, income - liabilities - availableMonthlyEmi)

        val status = when {
            income <= 0.0 || availableMonthlyEmi <= 0.0 -> DtiStatus.EXCEEDED
            existingDtiPercent <= dtiCap * 0.5 -> DtiStatus.HEALTHY
            else -> DtiStatus.MODERATE
        }

        return EligibilityResult(
            monthlyIncome = income,
            existingLiabilities = liabilities,
            maxDtiPercent = dtiCap,
            totalDebtBudget = totalDebtBudget,
            availableMonthlyEmi = availableMonthlyEmi,
            maxLoanAmount = maxLoanAmount,
            totalPayable = totalPayable,
            totalInterest = totalInterest,
            existingDtiPercent = existingDtiPercent,
            postLoanDtiPercent = postLoanDtiPercent,
            remainingIncome = remainingIncome,
            effectiveMonths = months,
            status = status
        )
    }

    private fun computeMonthlyEmi(principal: Double, monthlyRate: Double, months: Int): Double {
        if (months <= 0) return 0.0
        if (monthlyRate <= 0.0) return principal / months
        val factor = (1.0 + monthlyRate).pow(months.toDouble())
        return principal * monthlyRate * factor / (factor - 1.0)
    }
}
