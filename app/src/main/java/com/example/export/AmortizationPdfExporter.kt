package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import androidx.core.content.FileProvider
import com.example.domain.CurrencyOption
import com.example.domain.EmiResult
import com.example.domain.LoanInput
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.ui.ScheduleGranularity
import com.example.ui.components.CurrencyFormatter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AmortizationPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_H * 2

    private const val EMERALD_DARK = 0xFF064E3B.toInt()
    private const val EMERALD_PRIMARY = 0xFF0B6E4F.toInt()
    private const val EMERALD_TINT = 0xFFECFDF5.toInt()
    private const val AMBER_ACCENT = 0xFFB45309.toInt()
    private const val SLATE_DARK = 0xFF0F1B17.toInt()
    private const val SLATE_MUTED = 0xFF4B5F58.toInt()
    private const val BORDER_LIGHT = 0xFFD4DFDA.toInt()
    private const val ROW_ALT_BG = 0xFFF6F9F8.toInt()

    fun defaultFileName(preset: LoanPreset, granularity: ScheduleGranularity): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val presetSlug = preset.name.lowercase(Locale.US)
        val modeSlug = granularity.name.lowercase(Locale.US)
        return "emi_pulse_${presetSlug}_${modeSlug}_$stamp.pdf"
    }

    fun exportToCacheUri(
        context: Context,
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity,
        useDeviceLocale: Boolean = false
    ): Uri {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Clean previous temporary exports to keep cache tidy
        exportDir.listFiles()?.forEach { oldFile ->
            if (System.currentTimeMillis() - oldFile.lastModified() > 60 * 60 * 1000L) {
                oldFile.delete()
            }
        }
        val outFile = File(exportDir, defaultFileName(preset, granularity))
        FileOutputStream(outFile).use { fos ->
            writePdf(fos, preset, input, result, currency, granularity, useDeviceLocale)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun exportToCacheFile(
        context: Context,
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity,
        useDeviceLocale: Boolean = false
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, defaultFileName(preset, granularity))
        FileOutputStream(outFile).use { fos ->
            writePdf(fos, preset, input, result, currency, granularity, useDeviceLocale)
        }
        return outFile
    }

    fun writePdf(
        outputStream: OutputStream,
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity,
        useDeviceLocale: Boolean = false
    ) {
        val document = PdfDocument()
        try {
            val includePrepayCol = input.prepaymentFrequency != PrepaymentFrequency.NONE &&
                input.extraPayment > 0.0
            val pageChunks = buildPageChunks(result, currency, granularity, includePrepayCol, useDeviceLocale)

            val totalPages = pageChunks.size
            val generatedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date())

            pageChunks.forEachIndexed { index, chunk ->
                val pageNumber = index + 1
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                var cursorY = MARGIN_TOP
                if (pageNumber == 1) {
                    cursorY = drawFirstPageHeaderAndSummary(
                        canvas = canvas,
                        preset = preset,
                        input = input,
                        result = result,
                        currency = currency,
                        granularity = granularity,
                        generatedDate = generatedDate,
                        useDeviceLocale = useDeviceLocale
                    )
                } else {
                    cursorY = drawContinuationHeader(
                        canvas = canvas,
                        preset = preset,
                        granularity = granularity
                    )
                }

                drawScheduleTable(
                    canvas = canvas,
                    startY = cursorY,
                    rows = chunk,
                    includePrepayCol = includePrepayCol
                )

                drawPageFooter(canvas, pageNumber, totalPages)
                document.finishPage(page)
            }

            document.writeTo(outputStream)
            outputStream.flush()
        } finally {
            document.close()
        }
    }

    fun createPrintAdapter(
        context: Context,
        pdfFile: File,
        jobTitle: String
    ): PrintDocumentAdapter {
        return object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobTitle)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message ?: "Failed to write PDF")
                }
            }
        }
    }

    internal data class PdfScheduleRow(
        val period: String,
        val principal: String,
        val interest: String,
        val extra: String,
        val balance: String
    )

    internal fun buildPageChunks(
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity,
        includePrepayCol: Boolean,
        useDeviceLocale: Boolean = false
    ): List<List<PdfScheduleRow>> {
        val rows = buildTableRows(result, currency, granularity, useDeviceLocale)
        val firstPageTableTop = if (includePrepayCol) 288f else 252f
        val subsequentPageTableTop = 76f
        val tableHeaderHeight = 24f
        val rowHeight = 20f
        val usableBottom = PAGE_HEIGHT - MARGIN_BOTTOM

        val firstPageRowsCapacity = ((usableBottom - firstPageTableTop - tableHeaderHeight) / rowHeight)
            .toInt()
            .coerceAtLeast(1)
        val subsequentPageRowsCapacity = ((usableBottom - subsequentPageTableTop - tableHeaderHeight) / rowHeight)
            .toInt()
            .coerceAtLeast(1)

        val pageChunks = mutableListOf<List<PdfScheduleRow>>()
        if (rows.isEmpty()) {
            pageChunks.add(emptyList())
        } else {
            var offset = 0
            val firstEnd = minOf(rows.size, firstPageRowsCapacity)
            pageChunks.add(rows.subList(0, firstEnd))
            offset = firstEnd
            while (offset < rows.size) {
                val nextEnd = minOf(rows.size, offset + subsequentPageRowsCapacity)
                pageChunks.add(rows.subList(offset, nextEnd))
                offset = nextEnd
            }
        }
        return pageChunks
    }

    private fun buildTableRows(
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity,
        useDeviceLocale: Boolean
    ): List<PdfScheduleRow> {
        return when (granularity) {
            ScheduleGranularity.YEARLY -> result.yearlySchedule.map { year ->
                PdfScheduleRow(
                    period = "Year ${year.yearIndex}",
                    principal = CurrencyFormatter.format(year.totalPrincipalPaid, currency, useDeviceLocale = useDeviceLocale),
                    interest = CurrencyFormatter.format(year.totalInterestPaid, currency, useDeviceLocale = useDeviceLocale),
                    extra = CurrencyFormatter.format(year.totalExtraPaid, currency, useDeviceLocale = useDeviceLocale),
                    balance = CurrencyFormatter.format(year.endingBalance, currency, useDeviceLocale = useDeviceLocale)
                )
            }
            ScheduleGranularity.MONTHLY -> result.monthlySchedule.map { month ->
                PdfScheduleRow(
                    period = "M${month.monthIndex} (Yr ${month.yearIndex})",
                    principal = CurrencyFormatter.format(month.principalPaid, currency, useDeviceLocale = useDeviceLocale),
                    interest = CurrencyFormatter.format(month.interestPaid, currency, useDeviceLocale = useDeviceLocale),
                    extra = CurrencyFormatter.format(month.extraPaid, currency, useDeviceLocale = useDeviceLocale),
                    balance = CurrencyFormatter.format(month.closingBalance, currency, useDeviceLocale = useDeviceLocale)
                )
            }
        }
    }

    private fun drawFirstPageHeaderAndSummary(
        canvas: Canvas,
        preset: LoanPreset,
        input: LoanInput,
        result: EmiResult,
        currency: CurrencyOption,
        granularity: ScheduleGranularity,
        generatedDate: String,
        useDeviceLocale: Boolean
    ): Float {
        // Brand Header Banner
        val bannerRect = RectF(MARGIN_H, MARGIN_TOP, PAGE_WIDTH - MARGIN_H, MARGIN_TOP + 64f)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EMERALD_DARK }
        canvas.drawRoundRect(bannerRect, 10f, 10f, bannerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(194, 239, 224)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val rightMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(194, 239, 224)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }

        val presetTitle = when (preset) {
            LoanPreset.HOME -> "Home Loan"
            LoanPreset.CAR -> "Car Loan"
            LoanPreset.PERSONAL -> "Personal Loan"
            LoanPreset.EDUCATION -> "Education Loan"
        }
        val granLabel = if (granularity == ScheduleGranularity.YEARLY) "Yearly Summary" else "Monthly Schedule"

        canvas.drawText("EMI Pulse — Amortization Report", MARGIN_H + 16f, MARGIN_TOP + 27f, titlePaint)
        canvas.drawText("$presetTitle • $granLabel", MARGIN_H + 16f, MARGIN_TOP + 46f, subtitlePaint)
        canvas.drawText("Generated $generatedDate", PAGE_WIDTH - MARGIN_H - 16f, MARGIN_TOP + 27f, rightMetaPaint)
        canvas.drawText("Currency: ${currency.code} (${currency.symbol})", PAGE_WIDTH - MARGIN_H - 16f, MARGIN_TOP + 46f, rightMetaPaint)

        // Summary Card
        val hasPrepay = input.prepaymentFrequency != PrepaymentFrequency.NONE && input.extraPayment > 0.0
        val cardTop = MARGIN_TOP + 78f
        val cardHeight = if (hasPrepay) 128f else 92f
        val cardRect = RectF(MARGIN_H, cardTop, PAGE_WIDTH - MARGIN_H, cardTop + cardHeight)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EMERALD_TINT }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BORDER_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(cardRect, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 10f, 10f, cardBorderPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SLATE_MUTED
            textSize = 9f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SLATE_DARK
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val emiValuePaint = Paint(valuePaint).apply {
            color = EMERALD_PRIMARY
            textSize = 15f
        }
        val interestValuePaint = Paint(valuePaint).apply {
            color = AMBER_ACCENT
        }

        val colWidth = CONTENT_WIDTH / 3f
        val row1Y = cardTop + 20f
        val row2Y = cardTop + 60f

        // Row 1: Monthly EMI | Principal | Interest Rate
        canvas.drawText("MONTHLY EMI", MARGIN_H + 14f, row1Y, labelPaint)
        canvas.drawText(CurrencyFormatter.format(result.monthlyEmi, currency, useDeviceLocale = useDeviceLocale), MARGIN_H + 14f, row1Y + 17f, emiValuePaint)

        canvas.drawText("LOAN PRINCIPAL", MARGIN_H + colWidth + 14f, row1Y, labelPaint)
        canvas.drawText(CurrencyFormatter.format(result.principal, currency, useDeviceLocale = useDeviceLocale), MARGIN_H + colWidth + 14f, row1Y + 17f, valuePaint)

        canvas.drawText("INTEREST RATE", MARGIN_H + colWidth * 2f + 14f, row1Y, labelPaint)
        canvas.drawText("${CurrencyFormatter.formatPercent(input.annualRatePercent, 2, currency, useDeviceLocale)} p.a.", MARGIN_H + colWidth * 2f + 14f, row1Y + 17f, valuePaint)

        // Row 2: Tenure | Total Interest | Total Payable
        canvas.drawText("TENURE", MARGIN_H + 14f, row2Y, labelPaint)
        canvas.drawText(
            "${CurrencyFormatter.formatTenureMonths(result.effectiveMonths)} (${result.effectiveMonths} mos)",
            MARGIN_H + 14f,
            row2Y + 17f,
            valuePaint
        )

        canvas.drawText("TOTAL INTEREST", MARGIN_H + colWidth + 14f, row2Y, labelPaint)
        canvas.drawText(CurrencyFormatter.format(result.effectiveTotalInterest, currency, useDeviceLocale = useDeviceLocale), MARGIN_H + colWidth + 14f, row2Y + 17f, interestValuePaint)

        canvas.drawText("TOTAL PAYABLE", MARGIN_H + colWidth * 2f + 14f, row2Y, labelPaint)
        canvas.drawText(CurrencyFormatter.format(result.effectiveTotalPayable, currency, useDeviceLocale = useDeviceLocale), MARGIN_H + colWidth * 2f + 14f, row2Y + 17f, valuePaint)

        if (hasPrepay) {
            val prepayY = cardTop + 98f
            val freqText = if (input.prepaymentFrequency == PrepaymentFrequency.MONTHLY) "Monthly" else "Yearly"
            val prepayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = EMERALD_DARK
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(
                "Prepayment ($freqText ${CurrencyFormatter.format(input.extraPayment, currency, useDeviceLocale = useDeviceLocale)}): " +
                    "Saved ${CurrencyFormatter.format(result.interestSaved, currency, useDeviceLocale = useDeviceLocale)} interest • ${result.monthsSaved} months cut",
                MARGIN_H + 14f,
                prepayY + 14f,
                prepayPaint
            )
        }

        return cardTop + cardHeight + 16f
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        preset: LoanPreset,
        granularity: ScheduleGranularity
    ): Float {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = EMERALD_DARK
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SLATE_MUTED
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val granLabel = if (granularity == ScheduleGranularity.YEARLY) "Yearly Summary" else "Monthly Schedule"
        canvas.drawText("EMI Pulse — Amortization Schedule (cont.)", MARGIN_H, MARGIN_TOP + 16f, headerPaint)
        canvas.drawText("${preset.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }} • $granLabel", PAGE_WIDTH - MARGIN_H, MARGIN_TOP + 16f, subPaint)

        val linePaint = Paint().apply {
            color = BORDER_LIGHT
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_H, MARGIN_TOP + 26f, PAGE_WIDTH - MARGIN_H, MARGIN_TOP + 26f, linePaint)
        return MARGIN_TOP + 38f
    }

    private fun drawScheduleTable(
        canvas: Canvas,
        startY: Float,
        rows: List<PdfScheduleRow>,
        includePrepayCol: Boolean
    ) {
        val headerHeight = 24f
        val rowHeight = 20f

        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EMERALD_PRIMARY }
        val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerTextRight = Paint(headerText).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawRoundRect(
            RectF(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + headerHeight),
            6f,
            6f,
            headerBg
        )

        val periodX = MARGIN_H + 10f
        val principalX: Float
        val interestX: Float
        val extraX: Float
        val balanceX = PAGE_WIDTH - MARGIN_H - 10f

        if (includePrepayCol) {
            principalX = MARGIN_H + CONTENT_WIDTH * 0.36f
            interestX = MARGIN_H + CONTENT_WIDTH * 0.56f
            extraX = MARGIN_H + CONTENT_WIDTH * 0.76f
        } else {
            principalX = MARGIN_H + CONTENT_WIDTH * 0.44f
            interestX = MARGIN_H + CONTENT_WIDTH * 0.70f
            extraX = 0f
        }

        val headerBaseline = startY + 15.5f
        canvas.drawText("PERIOD", periodX, headerBaseline, headerText)
        canvas.drawText("PRINCIPAL", principalX, headerBaseline, headerTextRight)
        canvas.drawText("INTEREST", interestX, headerBaseline, headerTextRight)
        if (includePrepayCol) {
            canvas.drawText("PREPAY", extraX, headerBaseline, headerTextRight)
        }
        canvas.drawText("BALANCE", balanceX, headerBaseline, headerTextRight)

        val rowBgAlt = Paint().apply { color = ROW_ALT_BG }
        val dividerPaint = Paint().apply {
            color = BORDER_LIGHT
            strokeWidth = 0.6f
        }
        val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SLATE_DARK
            textSize = 9.5f
        }
        val cellRight = Paint(cellText).apply { textAlign = Paint.Align.RIGHT }
        val principalPaint = Paint(cellRight).apply { color = EMERALD_PRIMARY }
        val interestPaint = Paint(cellRight).apply { color = AMBER_ACCENT }

        rows.forEachIndexed { idx, row ->
            val top = startY + headerHeight + idx * rowHeight
            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top + rowHeight, rowBgAlt)
            }
            val baseline = top + 13.5f
            canvas.drawText(row.period, periodX, baseline, cellText)
            canvas.drawText(row.principal, principalX, baseline, principalPaint)
            canvas.drawText(row.interest, interestX, baseline, interestPaint)
            if (includePrepayCol) {
                canvas.drawText(row.extra, extraX, baseline, cellRight)
            }
            canvas.drawText(row.balance, balanceX, baseline, cellRight)
            canvas.drawLine(MARGIN_H, top + rowHeight, PAGE_WIDTH - MARGIN_H, top + rowHeight, dividerPaint)
        }
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SLATE_MUTED
            textSize = 8.5f
        }
        val rightFooterPaint = Paint(footerPaint).apply { textAlign = Paint.Align.RIGHT }
        val y = PAGE_HEIGHT - 20f
        canvas.drawText("Generated by EMI Pulse", MARGIN_H, y, footerPaint)
        canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN_H, y, rightFooterPaint)
    }
}
