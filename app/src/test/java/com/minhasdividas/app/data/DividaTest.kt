package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DividaTest {
    private val base = Divida(
        descricao = "Notebook",
        valorCentavos = 50_000,
        vencimentoEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
        categoria = Categoria.CARTAO,
        totalParcelas = 3,
        diaVencimento = 31,
    )

    @Test
    fun pagarParcelaAvancaUmMesERespeitaFimDoMes() {
        val depois = base.pagarParcela()
        assertEquals(1, depois.parcelasPagas)
        assertFalse(depois.paga)
        assertEquals(LocalDate.of(2026, 2, 28), depois.vencimento)
        assertEquals(2, depois.parcelaAtual)
        assertEquals(100_000, depois.valorRestanteCentavos)
    }

    @Test
    fun ultimaParcelaQuitaSemMudarVencimento() {
        val quitada = base.pagarParcela().pagarParcela().pagarParcela()
        assertTrue(quitada.paga)
        assertEquals(3, quitada.parcelasPagas)
        assertEquals(0, quitada.valorRestanteCentavos)
        // Dia original (31) volta a valer em março, mesmo após fevereiro.
        assertEquals(LocalDate.of(2026, 3, 31), quitada.vencimento)
    }

    @Test
    fun reabrirVoltaParaUltimaParcela() {
        val reaberta = base.pagarParcela().pagarParcela().pagarParcela().reabrir()
        assertFalse(reaberta.paga)
        assertEquals(2, reaberta.parcelasPagas)
        assertEquals(3, reaberta.parcelaAtual)
        assertEquals(50_000, reaberta.valorRestanteCentavos)
    }

    @Test
    fun dividaAVistaQuitaDeUmaVez() {
        val avista = base.copy(totalParcelas = 1)
        val paga = avista.pagarParcela()
        assertTrue(paga.paga)
        assertEquals(avista.vencimentoEpochDay, paga.vencimentoEpochDay)
        assertEquals(0, paga.reabrir().parcelasPagas)
    }

    @Test
    fun contaMensalFixaNuncaQuitaEMantemODia() {
        val luz = base.copy(descricao = "Luz", totalParcelas = 1, recorrente = true, categoria = Categoria.CONTA_MENSAL)
        var atual = luz
        repeat(14) { atual = atual.pagarParcela() }
        assertFalse(atual.paga)
        assertEquals(LocalDate.of(2027, 3, 31), atual.vencimento)
        assertEquals(50_000, atual.valorRestanteCentavos)
    }

    @Test
    fun contaMensalSemDiaGravadoUsaODiaAtual() {
        val agua = base.copy(totalParcelas = 1, recorrente = true, diaVencimento = 0,
            vencimentoEpochDay = LocalDate.of(2026, 10, 15).toEpochDay())
        assertEquals(LocalDate.of(2026, 11, 15), agua.pagarParcela().vencimento)
    }

    @Test
    fun textosDeVencimento() {
        val hoje = LocalDate.of(2026, 10, 7)
        assertEquals("Vence hoje", textoVencimento(hoje, hoje))
        assertEquals("Vence amanhã", textoVencimento(hoje.plusDays(1), hoje))
        assertEquals("Vence em 5 dias", textoVencimento(hoje.plusDays(5), hoje))
        assertEquals("Venceu ontem", textoVencimento(hoje.minusDays(1), hoje))
        assertEquals("Venceu há 3 dias", textoVencimento(hoje.minusDays(3), hoje))
    }

    @Test
    fun moedaEmReais() {
        assertEquals("R$ 1.234,56", formatarMoeda(123_456).replace(' ', ' '))
    }
}
