package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class HistoricoTest {
    private val zona = ZoneId.of("America/Sao_Paulo")

    private fun millis(ano: Int, mes: Int, dia: Int, hora: Int = 12) =
        LocalDateTime.of(ano, mes, dia, hora, 0).atZone(zona).toInstant().toEpochMilli()

    private val luz = Divida(
        id = 1,
        descricao = "Luz",
        valorCentavos = 15_000,
        vencimentoEpochDay = LocalDate.of(2026, 11, 10).toEpochDay(),
        categoria = Categoria.CONTA_MENSAL,
        recorrente = true,
        parcelasPagas = 2,
        diaVencimento = 10,
    )
    private val celular = Divida(
        id = 2,
        descricao = "Celular",
        valorCentavos = 50_000,
        vencimentoEpochDay = LocalDate.of(2026, 10, 5).toEpochDay(),
        categoria = Categoria.CARTAO,
        totalParcelas = 2,
        parcelasPagas = 2,
        paga = true,
    )
    private val antiga = Divida(
        id = 3,
        descricao = "Quitada antes do histórico",
        valorCentavos = 1_000,
        vencimentoEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
        categoria = Categoria.OUTROS,
        paga = true,
    )
    private val pagamentos = listOf(
        // Luz de setembro paga em 12/09 e de outubro paga atrasada em 02/11.
        Pagamento(10, 1, 15_000, LocalDate.of(2026, 9, 10).toEpochDay(), millis(2026, 9, 12), 1),
        Pagamento(11, 1, 15_000, LocalDate.of(2026, 10, 10).toEpochDay(), millis(2026, 11, 2), 2, FormaPagamento.PIX),
        // Celular: parcela de setembro e de outubro, ambas pagas em outubro.
        Pagamento(12, 2, 50_000, LocalDate.of(2026, 9, 5).toEpochDay(), millis(2026, 10, 1), 1),
        Pagamento(13, 2, 50_000, LocalDate.of(2026, 10, 5).toEpochDay(), millis(2026, 10, 20), 2),
    )

    @Test
    fun agrupaPeloMesDoPagamentoNaoDoVencimento() {
        val meses = agruparPorMes(listOf(luz, celular), pagamentos, zona)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10), YearMonth.of(2026, 9)), meses.map { it.mes })

        val novembro = meses[0]
        // A luz de outubro foi paga em novembro, e a de novembro ainda está em aberto.
        assertEquals(listOf(11L), novembro.pagos.map { it.pagamento.id })
        assertEquals(listOf(luz), novembro.pendentes)
        assertFalse(novembro.completo)

        val outubro = meses[1]
        assertEquals(listOf(13L, 12L), outubro.pagos.map { it.pagamento.id }) // mais recente primeiro
        assertEquals(100_000, outubro.totalPago)
        assertTrue(outubro.completo)
    }

    @Test
    fun pagamentoAtrasado() {
        assertTrue(pagamentos[1].emAtraso(zona))
        // Pago às 23h do próprio dia do vencimento: em dia.
        val noDia = pagamentos[0].copy(pagoEmMillis = millis(2026, 9, 10, hora = 23))
        assertFalse(noDia.emAtraso(zona))
    }

    @Test
    fun quitadasSemHistoricoFicamAParte() {
        assertEquals(listOf(antiga), quitadasSemHistorico(listOf(luz, celular, antiga), pagamentos))
    }

    @Test
    fun registrarPagamentoUsaAParcelaAtual() {
        val p = luz.registrarPagamento(123L, FormaPagamento.BOLETO)
        assertEquals(3, p.parcela)
        assertEquals(luz.vencimentoEpochDay, p.vencimentoEpochDay)
        assertEquals(FormaPagamento.BOLETO, p.forma)
    }

    @Test
    fun apagarMesSoRemoveQuitadasSemOutrosPagamentos() {
        val recebido = Recebimento(1, "Freela", 10_000, LocalDate.of(2026, 10, 15).toEpochDay(), recebido = true, vezesRecebido = 1)
        val mensal = Recebimento(2, "Aluguel", 10_000, LocalDate.of(2026, 10, 15).toEpochDay(), recorrente = true)
        val plano = planejarLimpeza(
            YearMonth.of(2026, 10),
            listOf(luz, celular, antiga),
            pagamentos,
            listOf(recebido, mensal),
            zona,
        )
        assertEquals(listOf(12L, 13L), plano.pagamentos.map { it.id })
        // Celular quitado com todos os pagamentos em outubro sai; a luz (mensal) fica.
        assertEquals(listOf(celular), plano.dividas)
        assertEquals(listOf(recebido), plano.recebimentos)

        // Setembro: a luz perde só o pagamento daquele mês.
        val setembro = planejarLimpeza(YearMonth.of(2026, 9), listOf(luz, celular), pagamentos, emptyList(), zona)
        assertEquals(listOf(10L), setembro.pagamentos.map { it.id })
        assertTrue(setembro.dividas.isEmpty())
    }

    @Test
    fun mesesParaApagar() {
        assertEquals(
            listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10), YearMonth.of(2026, 9)),
            mesesComHistorico(pagamentos, emptyList(), zona),
        )
    }

    @Test
    fun nomeDoMesEmPortugues() {
        assertEquals("Outubro de 2026", nomeDoMes(YearMonth.of(2026, 10)))
        assertEquals("Março de 2027", nomeDoMes(YearMonth.of(2027, 3)))
    }
}
