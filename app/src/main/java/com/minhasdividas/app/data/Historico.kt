package com.minhasdividas.app.data

import java.time.Instant
import java.time.LocalDate
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

/** Saldo da conta informado pelo usuário, com o que já tinha entrado no mês naquele momento. */
data class SaldoInformado(
    val centavos: Long,
    val emMillis: Long,
    /** [recebidoNoMes] no momento em que o saldo foi informado. */
    val recebidoAteEntao: Long,
    /** O salário deste mês ainda não tinha caído na conta. */
    val salarioPendente: Boolean,
)

/** As parcelas do "Livre até o fim do mês", para mostrar de onde vem o valor. */
data class SaldoLivre(
    val saldoInformado: Long,
    /** Pago no app depois de informar o saldo (já saiu da conta). */
    val pagoDepois: Long,
    /** Recebido no app depois de informar o saldo (já entrou na conta). */
    val recebidoDepois: Long,
    val aReceber: Long,
    val salario: Long,
    val aPagar: Long,
) {
    val livre: Long get() = saldoInformado - pagoDepois + recebidoDepois + aReceber + salario - aPagar
}

/**
 * Quanto fica livre até o fim do mês: saldo informado + o que ainda vai entrar − o que ainda vai sair.
 * O que foi marcado como pago ou recebido DEPOIS de informar o saldo já mexeu no dinheiro real, então
 * ajusta o saldo. Null quando o saldo é de um mês anterior (precisa ser informado de novo).
 */
fun saldoLivre(
    saldo: SaldoInformado,
    salarioCentavos: Long,
    dividas: List<Divida>,
    pagamentos: List<Pagamento>,
    recebimentos: List<Recebimento>,
    hoje: LocalDate,
    zona: ZoneId,
): SaldoLivre? {
    val mes = YearMonth.from(hoje)
    val informadoEm = Instant.ofEpochMilli(saldo.emMillis).atZone(zona).toLocalDate()
    if (YearMonth.from(informadoEm) != mes) return null
    val fim = mes.atEndOfMonth().toEpochDay()

    val pagoDepois = pagamentos.filter { it.pagoEmMillis > saldo.emMillis }.sumOf { it.valorCentavos }
    val recebidoDepois = recebidoNoMes(recebimentos, hoje) - saldo.recebidoAteEntao
    val aPagar = dividas.filter { !it.paga && it.vencimentoEpochDay <= fim }.sumOf { it.valorCentavos }
    val aReceber = recebimentos
        .filter { !it.recebido && !it.recebidoNoMes(hoje) && it.dataEpochDay <= fim }
        .sumOf { it.valorCentavos }
    val salario = if (saldo.salarioPendente) salarioCentavos else 0L
    return SaldoLivre(saldo.centavos, pagoDepois, recebidoDepois, aReceber, salario, aPagar)
}
