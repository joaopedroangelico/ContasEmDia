package com.minhasdividas.app.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")
private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocaleBR)
private val formatoDataCurta = DateTimeFormatter.ofPattern("dd/MM", LocaleBR)
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", LocaleBR)
private val formatoMes = DateTimeFormatter.ofPattern("LLLL 'de' yyyy", LocaleBR)

fun formatarMoeda(centavos: Long): String =
    NumberFormat.getCurrencyInstance(LocaleBR).format(centavos / 100.0)

fun formatarData(data: LocalDate): String = formatoData.format(data)

fun formatarDataCurta(data: LocalDate): String = formatoDataCurta.format(data)

fun formatarHora(momento: LocalDateTime): String = formatoHora.format(momento)

/** "12/10 às 14:32". */
fun formatarDataHoraCurta(momento: LocalDateTime): String =
    "${formatarDataCurta(momento.toLocalDate())} às ${formatarHora(momento)}"

/** "Outubro de 2026". */
fun nomeDoMes(mes: YearMonth): String =
    formatoMes.format(mes).replaceFirstChar { it.titlecase(LocaleBR) }

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
