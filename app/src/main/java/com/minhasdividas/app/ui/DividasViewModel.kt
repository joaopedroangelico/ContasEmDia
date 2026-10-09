package com.minhasdividas.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minhasdividas.app.data.AppDatabase
import com.minhasdividas.app.data.Armazenamento
import com.minhasdividas.app.data.DadosBackup
import com.minhasdividas.app.data.FormaPagamento
import com.minhasdividas.app.data.LeituraBackup
import com.minhasdividas.app.data.LimpezaMes
import com.minhasdividas.app.data.MesHistorico
import com.minhasdividas.app.data.Pagamento
import com.minhasdividas.app.data.agruparPorMes
import com.minhasdividas.app.data.gerarArquivoBackup
import com.minhasdividas.app.data.inicioBackup
import com.minhasdividas.app.data.lerArquivoBackup
import com.minhasdividas.app.data.medirArmazenamento
import com.minhasdividas.app.data.mesesComHistorico
import com.minhasdividas.app.data.montarBackup
import com.minhasdividas.app.data.planejarLimpeza
import com.minhasdividas.app.data.quitadasSemHistorico
import com.minhasdividas.app.data.registrarPagamento
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
import com.minhasdividas.app.lembretes.Lembretes
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Collator
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
    /** Soma da parcela em atraso de cada dívida vencida. */
    val valorVencidas: Long = 0,
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
    /** Em aberto (nem quitadas nem pagas neste mês): o que o "Pagar tudo" paga. */
    val emAberto: List<Divida>,
    /** Peso da categoria no mês: pagas neste mês + a pagar (inclui vencidas). */
    val totalDoMes: Long,
)

/** A gaveta "A receber": renda extra prevista e já recebida. */
data class GrupoReceber(
    val itens: List<Recebimento>,
    val aReceber: Int,
    val recebidosNoMes: Int,
    val recebidos: Int,
    val totalAReceber: Long,
    val proximo: Recebimento?,
    val emAberto: List<Recebimento>,
    val totalDoMes: Long,
)

data class UiState(
    val todas: List<Divida> = emptyList(),
    val recebimentos: List<Recebimento> = emptyList(),
    val pagamentos: List<Pagamento> = emptyList(),
    /** Aba Pendentes: gavetas por categoria. */
    val grupos: List<Grupo> = emptyList(),
    /** Abas Pagas e Todas: gavetas por mês do pagamento. */
    val meses: List<MesHistorico> = emptyList(),
    /** Quitadas antes do histórico existir: sem data de pagamento, ficam numa gaveta à parte. */
    val semData: List<Divida> = emptyList(),
    /** Ids do último pagamento de cada dívida: só ele pode ser desfeito. */
    val ultimosPagamentos: Set<Long> = emptySet(),
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
const val GAVETA_SEM_DATA = "SEM_DATA"

fun chaveMes(mes: YearMonth) = "MES:$mes"

class DividasViewModel(app: Application) : AndroidViewModel(app) {
    private val banco = AppDatabase.get(app)
    private val dao = banco.dividaDao()
    private val recebimentoDao = banco.recebimentoDao()
    private val pagamentoDao = banco.pagamentoDao()
    private val prefsRepo = PreferenciasRepo(app)
    private val filtro = MutableStateFlow(Filtro())
    // O mês atual já começa aberto nas abas Pagas e Todas.
    private val abertas = MutableStateFlow(setOf(chaveMes(YearMonth.now())))
    private val eventos = Channel<Desfazer>(Channel.BUFFERED)
    private val collator = Collator.getInstance(LocaleBR).apply { strength = Collator.PRIMARY }

    val desfazer: Flow<Desfazer> = eventos.receiveAsFlow()

    val preferencias: StateFlow<Preferencias?> =
        prefsRepo.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ui: StateFlow<UiState> =
        combine(
            dao.observarTodas(),
            recebimentoDao.observarTodos(),
            pagamentoDao.observarTodos(),
            filtro,
            abertas,
        ) { todas, recebimentos, pagamentos, f, a ->
            montar(todas, recebimentos, pagamentos, f, a)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun montar(
        todas: List<Divida>,
        recebimentos: List<Recebimento>,
        pagamentos: List<Pagamento>,
        f: Filtro,
        abertas: Set<String>,
    ): UiState {
        val hoje = LocalDate.now()
        val daCategoria = todas.filter { f.categoria == null || it.categoria == f.categoria }
        val pendentes = daCategoria.filter { !it.paga }
        val fimDoMes = hoje.withDayOfMonth(hoje.lengthOfMonth()).toEpochDay()

        val vencidas = pendentes.filter { it.vencimentoEpochDay < hoje.toEpochDay() }
        val resumo = Resumo(
            pendenteMes = pendentes.filter { it.vencimentoEpochDay <= fimDoMes }.sumOf { it.valorCentavos },
            totalRestante = pendentes.sumOf { if (it.recorrente && it.pagaNoMes(hoje)) 0L else it.valorRestanteCentavos },
            vencidas = vencidas.size,
            valorVencidas = vencidas.sumOf { it.valorCentavos },
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
        val porMes = f.status != FiltroStatus.PENDENTES
        val grupos = if (porMes) emptyList() else ordenadas.groupBy { it.categoria }.map { (categoria, dividas) ->
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
                emAberto = emAberto,
                // Todas da categoria (não só as visíveis no filtro), como no total do cabeçalho.
                totalDoMes = todas.filter { it.categoria == categoria }.sumOf { it.valorNoMes(hoje) },
            )
        }.sortedWith(
            // Gavetas com vencidas primeiro, depois pela próxima conta a vencer; as já em dia vão para o fim.
            compareByDescending<Grupo> { it.vencidas > 0 }
                .thenBy { it.proxima?.vencimentoEpochDay ?: Long.MAX_VALUE }
                .thenBy { it.categoria.ordinal },
        )

        val zona = ZoneId.systemDefault()
        val meses = if (!porMes) {
            emptyList()
        } else {
            agruparPorMes(daCategoria, pagamentos, zona)
                // Em Pagas, meses só com contas em aberto não têm o que mostrar.
                .filter { f.status == FiltroStatus.TODAS || it.pagos.isNotEmpty() }
                .map { it.ordenado(f.ordem) }
        }

        return UiState(
            todas = todas,
            recebimentos = recebimentos,
            pagamentos = pagamentos,
            grupos = grupos,
            meses = meses,
            semData = if (porMes) {
                quitadasSemHistorico(daCategoria, pagamentos).sortedByDescending { it.vencimentoEpochDay }
            } else {
                emptyList()
            },
            ultimosPagamentos = pagamentos.groupBy { it.dividaId }.values
                .mapTo(HashSet()) { lista -> lista.maxWith(compareBy<Pagamento> { it.pagoEmMillis }.thenBy { it.id }).id },
            receber = montarReceber(recebimentos, f, hoje),
            abertas = abertas,
            resumo = resumo,
            filtro = f,
            hoje = hoje,
            carregando = false,
        )
    }

    /** "Vencimento" mantém o padrão do mês: pagamentos do mais recente, em aberto pelo vencimento. */
    private fun MesHistorico.ordenado(ordem: Ordem): MesHistorico = when (ordem) {
        Ordem.VENCIMENTO -> this
        Ordem.MAIOR_VALOR -> copy(
            pagos = pagos.sortedByDescending { it.pagamento.valorCentavos },
            pendentes = pendentes.sortedByDescending { it.valorCentavos },
        )
        Ordem.MENOR_VALOR -> copy(
            pagos = pagos.sortedBy { it.pagamento.valorCentavos },
            pendentes = pendentes.sortedBy { it.valorCentavos },
        )
        Ordem.NOME -> copy(
            pagos = pagos.sortedWith { a, b -> collator.compare(a.divida.descricao, b.divida.descricao) },
            pendentes = pendentes.sortedWith { a, b -> collator.compare(a.descricao, b.descricao) },
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
            emAberto = emAberto,
            totalDoMes = recebimentos.sumOf { it.valorNoMes(hoje) },
        )
    }

    private fun contasDoMes(todas: List<Divida>, hoje: LocalDate): Long = todas.sumOf { it.valorNoMes(hoje) }

    private fun extrasDoMes(recebimentos: List<Recebimento>, hoje: LocalDate): Long =
        recebimentos.sumOf { it.valorNoMes(hoje) }

    /** Quanto a dívida pesa no mês corrente. */
    private fun Divida.valorNoMes(hoje: LocalDate): Long {
        val inicio = hoje.withDayOfMonth(1).toEpochDay()
        val fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).toEpochDay()
        return when {
            // Quitada: conta só se o vencimento era deste mês.
            paga -> if (vencimentoEpochDay in inicio..fim) valorCentavos else 0L
            // Mensal/parcelada já paga neste mês: a parcela deste mês conta.
            pagaNoMes(hoje) -> valorCentavos
            // Em aberto que vence até o fim do mês (ou já venceu).
            vencimentoEpochDay <= fim -> valorCentavos
            else -> 0L
        }
    }

    /** Quanto o recebimento soma no mês corrente. */
    private fun Recebimento.valorNoMes(hoje: LocalDate): Long {
        val inicio = hoje.withDayOfMonth(1).toEpochDay()
        val fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).toEpochDay()
        return when {
            // Já recebido (único): entra se era previsto para este mês.
            recebido -> if (dataEpochDay in inicio..fim) valorCentavos else 0L
            // Mensal que já entrou neste mês.
            recebidoNoMes(hoje) -> valorCentavos
            // Previsto até o fim do mês (inclui atrasados).
            dataEpochDay <= fim -> valorCentavos
            else -> 0L
        }
    }

    fun alternarGaveta(chave: String) =
        abertas.update { if (chave in it) it - chave else it + chave }

    fun definirStatus(status: FiltroStatus) = filtro.update { it.copy(status = status) }
    fun definirCategoria(categoria: Categoria?) = filtro.update { it.copy(categoria = categoria) }
    fun definirOrdem(ordem: Ordem) = filtro.update { it.copy(ordem = ordem) }

    // --- Dívidas ---

    /** Paga a parcela/conta atual e registra quando e como no histórico. */
    fun pagar(divida: Divida, forma: FormaPagamento?, tornarPadrao: Boolean) {
        viewModelScope.launch {
            val base = if (tornarPadrao) divida.copy(formaPagamento = forma) else divida
            val paga = base.pagarParcela()
            val registro = divida.registrarPagamento(System.currentTimeMillis(), forma)
            val idRegistro = banco.withTransaction {
                dao.salvar(paga)
                pagamentoDao.inserir(registro)
            }
            val via = forma?.let { " via ${it.rotulo}" }.orEmpty()
            val mensagem = when {
                paga.paga -> "“${divida.descricao}” quitada$via"
                divida.recorrente -> "“${divida.descricao}” paga$via · próxima em ${formatarData(paga.vencimento)}"
                else -> "Parcela ${divida.parcelasPagas + 1}/${divida.totalParcelas} paga$via · " +
                    "próxima em ${formatarData(paga.vencimento)}"
            }
            eventos.send(
                Desfazer(mensagem) {
                    viewModelScope.launch {
                        banco.withTransaction {
                            dao.salvar(divida)
                            pagamentoDao.excluirPorId(idRegistro)
                        }
                    }
                },
            )
        }
    }

    /** Desfaz o último pagamento: volta uma parcela (ou reabre a quitada) e apaga o registro dele. */
    fun desfazerPagamento(divida: Divida) {
        viewModelScope.launch {
            val nova = if (divida.paga) divida.reabrir() else divida.desfazerUltimoPagamento()
            val ultimo = pagamentoDao.ultimoDa(divida.id)
            banco.withTransaction {
                dao.salvar(nova)
                ultimo?.let { pagamentoDao.excluir(it) }
            }
            val mensagem = if (divida.paga) {
                "“${divida.descricao}” voltou para pendentes"
            } else {
                "Pagamento de “${divida.descricao}” desfeito"
            }
            eventos.send(
                Desfazer(mensagem) {
                    viewModelScope.launch {
                        banco.withTransaction {
                            dao.salvar(divida)
                            ultimo?.let { pagamentoDao.inserir(it) }
                        }
                    }
                },
            )
        }
    }

    fun alterarForma(pagamento: Pagamento, forma: FormaPagamento?) {
        viewModelScope.launch { pagamentoDao.atualizar(pagamento.copy(forma = forma)) }
    }

    /**
     * "Pagar tudo" da gaveta: paga a parcela/conta atual de cada dívida em aberto, sem perguntar a forma:
     * cada uma fica com a sua forma padrão.
     */
    fun pagarTodas(dividas: List<Divida>) {
        if (dividas.isEmpty()) return
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            val registros = dividas.map { it.registrarPagamento(agora, it.formaPagamento) }
            val ids = banco.withTransaction {
                dividas.forEach { dao.salvar(it.pagarParcela()) }
                registros.map { pagamentoDao.inserir(it) }
            }
            val mensagem = if (dividas.size == 1) "1 conta paga" else "${dividas.size} contas pagas"
            eventos.send(
                Desfazer(mensagem) {
                    viewModelScope.launch {
                        banco.withTransaction {
                            dividas.forEach { dao.salvar(it) }
                            ids.forEach { pagamentoDao.excluirPorId(it) }
                        }
                    }
                },
            )
        }
    }

    fun salvar(divida: Divida) {
        // Abre a gaveta da categoria para a dívida recém-salva aparecer.
        abertas.update { it + divida.categoria.name }
        viewModelScope.launch { dao.salvar(divida) }
    }

    /** Exclui a dívida com o histórico de pagamentos dela; desfazer traz os dois de volta. */
    fun excluir(divida: Divida) {
        viewModelScope.launch {
            val historico = pagamentoDao.daDivida(divida.id)
            banco.withTransaction {
                pagamentoDao.excluirDaDivida(divida.id)
                dao.excluir(divida)
            }
            eventos.send(
                Desfazer("“${divida.descricao}” excluída") {
                    viewModelScope.launch {
                        banco.withTransaction {
                            dao.salvar(divida)
                            pagamentoDao.inserirTodos(historico)
                        }
                    }
                },
            )
        }
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

    /** "Receber tudo" da gaveta "A receber". */
    fun receberTodos(recebimentos: List<Recebimento>) {
        if (recebimentos.isEmpty()) return
        viewModelScope.launch {
            banco.withTransaction { recebimentos.forEach { recebimentoDao.salvar(it.receber()) } }
            val mensagem = if (recebimentos.size == 1) "1 valor recebido" else "${recebimentos.size} valores recebidos"
            eventos.send(
                Desfazer(mensagem) {
                    viewModelScope.launch {
                        banco.withTransaction { recebimentos.forEach { recebimentoDao.salvar(it) } }
                    }
                },
            )
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

    // --- Backup e dados ---

    /** Grava o backup do período escolhido em Ajustes no arquivo [uri]. Retorna o tamanho em bytes. */
    suspend fun exportar(uri: Uri, senha: String?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = prefsRepo.preferencias.first()
            val dados = montarBackup(
                dividas = dao.todas(),
                pagamentos = pagamentoDao.todos(),
                recebimentos = recebimentoDao.todos(),
                salarioCentavos = prefs.salarioCentavos,
                desde = prefs.inicioBackup(LocalDate.now()),
                agoraMillis = System.currentTimeMillis(),
                zona = ZoneId.systemDefault(),
            )
            val bytes = gerarArquivoBackup(dados, senha).toByteArray(Charsets.UTF_8)
            val saida = checkNotNull(getApplication<Application>().contentResolver.openOutputStream(uri, "wt"))
            saida.use { it.write(bytes) }
            bytes.size
        }
    }

    suspend fun lerBackup(uri: Uri, senha: String?): LeituraBackup {
        val texto = withContext(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { entrada ->
                    // Um backup tem poucos KB; um arquivo enorme não é backup do app.
                    val bytes = entrada.lerAte(LIMITE_BACKUP + 1)
                    if (bytes.size > LIMITE_BACKUP) null else String(bytes, Charsets.UTF_8)
                }
            }.getOrNull()
        } ?: return LeituraBackup.Invalido
        // Derivar a chave da senha leva um instante: fora da thread principal.
        return withContext(Dispatchers.Default) { lerArquivoBackup(texto, senha) }
    }

    /** Substitui todos os dados do app pelos do backup. */
    suspend fun importar(dados: DadosBackup) {
        withContext(Dispatchers.IO) {
            banco.withTransaction {
                pagamentoDao.limpar()
                dao.limpar()
                recebimentoDao.limpar()
                dao.inserirTodas(dados.dividas)
                pagamentoDao.inserirTodos(dados.pagamentos)
                recebimentoDao.inserirTodos(dados.recebimentos)
            }
            prefsRepo.definirSalario(dados.salarioCentavos)
        }
    }

    suspend fun mesesParaApagar(): List<YearMonth> =
        mesesComHistorico(pagamentoDao.todos(), recebimentoDao.todos(), ZoneId.systemDefault())

    suspend fun planejarApagarMes(mes: YearMonth): LimpezaMes =
        planejarLimpeza(mes, dao.todas(), pagamentoDao.todos(), recebimentoDao.todos(), ZoneId.systemDefault())

    suspend fun apagarMes(mes: YearMonth) {
        banco.withTransaction {
            val plano = planejarApagarMes(mes)
            pagamentoDao.excluirTodos(plano.pagamentos)
            dao.excluirTodas(plano.dividas)
            recebimentoDao.excluirTodos(plano.recebimentos)
        }
    }

    suspend fun armazenamento(): Armazenamento =
        withContext(Dispatchers.IO) { medirArmazenamento(getApplication()) }

    fun definirPeriodoBackup(meses: Int, desdeEpochDay: Long? = null) {
        viewModelScope.launch { prefsRepo.definirPeriodoBackup(meses, desdeEpochDay) }
    }

    fun definirBackupComSenha(ativo: Boolean) {
        viewModelScope.launch { prefsRepo.definirBackupComSenha(ativo) }
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

    fun definirHorarioAviso(minutos: Int) {
        viewModelScope.launch {
            prefsRepo.definirMinutosAviso(minutos)
            Lembretes.agendar(getApplication(), minutos, substituir = true)
        }
    }
}

private const val LIMITE_BACKUP = 20 * 1024 * 1024

/** Lê até [limite] bytes (InputStream.readNBytes só existe a partir do Android 13). */
private fun InputStream.lerAte(limite: Int): ByteArray {
    val saida = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (saida.size() < limite) {
        val lidos = read(buffer, 0, minOf(buffer.size, limite - saida.size()))
        if (lidos < 0) break
        saida.write(buffer, 0, lidos)
    }
    return saida.toByteArray()
}
