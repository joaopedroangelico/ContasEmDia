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
    /** De onde vem a cobrança (banco, loja, empresa). Opcional: vazio quando não informado. */
    val origem: String = "",
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

/** Vencimento deslocado em [meses], mantendo o dia original quando o mês o comporta. */
private fun Divida.vencimentoDeslocado(meses: Long): LocalDate {
    val alvo = vencimento.plusMonths(meses)
    val dia = if (diaVencimento > 0) diaVencimento else vencimento.dayOfMonth
    return alvo.withDayOfMonth(minOf(dia, alvo.lengthOfMonth()))
}

fun Divida.proximoVencimento(): LocalDate = vencimentoDeslocado(1)

/**
 * Parcelada ou mensal fixa cuja conta deste mês já foi paga: houve pagamento e o
 * próximo vencimento já caiu num mês futuro.
 */
fun Divida.pagaNoMes(hoje: LocalDate): Boolean =
    !paga && (recorrente || totalParcelas > 1) && parcelasPagas > 0 &&
        vencimento.isAfter(hoje.withDayOfMonth(hoje.lengthOfMonth()))

/** Desfaz o último pagamento de parcelada/mensal fixa: volta uma parcela e um mês. */
fun Divida.desfazerUltimoPagamento(): Divida =
    copy(parcelasPagas = (parcelasPagas - 1).coerceAtLeast(0), vencimentoEpochDay = vencimentoDeslocado(-1).toEpochDay())

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

/** Compras de um cartão dentro da gaveta "Cartão de crédito". [nome] vazio = cartão não informado. */
data class Cartao(val nome: String, val dividas: List<Divida>)

/**
 * Agrupa pelo nome do cartão ([Divida.origem]), ignorando maiúsculas e espaços ("nubank" = "Nubank "),
 * em ordem alfabética, com as compras sem cartão no fim. Mantém a ordem das dívidas dentro de cada grupo.
 */
fun agruparPorCartao(dividas: List<Divida>): List<Cartao> =
    dividas.groupBy { it.origem.trim().lowercase(LocaleBR) }
        .map { (chave, lista) -> Cartao(if (chave.isEmpty()) "" else lista.first().origem.trim(), lista) }
        .sortedWith(compareBy<Cartao> { it.nome.isEmpty() }.thenBy { it.nome.lowercase(LocaleBR) })

/** Nomes de cartão já usados, para sugerir no cadastro. */
fun nomesDeCartao(dividas: List<Divida>): List<String> =
    agruparPorCartao(dividas.filter { it.categoria == Categoria.CARTAO }).map { it.nome }.filter { it.isNotEmpty() }
