package nl.ecannemeijer.casacheck.ui.common

import java.text.NumberFormat
import java.util.Locale

object Fmt {
    fun euro(v: Long?, locale: Locale = Locale.getDefault()): String {
        if (v == null) return "–"
        val f = NumberFormat.getCurrencyInstance(locale).apply { currency = java.util.Currency.getInstance("EUR"); maximumFractionDigits = 0 }
        return f.format(v)
    }
    fun range(min: Long, max: Long, locale: Locale = Locale.getDefault()): String =
        if (min == max) euro(min, locale) else euro(min, locale) + " – " + euro(max, locale)
    fun sqm(v: Double?): String = v?.let { (if (it % 1.0 == 0.0) it.toLong().toString() else "%.1f".format(it)) + " m²" } ?: "–"
    fun num(v: Double?): String = v?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
    fun parseLong(s: String): Long? = s.filter { it.isDigit() }.takeIf { it.isNotEmpty() && it.length < 13 }?.toLong()
    fun parseDouble(s: String): Double? = s.replace(',', '.').trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}
