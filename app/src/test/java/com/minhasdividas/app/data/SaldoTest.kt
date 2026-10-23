package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SaldoTest {
    private val zona = ZoneId.of("America/Sao_Paulo")
    private val hoje = LocalDate.of(2026, 10, 23)

    private fun millis(mes: Int, dia: Int, hora: Int = 12) =
        LocalDateTime.of(2026, mes, dia, hora, 0).atZone(zona).toInstant().toEpochMilli()

    private fun dia(mes: Int, d: Int) = LocalDate.of(2026, mes, d).toEpochDay()

    // Conta de setembro, vencida, paga em outubro: pelo histórico, o dinheiro saiu em outubro.
    private val atrasada = Divida(
        id = 1, descricao = "Internet de setembro", valorCentavos = 10_000,
        vencimentoEpochDay = dia(9, 20), categoria = Categoria.CONTA_MENSAL, parcelasPagas = 1, paga = true,
    )
    // Mensal fixa paga em outubro (próximo vencimento em novembro).
    private val luz = Divida(
        id = 2, descricao = "Luz", valorCentavos = 20_000, vencimentoEpochDay = dia(11, 10),
        categoria = Categoria.CONTA_MENSAL, recorrente = true, parcelasPagas = 3, diaVencimento = 10,
    )
    // Em aberto, vence ainda em outubro.
    private val cartao = Divida(
        id = 3, descricao = "Fatura", valorCentavos = 50_000, vencimentoEpochDay = dia(10, 28), categoria = Categoria.CARTAO,
    )
    // Quitada antes do histórico existir (v1.3), com vencimento em outubro: regra antiga.
    private val antiga = Divida(
        id = 4, descricao = "Remédio", valorCentavos = 3_000, vencimentoEpochDay = dia(10, 2),
        categoria = Categoria.OUTROS, parcelasPagas = 1, paga = true,
    )
    private val pagamentos = listOf(
        Pagamento(1, 1, 10_000, dia(9, 20), millis(10, 5), 1),
        Pagamento(2, 2, 20_000, dia(10, 10), millis(10, 10), 3),
    )

    @Test
    fun contasDoMesUsamOHistorico() {
        // 100 (setembro pago em outubro) + 200 (luz) + 500 (a pagar) + 30 (sem histórico, vence em outubro)
        assertEquals(83_000, contasDoMes(listOf(atrasada, luz, cartao, antiga), pagamentos, hoje, zona))
        // Antes (só pelo vencimento), a conta de setembro paga em outubro ficava de fora do mês.
        assertEquals(0, atrasada.pesoNoMes(emptyList(), hoje, zona))
    }

    @Test
    fun saldoLivreAjustaPeloQueAconteceuDepois() {
        val salario = Recebimento(1, "Freela", 30_000, dia(10, 25))
        val saldo = SaldoInformado(centavos = 100_000, emMillis = millis(10, 8), recebidoAteEntao = 0, salarioPendente = false)
        val livre = saldoLivre(saldo, 0, listOf(atrasada, luz, cartao, antiga), pagamentos, listOf(salario), hoje, zona)!!
        // A luz foi paga (10/10) depois de informar o saldo (08/10); a internet (05/10) já estava no saldo.
        assertEquals(20_000, livre.pagoDepois)
        assertEquals(30_000, livre.aReceber)
        assertEquals(50_000, livre.aPagar)
        assertEquals(100_000 - 20_000 + 30_000 - 50_000, livre.livre)
    }

    @Test
    fun recebimentoDepoisDoSaldoEntraNoLivre() {
        val freela = Recebimento(1, "Freela", 30_000, dia(10, 15), recebido = true, vezesRecebido = 1)
        val saldo = SaldoInformado(100_000, millis(10, 8), recebidoAteEntao = 0, salarioPendente = true)
        val livre = saldoLivre(saldo, 400_000, listOf(cartao), emptyList(), listOf(freela), hoje, zona)!!
        assertEquals(30_000, livre.recebidoDepois)
        assertEquals(0, livre.aReceber)
        assertEquals(400_000, livre.salario)
        assertEquals(100_000 + 30_000 + 400_000 - 50_000, livre.livre)
    }

    @Test
    fun saldoDeOutroMesPrecisaSerAtualizado() {
        val saldo = SaldoInformado(100_000, millis(9, 30), 0, false)
        assertNull(saldoLivre(saldo, 0, emptyList(), emptyList(), emptyList(), hoje, zona))
    }
}
