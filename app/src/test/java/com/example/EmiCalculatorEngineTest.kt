package com.example

import com.example.domain.CurrencyOption
import com.example.domain.EmiCalculatorEngine
import com.example.domain.LoanInput
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.ui.components.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmiCalculatorEngineTest {

    @Test
    fun standardEmi_matchesExpectedFormula() {
        val input = LoanInput(
            principal = 100000.0,
            annualRatePercent = 12.0,
            tenureValue = 1,
            tenureUnit = TenureUnit.YEARS
        )
        val result = EmiCalculatorEngine.calculate(input)
        // P=100,000, r=1%/mo, n=12 => EMI = 8,884.88
        assertEquals(8884.88, result.monthlyEmi, 0.05)
        assertEquals(12, result.effectiveMonths)
        assertEquals(0.0, result.monthlySchedule.last().closingBalance, 0.05)
    }

    @Test
    fun zeroInterest_dividesPrincipalEvenly() {
        val input = LoanInput(
            principal = 12000.0,
            annualRatePercent = 0.0,
            tenureValue = 12,
            tenureUnit = TenureUnit.MONTHS
        )
        val result = EmiCalculatorEngine.calculate(input)
        assertEquals(1000.0, result.monthlyEmi, 0.01)
        assertEquals(0.0, result.effectiveTotalInterest, 0.01)
        assertEquals(12000.0, result.effectiveTotalPayable, 0.01)
    }

    @Test
    fun monthlyPrepayment_reducesInterestAndTenure() {
        val baseline = EmiCalculatorEngine.calculate(
            LoanInput(
                principal = 250000.0,
                annualRatePercent = 7.5,
                tenureValue = 20,
                tenureUnit = TenureUnit.YEARS
            )
        )
        val prepaid = EmiCalculatorEngine.calculate(
            LoanInput(
                principal = 250000.0,
                annualRatePercent = 7.5,
                tenureValue = 20,
                tenureUnit = TenureUnit.YEARS,
                extraPayment = 300.0,
                prepaymentFrequency = PrepaymentFrequency.MONTHLY
            )
        )
        assertTrue(prepaid.interestSaved > 0.0)
        assertTrue(prepaid.monthsSaved > 0)
        assertTrue(prepaid.effectiveMonths < baseline.originalMonths)
        assertEquals(0.0, prepaid.monthlySchedule.last().closingBalance, 0.05)
    }

    @Test
    fun comparison_accountsForProcessingFees() {
        val loanA = LoanInput(
            principal = 100000.0,
            annualRatePercent = 8.0,
            tenureValue = 5,
            tenureUnit = TenureUnit.YEARS,
            processingFeePercent = 2.0
        )
        val loanB = LoanInput(
            principal = 100000.0,
            annualRatePercent = 8.0,
            tenureValue = 5,
            tenureUnit = TenureUnit.YEARS,
            processingFeePercent = 0.0
        )
        val comp = EmiCalculatorEngine.compare(loanA, loanB)
        assertEquals(1, comp.cheaperSide)
        assertEquals(2000.0, comp.totalSavings, 0.5)
    }

    @Test
    fun comparison_differentRatesAndTenures_computesEmiAndTotalDiff() {
        val loanA = LoanInput(
            principal = 250000.0,
            annualRatePercent = 7.25,
            tenureValue = 20,
            tenureUnit = TenureUnit.YEARS
        )
        val loanB = LoanInput(
            principal = 250000.0,
            annualRatePercent = 7.75,
            tenureValue = 15,
            tenureUnit = TenureUnit.YEARS
        )
        val comp = EmiCalculatorEngine.compare(loanA, loanB)
        // Loan A has lower monthly EMI, while Loan B has lower total cost due to shorter 15y tenure
        assertTrue(comp.resultA.monthlyEmi < comp.resultB.monthlyEmi)
        assertEquals(1, comp.cheaperSide)
        assertTrue(comp.monthlyEmiDiff > 300.0)
        assertTrue(comp.totalSavings > 40000.0)
    }

    @Test
    fun indianCurrencyFormatting_usesLakhsGrouping() {
        assertEquals("₹25,00,000.00", CurrencyFormatter.format(2500000.0, CurrencyOption.INR))
        assertEquals("₹25,00,000", CurrencyFormatter.format(2500000.0, CurrencyOption.INR, showDecimals = false))
        assertEquals("$2,500,000.00", CurrencyFormatter.format(2500000.0, CurrencyOption.USD))
        assertEquals("$2,500,000", CurrencyFormatter.format(2500000.0, CurrencyOption.USD, showDecimals = false))
    }

    @Test
    fun euroFormatting_usesLocalSeparatorsAndSuffixPlacement() {
        assertEquals("2.500.000,00 €", CurrencyFormatter.format(2500000.0, CurrencyOption.EUR))
        assertEquals("-1.234,50 €", CurrencyFormatter.format(-1234.5, CurrencyOption.EUR))
        assertEquals("1,25M €", CurrencyFormatter.formatCompact(1250000.0, CurrencyOption.EUR))
        assertEquals("7,25%", CurrencyFormatter.formatPercent(7.25, 2, CurrencyOption.EUR))
    }

    @Test
    fun brazilianAndJapaneseFormatting_applyLocalDecimalsAndRounding() {
        assertEquals("R$ 1.234,56", CurrencyFormatter.format(1234.56, CurrencyOption.BRL))
        assertEquals("R$ 12,50M", CurrencyFormatter.formatCompact(12_500_000.0, CurrencyOption.BRL))
        assertEquals("¥12,346", CurrencyFormatter.format(12345.67, CurrencyOption.JPY))
        assertEquals("¥12.5K", CurrencyFormatter.formatCompact(12_500.0, CurrencyOption.JPY))
    }

    @Test
    fun localeOverride_adaptsSeparatorsAndSymbolPlacement() {
        val usEuro = CurrencyFormatter.format(
            amount = 1234.56,
            currency = CurrencyOption.EUR,
            localeOverride = java.util.Locale.US
        )
        assertEquals("€1,234.56", usEuro)

        val frUsd = CurrencyFormatter.format(
            amount = 1234.56,
            currency = CurrencyOption.USD,
            localeOverride = java.util.Locale.FRANCE
        )
        assertTrue(frUsd.startsWith("$"))
        assertTrue(frUsd.endsWith(",56"))
    }

    @Test
    fun principalVsInterestRatio_sumsToOneAndShiftsWithPrepayment() {
        val baseline = EmiCalculatorEngine.calculate(
            LoanInput(
                principal = 250000.0,
                annualRatePercent = 7.5,
                tenureValue = 20,
                tenureUnit = TenureUnit.YEARS
            )
        )
        assertEquals(1.0f, baseline.principalRatio + baseline.interestRatio, 0.001f)
        assertEquals(
            baseline.effectiveTotalPayable,
            baseline.principal + baseline.effectiveTotalInterest,
            1.0
        )

        val prepaid = EmiCalculatorEngine.calculate(
            LoanInput(
                principal = 250000.0,
                annualRatePercent = 7.5,
                tenureValue = 20,
                tenureUnit = TenureUnit.YEARS,
                extraPayment = 500.0,
                prepaymentFrequency = PrepaymentFrequency.MONTHLY
            )
        )
        assertEquals(1.0f, prepaid.principalRatio + prepaid.interestRatio, 0.001f)
        assertTrue(prepaid.principalRatio > baseline.principalRatio)
        assertTrue(prepaid.interestRatio < baseline.interestRatio)
    }

    @Test
    fun eligibilityCalculator_matchesInverseEmiAndDtiBudget() {
        val input = com.example.domain.EligibilityInput(
            monthlyIncome = 10_000.0,
            existingLiabilities = 1_300.0,
            annualRatePercent = 7.5,
            tenureValue = 20,
            tenureUnit = TenureUnit.YEARS,
            maxDtiPercent = 43.0
        )
        val elig = EmiCalculatorEngine.calculateEligibility(input)
        // 43% of 10,000 = 4,300; minus 1,300 existing liabilities = 3,000 available EMI
        assertEquals(4_300.0, elig.totalDebtBudget, 0.01)
        assertEquals(3_000.0, elig.availableMonthlyEmi, 0.01)
        assertEquals(43.0, elig.postLoanDtiPercent, 0.05)

        // Round-trip: EMI of maxLoanAmount should equal availableMonthlyEmi (3,000)
        val emiCheck = EmiCalculatorEngine.calculate(
            LoanInput(
                principal = elig.maxLoanAmount,
                annualRatePercent = 7.5,
                tenureValue = 20,
                tenureUnit = TenureUnit.YEARS
            )
        )
        assertEquals(3_000.0, emiCheck.monthlyEmi, 0.5)
    }

    @Test
    fun eligibilityCalculator_zeroInterestAndExceededLiabilities() {
        val zeroRateElig = EmiCalculatorEngine.calculateEligibility(
            com.example.domain.EligibilityInput(
                monthlyIncome = 5_000.0,
                existingLiabilities = 800.0,
                annualRatePercent = 0.0,
                tenureValue = 10,
                tenureUnit = TenureUnit.YEARS,
                maxDtiPercent = 36.0
            )
        )
        // 36% of 5,000 = 1,800; minus 800 = 1,000/mo * 120 mos = 120,000
        assertEquals(1_000.0, zeroRateElig.availableMonthlyEmi, 0.01)
        assertEquals(120_000.0, zeroRateElig.maxLoanAmount, 0.01)
        assertEquals(0.0, zeroRateElig.totalInterest, 0.01)

        val exceeded = EmiCalculatorEngine.calculateEligibility(
            com.example.domain.EligibilityInput(
                monthlyIncome = 4_000.0,
                existingLiabilities = 2_000.0,
                annualRatePercent = 8.0,
                tenureValue = 15,
                tenureUnit = TenureUnit.YEARS,
                maxDtiPercent = 43.0
            )
        )
        assertEquals(0.0, exceeded.availableMonthlyEmi, 0.01)
        assertEquals(0.0, exceeded.maxLoanAmount, 0.01)
        assertEquals(com.example.domain.DtiStatus.EXCEEDED, exceeded.status)
    }
}
