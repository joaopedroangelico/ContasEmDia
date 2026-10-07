package com.minhasdividas.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minhasdividas.app.data.AppDatabase
import com.minhasdividas.app.data.Categoria
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.LocaleBR
import com.minhasdividas.app.data.Preferencias
import com.minhasdividas.app.data.PreferenciasRepo
import com.minhasdividas.app.data.Recebimento
import com.minhasdividas.app.data.Tema
import com.minhasdividas.app.data.data
import com.minhasdividas.app.data.desfazerRecebimento
import com.minhasdividas.app.data.desfazerUltimoPagamento
import com.minhasdividas.app.data.formatarData
import com.minhasdividas.app.data.pagaNoMes
import com.minhasdividas.app.data.pagarParcela
import com.minhasdividas.app.data.reabrir
import com.minhasdividas.app.data.receber
import com.minhasdividas.app.data.recebidoNoMes
import com.minhasdividas.app.data.valorRestanteCentavos
import com.minhasdividas.app.data.vencimento
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.LocalDate

enum class FiltroStatus(val rotulo: String) { PENDENTES("Pendentes"), PAGAS("Pagas"), TODAS("Todas") }

enum class Ordem(val rotulo: String) {
    VENCIMENTO("Vencimento"),
    MAIOR_VALOR("Maior valor"),
    MENOR_VALOR("Menor valor"),
    NOME("Nome"),
}

data class Filtro(
    val status: FiltroStatus = FiltroStatus.PENDENTES,
    val categoria: Categoria? = null,
    val ordem: Ordem = Ordem.VENCIMENTO,
)

data class Resumo(
    val pendenteMes: Long = 0,
    val totalRestante: Long = 0,
    val vencidas: Int = 0,
    val pendentes: Int = 0,
    /** Tudo o que pesa no mês: contas já pagas neste mês + as que faltam (inclui vencidas). */
    val contasDoMes: Long = 0,
    /** Renda extra do mês: já recebida neste mês + a receber até o fim do mês. */
    val extrasDoMes: Long = 0,
)

/** Uma "gaveta": as dívidas visíveis de uma categoria, com o resumo mostrado quando fechada. */
data class Grupo(
    val categoria: Categoria,
    val dividas: List<Divida>,
    val pendentes: Int,
    val pagasNoMes: Int,
    val quitadas: Int,
    val vencidas: Int,
    val totalPendente: Long,
    val proxima: Divida?,
)

/** A gaveta "A receber": renda extra prevista e já recebida. */
data class GrupoReceber(
    val itens: List<Recebimento>,
    val aReceber: Int,
    val recebidosNoMes: Int,
    val recebidos: Int,
    val totalAReceber: Long,
    val proximo: Recebimento?,
)

data class UiState(
    val todas: List<Divida> = emptyList(),
    val recebimentos: List<Recebimento> = emptyList(),
    val grupos: List<Grupo> = emptyList(),
    val receber: GrupoReceber? = null,
    val abertas: Set<String> = emptySet(),
    val resumo: Resumo = Resumo(),
    val filtro: Filtro = Filtro(),
    val hoje: LocalDate = LocalDate.now(),
    val carregando: Boolean = true,
)

/** Mensagem da barra inferior com a ação que desfaz o que acabou de acontecer. */
class Desfazer(val mensagem: String, val desfazer: () -> Unit)

/** Chave da gaveta "A receber" no conjunto de gavetas abertas (as demais usam o nome da categoria). */
const val GAVETA_RECEBER = "RECEBER"

class DividasViewModel(app: Application) : AndroidViewModel(app) {
    private val banco = AppDatabase.get(app)
    private val dao = banco.dividaDao()
    private val recebimentoDao = banco.recebimentoDao()
    private val prefsRepo = PreferenciasRepo(app)
    private val filtro = MutableStateFlow(Filtro())
    private val abertas = MutableStateFlow<Set<String>>(emptySet())
    private val eventos = Channel<Desfazer>(Channel.BUFFERED)
    private val collator = Collator.getInstance(LocaleBR).apply { strength = Collator.PRIMARY }

    val desfazer: Flow<Desfazer> = eventos.receiveAsFlow()

    val preferencias: StateFlow<Preferencias?> =
        prefsRepo.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ui: StateFlow<UiState> =
        combine(dao.observarTodas(), recebimentoDao.observarTodos(), filtro, abertas) { todas, recebimentos, f, a ->
            montar(todas, recebimentos, f, a)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun montar(
        todas: List<Divida>,
        recebimentos: List<Recebimento>,
        f: Filtro,
        abertas: Set<String>,
    ): UiState {
        val hoje = LocalDate.now()
        val daCategoria = todas.filter { f.categoria == null || it.categoria == f.categoria }
        val pendentes = daCategoria.filter { !it.paga }
        val fimDoMes = hoje.withDayOfMonth(hoje.lengthOfMonth()).toEpochDay()

        val resumo = Resumo(
            pendenteMes = pendentes.filter { it.vencimentoEpochDay <= fimDoMes }.sumOf { it.valorCentavos },
            totalRestante = pendentes.sumOf { if (it.recorrente && it.pagaNoMes(hoje)) 0L else it.valorRestanteCentavos },
            vencidas = pendentes.count { it.vencimentoEpochDay < hoje.toEpochDay() },
            pendentes = pendentes.size,
            contasDoMes = contasDoMes(todas, hoje),
            extrasDoMes = extrasDoMes(recebimentos, hoje),
        )

        val porStatus = daCategoria.filter {
            when (f.status) {
                FiltroStatus.PENDENTES -> !it.paga
                FiltroStatus.PAGAS -> it.paga
                FiltroStatus.TODAS -> true
            }
        }
        val ordenadas = when (f.ordem) {
            Ordem.VENCIMENTO -> porStatus.sortedWith(compareBy<Divida> { it.paga }.thenBy { it.vencimentoEpochDay })
            Ordem.MAIOR_VALOR -> porStatus.sortedByDescending { it.valorCentavos }
            Ordem.MENOR_VALOR -> porStatus.sortedBy { it.valorCentavos }
            Ordem.NOME -> porStatus.sortedWith { a, b -> collator.compare(a.descricao, b.descricao) }
        }
        val grupos = ordenadas.groupBy { it.categoria }.map { (categoria, dividas) ->
            val emAberto = dividas.filter { !it.paga && !it.pagaNoMes(hoje) }
            Grupo(
                categoria = categoria,
                dividas = dividas,
                pendentes = emAberto.size,
                pagasNoMes = dividas.count { it.pagaNoMes(hoje) },
                quitadas = dividas.count { it.paga },
                vencidas = emAberto.count { it.vencimentoEpochDay < hoje.toEpochDay() },
                totalPendente = emAberto.sumOf { it.valorCentavos },
                proxima = emAberto.minByOrNull { it.vencimentoEpochDay },
            )
        }.sortedWith(
            // Gavetas com vencidas primeiro, depois pela próxima conta a vencer; as já em dia vão para o fim.
            compareByDescending<Grupo> { it.vencidas > 0 }
                .thenBy { it.proxima?.vencimentoEpochDay ?: Long.MAX_VALUE }
                .thenBy { it.categoria.ordinal },
        )

        return UiState(
            todas = todas,
            recebimentos = recebimentos,
            grupos = grupos,
            receber = montarReceber(recebimentos, f, hoje),
            abertas = abertas,
            resumo = resumo,
            filtro = f,
            hoje = hoje,
            carregando = false,
        )
    }

    /** Renda extra não tem categoria de dívida: com uma categoria filtrada, a gaveta some. */
    private fun montarReceber(recebimentos: List<Recebimento>, f: Filtro, hoje: LocalDate): GrupoReceber? {
        if (f.categoria != null) return null
        val visiveis = recebimentos.filter {
            when (f.status) {
                FiltroStatus.PENDENTES -> !it.recebido
                FiltroStatus.PAGAS -> it.recebido
                FiltroStatus.TODAS -> true
            }
        }
        if (visiveis.isEmpty()) return null
        val ordenados = when (f.ordem) {
            Ordem.VENCIMENTO -> visiveis.sortedWith(compareBy<Recebimento> { it.recebido }.thenBy { it.dataEpochDay })
            Ordem.MAIOR_VALOR -> visiveis.sortedByDescending { it.valorCentavos }
            Ordem.MENOR_VALOR -> visiveis.sortedBy { it.valorCentavos }
            Ordem.NOME -> visiveis.sortedWith { a, b -> collator.compare(a.descricao, b.descricao) }
        }
        val emAberto = ordenados.filter { !it.recebido && !it.recebidoNoMes(hoje) }
        return GrupoReceber(
            itens = ordenados,
            aReceber = emAberto.size,
            recebidosNoMes = ordenados.count { it.recebidoNoMes(hoje) },
            recebidos = ordenados.count { it.recebido },
            totalAReceber = emAberto.sumOf { it.valorCentavos },
            proximo = emAberto.minByOrNull { it.dataEpochDay },
        )
    }

    private fun contasDoMes(todas: List<Divida>, hoje: LocalDate): Long {
        val inicio = hoje.withDayOfMonth(1).toEpochDay()
        val fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).toEpochDay()
        return todas.sumOf { d ->
            when {
                // Quitada: conta só se o vencimento era deste mês.
                d.paga -> if (d.vencimentoEpochDay in inicio..fim) d.valorCentavos else 0L
                // Mensal/parcelada já paga neste mês: a parcela deste mês conta.
                d.pagaNoMes(hoje) -> d.valorCentavos
                // Em aberto que vence até o fim do mês (ou já venceu).
                d.vencimentoEpochDay <= fim -> d.valorCentavos
                else -> 0L
            }
        }
    }

    private fun extrasDoMes(recebimentos: List<Recebimento>, hoje: LocalDate): Long {
        val inicio = hoje.withDayOfMonth(1).toEpochDay()
        val fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).toEpochDay()
        return recebimentos.sumOf { r ->
            when {
                // Já recebido (único): entra se era previsto para este mês.
                r.recebido -> if (r.dataEpochDay in inicio..fim) r.valorCentavos else 0L
                // Mensal que já entrou neste mês.
                r.recebidoNoMes(hoje) -> r.valorCentavos
                // Previsto até o fim do mês (inclui atrasados).
                r.dataEpochDay <= fim -> r.valorCentavos
                else -> 0L
            }
        }
    }

    fun alternarGaveta(chave: String) =
        abertas.update { if (chave in it) it - chave else it + chave }

    fun definirStatus(status: FiltroStatus) = filtro.update { it.copy(status = status) }
    fun definirCategoria(categoria: Categoria?) = filtro.update { it.copy(categoria = categoria) }
    fun definirOrdem(ordem: Ordem) = filtro.update { it.copy(ordem = ordem) }

    // --- Dívidas ---

    fun alternarPaga(divida: Divida) {
        viewModelScope.launch {
            val (nova, mensagem) = when {
                divida.paga -> divida.reabrir() to "“${divida.descricao}” voltou para pendentes"
                divida.pagaNoMes(LocalDate.now()) ->
                    divida.desfazerUltimoPagamento() to "Pagamento de “${divida.descricao}” desfeito"
                else -> {
                    val paga = divida.pagarParcela()
                    paga to when {
                        paga.paga -> "“${divida.descricao}” quitada"
                        divida.recorrente -> "“${divida.descricao}” paga · próxima em ${formatarData(paga.vencimento)}"
                        else -> "Parcela ${divida.parcelasPagas + 1}/${divida.totalParcelas} paga · " +
                            "próxima em ${formatarData(paga.vencimento)}"
                    }
                }
            }
            dao.salvar(nova)
            eventos.send(Desfazer(mensagem) { restaurar(divida) })
        }
    }

    fun salvar(divida: Divida) {
        // Abre a gaveta da categoria para a dívida recém-salva aparecer.
        abertas.update { it + divida.categoria.name }
        viewModelScope.launch { dao.salvar(divida) }
    }

    fun excluir(divida: Divida) {
        viewModelScope.launch {
            dao.excluir(divida)
            eventos.send(Desfazer("“${divida.descricao}” excluída") { restaurar(divida) })
        }
    }

    private fun restaurar(divida: Divida) {
        viewModelScope.launch { dao.salvar(divida) }
    }

    // --- Recebimentos ---

    fun alternarRecebido(r: Recebimento) {
        viewModelScope.launch {
            val desfazendo = r.recebido || r.recebidoNoMes(LocalDate.now())
            val novo = if (desfazendo) r.desfazerRecebimento() else r.receber()
            recebimentoDao.salvar(novo)
            val mensagem = when {
                desfazendo -> "Recebimento de “${r.descricao}” desfeito"
                r.recorrente -> "“${r.descricao}” recebido · próximo em ${formatarData(novo.data)}"
                else -> "“${r.descricao}” recebido"
            }
            eventos.send(Desfazer(mensagem) { restaurar(r) })
        }
    }

    fun salvar(recebimento: Recebimento) {
        abertas.update { it + GAVETA_RECEBER }
        viewModelScope.launch { recebimentoDao.salvar(recebimento) }
    }

    fun excluir(recebimento: Recebimento) {
        viewModelScope.launch {
            recebimentoDao.excluir(recebimento)
            eventos.send(Desfazer("“${recebimento.descricao}” excluído") { restaurar(recebimento) })
        }
    }

    private fun restaurar(recebimento: Recebimento) {
        viewModelScope.launch { recebimentoDao.salvar(recebimento) }
    }

    // --- Preferências ---

    fun definirTema(tema: Tema) {
        viewModelScope.launch { prefsRepo.definirTema(tema) }
    }

    fun definirSalario(centavos: Long) {
        viewModelScope.launch { prefsRepo.definirSalario(centavos) }
    }

    fun definirLembretes(ativos: Boolean) {
        viewModelScope.launch { prefsRepo.definirLembretes(ativos) }
    }

    fun definirDiasAntecedencia(dias: Int) {
        viewModelScope.launch { prefsRepo.definirDiasAntecedencia(dias) }
    }
}
