package com.minhasdividas.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Categoria
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.formatarData
import com.minhasdividas.app.data.formatarMoeda
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val MILIS_POR_DIA = 86_400_000L
private const val MAX_PARCELAS = 420

enum class TipoPagamento(val rotulo: String, val explicacao: String) {
    UNICA("À vista", "Pagamento único: ao marcar como paga, a dívida fica quitada."),
    PARCELADA("Parcelada", "Parcelas mensais com fim: a última parcela quita a dívida."),
    MENSAL(
        "Mensal fixa",
        "Sem última parcela (luz, internet, assinatura): ao pagar, o vencimento vai para o mês seguinte. " +
            "Se o valor variar, é só editar.",
    ),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolhaFormulario(
    inicial: Divida?,
    onFechar: () -> Unit,
    onSalvar: (Divida) -> Unit,
    onExcluir: (Divida) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val escopo = rememberCoroutineScope()
    fun fecharComAnimacao(depois: () -> Unit = {}) {
        escopo.launch { sheetState.hide() }.invokeOnCompletion {
            depois()
            onFechar()
        }
    }

    val jaQuitada = inicial?.paga == true
    var descricao by rememberSaveable { mutableStateOf(inicial?.descricao ?: "") }
    var valor by rememberSaveable { mutableLongStateOf(inicial?.valorCentavos ?: 0L) }
    var vencimento by rememberSaveable { mutableLongStateOf(inicial?.vencimentoEpochDay ?: LocalDate.now().toEpochDay()) }
    var categoria by rememberSaveable { mutableStateOf(inicial?.categoria ?: Categoria.CARTAO) }
    var tipo by rememberSaveable {
        mutableStateOf(
            when {
                inicial == null -> TipoPagamento.UNICA
                inicial.recorrente -> TipoPagamento.MENSAL
                inicial.totalParcelas > 1 -> TipoPagamento.PARCELADA
                else -> TipoPagamento.UNICA
            },
        )
    }
    val parcelado = tipo == TipoPagamento.PARCELADA
    var totalParcelas by rememberSaveable { mutableIntStateOf((inicial?.totalParcelas ?: 2).coerceAtLeast(2)) }
    var parcelasPagas by rememberSaveable { mutableIntStateOf(if (jaQuitada) 0 else inicial?.parcelasPagas ?: 0) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var calendarioAberto by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val erroDescricao = descricao.isBlank()
    val erroValor = valor <= 0
    val erroTotal = parcelado && totalParcelas !in 2..MAX_PARCELAS
    val erroPagas = parcelado && !jaQuitada && parcelasPagas !in 0 until totalParcelas

    fun salvar() {
        tentouSalvar = true
        if (erroDescricao || erroValor || erroTotal || erroPagas) return
        val total = if (parcelado) totalParcelas else 1
        val dataEscolhida = LocalDate.ofEpochDay(vencimento)
        // Mantém o dia original (ex.: 31) se a data não foi alterada; senão, adota o dia escolhido.
        val dia = if (inicial != null && inicial.diaVencimento > 0 && vencimento == inicial.vencimentoEpochDay) {
            inicial.diaVencimento
        } else {
            dataEscolhida.dayOfMonth
        }
        val recorrente = tipo == TipoPagamento.MENSAL
        val divida = Divida(
            id = inicial?.id ?: 0,
            descricao = descricao.trim(),
            valorCentavos = valor,
            vencimentoEpochDay = vencimento,
            categoria = categoria,
            totalParcelas = total,
            parcelasPagas = when {
                recorrente -> inicial?.parcelasPagas ?: 0
                jaQuitada -> total
                parcelado -> parcelasPagas
                else -> 0
            },
            paga = jaQuitada && !recorrente,
            recorrente = recorrente,
            diaVencimento = dia,
        )
        fecharComAnimacao { onSalvar(divida) }
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
            Text(
                if (inicial == null) "Nova dívida" else "Editar dívida",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it.take(60) },
                label = { Text("Descrição") },
                placeholder = { Text("Ex.: Fatura do cartão") },
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
                rotulo = if (parcelado) "Valor da parcela" else "Valor",
                erro = tentouSalvar && erroValor,
            )

            CampoData(
                rotulo = if (inicial != null && tipo != TipoPagamento.UNICA) "Próximo vencimento" else "Vencimento",
                data = LocalDate.ofEpochDay(vencimento),
                onClick = { calendarioAberto = true },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Categoria", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Categoria.entries.forEach { c ->
                        FilterChip(
                            selected = categoria == c,
                            onClick = {
                                categoria = c
                                // Sugestão: conta mensal nova costuma ser fixa (luz, internet, aluguel).
                                if (inicial == null && c == Categoria.CONTA_MENSAL && tipo == TipoPagamento.UNICA) {
                                    tipo = TipoPagamento.MENSAL
                                }
                            },
                            label = { Text(c.rotulo) },
                            leadingIcon = { Icon(c.icone, null, Modifier.size(FilterChipDefaults.IconSize)) },
                            shape = CircleShape,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pagamento", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TipoPagamento.entries.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            shape = SegmentedButtonDefaults.itemShape(i, TipoPagamento.entries.size),
                            icon = {},
                        ) { Text(t.rotulo, maxLines = 1) }
                    }
                }
                Text(
                    tipo.explicacao,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = parcelado) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero(
                        rotulo = "Número de parcelas",
                        valor = totalParcelas,
                        faixa = 2..MAX_PARCELAS,
                        erro = tentouSalvar && erroTotal,
                        onChange = { novo ->
                            totalParcelas = novo
                            if (novo >= 2 && parcelasPagas >= novo) parcelasPagas = novo - 1
                        },
                    )
                    if (!jaQuitada) {
                        CampoNumero(
                            rotulo = "Parcelas já pagas",
                            valor = parcelasPagas,
                            faixa = 0..(totalParcelas - 1).coerceAtLeast(0),
                            erro = tentouSalvar && erroPagas,
                            onChange = { parcelasPagas = it },
                        )
                        if (!erroTotal && !erroPagas && valor > 0) {
                            val restantes = totalParcelas - parcelasPagas
                            Text(
                                "Faltam $restantes ${if (restantes == 1) "parcela" else "parcelas"} · " +
                                    "${formatarMoeda(valor * restantes)} no total",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = ::salvar,
                shape = RoundedCornerShape(18.dp),
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
                    Text("Excluir dívida")
                }
            }
        }
    }

    if (calendarioAberto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = vencimento * MILIS_POR_DIA)
        DatePickerDialog(
            onDismissRequest = { calendarioAberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { vencimento = Math.floorDiv(it, MILIS_POR_DIA) }
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
            title = { Text("Excluir dívida?") },
            text = { Text("“${inicial.descricao}” será removida da lista.") },
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

/** Campo de moeda no estilo de app de banco: os dígitos digitados entram pela direita (1 → R$ 0,01). */
@Composable
internal fun CampoValor(centavos: Long, onChange: (Long) -> Unit, rotulo: String, erro: Boolean) {
    val texto = if (centavos == 0L) "" else formatarMoeda(centavos)
    OutlinedTextField(
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { novo ->
            val digitos = novo.text.filter(Char::isDigit).take(11)
            onChange(digitos.toLongOrNull() ?: 0L)
        },
        label = { Text(rotulo) },
        placeholder = { Text(formatarMoeda(0)) },
        singleLine = true,
        isError = erro,
        supportingText = if (erro) {
            { Text("Informe um valor maior que zero") }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun CampoData(rotulo: String, data: LocalDate, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = formatarData(data),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = "Escolher data") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        // Camada clicável por cima: o campo é só leitura e abre o calendário.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun CampoNumero(rotulo: String, valor: Int, faixa: IntRange, erro: Boolean, onChange: (Int) -> Unit) {
    var texto by remember(valor) { mutableStateOf(valor.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = if (erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = { onChange((valor - 1).coerceIn(faixa)) },
            enabled = valor > faixa.first,
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Diminuir") }
        OutlinedTextField(
            value = texto,
            onValueChange = { novo ->
                val digitos = novo.filter(Char::isDigit).take(3)
                texto = digitos
                digitos.toIntOrNull()?.let(onChange)
            },
            singleLine = true,
            isError = erro,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(76.dp),
        )
        FilledTonalIconButton(
            onClick = { onChange((valor + 1).coerceIn(faixa)) },
            enabled = valor < faixa.last,
        ) { Icon(Icons.Rounded.Add, contentDescription = "Aumentar") }
    }
}
