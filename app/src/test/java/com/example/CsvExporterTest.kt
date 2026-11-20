package com.example

import com.example.domain.CurrencyOption
import com.example.domain.EmiCalculatorEngine
import com.example.domain.LoanInput
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.export.CsvExporter
import com.example.ui.ScheduleGranularity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvExporterTest {

    @Test
    fun `schedule yearly csv includes summary header and yearly rows`() {
        val input = LoanInput(
            principal = 120000.0,
            annualRatePercent = 6.0,
            tenureValue = 5,
            tenureUnit = TenureUnit.YEARS
        )
        val result = EmiCalculatorEngine.calculate(input)

        val csv = CsvExporter.buildScheduleCsvContent(
            preset = LoanPreset.CAR,
            input = input,
            result = result,
            currency = CurrencyOption.USD,
            granularity = ScheduleGranularity.YEARLY
        )

        assertTrue(csv.contains("Report,EMI Pulse Amortization Schedule"))
        assertTrue(csv.contains("Loan Type,Car Loan"))
        assertTrue(csv.contains("Currency,USD ($)"))
        assertTrue(csv.contains("Year,Principal Paid (USD),Interest Paid (USD),Extra Prepayment (USD),Total Paid (USD),Ending Balance (USD)"))
        assertTrue(csv.contains("\n5,"))
        assertTrue(csv.contains(",0.00\n"))
    }

    @Test
    fun `schedule monthly csv includes prepayment summary and monthly rows`() {
        val input = LoanInput(
            principal = 60000.0,
            annualRatePercent = 8.0,
            tenureValue = 2,
            tenureUnit = TenureUnit.YEARS,
            extraPayment = 500.0,
            prepaymentFrequency = PrepaymentFrequency.MONTHLY
        )
        val result = EmiCalculatorEngine.calculate(input)

        val csv = CsvExporter.buildScheduleCsvContent(
            preset = LoanPreset.PERSONAL,
            input = input,
            result = result,
            currency = CurrencyOption.INR,
            granularity = ScheduleGranularity.MONTHLY
        )

        assertTrue(csv.contains("Prepayment Mode,Monthly Extra"))
        assertTrue(csv.contains("Month,Year,Opening Balance (INR),EMI (INR),Principal Paid (INR),Interest Paid (INR),Extra Prepayment (INR),Closing Balance (INR)"))
        assertTrue(csv.contains("\n1,1,60000.00,"))
    }

    @Test
    fun `comparison csv includes verdict and side by side metrics`() {
        val loanA = LoanInput(
            principal = 250000.0,
            annualRatePercent = 7.0,
            tenureValue = 20,
            tenureUnit = TenureUnit.YEARS,
            processingFeePercent = 0.5
        )
        val loanB = LoanInput(
            principal = 250000.0,
            annualRatePercent = 7.5,
            tenureValue = 15,
            tenureUnit = TenureUnit.YEARS,
            processingFeePercent = 0.25
        )
        val comparison = EmiCalculatorEngine.compare(loanA, loanB)

        val csv = CsvExporter.buildComparisonCsvContent(
            loanA = loanA,
            loanB = loanB,
            comparison = comparison,
            currency = CurrencyOption.EUR
        )

        assertTrue(csv.contains("Report,EMI Pulse Loan Comparison"))
        assertTrue(csv.contains("Metric,Loan A,Loan B,Difference"))
        assertTrue(csv.contains("Interest Rate (% p.a.),7.00,7.50,0.50"))
        assertTrue(csv.contains("Tenure (Months),240,180,60"))
        assertTrue(csv.contains("Total Payable (EUR)"))
    }

    @Test
    fun `csv escape quotes commas and double quotes properly`() {
        assertEquals("plain", CsvExporter.csvEscape("plain"))
        assertEquals("\"hello, world\"", CsvExporter.csvEscape("hello, world"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.csvEscape("say \"hi\""))
    }

    @Test
    fun `writeComparisonCsv emits utf8 bom prefix`() {
        val loanA = LoanInput()
        val loanB = LoanInput(annualRatePercent = 8.0)
        val cmp = EmiCalculatorEngine.compare(loanA, loanB)
        val out = ByteArrayOutputStream()

        CsvExporter.writeComparisonCsv(out, loanA, loanB, cmp, CurrencyOption.USD)
        val bytes = out.toByteArray()

        assertTrue(bytes.size > 3)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
    }
}
