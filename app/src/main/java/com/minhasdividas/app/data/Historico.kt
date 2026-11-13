package com.minhasdividas.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Um pagamento com a dívida a que pertence (para mostrar descrição, categoria e parcelas). */
data class PagamentoDaDivida(val pagamento: Pagamento, val divida: Divida)

/**
 * Um mês do histórico: o que foi pago nele (pela data do pagamento, não do vencimento) e as contas em
 * aberto que vencem nele. [completo] = nada daquele mês ficou em aberto.
 */
data class MesHistorico(
    val mes: YearMonth,
    val pagos: List<PagamentoDaDivida>,
    val pendentes: List<Divida>,
) {
    val totalPago: Long get() = pagos.sumOf { it.pagamento.valorCentavos }
    val totalPendente: Long get() = pendentes.sumOf { it.valorCentavos }
    val completo: Boolean get() = pendentes.isEmpty()
}

/**
 * Agrupa por mês, do mais recente para o mais antigo. Pagamentos cuja dívida não está em [dividas]
 * (filtrada por categoria, por exemplo) ficam de fora.
 */
fun agruparPorMes(dividas: List<Divida>, pagamentos: List<Pagamento>, zona: ZoneId): List<MesHistorico> {
    val porId = dividas.associateBy { it.id }
    val pagosPorMes = pagamentos
        .mapNotNull { p -> porId[p.dividaId]?.let { PagamentoDaDivida(p, it) } }
        .groupBy { it.pagamento.mes(zona) }
    val pendentesPorMes = dividas.filter { !it.paga }.groupBy { YearMonth.from(it.vencimento) }
    return (pagosPorMes.keys + pendentesPorMes.keys)
        .sortedDescending()
        .map { mes ->
            MesHistorico(
                mes = mes,
                pagos = pagosPorMes[mes].orEmpty().sortedByDescending { it.pagamento.pagoEmMillis },
                pendentes = pendentesPorMes[mes].orEmpty().sortedBy { it.vencimentoEpochDay },
            )
        }
}

/** Quitadas antes do histórico existir (ou importadas assim): não há data de pagamento para agrupar. */
fun quitadasSemHistorico(dividas: List<Divida>, pagamentos: List<Pagamento>): List<Divida> {
    val comPagamento = pagamentos.mapTo(HashSet()) { it.dividaId }
    return dividas.filter { it.paga && it.id !in comPagamento }
}

/** O que "Apagar mês" remove. */
data class LimpezaMes(
    val pagamentos: List<Pagamento>,
    /** Quitadas cujo histórico inteiro está neste mês: sem os pagamentos, não sobra nada delas. */
    val dividas: List<Divida>,
    /** Valores a receber únicos, já recebidos, previstos para este mês. */
    val recebimentos: List<Recebimento>,
) {
    val vazia: Boolean get() = pagamentos.isEmpty() && dividas.isEmpty() && recebimentos.isEmpty()
}

/**
 * Apagar um mês nunca mexe no que está em andamento: contas em aberto, parceladas e mensais fixas
 * continuam (só perdem o registro dos pagamentos daquele mês).
 */
fun planejarLimpeza(
    mes: YearMonth,
    dividas: List<Divida>,
    pagamentos: List<Pagamento>,
    recebimentos: List<Recebimento>,
    zona: ZoneId,
): LimpezaMes {
    val (doMes, outros) = pagamentos.partition { it.mes(zona) == mes }
    val tocadas = doMes.mapTo(HashSet()) { it.dividaId }
    val restantes = outros.mapTo(HashSet()) { it.dividaId }
    return LimpezaMes(
        pagamentos = doMes,
        dividas = dividas.filter { it.paga && it.id in tocadas && it.id !in restantes },
        recebimentos = recebimentos.filter { !it.recorrente && it.recebido && YearMonth.from(it.data) == mes },
    )
}

/** Meses que têm algo para apagar, do mais recente para o mais antigo. */
fun mesesComHistorico(pagamentos: List<Pagamento>, recebimentos: List<Recebimento>, zona: ZoneId): List<YearMonth> =
    (
        pagamentos.map { it.mes(zona) } +
            recebimentos.filter { !it.recorrente && it.recebido }.map { YearMonth.from(it.data) }
        ).distinct().sortedDescending()

// ---------------------------------------------------------------------------
// Sobra do mês e saldo livre
// ---------------------------------------------------------------------------

/**
 * Quanto a dívida pesa no mês de [hoje]: o que foi pago nele (pelo histórico, qualquer que fosse o
 * vencimento) + o que ainda falta pagar até o fim dele (inclui vencidas). Dívidas sem nenhum pagamento
 * registrado (pagas antes da v1.4) seguem a regra antiga, baseada só na situação da conta.
 */
fun Divida.pesoNoMes(pagamentosDela: List<Pagamento>, hoje: LocalDate, zona: ZoneId): Long {
    val mes = YearMonth.from(hoje)
    val fim = mes.atEndOfMonth().toEpochDay()
    val aPagar = if (!paga && vencimentoEpochDay <= fim) valorCentavos else 0L
    if (pagamentosDela.isEmpty()) {
        val inicio = mes.atDay(1).toEpochDay()
        return when {
            paga -> if (vencimentoEpochDay in inicio..fim) valorCentavos else 0L
            pagaNoMes(hoje) -> valorCentavos
            else -> aPagar
        }
    }
    return pagamentosDela.filter { it.mes(zona) == mes }.sumOf { it.valorCentavos } + aPagar
}

/** Contas do mês: soma de [pesoNoMes] de cada dívida. */
fun contasDoMes(dividas: List<Divida>, pagamentos: List<Pagamento>, hoje: LocalDate, zona: ZoneId): Long {
    val porDivida = pagamentos.groupBy { it.dividaId }
    return dividas.sumOf { it.pesoNoMes(porDivida[it.id].orEmpty(), hoje, zona) }
}

/** Renda extra que já entrou no mês de [hoje]. */
fun recebidoNoMes(recebimentos: List<Recebimento>, hoje: LocalDate): Long {
    val mes = YearMonth.from(hoje)
    return recebimentos.sumOf {
        when {
            it.recebido -> if (YearMonth.from(it.data) == mes) it.valorCentavos else 0L
            it.recebidoNoMes(hoje) -> it.valorCentavos
            else -> 0L
        }
    }
}

/**
 * A conta do mês, que recomeça todo dia 1º: só o salário do mês + o que entrou de extra nele, menos
 * o que foi pago nele e o que falta pagar até o fim dele. Nada do mês anterior (saldo, sobra ou
 * valores a receber atrasados) entra; contas vencidas e não pagas, sim, porque ainda vão sair.
 */
data class ResumoMes(
    val salario: Long,
    /** Renda extra marcada como recebida neste mês. */
    val recebido: Long,
    /** Renda extra prevista até o fim do mês e ainda não recebida (só informativo: não entra na sobra). */
    val aReceber: Long,
    /** Pago neste mês, pelo histórico (inclui gastos rápidos). */
    val pago: Long,
    /** Em aberto com vencimento até o fim do mês, inclusive vencidas. */
    val aPagar: Long,
) {
    val renda: Long get() = salario + recebido
    val sobra: Long get() = renda - pago - aPagar
}

/** O que falta pagar da dívida até o fim do mês de [hoje] (inclui vencidas). */
private fun Divida.aPagarNoMes(hoje: LocalDate): Long =
    if (!paga && vencimentoEpochDay <= YearMonth.from(hoje).atEndOfMonth().toEpochDay()) valorCentavos else 0L

fun resumoDoMes(
    salarioCentavos: Long,
    dividas: List<Divida>,
    pagamentos: List<Pagamento>,
    recebimentos: List<Recebimento>,
    hoje: LocalDate,
    zona: ZoneId,
): ResumoMes {
    val porDivida = pagamentos.groupBy { it.dividaId }
    val fim = YearMonth.from(hoje).atEndOfMonth().toEpochDay()
    var pago = 0L
    var aPagar = 0L
    dividas.forEach { d ->
        val falta = d.aPagarNoMes(hoje)
        aPagar += falta
        pago += d.pesoNoMes(porDivida[d.id].orEmpty(), hoje, zona) - falta
    }
    return ResumoMes(
        salario = salarioCentavos,
        recebido = recebidoNoMes(recebimentos, hoje),
        aReceber = recebimentos
            .filter { !it.recebido && !it.recebidoNoMes(hoje) && it.dataEpochDay <= fim }
            .sumOf { it.valorCentavos },
        pago = pago,
        aPagar = aPagar,
    )
}

/** Uma coluna do gráfico "Próximos meses". */
data class MesPrevisto(
    val mes: YearMonth,
    /** Cada conta com quanto pesa no mês, da maior para a menor. */
    val contas: List<Pair<Divida, Long>>,
    /** Parte já paga (só no mês atual). */
    val jaPago: Long,
) {
    val total: Long get() = contas.sumOf { it.second }
}

/**
 * Despesas previstas mês a mês, a partir do mês de [hoje]. O mês atual é a conta do mês (pago +
 * a pagar). Nos seguintes: mensais fixas todo mês, parceladas uma parcela por mês até acabar e à
 * vista no mês do vencimento. O que está atrasado entra no mês atual e as parcelas seguintes vêm
 * em fila, uma por mês, já que é assim que vão ser pagas.
 */
fun previsaoMensal(
    dividas: List<Divida>,
    pagamentos: List<Pagamento>,
    hoje: LocalDate,
    zona: ZoneId,
    meses: Int = 12,
): List<MesPrevisto> {
    val atual = YearMonth.from(hoje)
    val porDivida = pagamentos.groupBy { it.dividaId }
    val porMes = List(meses) { mutableListOf<Pair<Divida, Long>>() }
    var jaPago = 0L

    dividas.forEach { d ->
        val peso = d.pesoNoMes(porDivida[d.id].orEmpty(), hoje, zona)
        if (peso > 0) porMes[0] += d to peso
        jaPago += peso - d.aPagarNoMes(hoje)
        if (d.paga) return@forEach

        // Primeiro mês em que a conta ainda vai pesar (atrasada = mês atual).
        val inicio = maxOf(0L, ChronoUnit.MONTHS.between(atual, YearMonth.from(d.vencimento))).toInt()
        val quantas = when {
            d.recorrente -> meses
            else -> d.totalParcelas - d.parcelasPagas
        }
        for (i in 0 until quantas) {
            val k = inicio + i
            if (k >= meses) break
            if (k == 0) continue // o mês atual já foi contado pela conta do mês
            porMes[k] += d to d.valorCentavos
        }
    }
    return porMes.mapIndexed { k, contas ->
        MesPrevisto(atual.plusMonths(k.toLong()), contas.sortedByDescending { it.second }, if (k == 0) jaPago else 0L)
    }
}
