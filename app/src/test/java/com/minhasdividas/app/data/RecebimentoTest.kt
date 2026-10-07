package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecebimentoTest {
    private val hoje = LocalDate.of(2026, 10, 7)

    @Test
    fun recebimentoUnicoMarcaEDesmarca() {
        val freela = Recebimento(descricao = "Freela", valorCentavos = 80_000, dataEpochDay = hoje.toEpochDay())
        val recebido = freela.receber()
        assertTrue(recebido.recebido)
        assertEquals(freela.dataEpochDay, recebido.dataEpochDay)
        assertFalse(recebido.recebidoNoMes(hoje))

        val desfeito = recebido.desfazerRecebimento()
        assertFalse(desfeito.recebido)
        assertEquals(0, desfeito.vezesRecebido)
    }

    @Test
    fun recorrenteAvancaMantendoODiaEFicaMarcadoNoMes() {
        val aluguel = Recebimento(
            descricao = "Aluguel", valorCentavos = 120_000, recorrente = true, diaDoMes = 31,
            dataEpochDay = LocalDate.of(2026, 10, 31).toEpochDay(),
        )
        val recebido = aluguel.receber()
        assertFalse(recebido.recebido)
        assertEquals(LocalDate.of(2026, 11, 30), recebido.data)
        assertTrue(recebido.recebidoNoMes(hoje))
        assertEquals(LocalDate.of(2026, 12, 31), recebido.receber().data)

        val desfeito = recebido.desfazerRecebimento()
        assertEquals(aluguel.data, desfeito.data)
        assertEquals(0, desfeito.vezesRecebido)
    }
}
