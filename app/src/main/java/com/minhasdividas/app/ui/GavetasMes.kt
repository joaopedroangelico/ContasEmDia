package com.minhasdividas.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.FormaPagamento
import com.minhasdividas.app.data.MesHistorico
import com.minhasdividas.app.data.PagamentoDaDivida
import com.minhasdividas.app.data.diasAte
import com.minhasdividas.app.data.emAtraso
import com.minhasdividas.app.data.formatarData
import com.minhasdividas.app.data.formatarDataCurta
import com.minhasdividas.app.data.formatarDataHoraCurta
import com.minhasdividas.app.data.formatarHora
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.nomeDoMes
import com.minhasdividas.app.data.pagoEm
import com.minhasdividas.app.data.parcelaAtual
import com.minhasdividas.app.data.parcelada
import com.minhasdividas.app.data.vencimento
import com.minhasdividas.app.ui.theme.aviso
import com.minhasdividas.app.ui.theme.sucesso
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// Gaveta de um mês (abas Pagas e Todas)
// ---------------------------------------------------------------------------

/**
 * Um mês: o que foi pago nele (com dia e hora do pagamento) e, em "Todas", as contas em aberto
 * que vencem nele. O check aparece quando nada daquele mês ficou em aberto.
 */
@Composable
internal fun GavetaMes(
    mes: MesHistorico,
    mostrarPendentes: Boolean,
    aberta: Boolean,
    hoje: LocalDate,
    ultimosPagamentos: Set<Long>,
    onAlternar: () -> Unit,
    onAbrirDivida: (Divida) -> Unit,
    onAbrirPagamento: (PagamentoDaDivida) -> Unit,
    onPagar: (Divida, FormaPagamento?, Boolean) -> Unit,
    onDesfazer: (Divida) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val vencidas = mes.pendentes.count { it.vencimento.isBefore(hoje) }
    val subtitulo = buildList {
        if (mes.pagos.isNotEmpty()) add(contar(mes.pagos.size, "paga", "pagas"))
        if (mes.pendentes.isNotEmpty()) add(contar(mes.pendentes.size, "em aberto", "em aberto"))
    }.joinToString(" · ")
    val esteMes = mes.mes == YearMonth.from(hoje)

    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = vencidas > 0,
        icone = {
            val cor = if (mes.completo) cores.sucesso else cores.primary
            Box(
                Modifier
                    .size(44.dp)
                    .background(cor.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (mes.completo) Icons.Rounded.CheckCircle else Icons.Rounded.CalendarMonth,
                    contentDescription = if (mes.completo) "Todas as contas do mês pagas" else null,
                    tint = cor,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        titulo = nomeDoMes(mes.mes),
        subtitulo = subtitulo,
        etiqueta = when {
            vencidas > 0 -> {
                { Etiqueta(if (vencidas == 1) "1 vencida" else "$vencidas vencidas", cores.error) }
            }
            mes.completo -> {
                { Etiqueta("Tudo pago", cores.sucesso) }
            }
            esteMes -> {
                { Etiqueta("Este mês", cores.onSurfaceVariant) }
            }
            else -> null
        },
        lateral = {
            Column(horizontalAlignment = Alignment.End) {
                // Mês sem nada pago ainda: o destaque vai para o que está em aberto.
                if (mes.pagos.isEmpty()) {
                    Text(
                        formatarMoeda(mes.totalPendente),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (vencidas > 0) cores.error else cores.onSurface,
                    )
                    Text("em aberto", style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
                    return@Column
                }
                Text(formatarMoeda(mes.totalPago), style = MaterialTheme.typography.titleMedium)
                Text("pago", style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
                if (mes.totalPendente > 0) {
                    Text(
                        "${formatarMoeda(mes.totalPendente)} em aberto",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (vencidas > 0) cores.error else cores.onSurfaceVariant,
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        var primeira = true
        if (mostrarPendentes) {
            mes.pendentes.forEach { divida ->
                key("d${divida.id}") {
                    if (!primeira) Divisoria()
                    primeira = false
                    LinhaDivida(
                        divida,
                        hoje,
                        onClick = { onAbrirDivida(divida) },
                        onPagar = { forma, padrao -> onPagar(divida, forma, padrao) },
                        onDesfazer = { onDesfazer(divida) },
                        modoMes = true,
                    )
                }
            }
        }
        mes.pagos.forEach { item ->
            key("p${item.pagamento.id}") {
                if (!primeira) Divisoria()
                primeira = false
                LinhaPagamento(
                    item,
                    desfazivel = item.pagamento.id in ultimosPagamentos,
                    onClick = { onAbrirPagamento(item) },
                    onDesfazer = { onDesfazer(item.divida) },
                )
            }
        }
        // Em Pagas, as contas em aberto não aparecem, mas explicam por que o mês ainda não tem o check.
        if (!mostrarPendentes && mes.pendentes.isNotEmpty()) {
            Divisoria()
            Text(
                "Ainda em aberto neste mês: ${contar(mes.pendentes.size, "conta", "contas")} " +
                    "(${formatarMoeda(mes.totalPendente)}). Veja em Pendentes.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** Pagamento registrado: vencimento e forma em cima, dia e hora do pagamento na etiqueta. */
@Composable
private fun LinhaPagamento(
    item: PagamentoDaDivida,
    desfazivel: Boolean,
    onClick: () -> Unit,
    onDesfazer: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val (p, divida) = item
    val atrasado = p.emAtraso()
    LinhaCompacta(
        titulo = divida.descricao,
        subtitulo = listOfNotNull("Vencimento ${formatarDataCurta(p.vencimento)}", p.forma?.rotulo).joinToString(" · "),
        riscado = false,
        // Pago com atraso fica em âmbar; quantos dias, no detalhe do pagamento (a etiqueta tem uma linha só).
        etiqueta = "Pago ${formatarDataHoraCurta(p.pagoEm())}",
        corEtiqueta = if (atrasado) cores.aviso else cores.sucesso,
        valor = formatarMoeda(p.valorCentavos),
        corValor = cores.onSurfaceVariant,
        detalhe = when {
            divida.recorrente -> "conta do mês"
            divida.parcelada -> "${p.parcela} de ${divida.totalParcelas}"
            else -> null
        },
        onClick = onClick,
    ) {
        if (desfazivel) {
            BotaoConfirmar(
                chave = p,
                marcado = true,
                descricaoMarcar = "",
                descricaoDesfazer = "Desfazer pagamento",
                onConfirmar = {},
                onDesfazer = onDesfazer,
            )
        } else {
            // Só o último pagamento de cada conta pode ser desfeito: os anteriores ficam só como registro.
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Pago",
                    tint = cores.sucesso.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/** Quitadas antes do histórico existir: não há data de pagamento para pôr num mês. */
@Composable
internal fun GavetaSemData(
    dividas: List<Divida>,
    aberta: Boolean,
    hoje: LocalDate,
    onAlternar: () -> Unit,
    onAbrirDivida: (Divida) -> Unit,
    onDesfazer: (Divida) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = false,
        icone = {
            Box(
                Modifier
                    .size(44.dp)
                    .background(cores.onSurfaceVariant.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        },
        titulo = "Sem data de pagamento",
        // O total vai no subtítulo: o título é longo e não cabe ao lado de um valor em telas estreitas.
        subtitulo = "${contar(dividas.size, "quitada", "quitadas")} · " +
            formatarMoeda(dividas.sumOf { it.valorCentavos * it.totalParcelas }),
        etiqueta = null,
        lateral = {},
        modifier = modifier,
    ) {
        Text(
            "Quitadas antes da versão 1.4, quando o app ainda não guardava o dia do pagamento.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
        dividas.forEach { divida ->
            key(divida.id) {
                Divisoria()
                LinhaDivida(
                    divida,
                    hoje,
                    onClick = { onAbrirDivida(divida) },
                    onPagar = { _, _ -> },
                    onDesfazer = { onDesfazer(divida) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Forma de pagamento
// ---------------------------------------------------------------------------

/** Chips das formas de pagamento; tocar na selecionada desmarca (forma não informada). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeletorForma(forma: FormaPagamento?, onEscolher: (FormaPagamento?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormaPagamento.entries.forEach { f ->
            FilterChip(
                selected = forma == f,
                onClick = { onEscolher(if (forma == f) null else f) },
                label = { Text(f.rotulo) },
                shape = CircleShape,
            )
        }
    }
}

/** Confirmação do pagamento, com a forma padrão da conta já marcada. */
@Composable
internal fun DialogoPagar(divida: Divida, onPagar: (FormaPagamento?, Boolean) -> Unit, onFechar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    var forma by remember { mutableStateOf(divida.formaPagamento) }
    // Sem forma padrão, a escolha já vira o padrão (marcado); com padrão, trocar vale só desta vez.
    var tornarPadrao by remember { mutableStateOf(divida.formaPagamento == null) }
    val mudouPadrao = forma != null && forma != divida.formaPagamento

    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.Payments, contentDescription = null) },
        title = {
            Text(
                when {
                    divida.parcelada -> "Pagar parcela ${divida.parcelaAtual}/${divida.totalParcelas}?"
                    divida.recorrente -> "Pagar a conta do mês?"
                    else -> "Marcar como paga?"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(divida.descricao, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatarMoeda(divida.valorCentavos)} · vencimento ${formatarData(divida.vencimento)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cores.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
                SeletorForma(forma) { forma = it }
                AnimatedVisibility(visible = mudouPadrao) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = tornarPadrao, role = Role.Checkbox, onValueChange = { tornarPadrao = it }),
                    ) {
                        Checkbox(checked = tornarPadrao, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Usar sempre nesta conta", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onFechar()
                onPagar(forma, mudouPadrao && tornarPadrao)
            }) { Text("Pagar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

/** Detalhes de um pagamento do histórico; a forma pode ser corrigida aqui. */
@Composable
internal fun DialogoPagamento(
    item: PagamentoDaDivida,
    onAlterarForma: (FormaPagamento?) -> Unit,
    onEditarConta: () -> Unit,
    onFechar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val (p, divida) = item
    val pagoEm = p.pagoEm()
    val dias = diasAte(pagoEm.toLocalDate(), p.vencimento)
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = cores.sucesso) },
        title = { Text(divida.descricao) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinhaDetalhe("Valor", formatarMoeda(p.valorCentavos))
                if (divida.parcelada) LinhaDetalhe("Parcela", "${p.parcela} de ${divida.totalParcelas}")
                LinhaDetalhe("Vencimento", formatarData(p.vencimento))
                LinhaDetalhe("Pago em", "${formatarData(pagoEm.toLocalDate())} às ${formatarHora(pagoEm)}")
                Text(
                    when {
                        dias > 1 -> "Pago $dias dias depois do vencimento"
                        dias == 1L -> "Pago 1 dia depois do vencimento"
                        dias == 0L -> "Pago no dia do vencimento"
                        dias == -1L -> "Pago 1 dia antes do vencimento"
                        else -> "Pago ${-dias} dias antes do vencimento"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dias > 0) cores.aviso else cores.sucesso,
                )
                Spacer(Modifier.height(4.dp))
                Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
                SeletorForma(p.forma, onAlterarForma)
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
        dismissButton = {
            TextButton(onClick = {
                onFechar()
                onEditarConta()
            }) { Text("Abrir conta") }
        },
    )
}

@Composable
private fun LinhaDetalhe(rotulo: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
