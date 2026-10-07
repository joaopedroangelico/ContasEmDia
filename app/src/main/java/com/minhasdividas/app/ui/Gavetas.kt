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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.DoneAll
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
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.Recebimento
import com.minhasdividas.app.data.data
import com.minhasdividas.app.data.diasAte
import com.minhasdividas.app.data.formatarDataCurta
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.pagaNoMes
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
private fun GavetaBase(
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
private fun Divisoria() {
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
    var confirmar by remember { mutableStateOf(false) }
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
        FilledTonalButton(
            onClick = { confirmar = true },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = cores.sucesso.copy(alpha = 0.16f),
                contentColor = cores.sucesso,
            ),
        ) {
            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(textoBotao)
        }
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

private fun contar(n: Int, singular: String, plural: String) = "$n ${if (n == 1) singular else plural}"

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
private fun LinhaCompacta(
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
 * tocar de novo desfaz.
 */
@Composable
private fun BotaoConfirmar(
    chave: Any,
    marcado: Boolean,
    descricaoMarcar: String,
    descricaoDesfazer: String,
    onConfirmar: () -> Unit,
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
            marcado -> onConfirmar()
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
    onPagar: (Divida) -> Unit,
    onPagarTodas: (List<Divida>) -> Unit,
    modifier: Modifier = Modifier,
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
                LinhaDivida(divida, hoje, onClick = { onAbrirDivida(divida) }, onPagar = { onPagar(divida) })
            }
        }
    }
}

@Composable
private fun LinhaDivida(divida: Divida, hoje: LocalDate, onClick: () -> Unit, onPagar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val dias = diasAte(divida.vencimento, hoje)
    val pagaNoMes = divida.pagaNoMes(hoje)
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
        subtitulo = divida.origem,
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
            onConfirmar = onPagar,
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
