package com.minhasdividas.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.LocaleBR
import com.minhasdividas.app.data.ResumoMes
import com.minhasdividas.app.data.formatarDataCurta
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.ui.theme.sucesso
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.abs

/** "novembro" */
internal fun nomeCurtoDoMes(hoje: LocalDate): String =
    hoje.month.getDisplayName(TextStyle.FULL_STANDALONE, LocaleBR).lowercase()

/**
 * Topo da tela: o que entra no mês (tocar edita o salário) e quanto sobra dele (tocar mostra a conta).
 * Recomeça todo dia 1º, só com o salário do mês.
 */
@Composable
internal fun Cabecalho(
    resumo: ResumoMes,
    hoje: LocalDate,
    onEditarSalario: () -> Unit,
    onVerConta: () -> Unit,
    onAjustes: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val mes = nomeCurtoDoMes(hoje)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (resumo.renda <= 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Informar salário", onClick = onEditarSalario)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = cores.primary, modifier = Modifier.size(18.dp))
                    Text("Informar salário", style = MaterialTheme.typography.titleSmall, color = cores.primary)
                }
                return@Row
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Editar salário", onClick = onEditarSalario)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
            ) {
                Text(
                    if (resumo.recebido > 0) "Renda de $mes" else "Salário",
                    style = MaterialTheme.typography.labelMedium,
                    color = cores.onSurfaceVariant,
                )
                Text(formatarMoeda(resumo.renda), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Ver a conta do mês", onClick = onVerConta)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
            ) {
                Text(
                    if (resumo.sobra < 0) "Faltam em $mes" else "Sobra de $mes",
                    style = MaterialTheme.typography.labelMedium,
                    color = cores.onSurfaceVariant,
                )
                AnimatedContent(
                    targetState = resumo.sobra,
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
        FilledTonalIconButton(onClick = onAjustes) {
            Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
        }
    }
}

/** A conta do mês, linha por linha. */
@Composable
internal fun DialogoResumoMes(resumo: ResumoMes, hoje: LocalDate, onEditarSalario: () -> Unit, onFechar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val mes = nomeCurtoDoMes(hoje)
    val fim = formatarDataCurta(hoje.withDayOfMonth(hoje.lengthOfMonth()))
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
        title = { Text("A conta de $mes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinhaConta("Salário", formatarMoeda(resumo.salario))
                if (resumo.recebido > 0) LinhaConta("+ Recebido em $mes", formatarMoeda(resumo.recebido))
                LinhaConta("− Pago em $mes", formatarMoeda(resumo.pago))
                LinhaConta("− A pagar até $fim", formatarMoeda(resumo.aPagar))
                HorizontalDivider(color = cores.outlineVariant)
                val sobra = resumo.sobra
                LinhaConta(
                    if (sobra < 0) "= Faltam em $mes" else "= Sobra de $mes",
                    formatarMoeda(abs(sobra)),
                    destaque = if (sobra < 0) cores.error else cores.sucesso,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "“Pago” inclui os gastos rápidos. “A pagar” inclui contas vencidas de meses anteriores, " +
                        "porque elas ainda vão sair do salário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
                if (resumo.aReceber > 0) {
                    Text(
                        "Há ${formatarMoeda(resumo.aReceber)} a receber até $fim: entra na conta quando você " +
                            "marcar como recebido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
                Text(
                    "Todo dia 1º a conta recomeça só com o salário do mês: o que sobrou ou faltou no mês " +
                        "anterior não entra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
        dismissButton = {
            TextButton(onClick = {
                onFechar()
                onEditarSalario()
            }) { Text("Editar salário") }
        },
    )
}

@Composable
private fun LinhaConta(rotulo: String, valor: String, destaque: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = if (destaque != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (destaque != null) FontWeight.SemiBold else null,
            modifier = Modifier.weight(1f),
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            color = destaque ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = if (destaque != null) FontWeight.SemiBold else null,
        )
    }
}
