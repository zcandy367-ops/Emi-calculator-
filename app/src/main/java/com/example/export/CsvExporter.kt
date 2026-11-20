package com.example.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.ComparisonResult
import com.example.domain.CurrencyOption
import com.example.domain.EmiResult
import com.example.domain.LoanInput
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.ui.ScheduleGranularity
import com.example.ui.components.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun defaultScheduleFileName(preset: LoanPreset, granularity: ScheduleGranularity): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val presetSlug = preset.name.lowercase(Locale.US)
        val modeSlug = granularity.name.lowercase(Locale.US)
        return "emi_pulse_${presetSlug}_${modeSlug}_$stamp.csv"
    }

    fun defaultComparisonFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "emi_pulse_loan_comparison_$stamp.csv"
    }

    fun exportScheduleToCacheUri(
        context: Context,
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity
    ): Uri {
        val exportDir = prepareExportDir(context)
        val outFile = File(exportDir, defaultScheduleFileName(preset, granularity))
        FileOutputStream(outFile).use { fos ->
            writeScheduleCsv(fos, preset, input, result, currency, granularity)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun exportComparisonToCacheUri(
        context: Context,
        loanA: LoanInput,
        loanB: LoanInput,
        comparison: ComparisonResult,
        currency: CurrencyOption
    ): Uri {
        val exportDir = prepareExportDir(context)
        val outFile = File(exportDir, defaultComparisonFileName())
        FileOutputStream(outFile).use { fos ->
            writeComparisonCsv(fos, loanA, loanB, comparison, currency)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun writeScheduleCsv(
        outputStream: OutputStream,
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity
    ) {
        val content = buildScheduleCsvContent(
            preset = preset,
            input = input,
            result = result,
            currency = currency,
            granularity = granularity
        )
        writeUtf8WithBom(outputStream, content)
    }

    fun writeComparisonCsv(
        outputStream: OutputStream,
        loanA: LoanInput,
        loanB: LoanInput,
        comparison: ComparisonResult,
        currency: CurrencyOption
    ) {
        val content = buildComparisonCsvContent(
            loanA = loanA,
            loanB = loanB,
            comparison = comparison,
            currency = currency
        )
        writeUtf8WithBom(outputStream, content)
    }

    fun buildScheduleCsvContent(
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity
    ): String {
        val sb = StringBuilder()
        val presetName = presetDisplayName(preset)
        val granularityName = if (granularity == ScheduleGranularity.YEARLY) "Yearly" else "Monthly"

        appendCsvRow(sb, "Report", "EMI Pulse Amortization Schedule")
        appendCsvRow(sb, "Loan Type", presetName)
        appendCsvRow(sb, "Currency", "${currency.code} (${currency.symbol})")
        appendCsvRow(sb, "Schedule View", granularityName)
        appendCsvRow(sb, "Principal", formatNumeric(result.principal))
        appendCsvRow(sb, "Annual Interest Rate (%)", formatNumeric(input.annualRatePercent))
        appendCsvRow(sb, "Tenure", CurrencyFormatter.formatTenureMonths(result.effectiveMonths))
        appendCsvRow(sb, "Monthly EMI", formatNumeric(result.monthlyEmi))
        appendCsvRow(sb, "Total Interest", formatNumeric(result.effectiveTotalInterest))
        appendCsvRow(sb, "Total Payable", formatNumeric(result.effectiveTotalPayable))

        if (input.prepaymentFrequency != PrepaymentFrequency.NONE && input.extraPayment > 0.0) {
            val freqLabel = if (input.prepaymentFrequency == PrepaymentFrequency.MONTHLY) "Monthly Extra" else "Yearly Lump Sum"
            appendCsvRow(sb, "Prepayment Mode", freqLabel)
            appendCsvRow(sb, "Extra Payment", formatNumeric(input.extraPayment))
            appendCsvRow(sb, "Interest Saved", formatNumeric(result.interestSaved))
            appendCsvRow(sb, "Months Saved", result.monthsSaved.toString())
        }

        sb.append('\n')

        if (granularity == ScheduleGranularity.YEARLY) {
            appendCsvRow(
                sb,
                "Year",
                "Principal Paid (${currency.code})",
                "Interest Paid (${currency.code})",
                "Extra Prepayment (${currency.code})",
                "Total Paid (${currency.code})",
                "Ending Balance (${currency.code})"
            )
            result.yearlySchedule.forEach { year ->
                val totalPaid = year.totalPrincipalPaid + year.totalInterestPaid + year.totalExtraPaid
                appendCsvRow(
                    sb,
                    year.yearIndex.toString(),
                    formatNumeric(year.totalPrincipalPaid),
                    formatNumeric(year.totalInterestPaid),
                    formatNumeric(year.totalExtraPaid),
                    formatNumeric(totalPaid),
                    formatNumeric(year.endingBalance)
                )
            }
        } else {
            appendCsvRow(
                sb,
                "Month",
                "Year",
                "Opening Balance (${currency.code})",
                "EMI (${currency.code})",
                "Principal Paid (${currency.code})",
                "Interest Paid (${currency.code})",
                "Extra Prepayment (${currency.code})",
                "Closing Balance (${currency.code})"
            )
            result.monthlySchedule.forEach { month ->
                appendCsvRow(
                    sb,
                    month.monthIndex.toString(),
                    month.yearIndex.toString(),
                    formatNumeric(month.openingBalance),
                    formatNumeric(month.emiPaid),
                    formatNumeric(month.principalPaid),
                    formatNumeric(month.interestPaid),
                    formatNumeric(month.extraPaid),
                    formatNumeric(month.closingBalance)
                )
            }
        }

        return sb.toString()
    }

    fun buildComparisonCsvContent(
        loanA: LoanInput,
        loanB: LoanInput,
        comparison: ComparisonResult,
        currency: CurrencyOption
    ): String {
        val sb = StringBuilder()
        val resA = comparison.resultA
        val resB = comparison.resultB
        val verdict = when (comparison.cheaperSide) {
            -1 -> "Loan A cheaper overall"
            1 -> "Loan B cheaper overall"
            else -> "Both loans cost the same overall"
        }

        val intPctA = if (resA.principal > 0.0) (resA.baselineTotalInterest / resA.principal) * 100.0 else 0.0
        val intPctB = if (resB.principal > 0.0) (resB.baselineTotalInterest / resB.principal) * 100.0 else 0.0

        appendCsvRow(sb, "Report", "EMI Pulse Loan Comparison")
        appendCsvRow(sb, "Currency", "${currency.code} (${currency.symbol})")
        appendCsvRow(sb, "Verdict", verdict)
        appendCsvRow(sb, "Overall Savings (${currency.code})", formatNumeric(comparison.totalSavings))
        appendCsvRow(sb, "Monthly EMI Difference (${currency.code})", formatNumeric(comparison.monthlyEmiDiff))
        sb.append('\n')

        appendCsvRow(sb, "Metric", "Loan A", "Loan B", "Difference")
        appendCsvRow(
            sb,
            "Principal (${currency.code})",
            formatNumeric(loanA.principal),
            formatNumeric(loanB.principal),
            formatNumeric(kotlin.math.abs(loanA.principal - loanB.principal))
        )
        appendCsvRow(
            sb,
            "Interest Rate (% p.a.)",
            formatNumeric(loanA.annualRatePercent),
            formatNumeric(loanB.annualRatePercent),
            formatNumeric(kotlin.math.abs(loanA.annualRatePercent - loanB.annualRatePercent))
        )
        appendCsvRow(
            sb,
            "Tenure (Months)",
            loanA.totalMonths.toString(),
            loanB.totalMonths.toString(),
            kotlin.math.abs(loanA.totalMonths - loanB.totalMonths).toString()
        )
        appendCsvRow(
            sb,
            "Monthly EMI (${currency.code})",
            formatNumeric(resA.monthlyEmi),
            formatNumeric(resB.monthlyEmi),
            formatNumeric(comparison.monthlyEmiDiff)
        )
        appendCsvRow(
            sb,
            "Total Interest (${currency.code})",
            formatNumeric(resA.baselineTotalInterest),
            formatNumeric(resB.baselineTotalInterest),
            formatNumeric(kotlin.math.abs(resA.baselineTotalInterest - resB.baselineTotalInterest))
        )
        appendCsvRow(
            sb,
            "Interest to Principal (%)",
            formatNumeric(intPctA),
            formatNumeric(intPctB),
            formatNumeric(kotlin.math.abs(intPctA - intPctB))
        )
        appendCsvRow(
            sb,
            "Processing Fee %",
            formatNumeric(loanA.processingFeePercent),
            formatNumeric(loanB.processingFeePercent),
            formatNumeric(kotlin.math.abs(loanA.processingFeePercent - loanB.processingFeePercent))
        )
        appendCsvRow(
            sb,
            "Processing Fee (${currency.code})",
            formatNumeric(comparison.feeA),
            formatNumeric(comparison.feeB),
            formatNumeric(kotlin.math.abs(comparison.feeA - comparison.feeB))
        )
        appendCsvRow(
            sb,
            "Total Payable (${currency.code})",
            formatNumeric(comparison.totalCostA),
            formatNumeric(comparison.totalCostB),
            formatNumeric(comparison.totalSavings)
        )

        return sb.toString()
    }

    private fun prepareExportDir(context: Context): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        exportDir.listFiles()?.forEach { oldFile ->
            if (System.currentTimeMillis() - oldFile.lastModified() > 60 * 60 * 1000L) {
                oldFile.delete()
            }
        }
        return exportDir
    }

    private fun writeUtf8WithBom(outputStream: OutputStream, content: String) {
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    private fun appendCsvRow(sb: StringBuilder, vararg fields: String) {
        fields.forEachIndexed { index, value ->
            if (index > 0) sb.append(',')
            sb.append(csvEscape(value))
        }
        sb.append('\n')
    }

    internal fun csvEscape(value: String): String {
        val needsQuoting = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        return if (needsQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatNumeric(value: Double): String {
        val safe = if (value.isNaN() || value.isInfinite()) 0.0 else value
        return String.format(Locale.US, "%.2f", safe)
    }

    private fun presetDisplayName(preset: LoanPreset): String = when (preset) {
        LoanPreset.HOME -> "Home Loan"
        LoanPreset.CAR -> "Car Loan"
        LoanPreset.PERSONAL -> "Personal Loan"
        LoanPreset.EDUCATION -> "Education Loan"
    }
}
