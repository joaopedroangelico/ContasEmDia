package com.minhasdividas.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minhasdividas.app.data.Categoria
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.Preferencias
import com.minhasdividas.app.data.diasAte
import com.minhasdividas.app.data.formatarDataCurta
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.parcelaAtual
import com.minhasdividas.app.data.parcelada
import com.minhasdividas.app.data.textoVencimento
import com.minhasdividas.app.data.valorRestanteCentavos
import com.minhasdividas.app.data.vencimento
import com.minhasdividas.app.lembretes.Lembretes
import com.minhasdividas.app.ui.theme.aviso
import com.minhasdividas.app.ui.theme.escuro
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import com.minhasdividas.app.data.pagaNoMes
import com.minhasdividas.app.ui.theme.sucesso
import kotlinx.coroutines.delay
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import kotlin.math.abs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val NOVA = 0L
private const val NENHUMA = -1L

@Composable
fun TelaDividas(vm: DividasViewModel, preferencias: Preferencias) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Id da dívida aberta no formulário: NOVA para cadastro, NENHUMA quando fechado.
    var formularioId by rememberSaveable { mutableLongStateOf(NENHUMA) }
    var ajustesAbertos by rememberSaveable { mutableStateOf(false) }
    var editandoSalario by rememberSaveable { mutableStateOf(false) }
    var recebimentoId by rememberSaveable { mutableLongStateOf(NENHUMA) }
    var menuAdicionar by rememberSaveable { mutableStateOf(false) }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun pedirPermissaoSeNecessario() {
        if (preferencias.lembretesAtivos && !Lembretes.temPermissao(context) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(vm) {
        vm.desfazer.collect { evento ->
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val resultado = snackbar.showSnackbar(
                    message = evento.mensagem,
                    actionLabel = "Desfazer",
                    duration = SnackbarDuration.Short,
                )
                if (resultado == SnackbarResult.ActionPerformed) evento.desfazer()
            }
        }
    }

    val fabExpandido by remember { derivedStateOf { listState.firstVisibleItemIndex < 3 } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            BotaoAdicionar(
                aberto = menuAdicionar,
                rotuloVisivel = fabExpandido,
                onAlternar = { menuAdicionar = !menuAdicionar },
                onNovaDivida = {
                    menuAdicionar = false
                    formularioId = NOVA
                },
                onAReceber = {
                    menuAdicionar = false
                    recebimentoId = NOVA
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "cabecalho") {
                Cabecalho(
                    salario = preferencias.salarioCentavos,
                    extras = ui.resumo.extrasDoMes,
                    contasDoMes = ui.resumo.contasDoMes,
                    onEditarSalario = { editandoSalario = true },
                    onAjustes = { ajustesAbertos = true },
                )
            }
            item(key = "resumo") {
                CartaoResumo(resumo = ui.resumo, categoria = ui.filtro.categoria)
            }
            item(key = "filtros") {
                BarraFiltros(
                    filtro = ui.filtro,
                    onStatus = vm::definirStatus,
                    onCategoria = vm::definirCategoria,
                    onOrdem = vm::definirOrdem,
                )
            }
            ui.receber?.let { grupo ->
                item(key = GAVETA_RECEBER) {
                    GavetaReceber(
                        grupo = grupo,
                        aberta = GAVETA_RECEBER in ui.abertas,
                        hoje = ui.hoje,
                        onAlternar = { vm.alternarGaveta(GAVETA_RECEBER) },
                        onAbrir = { recebimentoId = it.id },
                        onReceber = vm::alternarRecebido,
                        onReceberTodos = vm::receberTodos,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (!ui.carregando && ui.grupos.isEmpty() && ui.receber == null) {
                item(key = "vazio") { EstadoVazio(ui.filtro, Modifier.animateItem()) }
            }
            items(ui.grupos, key = { it.categoria.name }) { grupo ->
                GavetaCategoria(
                    grupo = grupo,
                    // Com uma categoria filtrada, só há uma gaveta: já vem aberta.
                    aberta = ui.filtro.categoria != null || grupo.categoria.name in ui.abertas,
                    hoje = ui.hoje,
                    onAlternar = { vm.alternarGaveta(grupo.categoria.name) },
                    onAbrirDivida = { formularioId = it.id },
                    onPagar = vm::alternarPaga,
                    onPagarTodas = vm::pagarTodas,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        // Véu atrás do menu "+" (o Scaffold empilha o conteúdo): escurece a lista e fecha ao tocar fora.
        AnimatedVisibility(visible = menuAdicionar, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Fechar menu",
                    ) { menuAdicionar = false },
            )
        }
    }
    BackHandler(enabled = menuAdicionar) { menuAdicionar = false }

    val inicial = ui.todas.firstOrNull { it.id == formularioId }
    // Ao editar, espera a dívida carregar (ex.: após girar a tela) para não abrir um cadastro vazio.
    if (formularioId == NOVA || inicial != null) {
        FolhaFormulario(
            inicial = inicial,
            onFechar = { formularioId = NENHUMA },
            onSalvar = { divida ->
                vm.salvar(divida)
                pedirPermissaoSeNecessario()
            },
            onExcluir = vm::excluir,
        )
    }

    val recebimentoInicial = ui.recebimentos.firstOrNull { it.id == recebimentoId }
    if (recebimentoId == NOVA || recebimentoInicial != null) {
        FolhaRecebimento(
            inicial = recebimentoInicial,
            onFechar = { recebimentoId = NENHUMA },
            onSalvar = vm::salvar,
            onExcluir = vm::excluir,
        )
    }

    if (editandoSalario) {
        DialogoSalario(
            atual = preferencias.salarioCentavos,
            onConfirmar = vm::definirSalario,
            onFechar = { editandoSalario = false },
        )
    }

    if (ajustesAbertos) {
        FolhaAjustes(
            preferencias = preferencias,
            onFechar = { ajustesAbertos = false },
            onTema = vm::definirTema,
            onLembretes = vm::definirLembretes,
            onDias = vm::definirDiasAntecedencia,
        )
    }
}

@Composable
private fun Cabecalho(salario: Long, extras: Long, contasDoMes: Long, onEditarSalario: () -> Unit, onAjustes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Column(Modifier.weight(1f)) {
            LinhaSalario(salario, extras, contasDoMes, onEditarSalario)
        }
        FilledTonalIconButton(onClick = onAjustes) {
            Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
        }
    }
}

/**
 * Renda do mês (salário + renda extra) e quanto sobra depois de todas as contas do mês
 * (pagas e a pagar). Tocar abre a edição do salário.
 */
@Composable
private fun LinhaSalario(salario: Long, extras: Long, contasDoMes: Long, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Editar salário", onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        val renda = salario + extras
        if (renda <= 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Payments, contentDescription = null, tint = cores.primary, modifier = Modifier.size(18.dp))
                Text("Informar salário", style = MaterialTheme.typography.titleSmall, color = cores.primary)
            }
            return@Row
        }
        val sobra = renda - contasDoMes
        Column {
            Text(
                if (extras > 0) "Renda do mês" else "Salário",
                style = MaterialTheme.typography.labelMedium,
                color = cores.onSurfaceVariant,
            )
            Text(formatarMoeda(renda), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
        Column {
            Text(
                if (sobra < 0) "Faltam no mês" else "Sobra no mês",
                style = MaterialTheme.typography.labelMedium,
                color = cores.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = sobra,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "sobra",
            ) { valor ->
                Text(
                    formatarMoeda(abs(valor)),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (valor < 0) cores.error else cores.sucesso,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CartaoResumo(resumo: Resumo, categoria: Categoria?) {
    val cores = MaterialTheme.colorScheme
    // No escuro, o primário é claro demais para um bloco grande: usa o container (fundo escuro, texto claro).
    val base = if (cores.escuro) cores.primaryContainer else cores.primary
    val texto = if (cores.escuro) cores.onPrimaryContainer else cores.onPrimary
    val fundo = Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.22f)))
    Box(
        Modifier
            .fillMaxWidth()
            .background(fundo, RoundedCornerShape(28.dp))
            .padding(22.dp),
    ) {
        Column(Modifier.animateContentSize()) {
            Text(
                text = if (categoria == null) "Pendente este mês" else "Pendente este mês · ${categoria.rotulo}",
                style = MaterialTheme.typography.labelLarge,
                color = texto.copy(alpha = 0.85f),
            )
            AnimatedContent(
                targetState = resumo.pendenteMes,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "pendenteMes",
            ) { valor ->
                Text(
                    formatarMoeda(valor),
                    style = MaterialTheme.typography.displaySmall,
                    color = texto,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniEstatistica("Total restante", formatarMoeda(resumo.totalRestante), texto, Modifier.weight(1f))
                MiniEstatistica(
                    rotulo = if (resumo.vencidas == 0) "Vencidas" else "Vencidas (${resumo.vencidas})",
                    valor = if (resumo.vencidas == 0) "Nenhuma" else formatarMoeda(resumo.valorVencidas),
                    corTexto = texto,
                    modifier = Modifier.weight(1f),
                    destaque = resumo.vencidas > 0,
                )
            }
        }
    }
}

@Composable
private fun DialogoSalario(atual: Long, onConfirmar: (Long) -> Unit, onFechar: () -> Unit) {
    var valor by rememberSaveable { mutableLongStateOf(atual) }
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.Payments, contentDescription = null) },
        title = { Text("Salário mensal") },
        text = {
            Column {
                Text(
                    "Usado para calcular quanto sobra depois das contas do mês. Fica salvo só no seu celular.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                CampoValor(centavos = valor, onChange = { valor = it }, rotulo = "Salário líquido", erro = false)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmar(valor)
                    onFechar()
                },
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (atual > 0) {
                    TextButton(
                        onClick = {
                            onConfirmar(0)
                            onFechar()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Remover") }
                }
                TextButton(onClick = onFechar) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun MiniEstatistica(
    rotulo: String,
    valor: String,
    corTexto: Color,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
) {
    val onPrimary = corTexto
    Column(
        modifier
            .background(onPrimary.copy(alpha = if (destaque) 0.22f else 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (destaque) {
                Icon(Icons.Rounded.WarningAmber, null, tint = onPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.8f))
        }
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium,
            color = onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraFiltros(
    filtro: Filtro,
    onStatus: (FiltroStatus) -> Unit,
    onCategoria: (Categoria?) -> Unit,
    onOrdem: (Ordem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                FiltroStatus.entries.forEachIndexed { i, status ->
                    SegmentedButton(
                        selected = filtro.status == status,
                        onClick = { onStatus(status) },
                        shape = SegmentedButtonDefaults.itemShape(i, FiltroStatus.entries.size),
                        icon = {},
                    ) {
                        Text(status.rotulo, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            var menuAberto by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuAberto = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Ordenar")
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    Text(
                        "Ordenar por",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Ordem.entries.forEach { ordem ->
                        DropdownMenuItem(
                            text = { Text(ordem.rotulo) },
                            onClick = {
                                onOrdem(ordem)
                                menuAberto = false
                            },
                            trailingIcon = {
                                if (ordem == filtro.ordem) Icon(Icons.Rounded.Check, contentDescription = "Selecionado")
                            },
                        )
                    }
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filtro.categoria == null,
                    onClick = { onCategoria(null) },
                    label = { Text("Todas") },
                    shape = CircleShape,
                )
            }
            items(Categoria.entries) { categoria ->
                FilterChip(
                    selected = filtro.categoria == categoria,
                    onClick = { onCategoria(if (filtro.categoria == categoria) null else categoria) },
                    label = { Text(categoria.rotulo) },
                    leadingIcon = { Icon(categoria.icone, null, Modifier.size(FilterChipDefaults.IconSize)) },
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun EstadoVazio(filtro: Filtro, modifier: Modifier = Modifier) {
    val (titulo, texto) = when (filtro.status) {
        FiltroStatus.PENDENTES -> "Tudo em dia" to "Nenhuma dívida pendente por aqui. Toque em “Nova dívida” para cadastrar."
        FiltroStatus.PAGAS -> "Nenhuma dívida paga ainda" to "Quando você marcar uma dívida como paga, ela aparece aqui."
        FiltroStatus.TODAS -> "Sua lista está vazia" to "Toque em “Nova dívida” para começar a organizar suas contas."
    }
    AnimatedVisibility(visible = true, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 24.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
