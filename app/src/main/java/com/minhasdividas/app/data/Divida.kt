package com.minhasdividas.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Categoria(val rotulo: String) {
    CARTAO("Cartão de crédito"),
    CONTA_MENSAL("Conta mensal"),
    EMPRESTIMO("Empréstimo"),
    FINANCIAMENTO("Financiamento"),
    OUTROS("Outros"),
}

@Entity(tableName = "dividas")
data class Divida(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    /** Valor de cada parcela (ou o valor total, se não for parcelada), em centavos. */
    val valorCentavos: Long,
    /** Próximo vencimento, em dias desde 1970-01-01. */
    val vencimentoEpochDay: Long,
    val categoria: Categoria,
    val totalParcelas: Int = 1,
    val parcelasPagas: Int = 0,
    val paga: Boolean = false,
    /** Conta mensal fixa (luz, internet, assinatura): nunca é quitada, só avança para o próximo mês. */
    val recorrente: Boolean = false,
    /** Dia do mês do vencimento original, para não "escorregar" após meses curtos (31 → 28 → 31). */
    val diaVencimento: Int = 0,
)

val Divida.vencimento: LocalDate get() = LocalDate.ofEpochDay(vencimentoEpochDay)
val Divida.parcelada: Boolean get() = totalParcelas > 1
val Divida.parcelaAtual: Int get() = (parcelasPagas + 1).coerceAtMost(totalParcelas)
val Divida.parcelasRestantes: Int get() = when {
    paga -> 0
    recorrente -> 1
    else -> totalParcelas - parcelasPagas
}
val Divida.valorRestanteCentavos: Long get() = valorCentavos * parcelasRestantes

/** Próximo vencimento mensal, mantendo o dia original quando o mês o comporta. */
fun Divida.proximoVencimento(): LocalDate {
    val proximo = vencimento.plusMonths(1)
    val dia = if (diaVencimento > 0) diaVencimento else vencimento.dayOfMonth
    return proximo.withDayOfMonth(minOf(dia, proximo.lengthOfMonth()))
}

/**
 * Paga a parcela atual. Conta mensal fixa: avança um mês, sem fim.
 * Parcelada: avança um mês, e a última parcela quita a dívida. À vista: quita.
 */
fun Divida.pagarParcela(): Divida = when {
    recorrente -> copy(parcelasPagas = parcelasPagas + 1, vencimentoEpochDay = proximoVencimento().toEpochDay())
    parcelasPagas + 1 >= totalParcelas -> copy(parcelasPagas = totalParcelas, paga = true)
    else -> copy(parcelasPagas = parcelasPagas + 1, vencimentoEpochDay = proximoVencimento().toEpochDay())
}

/** Volta uma dívida quitada para pendente, reabrindo a última parcela. */
fun Divida.reabrir(): Divida =
    copy(paga = false, parcelasPagas = (totalParcelas - 1).coerceAtLeast(0))
