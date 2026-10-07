package com.minhasdividas.app.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")
private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocaleBR)
private val formatoDataCurta = DateTimeFormatter.ofPattern("dd/MM", LocaleBR)

fun formatarMoeda(centavos: Long): String =
    NumberFormat.getCurrencyInstance(LocaleBR).format(centavos / 100.0)

fun formatarData(data: LocalDate): String = formatoData.format(data)

fun formatarDataCurta(data: LocalDate): String = formatoDataCurta.format(data)

fun diasAte(data: LocalDate, hoje: LocalDate): Long = ChronoUnit.DAYS.between(hoje, data)

fun textoVencimento(data: LocalDate, hoje: LocalDate): String {
    val dias = diasAte(data, hoje)
    return when {
        dias < -1 -> "Venceu há ${-dias} dias"
        dias == -1L -> "Venceu ontem"
        dias == 0L -> "Vence hoje"
        dias == 1L -> "Vence amanhã"
        dias <= 30 -> "Vence em $dias dias"
        else -> "Vence em ${formatarDataCurta(data)}"
    }
}
