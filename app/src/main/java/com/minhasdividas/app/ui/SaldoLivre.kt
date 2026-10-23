package com.minhasdividas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.SaldoInformado
import com.minhasdividas.app.data.SaldoLivre
import com.minhasdividas.app.data.formatarDataCurta
import com.minhasdividas.app.data.formatarHora
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.nomeDoMes
import com.minhasdividas.app.ui.theme.sucesso
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

private fun momento(millis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

/** "hoje às 14:00", "ontem às 09:12", "em 20/10". */
private fun quando(millis: Long, hoje: LocalDate): String {
    val m = momento(millis)
    return when (m.toLocalDate()) {
        hoje -> "hoje às ${formatarHora(m)}"
        hoje.minusDays(1) -> "ontem às ${formatarHora(m)}"
        else -> "em ${formatarDataCurta(m.toLocalDate())}"
    }
}

private fun sinal(valor: Long): String = if (valor < 0) "− ${formatarMoeda(abs(valor))}" else formatarMoeda(valor)

/**
 * Linha abaixo do salário: quanto fica livre até o fim do mês, a partir do saldo informado.
 * Sem saldo (ou com saldo de um mês anterior), convida a informar.
 */
@Composable
internal fun LinhaSaldoLivre(
    saldo: SaldoInformado?,
    livre: SaldoLivre?,
    hoje: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val fim = formatarDataCurta(hoje.withDayOfMonth(hoje.lengthOfMonth()))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = cores.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(cores.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = cores.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            when {
                saldo == null -> Column(Modifier.weight(1f)) {
                    Text("Informar saldo da conta", style = MaterialTheme.typography.titleSmall, color = cores.primary)
                    Text(
                        "Para ver quanto fica livre até o fim do mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
                livre == null -> Column(Modifier.weight(1f)) {
                    Text("Atualizar saldo da conta", style = MaterialTheme.typography.titleSmall, color = cores.primary)
                    Text(
                        "O último é de ${nomeDoMes(YearMonth.from(momento(saldo.emMillis))).lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
                else -> {
                    Column(Modifier.weight(1f)) {
                        Text("Livre até $fim", style = MaterialTheme.typography.labelMedium, color = cores.onSurfaceVariant)
                        Text(
                            sinal(livre.livre),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (livre.livre < 0) cores.error else cores.sucesso,
                        )
                    }
                    Text(
                        "saldo de ${quando(saldo.emMillis, hoje)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** De onde vem o "Livre até o fim do mês", linha por linha. */
@Composable
internal fun DialogoSaldoLivre(
    saldo: SaldoInformado,
    livre: SaldoLivre,
    hoje: LocalDate,
    onAtualizar: () -> Unit,
    onFechar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val fim = formatarDataCurta(hoje.withDayOfMonth(hoje.lengthOfMonth()))
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
        title = { Text("Livre até $fim") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinhaConta("Saldo informado ${quando(saldo.emMillis, hoje)}", formatarMoeda(livre.saldoInformado))
                if (livre.pagoDepois > 0) LinhaConta("− Pago no app depois disso", formatarMoeda(livre.pagoDepois))
                if (livre.recebidoDepois != 0L) LinhaConta("+ Recebido no app depois disso", sinal(livre.recebidoDepois))
                if (livre.aReceber > 0) LinhaConta("+ A receber até $fim", formatarMoeda(livre.aReceber))
                if (livre.salario > 0) LinhaConta("+ Salário (ainda vai cair)", formatarMoeda(livre.salario))
                LinhaConta("− A pagar até $fim (com vencidas)", formatarMoeda(livre.aPagar))
                HorizontalDivider(color = cores.outlineVariant)
                LinhaConta(
                    "= Livre até $fim",
                    sinal(livre.livre),
                    destaque = if (livre.livre < 0) cores.error else cores.sucesso,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Conta só o que está no app. Um gasto feito fora dele (Pix, débito) só entra quando você " +
                        "registrar, por exemplo pelo Gasto rápido, ou informar o saldo de novo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
        dismissButton = {
            TextButton(onClick = {
                onFechar()
                onAtualizar()
            }) { Text("Atualizar saldo") }
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

/** Informar o saldo atual da conta (ou contas) de onde saem os pagamentos. */
@Composable
internal fun DialogoSaldo(
    atual: SaldoInformado?,
    temSalario: Boolean,
    onSalvar: (centavos: Long, salarioPendente: Boolean) -> Unit,
    onRemover: () -> Unit,
    onFechar: () -> Unit,
) {
    var valor by remember { mutableLongStateOf(0L) }
    var salarioPendente by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
        title = { Text("Saldo da conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Quanto tem agora na conta de onde saem os pagamentos. Se usar mais de uma (ex.: PicPay e " +
                        "Nubank), some as duas. Fica salvo só no seu celular.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CampoValor(centavos = valor, onChange = { valor = it }, rotulo = "Saldo agora", erro = false)
                if (temSalario) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = salarioPendente, role = Role.Checkbox, onValueChange = { salarioPendente = it }),
                    ) {
                        Checkbox(checked = salarioPendente, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text("O salário deste mês ainda vai cair", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSalvar(valor, salarioPendente)
                onFechar()
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (atual != null) {
                    TextButton(
                        onClick = {
                            onRemover()
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
