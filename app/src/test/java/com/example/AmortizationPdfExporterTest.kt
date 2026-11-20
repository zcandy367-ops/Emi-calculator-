package com.example

import com.example.domain.CurrencyOption
import com.example.domain.EmiCalculatorEngine
import com.example.domain.LoanInput
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.export.AmortizationPdfExporter
import com.example.ui.ScheduleGranularity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmortizationPdfExporterTest {

    @Test
    fun buildPageChunks_paginatesYearlyAndMultipageMonthlyCorrectly() {
        val input = LoanInput(
            principal = 300000.0,
            annualRatePercent = 7.25,
            tenureValue = 20,
            tenureUnit = TenureUnit.YEARS,
            extraPayment = 250.0,
            prepaymentFrequency = PrepaymentFrequency.MONTHLY
        )
        val result = EmiCalculatorEngine.calculate(input)

        val yearlyPages = AmortizationPdfExporter.buildPageChunks(
            result = result,
            currency = CurrencyOption.USD,
            granularity = ScheduleGranularity.YEARLY,
            includePrepayCol = true
        )
        assertEquals(1, yearlyPages.size)
        assertEquals(result.yearlySchedule.size, yearlyPages.first().size)
        assertEquals("Year 1", yearlyPages.first().first().period)

        val monthlyPages = AmortizationPdfExporter.buildPageChunks(
            result = result,
            currency = CurrencyOption.INR,
            granularity = ScheduleGranularity.MONTHLY,
            includePrepayCol = true
        )
        assertTrue(monthlyPages.size > 3)
        assertEquals(result.effectiveMonths, monthlyPages.sumOf { it.size })
        assertTrue(monthlyPages.first().first().balance.startsWith("₹"))
    }

    @Test
    fun defaultFileName_containsPresetAndGranularity() {
        val name = AmortizationPdfExporter.defaultFileName(
            preset = LoanPreset.HOME,
            granularity = ScheduleGranularity.MONTHLY
        )
        assertTrue(name.startsWith("emi_pulse_home_monthly_"))
        assertTrue(name.endsWith(".pdf"))
    }
}
