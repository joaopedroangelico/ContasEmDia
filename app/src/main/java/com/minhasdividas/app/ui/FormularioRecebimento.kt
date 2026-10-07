package com.minhasdividas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Recebimento
import com.minhasdividas.app.ui.theme.sucesso
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val MILIS_POR_DIA = 86_400_000L

/** Cadastro/edição de renda extra ("A receber"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaRecebimento(
    inicial: Recebimento?,
    onFechar: () -> Unit,
    onSalvar: (Recebimento) -> Unit,
    onExcluir: (Recebimento) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val escopo = rememberCoroutineScope()
    fun fecharComAnimacao(depois: () -> Unit = {}) {
        escopo.launch { sheetState.hide() }.invokeOnCompletion {
            depois()
            onFechar()
        }
    }

    var descricao by rememberSaveable { mutableStateOf(inicial?.descricao ?: "") }
    var valor by rememberSaveable { mutableLongStateOf(inicial?.valorCentavos ?: 0L) }
    var data by rememberSaveable { mutableLongStateOf(inicial?.dataEpochDay ?: LocalDate.now().toEpochDay()) }
    var recorrente by rememberSaveable { mutableStateOf(inicial?.recorrente ?: false) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var calendarioAberto by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val erroDescricao = descricao.isBlank()
    val erroValor = valor <= 0

    fun salvar() {
        tentouSalvar = true
        if (erroDescricao || erroValor) return
        // Mantém o dia original (ex.: 31) se a data não foi alterada; senão, adota o dia escolhido.
        val dia = if (inicial != null && inicial.diaDoMes > 0 && data == inicial.dataEpochDay) {
            inicial.diaDoMes
        } else {
            LocalDate.ofEpochDay(data).dayOfMonth
        }
        val recebimento = Recebimento(
            id = inicial?.id ?: 0,
            descricao = descricao.trim(),
            valorCentavos = valor,
            dataEpochDay = data,
            recorrente = recorrente,
            diaDoMes = dia,
            // Recorrente nunca fica "recebido de vez": só avança de mês.
            recebido = !recorrente && inicial?.recebido == true,
            vezesRecebido = inicial?.vezesRecebido ?: 0,
        )
        fecharComAnimacao { onSalvar(recebimento) }
    }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Column {
                Text(
                    if (inicial == null) "A receber" else "Editar recebimento",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Renda extra, fora do salário. Soma no que sobra no fim do mês.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it.take(60) },
                label = { Text("Descrição") },
                placeholder = { Text("Ex.: Freela, venda, reembolso") },
                singleLine = true,
                isError = tentouSalvar && erroDescricao,
                supportingText = if (tentouSalvar && erroDescricao) {
                    { Text("Informe uma descrição") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoValor(
                centavos = valor,
                onChange = { valor = it },
                rotulo = "Valor",
                erro = tentouSalvar && erroValor,
            )

            CampoData(
                rotulo = if (inicial != null && recorrente) "Próximo recebimento" else "Data prevista",
                data = LocalDate.ofEpochDay(data),
                onClick = { calendarioAberto = true },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Todo mês", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Renda extra fixa (ex.: aluguel recebido). Ao receber, passa para o mês seguinte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = recorrente, onCheckedChange = { recorrente = it })
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = ::salvar,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.sucesso,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Salvar", style = MaterialTheme.typography.titleMedium)
            }
            if (inicial != null) {
                TextButton(
                    onClick = { confirmarExclusao = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir recebimento")
                }
            }
        }
    }

    if (calendarioAberto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = data * MILIS_POR_DIA)
        DatePickerDialog(
            onDismissRequest = { calendarioAberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { data = Math.floorDiv(it, MILIS_POR_DIA) }
                    calendarioAberto = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { calendarioAberto = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estado)
        }
    }

    if (confirmarExclusao && inicial != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Excluir recebimento?") },
            text = { Text("“${inicial.descricao}” será removido da lista.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarExclusao = false
                        fecharComAnimacao { onExcluir(inicial) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            },
        )
    }
}
