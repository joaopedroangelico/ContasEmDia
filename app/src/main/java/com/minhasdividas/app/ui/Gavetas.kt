package com.minhasdividas.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Cartao
import com.minhasdividas.app.data.Categoria
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.FormaPagamento
import com.minhasdividas.app.data.Recebimento
import com.minhasdividas.app.data.agruparPorCartao
import com.minhasdividas.app.data.data
import com.minhasdividas.app.data.diasAte
import com.minhasdividas.app.data.formatarDataCurta
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.ConfigCartao
import com.minhasdividas.app.data.chaveCartao
import com.minhasdividas.app.data.pagaNoPeriodo
import com.minhasdividas.app.data.parcelaAtual
import com.minhasdividas.app.data.parcelada
import com.minhasdividas.app.data.recebidoNoMes
import com.minhasdividas.app.data.textoVencimento
import com.minhasdividas.app.data.vencimento
import com.minhasdividas.app.ui.theme.aviso
import com.minhasdividas.app.ui.theme.sucesso
import kotlinx.coroutines.delay
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Estrutura comum das gavetas
// ---------------------------------------------------------------------------

/** Cabeçalho clicável com resumo; ao abrir, mostra [conteudo] num painel embutido. */
@Composable
internal fun GavetaBase(
    aberta: Boolean,
    onAlternar: () -> Unit,
    alerta: Boolean,
    icone: @Composable () -> Unit,
    titulo: String,
    subtitulo: String,
    etiqueta: (@Composable () -> Unit)?,
    lateral: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val rotacao by animateFloatAsState(if (aberta) 180f else 0f, label = "seta")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cores.surfaceContainerLow,
        border = if (alerta) BorderStroke(1.dp, cores.error.copy(alpha = 0.45f)) else null,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (aberta) "Recolher" else "Expandir", onClick = onAlternar)
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            ) {
                icone()
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (etiqueta != null) {
                        Spacer(Modifier.height(6.dp))
                        etiqueta()
                    }
                }
                Spacer(Modifier.width(8.dp))
                lateral()
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
                    content = conteudo,
                )
            }
        }
    }
}

@Composable
internal fun Divisoria() {
    HorizontalDivider(
        Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun ValorLateral(
    valor: Long,
    rotulo: String,
    totalDoMes: Long,
    textoEmDia: String = "Em dia",
    corValor: Color = Color.Unspecified,
) {
    val cores = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.End) {
        if (valor > 0) {
            Text(formatarMoeda(valor), style = MaterialTheme.typography.titleMedium, color = corValor)
            Text(rotulo, style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
            if (totalDoMes > valor) {
                Text(
                    "de ${formatarMoeda(totalDoMes)} no mês",
                    style = MaterialTheme.typography.labelSmall,
                    color = cores.onSurfaceVariant,
                )
            }
        } else {
            Text(textoEmDia, style = MaterialTheme.typography.titleSmall, color = cores.sucesso)
            if (totalDoMes > 0) {
                Text(
                    "${formatarMoeda(totalDoMes)} no mês",
                    style = MaterialTheme.typography.labelSmall,
                    color = cores.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Primeira linha da gaveta aberta: soma de tudo que está pendente e o botão de ação em massa,
 * com confirmação (a ação pode ser desfeita depois pela barra inferior).
 */
@Composable
private fun LinhaEmMassa(
    rotulo: String,
    total: Long,
    detalhe: String,
    textoBotao: String,
    tituloConfirmacao: String,
    textoConfirmacao: String,
    onConfirmar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = cores.onSurfaceVariant)
            Text(formatarMoeda(total), style = MaterialTheme.typography.titleMedium)
            Text(detalhe, style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
        }
        BotaoEmMassa(textoBotao, tituloConfirmacao, textoConfirmacao, onConfirmar)
    }
}

/** Botão verde de ação em massa ("Pagar tudo", "Pagar fatura"), com confirmação. */
@Composable
private fun BotaoEmMassa(
    textoBotao: String,
    tituloConfirmacao: String,
    textoConfirmacao: String,
    onConfirmar: () -> Unit,
    compacto: Boolean = false,
) {
    val cores = MaterialTheme.colorScheme
    var confirmar by remember { mutableStateOf(false) }
    FilledTonalButton(
        onClick = { confirmar = true },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = cores.sucesso.copy(alpha = 0.16f),
            contentColor = cores.sucesso,
        ),
        contentPadding = if (compacto) PaddingValues(horizontal = 12.dp) else ButtonDefaults.ContentPadding,
    ) {
        Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(if (compacto) 16.dp else 18.dp))
        Spacer(Modifier.width(if (compacto) 4.dp else 6.dp))
        Text(textoBotao, style = if (compacto) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            icon = { Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = cores.sucesso) },
            title = { Text(tituloConfirmacao) },
            text = { Text(textoConfirmacao) },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    onConfirmar()
                }) { Text(textoBotao) }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
        )
    }
}

internal fun contar(n: Int, singular: String, plural: String) = "$n ${if (n == 1) singular else plural}"

@Composable
internal fun Etiqueta(texto: String, cor: Color) {
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

/** Linha compacta: título + etiqueta à esquerda, valor + detalhe à direita, círculo de confirmação. */
@Composable
internal fun LinhaCompacta(
    titulo: String,
    subtitulo: String?,
    riscado: Boolean,
    etiqueta: String,
    corEtiqueta: Color,
    valor: String,
    corValor: Color,
    detalhe: String?,
    onClick: () -> Unit,
    botao: @Composable () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
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
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = if (riscado) cores.onSurfaceVariant else cores.onSurface,
                textDecoration = if (riscado) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitulo.isNullOrBlank()) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Etiqueta(etiqueta, corEtiqueta)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(valor, style = MaterialTheme.typography.titleSmall, color = corValor)
            if (detalhe != null) {
                Text(detalhe, style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
            }
        }
        botao()
    }
}

/**
 * Círculo de confirmação (pagar/receber). Ao tocar num item em aberto, ele fica verde com um
 * "check" (com um leve pulo) por um instante ANTES da lista mudar, para a ação ser visível
 * mesmo quando o item sai da aba ou avança para o mês seguinte. Verde = já confirmado;
 * tocar de novo chama [onDesfazer]. Com [pedirAntes], o toque primeiro abre uma pergunta
 * (ex.: forma de pagamento) e o check só aparece quando ela confirma.
 */
@Composable
internal fun BotaoConfirmar(
    chave: Any,
    marcado: Boolean,
    descricaoMarcar: String,
    descricaoDesfazer: String,
    onConfirmar: () -> Unit,
    onDesfazer: () -> Unit = onConfirmar,
    pedirAntes: ((confirmar: () -> Unit) -> Unit)? = null,
) {
    val cores = MaterialTheme.colorScheme
    val acaoAtual by rememberUpdatedState(onConfirmar)
    var confirmando by remember(chave) { mutableStateOf(false) }
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
            marcado -> onDesfazer()
            pedirAntes != null -> pedirAntes { confirmando = true }
            else -> confirmando = true
        }
    }) {
        Crossfade(targetState = ativo, label = "check") { ok ->
            if (ok) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = descricaoDesfazer,
                    tint = cores.sucesso,
                    modifier = Modifier.scale(escala),
                )
            } else {
                Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = descricaoMarcar, tint = cores.outline)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Gaveta de dívidas (uma por categoria)
// ---------------------------------------------------------------------------

@Composable
internal fun GavetaCategoria(
    grupo: Grupo,
    aberta: Boolean,
    hoje: LocalDate,
    onAlternar: () -> Unit,
    onAbrirDivida: (Divida) -> Unit,
    onPagar: (Divida, FormaPagamento?, Boolean) -> Unit,
    onDesfazer: (Divida) -> Unit,
    onPagarTodas: (List<Divida>) -> Unit,
    modifier: Modifier = Modifier,
    /** Fechamento e vencimento dos cartões (só usados na gaveta Cartão de crédito). */
    cartoes: List<ConfigCartao> = emptyList(),
    onSalvarCartao: (nome: String, diaFechamento: Int, diaVencimento: Int) -> Unit = { _, _, _ -> },
) {
    val cores = MaterialTheme.colorScheme
    val subtitulo = buildList {
        if (grupo.pendentes > 0) add(contar(grupo.pendentes, "pendente", "pendentes"))
        if (grupo.pagasNoMes > 0) add(contar(grupo.pagasNoMes, "paga", "pagas"))
        if (grupo.quitadas > 0) add(contar(grupo.quitadas, "quitada", "quitadas"))
    }.joinToString(" · ")
    val proxima = grupo.proxima

    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = grupo.vencidas > 0,
        icone = { IconeCategoria(grupo.categoria) },
        titulo = grupo.categoria.rotulo,
        subtitulo = subtitulo,
        etiqueta = when {
            grupo.vencidas > 0 -> {
                { Etiqueta(if (grupo.vencidas == 1) "1 vencida" else "${grupo.vencidas} vencidas", cores.error) }
            }
            proxima != null -> {
                {
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
            else -> null
        },
        lateral = { ValorLateral(grupo.totalPendente, "em aberto", grupo.totalDoMes) },
        modifier = modifier,
    ) {
        // Cartão de crédito com nome do cartão informado: separa as compras por cartão, cada um com a sua fatura.
        if (grupo.categoria == Categoria.CARTAO && grupo.dividas.any { it.origem.isNotBlank() }) {
            agruparPorCartao(grupo.dividas).forEachIndexed { i, cartao ->
                if (i > 0) Spacer(Modifier.height(6.dp))
                SecaoCartao(
                    cartao,
                    cartoes.firstOrNull { it.chave == chaveCartao(cartao.nome) },
                    hoje,
                    onAbrirDivida,
                    onPagar,
                    onDesfazer,
                    onPagarTodas,
                    onSalvarCartao,
                )
            }
            return@GavetaBase
        }
        val n = grupo.emAberto.size
        if (n > 0) {
            LinhaEmMassa(
                rotulo = "Total pendente",
                total = grupo.totalPendente,
                detalhe = contar(n, "conta", "contas"),
                textoBotao = "Pagar tudo",
                tituloConfirmacao = "Pagar tudo em ${grupo.categoria.rotulo}?",
                textoConfirmacao = "${contar(n, "conta será marcada como paga", "contas serão marcadas como pagas")}, " +
                    "somando ${formatarMoeda(grupo.totalPendente)}. Parceladas e mensais fixas avançam para o " +
                    "próximo mês. Você pode desfazer logo em seguida.",
                onConfirmar = { onPagarTodas(grupo.emAberto) },
            )
            Divisoria()
        }
        grupo.dividas.forEachIndexed { i, divida ->
            key(divida.id) {
                if (i > 0) Divisoria()
                LinhaDivida(
                    divida,
                    hoje,
                    onClick = { onAbrirDivida(divida) },
                    onPagar = { forma, padrao -> onPagar(divida, forma, padrao) },
                    onDesfazer = { onDesfazer(divida) },
                )
            }
        }
    }
}

/**
 * Um cartão dentro da gaveta: nome, fechamento/vencimento, a fatura atual com "Pagar fatura" e as compras.
 * Com fechamento configurado, a fatura atual é a de vencimento mais próximo; compras feitas depois do
 * fechamento já estão na seguinte e aparecem como "Próxima fatura", fora do "Pagar fatura".
 */
@Composable
private fun SecaoCartao(
    cartao: Cartao,
    config: ConfigCartao?,
    hoje: LocalDate,
    onAbrirDivida: (Divida) -> Unit,
    onPagar: (Divida, FormaPagamento?, Boolean) -> Unit,
    onDesfazer: (Divida) -> Unit,
    onPagarTodas: (List<Divida>) -> Unit,
    onSalvarCartao: (nome: String, diaFechamento: Int, diaVencimento: Int) -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val nome = cartao.nome.ifEmpty { "Cartão não informado" }
    val emAberto = cartao.dividas.filter { !it.paga && !it.pagaNoPeriodo(hoje) }
    val vencimentoAtual = emAberto.minOfOrNull { it.vencimentoEpochDay }
    val faturaAtual = if (config != null && vencimentoAtual != null) {
        emAberto.filter { it.vencimentoEpochDay <= vencimentoAtual }
    } else {
        emAberto
    }
    val proximas = emAberto - faturaAtual.toSet()
    val fatura = faturaAtual.sumOf { it.valorCentavos }
    var configurando by remember { mutableStateOf(false) }

    // Duas linhas, para nada ficar cortado em tela estreita: nome e datas em cima; fatura e botão embaixo.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .background(cores.primary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = cores.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(nome, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (cartao.nome.isNotEmpty()) {
                    Text(
                        if (config != null) {
                            "Fecha dia ${config.diaFechamento} · vence dia ${config.diaVencimento}"
                        } else {
                            "Definir fechamento e vencimento"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (config != null) cores.onSurfaceVariant else cores.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClickLabel = "Ajustar fechamento e vencimento") { configurando = true },
                    )
                }
            }
            if (cartao.nome.isNotEmpty()) {
                IconButton(onClick = { configurando = true }) {
                    Icon(
                        Icons.Rounded.EditCalendar,
                        contentDescription = "Fechamento e vencimento do $nome",
                        tint = cores.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 30.dp)) {
            Column(Modifier.weight(1f)) {
                if (fatura > 0) {
                    Text(
                        if (config != null && vencimentoAtual != null) {
                            "Fatura de ${formatarDataCurta(LocalDate.ofEpochDay(vencimentoAtual))}"
                        } else {
                            "Fatura"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                    Text(formatarMoeda(fatura), style = MaterialTheme.typography.titleSmall)
                } else {
                    Text("Fatura paga", style = MaterialTheme.typography.labelMedium, color = cores.sucesso)
                }
            }
            if (faturaAtual.isNotEmpty()) {
                BotaoEmMassa(
                    compacto = true,
                    textoBotao = "Pagar fatura",
                    tituloConfirmacao = "Pagar a fatura do $nome?",
                    textoConfirmacao = "${contar(faturaAtual.size, "compra será marcada como paga", "compras serão marcadas como pagas")}, " +
                        "somando ${formatarMoeda(fatura)}. Parceladas ficam pagas até o vencimento e depois a próxima " +
                        "parcela volta a ficar pendente. Você pode desfazer logo em seguida.",
                    onConfirmar = { onPagarTodas(faturaAtual) },
                )
            }
        }
    }
    if (proximas.isNotEmpty()) {
        val vencimentoProxima = LocalDate.ofEpochDay(proximas.minOf { it.vencimentoEpochDay })
        Text(
            "Próxima fatura (${formatarDataCurta(vencimentoProxima)}): ${formatarMoeda(proximas.sumOf { it.valorCentavos })} " +
                "· ${contar(proximas.size, "compra", "compras")} depois do fechamento",
            style = MaterialTheme.typography.labelSmall,
            color = cores.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
    cartao.dividas.forEachIndexed { i, divida ->
        key(divida.id) {
            if (i > 0) Divisoria()
            LinhaDivida(
                divida,
                hoje,
                onClick = { onAbrirDivida(divida) },
                onPagar = { forma, padrao -> onPagar(divida, forma, padrao) },
                onDesfazer = { onDesfazer(divida) },
                mostrarOrigem = false,
            )
        }
    }
    if (configurando) {
        // Sem configuração ainda: vencimento sugerido pelo dia das compras em aberto, fechamento 7 dias antes.
        val vencimentoSugerido = config?.diaVencimento
            ?: cartao.dividas.firstOrNull { !it.paga }?.vencimento?.dayOfMonth
            ?: 10
        DialogoCartao(
            nome = nome,
            diaFechamento = config?.diaFechamento ?: ((vencimentoSugerido + 30 - 7 - 1) % 30 + 1),
            diaVencimento = vencimentoSugerido,
            onSalvar = { fechamento, vencimento -> onSalvarCartao(cartao.nome, fechamento, vencimento) },
            onFechar = { configurando = false },
        )
    }
}

/** Fechamento e vencimento da fatura: dois dias do mês, sem mais nada. */
@Composable
private fun DialogoCartao(
    nome: String,
    diaFechamento: Int,
    diaVencimento: Int,
    onSalvar: (diaFechamento: Int, diaVencimento: Int) -> Unit,
    onFechar: () -> Unit,
) {
    var fechamento by remember { mutableIntStateOf(diaFechamento) }
    var vencimento by remember { mutableIntStateOf(diaVencimento) }
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.EditCalendar, contentDescription = null) },
        title = { Text(nome) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SeletorDia("Fecha no dia", fechamento) { fechamento = it }
                SeletorDia("Vence no dia", vencimento) { vencimento = it }
                Text(
                    "Compras a partir do dia do fechamento entram na fatura do mês seguinte. As compras em aberto " +
                        "deste cartão passam a vencer no dia $vencimento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSalvar(fechamento, vencimento)
                onFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

/** Dia do mês (1 a 31) com botões − e +; passa de 31 para 1 e vice-versa. */
@Composable
private fun SeletorDia(rotulo: String, dia: Int, onMudar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onMudar(if (dia <= 1) 31 else dia - 1) }) {
            Icon(Icons.Rounded.Remove, contentDescription = "Dia anterior")
        }
        Text(
            "$dia",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp),
        )
        FilledTonalIconButton(onClick = { onMudar(if (dia >= 31) 1 else dia + 1) }) {
            Icon(Icons.Rounded.Add, contentDescription = "Próximo dia")
        }
    }
}

@Composable
internal fun LinhaDivida(
    divida: Divida,
    hoje: LocalDate,
    onClick: () -> Unit,
    onPagar: (FormaPagamento?, Boolean) -> Unit,
    onDesfazer: () -> Unit,
    mostrarOrigem: Boolean = true,
    /**
     * Na gaveta de um mês, a conta aparece pelo vencimento daquele mês: a parcela do mês anterior
     * já paga não a deixa verde (esse pagamento aparece como linha própria no outro mês).
     */
    modoMes: Boolean = false,
) {
    val cores = MaterialTheme.colorScheme
    val dias = diasAte(divida.vencimento, hoje)
    val pagaNoMes = !modoMes && divida.pagaNoPeriodo(hoje)
    // Pergunta a forma de pagamento antes do check; a escolha vale para o pagamento que vem em seguida.
    var perguntar by remember { mutableStateOf<(() -> Unit)?>(null) }
    var escolha by remember { mutableStateOf<Pair<FormaPagamento?, Boolean>>(null to false) }
    val emDia = divida.paga || pagaNoMes
    val (texto, cor) = when {
        divida.paga -> "Quitada" to cores.sucesso
        pagaNoMes -> "Pago · próxima ${formatarDataCurta(divida.vencimento)}" to cores.sucesso
        dias < 0 -> textoVencimento(divida.vencimento, hoje) to cores.error
        dias <= 3 -> textoVencimento(divida.vencimento, hoje) to cores.aviso
        dias <= 30 -> "${textoVencimento(divida.vencimento, hoje)} · ${formatarDataCurta(divida.vencimento)}" to cores.onSurfaceVariant
        else -> textoVencimento(divida.vencimento, hoje) to cores.onSurfaceVariant
    }
    LinhaCompacta(
        titulo = divida.descricao,
        subtitulo = if (mostrarOrigem) divida.origem else null,
        riscado = divida.paga,
        etiqueta = texto,
        corEtiqueta = cor,
        valor = formatarMoeda(divida.valorCentavos),
        corValor = if (emDia) cores.onSurfaceVariant else cores.onSurface,
        detalhe = when {
            divida.recorrente -> "todo mês"
            divida.parcelada && divida.paga -> "${divida.totalParcelas}x"
            divida.parcelada -> "${divida.parcelaAtual} de ${divida.totalParcelas}"
            else -> null
        },
        onClick = onClick,
    ) {
        BotaoConfirmar(
            chave = divida,
            marcado = emDia,
            descricaoMarcar = when {
                divida.parcelada -> "Pagar parcela"
                divida.recorrente -> "Pagar conta do mês"
                else -> "Marcar como paga"
            },
            descricaoDesfazer = "Desfazer pagamento",
            onConfirmar = { onPagar(escolha.first, escolha.second) },
            onDesfazer = onDesfazer,
            pedirAntes = { confirmar -> perguntar = confirmar },
        )
    }
    perguntar?.let { confirmar ->
        DialogoPagar(
            divida = divida,
            onPagar = { forma, padrao ->
                escolha = forma to padrao
                confirmar()
            },
            onFechar = { perguntar = null },
        )
    }
}

// ---------------------------------------------------------------------------
// Gaveta "A receber" (renda extra)
// ---------------------------------------------------------------------------

@Composable
internal fun GavetaReceber(
    grupo: GrupoReceber,
    aberta: Boolean,
    hoje: LocalDate,
    onAlternar: () -> Unit,
    onAbrir: (Recebimento) -> Unit,
    onReceber: (Recebimento) -> Unit,
    onReceberTodos: (List<Recebimento>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val subtitulo = buildList {
        if (grupo.aReceber > 0) add(contar(grupo.aReceber, "a receber", "a receber"))
        val recebidos = grupo.recebidosNoMes + grupo.recebidos
        if (recebidos > 0) add(contar(recebidos, "recebido", "recebidos"))
    }.joinToString(" · ")
    val proximo = grupo.proximo

    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = false,
        icone = {
            Box(
                Modifier
                    .size(44.dp)
                    .background(cores.sucesso.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = cores.sucesso, modifier = Modifier.size(22.dp))
            }
        },
        titulo = "A receber",
        subtitulo = subtitulo,
        etiqueta = proximo?.let {
            {
                val dias = diasAte(it.data, hoje)
                Etiqueta(
                    when {
                        dias < 0 -> "Atrasado desde ${formatarDataCurta(it.data)}"
                        dias == 0L -> "Próximo hoje"
                        dias == 1L -> "Próximo amanhã"
                        else -> "Próximo: ${formatarDataCurta(it.data)}"
                    },
                    if (dias < 0) cores.aviso else cores.onSurfaceVariant,
                )
            }
        },
        lateral = {
            ValorLateral(
                grupo.totalAReceber,
                "a receber",
                grupo.totalDoMes,
                textoEmDia = "Recebido",
                corValor = cores.sucesso,
            )
        },
        modifier = modifier,
    ) {
        val n = grupo.emAberto.size
        if (n > 0) {
            LinhaEmMassa(
                rotulo = "Total a receber",
                total = grupo.totalAReceber,
                detalhe = contar(n, "valor", "valores"),
                textoBotao = "Receber tudo",
                tituloConfirmacao = "Receber tudo?",
                textoConfirmacao = "${contar(n, "valor será marcado como recebido", "valores serão marcados como recebidos")}, " +
                    "somando ${formatarMoeda(grupo.totalAReceber)}. Os mensais avançam para o próximo mês. " +
                    "Você pode desfazer logo em seguida.",
                onConfirmar = { onReceberTodos(grupo.emAberto) },
            )
            Divisoria()
        }
        grupo.itens.forEachIndexed { i, r ->
            key(r.id) {
                if (i > 0) Divisoria()
                LinhaRecebimento(r, hoje, onClick = { onAbrir(r) }, onReceber = { onReceber(r) })
            }
        }
    }
}

@Composable
private fun LinhaRecebimento(r: Recebimento, hoje: LocalDate, onClick: () -> Unit, onReceber: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val dias = diasAte(r.data, hoje)
    val noMes = r.recebidoNoMes(hoje)
    val confirmado = r.recebido || noMes
    val (texto, cor) = when {
        r.recebido -> "Recebido" to cores.sucesso
        noMes -> "Recebido · próximo ${formatarDataCurta(r.data)}" to cores.sucesso
        dias < 0 -> "Atrasado desde ${formatarDataCurta(r.data)}" to cores.aviso
        dias == 0L -> "Previsto para hoje" to cores.sucesso
        dias == 1L -> "Previsto para amanhã" to cores.onSurfaceVariant
        else -> "Previsto para ${formatarDataCurta(r.data)}" to cores.onSurfaceVariant
    }
    LinhaCompacta(
        titulo = r.descricao,
        subtitulo = null,
        riscado = false,
        etiqueta = texto,
        corEtiqueta = cor,
        valor = "+ ${formatarMoeda(r.valorCentavos)}",
        corValor = if (confirmado) cores.onSurfaceVariant else cores.sucesso,
        detalhe = if (r.recorrente) "todo mês" else null,
        onClick = onClick,
    ) {
        BotaoConfirmar(
            chave = r,
            marcado = confirmado,
            descricaoMarcar = "Marcar como recebido",
            descricaoDesfazer = "Desfazer recebimento",
            onConfirmar = onReceber,
        )
    }
}
