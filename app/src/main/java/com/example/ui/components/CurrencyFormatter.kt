package com.example.ui.components

import com.example.domain.CurrencyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

object CurrencyFormatter {

    fun format(
        amount: Double,
        currency: CurrencyOption,
        showDecimals: Boolean = true,
        useDeviceLocale: Boolean = false,
        localeOverride: Locale? = null
    ): String {
        val targetLocale = localeOverride ?: if (useDeviceLocale) Locale.getDefault() else currency.locale
        val symbols = DecimalFormatSymbols.getInstance(targetLocale)
        val fractionDigits = if (showDecimals) currency.fractionDigits else 0

        val isNegative = amount < -0.0005
        val absVal = abs(amount)

        val formattedNumber = if (currency.usesIndianGrouping && !useDeviceLocale && localeOverride == null) {
            formatIndianNumber(absVal, fractionDigits, symbols)
        } else {
            val pattern = if (fractionDigits > 0) {
                "#,##0." + "0".repeat(fractionDigits)
            } else {
                "#,##0"
            }
            val factor = 10.0.pow(fractionDigits)
            val rounded = kotlin.math.round(absVal * factor) / factor
            DecimalFormat(pattern, symbols).format(rounded)
        }

        val sign = if (isNegative) "-" else ""
        return placeCurrencySymbol(
            sign = sign,
            formattedNumber = formattedNumber,
            currency = currency,
            locale = targetLocale
        )
    }

    fun formatCompact(
        amount: Double,
        currency: CurrencyOption,
        useDeviceLocale: Boolean = false,
        localeOverride: Locale? = null
    ): String {
        val targetLocale = localeOverride ?: if (useDeviceLocale) Locale.getDefault() else currency.locale
        val symbols = DecimalFormatSymbols.getInstance(targetLocale)
        val absVal = abs(amount)
        val sign = if (amount < -0.0005) "-" else ""

        val useIndianScale = currency.usesIndianGrouping && !useDeviceLocale && localeOverride == null
        val numberPart = if (useIndianScale) {
            when {
                absVal >= 10_000_000 -> formatDecimal(absVal / 10_000_000.0, 2, symbols) + "Cr"
                absVal >= 100_000 -> formatDecimal(absVal / 100_000.0, 2, symbols) + "L"
                absVal >= 1_000 -> formatDecimal(absVal / 1_000.0, 1, symbols) + "K"
                else -> formatDecimal(absVal, currency.fractionDigits.coerceAtMost(2), symbols)
            }
        } else {
            when {
                absVal >= 1_000_000 -> formatDecimal(absVal / 1_000_000.0, 2, symbols) + "M"
                absVal >= 1_000 -> formatDecimal(absVal / 1_000.0, 1, symbols) + "K"
                else -> formatDecimal(absVal, currency.fractionDigits.coerceAtMost(2), symbols)
            }
        }

        return placeCurrencySymbol(
            sign = sign,
            formattedNumber = numberPart,
            currency = currency,
            locale = targetLocale
        )
    }

    fun formatPercent(
        percent: Double,
        decimals: Int = 2,
        currency: CurrencyOption = CurrencyOption.USD,
        useDeviceLocale: Boolean = false
    ): String {
        val targetLocale = if (useDeviceLocale) Locale.getDefault() else currency.locale
        val symbols = DecimalFormatSymbols.getInstance(targetLocale)
        val numStr = formatPlainNumber(percent, decimals, symbols.decimalSeparator)
        return "$numStr%"
    }

    fun formatPlainNumber(
        amount: Double,
        decimals: Int = 0,
        decimalSeparator: Char = '.'
    ): String {
        return if (decimals <= 0) {
            amount.roundToLong().toString()
        } else {
            val formatted = String.format(Locale.US, "%.${decimals}f", amount)
                .trimEnd('0')
                .trimEnd('.')
            if (decimalSeparator == '.') formatted else formatted.replace('.', decimalSeparator)
        }
    }

    fun formatTenureMonths(totalMonths: Int): String {
        if (totalMonths <= 0) return "0m"
        val years = totalMonths / 12
        val months = totalMonths % 12
        return when {
            years > 0 && months > 0 -> "${years}y ${months}m"
            years > 0 -> "${years}y"
            else -> "${months}m"
        }
    }

    fun decimalSeparator(currency: CurrencyOption, useDeviceLocale: Boolean = false): Char {
        val locale = if (useDeviceLocale) Locale.getDefault() else currency.locale
        return DecimalFormatSymbols.getInstance(locale).decimalSeparator
    }

    private fun placeCurrencySymbol(
        sign: String,
        formattedNumber: String,
        currency: CurrencyOption,
        locale: Locale
    ): String {
        val suffixPlacement = currency == CurrencyOption.EUR &&
            locale.language in setOf("de", "fr", "es", "it", "pt", "nl")
        return if (suffixPlacement) {
            "$sign$formattedNumber ${currency.symbol}"
        } else {
            val spacer = if (currency.symbol.length > 1) " " else ""
            "$sign${currency.symbol}$spacer$formattedNumber"
        }
    }

    private fun formatDecimal(value: Double, decimals: Int, symbols: DecimalFormatSymbols): String {
        val pattern = if (decimals > 0) {
            "#,##0." + "0".repeat(decimals)
        } else {
            "#,##0"
        }
        return DecimalFormat(pattern, symbols).format(value)
    }

    private fun formatIndianNumber(
        value: Double,
        fractionDigits: Int,
        symbols: DecimalFormatSymbols
    ): String {
        val factor = 10.0.pow(fractionDigits)
        val rounded = kotlin.math.round(value * factor) / factor
        val integerPart = rounded.toLong()
        val intStr = integerPart.toString()

        val sep = symbols.groupingSeparator
        val groupedInt = if (intStr.length <= 3) {
            intStr
        } else {
            val lastThree = intStr.takeLast(3)
            val rest = intStr.dropLast(3)
            val restGrouped = rest.reversed().chunked(2).joinToString(sep.toString()).reversed()
            "$restGrouped$sep$lastThree"
        }

        return if (fractionDigits > 0) {
            val frac = kotlin.math.round((rounded - integerPart) * factor)
                .toLong()
                .coerceIn(0, factor.toLong() - 1)
            val fracStr = frac.toString().padStart(fractionDigits, '0')
            "$groupedInt${symbols.decimalSeparator}$fracStr"
        } else {
            groupedInt
        }
    }
}
