package com.minhasdividas.app.data

import java.time.YearMonth
import java.time.ZoneId

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
