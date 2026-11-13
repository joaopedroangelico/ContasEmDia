package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class ResumoMesTest {
    private val zona = ZoneId.of("America/Sao_Paulo")
    private val hoje = LocalDate.of(2026, 11, 13)

    private fun millis(mes: Int, dia: Int) = LocalDateTime.of(2026, mes, dia, 12, 0).atZone(zona).toInstant().toEpochMilli()
    private fun dia(mes: Int, d: Int) = LocalDate.of(2026, mes, d).toEpochDay()

    // Mensal fixa já paga em novembro (próxima em dezembro).
    private val luz = Divida(
        id = 1, descricao = "Luz", valorCentavos = 20_000, vencimentoEpochDay = dia(12, 10),
        categoria = Categoria.CONTA_MENSAL, recorrente = true, parcelasPagas = 3, diaVencimento = 10,
    )
    // Parcelada em aberto: 3 parcelas restantes, a próxima vence em 20/11.
    private val celular = Divida(
        id = 2, descricao = "Celular", valorCentavos = 50_000, vencimentoEpochDay = dia(11, 20),
        categoria = Categoria.CARTAO, totalParcelas = 5, parcelasPagas = 2, origem = "Nubank",
    )
    // À vista vencida em outubro e não paga: entra como a pagar em novembro.
    private val atrasada = Divida(
        id = 3, descricao = "Internet", valorCentavos = 10_000, vencimentoEpochDay = dia(10, 15), categoria = Categoria.OUTROS,
    )
    // Gasto rápido de novembro.
    private val mercado = Divida(
        id = 4, descricao = "Mercado", valorCentavos = 5_000, vencimentoEpochDay = dia(11, 5),
        categoria = Categoria.OUTROS, parcelasPagas = 1, paga = true,
    )
    private val pagamentos = listOf(
        Pagamento(1, 1, 20_000, dia(11, 10), millis(11, 10), 3),
        Pagamento(2, 4, 5_000, dia(11, 5), millis(11, 5), 1),
        // Pago em outubro: não entra em novembro.
        Pagamento(3, 2, 50_000, dia(10, 20), millis(10, 20), 2),
    )
    private val dividas = listOf(luz, celular, atrasada, mercado)

    @Test
    fun contaDoMesUsaSoOSalarioEOQueEntrouNoMes() {
        val freelaRecebido = Recebimento(1, "Freela", 30_000, dia(11, 2), recebido = true, vezesRecebido = 1)
        val pedroAtrasado = Recebimento(2, "Pedro", 600_000, dia(10, 1))
        val resumo = resumoDoMes(400_000, dividas, pagamentos, listOf(freelaRecebido, pedroAtrasado), hoje, zona)

        assertEquals(30_000, resumo.recebido)
        // A receber atrasado de outubro aparece como aviso, mas não soma na renda.
        assertEquals(600_000, resumo.aReceber)
        assertEquals(430_000, resumo.renda)
        assertEquals(25_000, resumo.pago) // luz + mercado (gasto rápido)
        assertEquals(60_000, resumo.aPagar) // parcela de novembro + internet vencida
        assertEquals(430_000 - 25_000 - 60_000, resumo.sobra)
    }

    @Test
    fun previsaoMesAMes() {
        val previsao = previsaoMensal(dividas, pagamentos, hoje, zona, meses = 4)
        assertEquals(listOf(11, 12, 1, 2), previsao.map { it.mes.monthValue })

        // Novembro = conta do mês (pago + a pagar), com a parte já paga separada.
        assertEquals(85_000, previsao[0].total)
        assertEquals(25_000, previsao[0].jaPago)
        // Dezembro: luz + parcela 4 do celular. Janeiro: luz + parcela 5. Fevereiro: só a luz.
        assertEquals(70_000, previsao[1].total)
        assertEquals(70_000, previsao[2].total)
        assertEquals(20_000, previsao[3].total)
        assertEquals(YearMonth.of(2027, 2), previsao[3].mes)
    }

    @Test
    fun faturaDoCartaoPeloNome() {
        assertEquals(LocalDate.of(2026, 11, 20), vencimentoDaFatura(dividas, " nubank ", hoje))
        assertNull(vencimentoDaFatura(dividas, "Inter", hoje))
    }

    @Test
    fun faturaAtrasadaJaFechouCompraVaiParaAProxima() {
        val atrasada = celular.copy(vencimentoEpochDay = dia(10, 7), diaVencimento = 7)
        // Hoje é 13/11: o dia 7 de novembro já passou, a próxima fatura é 07/12.
        assertEquals(LocalDate.of(2026, 12, 7), vencimentoDaFatura(listOf(atrasada), "Nubank", hoje))
    }
}
