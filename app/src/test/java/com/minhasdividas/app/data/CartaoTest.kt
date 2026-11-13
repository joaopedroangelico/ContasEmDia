package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CartaoTest {
    private fun data(mes: Int, dia: Int, ano: Int = 2026) = LocalDate.of(ano, mes, dia)

    // Fecha dia 1, vence dia 8.
    private val nubank = ConfigCartao(chaveCartao("Nubank"), "Nubank", diaFechamento = 1, diaVencimento = 8)

    // Fecha dia 28, vence dia 5 do mês seguinte.
    private val inter = ConfigCartao(chaveCartao("Inter"), "Inter", diaFechamento = 28, diaVencimento = 5)

    @Test
    fun compraDepoisDoFechamentoVaiParaAFaturaSeguinte() {
        assertEquals(data(12, 8), nubank.vencimentoParaCompra(data(11, 13)))
        assertEquals(data(12, 8), nubank.vencimentoParaCompra(data(11, 30)))
        // No dia do fechamento a fatura já fechou.
        assertEquals(data(1, 8, 2027), nubank.vencimentoParaCompra(data(12, 1)))

        assertEquals(data(12, 5), inter.vencimentoParaCompra(data(11, 13)))
        assertEquals(data(1, 5, 2027), inter.vencimentoParaCompra(data(11, 28)))
    }

    @Test
    fun parcelaDoCartaoFicaPagaAteOVencimentoEDepoisVoltaAPendente() {
        // Parcela 2/5 da fatura de 08/12 paga: o vencimento já avançou para 08/01.
        val celular = Divida(
            id = 1, descricao = "Celular", valorCentavos = 50_000, vencimentoEpochDay = data(1, 8, 2027).toEpochDay(),
            categoria = Categoria.CARTAO, totalParcelas = 5, parcelasPagas = 2, diaVencimento = 8, origem = "Nubank",
        )
        assertTrue(celular.pagaNoPeriodo(data(12, 5)))
        assertTrue(celular.pagaNoPeriodo(data(12, 8)))
        // Passou o vencimento da fatura paga: a parcela de janeiro volta a ficar pendente, mesmo em dezembro.
        assertFalse(celular.pagaNoPeriodo(data(12, 9)))
    }

    @Test
    fun foraDoCartaoContinuaValendoOMes() {
        val luz = Divida(
            id = 2, descricao = "Luz", valorCentavos = 20_000, vencimentoEpochDay = data(1, 8, 2027).toEpochDay(),
            categoria = Categoria.CONTA_MENSAL, recorrente = true, parcelasPagas = 3, diaVencimento = 8,
        )
        assertTrue(luz.pagaNoPeriodo(data(12, 20)))
    }

    @Test
    fun mudarVencimentoMoveSoAsComprasEmAbertoDoCartao() {
        val aberta = Divida(
            id = 1, descricao = "Fone", valorCentavos = 8_990, vencimentoEpochDay = data(12, 7).toEpochDay(),
            categoria = Categoria.CARTAO, origem = "nubank ",
        )
        val paga = aberta.copy(id = 2, paga = true, parcelasPagas = 1)
        val outroCartao = aberta.copy(id = 3, origem = "Inter")
        val movidas = aplicarVencimento(listOf(aberta, paga, outroCartao), nubank)
        assertEquals(listOf(1L), movidas.map { it.id })
        assertEquals(data(12, 8), movidas[0].vencimento)
        assertEquals(8, movidas[0].diaVencimento)
    }

    @Test
    fun gastoRapidoUsaOFechamentoQuandoConfigurado() {
        assertEquals(data(1, 8, 2027), vencimentoDaFatura(emptyList(), "NUBANK", data(12, 3), listOf(nubank)))
    }

    @Test
    fun backupLevaOsCartoes() {
        val dados = DadosBackup(1L, null, 0, emptyList(), emptyList(), emptyList(), listOf(nubank, inter))
        assertEquals(LeituraBackup.Ok(dados), lerArquivoBackup(gerarArquivoBackup(dados, null), null))
    }
}
