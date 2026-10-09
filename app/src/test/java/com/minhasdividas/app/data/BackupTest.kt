package com.minhasdividas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BackupTest {
    private val zona = ZoneId.of("America/Sao_Paulo")

    private fun millis(ano: Int, mes: Int, dia: Int) =
        LocalDateTime.of(ano, mes, dia, 9, 30).atZone(zona).toInstant().toEpochMilli()

    private val emAberto = Divida(
        id = 1,
        descricao = "Financiamento “casa”",
        valorCentavos = 120_000,
        vencimentoEpochDay = LocalDate.of(2026, 11, 1).toEpochDay(),
        categoria = Categoria.FINANCIAMENTO,
        totalParcelas = 360,
        parcelasPagas = 40,
        diaVencimento = 1,
        origem = "Caixa",
        formaPagamento = FormaPagamento.DEBITO_AUTOMATICO,
    )
    private val quitadaRecente = Divida(
        id = 2, descricao = "Geladeira", valorCentavos = 300_000,
        vencimentoEpochDay = LocalDate.of(2026, 9, 1).toEpochDay(), categoria = Categoria.CARTAO, paga = true,
    )
    private val quitadaAntiga = Divida(
        id = 3, descricao = "TV", valorCentavos = 200_000,
        vencimentoEpochDay = LocalDate.of(2025, 1, 1).toEpochDay(), categoria = Categoria.CARTAO, paga = true,
    )
    private val semHistorico = Divida(
        id = 4, descricao = "Quitada antes da v1.4", valorCentavos = 30_000,
        vencimentoEpochDay = LocalDate.of(2024, 1, 1).toEpochDay(), categoria = Categoria.OUTROS, paga = true,
    )
    private val pagamentos = listOf(
        Pagamento(1, 1, 120_000, LocalDate.of(2025, 12, 1).toEpochDay(), millis(2025, 12, 1), 39),
        Pagamento(2, 1, 120_000, LocalDate.of(2026, 10, 1).toEpochDay(), millis(2026, 10, 1), 40, FormaPagamento.PIX),
        Pagamento(3, 2, 300_000, LocalDate.of(2026, 9, 1).toEpochDay(), millis(2026, 9, 2), 1),
        Pagamento(4, 3, 200_000, LocalDate.of(2025, 1, 1).toEpochDay(), millis(2025, 1, 1), 1),
    )
    private val recebimentos = listOf(
        Recebimento(1, "Freela antigo", 5_000, LocalDate.of(2025, 6, 1).toEpochDay(), recebido = true, vezesRecebido = 1),
        Recebimento(2, "Venda", 7_000, LocalDate.of(2026, 12, 1).toEpochDay()),
    )

    private fun backup(desde: LocalDate?) = montarBackup(
        listOf(emAberto, quitadaRecente, quitadaAntiga, semHistorico), pagamentos, recebimentos,
        salarioCentavos = 450_000, desde = desde, agoraMillis = millis(2026, 10, 9), zona = zona,
    )

    @Test
    fun periodoLimitaSoOHistorico() {
        val dados = backup(LocalDate.of(2026, 5, 1))
        // Em aberto sempre entra; quitada antiga fica de fora; quitada sem histórico não tem data, entra.
        assertEquals(listOf(1L, 2L, 4L), dados.dividas.map { it.id })
        assertEquals(listOf(2L, 3L), dados.pagamentos.map { it.id })
        assertEquals(listOf(2L), dados.recebimentos.map { it.id })
        assertEquals(LocalDate.of(2026, 5, 1).toEpochDay(), dados.desdeEpochDay)
    }

    @Test
    fun tudoLevaTudo() {
        val dados = backup(null)
        assertEquals(4, dados.dividas.size)
        assertEquals(4, dados.pagamentos.size)
        assertEquals(2, dados.recebimentos.size)
        assertNull(dados.desdeEpochDay)
    }

    @Test
    fun idaEVoltaSemSenha() {
        val dados = backup(null)
        val lido = lerArquivoBackup(gerarArquivoBackup(dados, null), null)
        assertEquals(LeituraBackup.Ok(dados), lido)
    }

    @Test
    fun idaEVoltaComSenha() {
        val dados = backup(LocalDate.of(2026, 1, 1))
        val arquivo = gerarArquivoBackup(dados, "minha senha")
        assertTrue("dados não podem ir em texto claro", "Financiamento" !in arquivo)
        assertEquals(LeituraBackup.PrecisaSenha, lerArquivoBackup(arquivo, null))
        assertEquals(LeituraBackup.SenhaIncorreta, lerArquivoBackup(arquivo, "outra"))
        assertEquals(LeituraBackup.Ok(dados), lerArquivoBackup(arquivo, "minha senha"))
    }

    @Test
    fun arquivoQualquerEhInvalido() {
        assertEquals(LeituraBackup.Invalido, lerArquivoBackup("não é json", null))
        assertEquals(LeituraBackup.Invalido, lerArquivoBackup("""{"app":"Outro","formato":1}""", null))
    }

    @Test
    fun inicioDoPeriodoNasPreferencias() {
        val hoje = LocalDate.of(2026, 10, 9)
        assertEquals(LocalDate.of(2026, 5, 1), Preferencias(mesesBackup = 6).inicioBackup(hoje))
        assertNull(Preferencias(mesesBackup = BACKUP_TUDO).inicioBackup(hoje))
        val escolhida = LocalDate.of(2026, 2, 14)
        assertEquals(
            escolhida,
            Preferencias(mesesBackup = BACKUP_DESDE_DATA, backupDesdeEpochDay = escolhida.toEpochDay()).inicioBackup(hoje),
        )
    }

    @Test
    fun formatosDeTamanho() {
        assertEquals("0,08 MB", formatarMB(80L * 1024 + 2_000))
        assertEquals("25,4 MB", formatarMB((25.4 * 1024 * 1024).toLong()))
        assertEquals("50%", formatarPorcentagem(1, 2))
        assertEquals("0,3%", formatarPorcentagem(3, 1_000))
        assertEquals("< 0,01%", formatarPorcentagem(1, 1_000_000))
    }
}
