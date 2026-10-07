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
import com.minhasdividas.app.data.Tema
import com.minhasdividas.app.data.formatarData
import com.minhasdividas.app.data.desfazerUltimoPagamento
import com.minhasdividas.app.data.pagaNoMes
import com.minhasdividas.app.data.pagarParcela
import com.minhasdividas.app.data.reabrir
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

data class UiState(
    val todas: List<Divida> = emptyList(),
    val grupos: List<Grupo> = emptyList(),
    val abertas: Set<Categoria> = emptySet(),
    val resumo: Resumo = Resumo(),
    val filtro: Filtro = Filtro(),
    val hoje: LocalDate = LocalDate.now(),
    val carregando: Boolean = true,
)

/** Mensagem com opção de desfazer: [anterior] é o estado da dívida antes da ação. */
data class Desfazer(val mensagem: String, val anterior: Divida)

class DividasViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dividaDao()
    private val prefsRepo = PreferenciasRepo(app)
    private val filtro = MutableStateFlow(Filtro())
    private val abertas = MutableStateFlow<Set<Categoria>>(emptySet())
    private val eventos = Channel<Desfazer>(Channel.BUFFERED)
    private val collator = Collator.getInstance(LocaleBR).apply { strength = Collator.PRIMARY }

    val desfazer: Flow<Desfazer> = eventos.receiveAsFlow()

    val preferencias: StateFlow<Preferencias?> =
        prefsRepo.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ui: StateFlow<UiState> = combine(dao.observarTodas(), filtro, abertas) { todas, f, a -> montar(todas, f, a) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun montar(todas: List<Divida>, f: Filtro, abertas: Set<Categoria>): UiState {
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
        return UiState(todas, grupos, abertas, resumo, f, hoje, carregando = false)
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

    fun alternarGaveta(categoria: Categoria) =
        abertas.update { if (categoria in it) it - categoria else it + categoria }

    fun definirStatus(status: FiltroStatus) = filtro.update { it.copy(status = status) }
    fun definirCategoria(categoria: Categoria?) = filtro.update { it.copy(categoria = categoria) }
    fun definirOrdem(ordem: Ordem) = filtro.update { it.copy(ordem = ordem) }

    fun alternarPaga(divida: Divida) {
        viewModelScope.launch {
            if (divida.paga) {
                dao.salvar(divida.reabrir())
                eventos.send(Desfazer("“${divida.descricao}” voltou para pendentes", divida))
            } else if (divida.pagaNoMes(LocalDate.now())) {
                dao.salvar(divida.desfazerUltimoPagamento())
                eventos.send(Desfazer("Pagamento de “${divida.descricao}” desfeito", divida))
            } else {
                val nova = divida.pagarParcela()
                dao.salvar(nova)
                val mensagem = if (nova.paga) {
                    "“${divida.descricao}” quitada"
                } else if (divida.recorrente) {
                    "“${divida.descricao}” paga · próxima em ${formatarData(nova.vencimento)}"
                } else {
                    "Parcela ${divida.parcelasPagas + 1}/${divida.totalParcelas} paga · próxima em ${formatarData(nova.vencimento)}"
                }
                eventos.send(Desfazer(mensagem, divida))
            }
        }
    }

    fun salvar(divida: Divida) {
        // Abre a gaveta da categoria para a dívida recém-salva aparecer.
        abertas.update { it + divida.categoria }
        viewModelScope.launch { dao.salvar(divida) }
    }

    fun excluir(divida: Divida) {
        viewModelScope.launch {
            dao.excluir(divida)
            eventos.send(Desfazer("“${divida.descricao}” excluída", divida))
        }
    }

    fun restaurar(divida: Divida) {
        viewModelScope.launch { dao.salvar(divida) }
    }

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
