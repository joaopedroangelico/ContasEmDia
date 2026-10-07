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
                if (resultado == SnackbarResult.ActionPerformed) vm.restaurar(evento.anterior)
            }
        }
    }

    val fabExpandido by remember { derivedStateOf { listState.firstVisibleItemIndex < 3 } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { formularioId = NOVA },
                expanded = fabExpandido,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nova dívida") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
            if (!ui.carregando && ui.grupos.isEmpty()) {
                item(key = "vazio") { EstadoVazio(ui.filtro, Modifier.animateItem()) }
            }
            items(ui.grupos, key = { it.categoria.name }) { grupo ->
                GavetaCategoria(
                    grupo = grupo,
                    // Com uma categoria filtrada, só há uma gaveta: já vem aberta.
                    aberta = ui.filtro.categoria != null || grupo.categoria in ui.abertas,
                    hoje = ui.hoje,
                    onAlternar = { vm.alternarGaveta(grupo.categoria) },
                    onAbrirDivida = { formularioId = it.id },
                    onPagar = vm::alternarPaga,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

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
private fun Cabecalho(salario: Long, contasDoMes: Long, onEditarSalario: () -> Unit, onAjustes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Column(Modifier.weight(1f)) {
            LinhaSalario(salario, contasDoMes, onEditarSalario)
            Text("Minhas dívidas", style = MaterialTheme.typography.headlineMedium)
        }
        FilledTonalIconButton(onClick = onAjustes) {
            Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
        }
    }
}

/**
 * Salário e quanto sobra depois de todas as contas do mês (pagas e a pagar).
 * Tocar abre a edição do salário.
 */
@Composable
private fun LinhaSalario(salario: Long, contasDoMes: Long, onClick: () -> Unit) {
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
        if (salario <= 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Payments, contentDescription = null, tint = cores.primary, modifier = Modifier.size(18.dp))
                Text("Informar salário", style = MaterialTheme.typography.titleSmall, color = cores.primary)
            }
            return@Row
        }
        val sobra = salario - contasDoMes
        Column {
            Text("Salário", style = MaterialTheme.typography.labelMedium, color = cores.onSurfaceVariant)
            Text(formatarMoeda(salario), style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
                    rotulo = "Vencidas",
                    valor = if (resumo.vencidas == 0) "Nenhuma" else resumo.vencidas.toString(),
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
private fun GavetaCategoria(
    grupo: Grupo,
    aberta: Boolean,
    hoje: LocalDate,
    onAlternar: () -> Unit,
    onAbrirDivida: (Divida) -> Unit,
    onPagar: (Divida) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val rotacao by animateFloatAsState(if (aberta) 180f else 0f, label = "seta")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cores.surfaceContainerLow,
        border = if (grupo.vencidas > 0) BorderStroke(1.dp, cores.error.copy(alpha = 0.45f)) else null,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (aberta) "Recolher" else "Expandir", onClick = onAlternar)
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            ) {
                IconeCategoria(grupo.categoria)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        grupo.categoria.rotulo,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        resumoDaGaveta(grupo),
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val proxima = grupo.proxima
                    when {
                        grupo.vencidas > 0 -> {
                            Spacer(Modifier.height(6.dp))
                            Etiqueta(if (grupo.vencidas == 1) "1 vencida" else "${grupo.vencidas} vencidas", cores.error)
                        }
                        proxima != null -> {
                            Spacer(Modifier.height(6.dp))
                            val dias = diasAte(proxima.vencimento, hoje)
                            Etiqueta(
                                when (dias) {
                                    0L -> "Próxima vence hoje"
                                    1L -> "Próxima vence amanhã"
                                    else -> "Próxima: ${formatarDataCurta(proxima.vencimento)}"
                                },
                                if (dias <= 3) cores.aviso else cores.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (grupo.totalPendente > 0) {
                        Text(formatarMoeda(grupo.totalPendente), style = MaterialTheme.typography.titleMedium)
                        Text("em aberto", style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
                    } else {
                        Text("Em dia", style = MaterialTheme.typography.titleSmall, color = cores.sucesso)
                    }
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = cores.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .rotate(rotacao),
                )
            }

            AnimatedVisibility(
                visible = aberta,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        .background(cores.surface, RoundedCornerShape(18.dp))
                        .padding(vertical = 4.dp),
                ) {
                    grupo.dividas.forEachIndexed { i, divida ->
                        key(divida.id) {
                            if (i > 0) {
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 14.dp),
                                    color = cores.outlineVariant.copy(alpha = 0.6f),
                                )
                            }
                            LinhaDivida(
                                divida = divida,
                                hoje = hoje,
                                onClick = { onAbrirDivida(divida) },
                                onPagar = { onPagar(divida) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resumoDaGaveta(grupo: Grupo): String {
    fun contar(n: Int, singular: String, plural: String) = "$n ${if (n == 1) singular else plural}"
    val partes = buildList {
        if (grupo.pendentes > 0) add(contar(grupo.pendentes, "pendente", "pendentes"))
        if (grupo.pagasNoMes > 0) add(contar(grupo.pagasNoMes, "paga este mês", "pagas este mês"))
        if (grupo.quitadas > 0) add(contar(grupo.quitadas, "quitada", "quitadas"))
    }
    return partes.joinToString(" · ")
}

@Composable
private fun Etiqueta(texto: String, cor: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = cor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(cor.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Linha compacta dentro da gaveta: a categoria já está no cabeçalho, então não se repete aqui. */
@Composable
private fun LinhaDivida(divida: Divida, hoje: LocalDate, onClick: () -> Unit, onPagar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val dias = diasAte(divida.vencimento, hoje)
    val pagaNoMes = divida.pagaNoMes(hoje)
    val emDia = divida.paga || pagaNoMes

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                divida.descricao,
                style = MaterialTheme.typography.titleSmall,
                color = if (divida.paga) cores.onSurfaceVariant else cores.onSurface,
                textDecoration = if (divida.paga) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            val (texto, cor) = when {
                divida.paga -> "Quitada" to cores.sucesso
                pagaNoMes -> "Pago · próxima ${formatarDataCurta(divida.vencimento)}" to cores.sucesso
                dias < 0 -> textoVencimento(divida.vencimento, hoje) to cores.error
                dias <= 3 -> textoVencimento(divida.vencimento, hoje) to cores.aviso
                dias <= 30 -> "${textoVencimento(divida.vencimento, hoje)} · ${formatarDataCurta(divida.vencimento)}" to cores.onSurfaceVariant
                else -> textoVencimento(divida.vencimento, hoje) to cores.onSurfaceVariant
            }
            Etiqueta(texto, cor)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatarMoeda(divida.valorCentavos),
                style = MaterialTheme.typography.titleSmall,
                color = if (emDia) cores.onSurfaceVariant else cores.onSurface,
            )
            val detalhe = when {
                divida.recorrente -> "todo mês"
                divida.parcelada && divida.paga -> "${divida.totalParcelas}x"
                divida.parcelada -> "${divida.parcelaAtual} de ${divida.totalParcelas}"
                else -> null
            }
            if (detalhe != null) {
                Text(detalhe, style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
            }
        }
        BotaoPagar(divida = divida, marcado = emDia, onPagar = onPagar)
    }
}

/**
 * Círculo de pagamento. Ao tocar num item em aberto, ele fica verde com um "check"
 * (com um leve pulo) por um instante ANTES da lista mudar, para o pagamento ser visível
 * mesmo quando a dívida sai da aba ou avança para o mês seguinte.
 */
@Composable
private fun BotaoPagar(divida: Divida, marcado: Boolean, onPagar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val acaoAtual by rememberUpdatedState(onPagar)
    var confirmando by remember(divida) { mutableStateOf(false) }
    LaunchedEffect(confirmando) {
        if (confirmando) {
            delay(650)
            acaoAtual()
        }
    }
    val ativo = marcado || confirmando
    val escala by animateFloatAsState(
        targetValue = if (confirmando) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escala",
    )

    IconButton(onClick = {
        when {
            confirmando -> Unit
            marcado -> onPagar()
            else -> confirmando = true
        }
    }) {
        Crossfade(targetState = ativo, label = "check") { ok ->
            if (ok) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Desfazer pagamento",
                    tint = cores.sucesso,
                    modifier = Modifier.scale(escala),
                )
            } else {
                Icon(
                    Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = when {
                        divida.parcelada -> "Pagar parcela"
                        divida.recorrente -> "Pagar conta do mês"
                        else -> "Marcar como paga"
                    },
                    tint = cores.outline,
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
