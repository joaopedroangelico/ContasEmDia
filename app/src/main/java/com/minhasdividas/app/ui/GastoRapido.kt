package com.minhasdividas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Categoria
import com.minhasdividas.app.data.FormaPagamento
import com.minhasdividas.app.data.formatarData
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val MILIS_POR_DIA = 86_400_000L

/**
 * Gasto que já saiu da conta (Pix, mercado, app): valor e descrição bastam. Nasce paga, direto no
 * histórico do mês. [formaSugerida] é a forma do último pagamento registrado.
 *
 * Na categoria Cartão de crédito o dinheiro ainda não saiu: vira uma compra em aberto na fatura do
 * cartão (vencimento da fatura em aberto dele, [vencimentoDaFatura]), e só fica paga no "Pagar fatura".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolhaGastoRapido(
    formaSugerida: FormaPagamento?,
    cartoes: List<String>,
    vencimentoDaFatura: (cartao: String) -> LocalDate?,
    onFechar: () -> Unit,
    onSalvar: (descricao: String, valorCentavos: Long, data: LocalDate, categoria: Categoria, forma: FormaPagamento?) -> Unit,
    onSalvarNoCartao: (descricao: String, valorCentavos: Long, cartao: String, vencimento: LocalDate) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()

    var descricao by rememberSaveable { mutableStateOf("") }
    var valor by rememberSaveable { mutableLongStateOf(0L) }
    var data by rememberSaveable { mutableLongStateOf(hoje.toEpochDay()) }
    var categoria by rememberSaveable { mutableStateOf(Categoria.OUTROS) }
    var forma by rememberSaveable { mutableStateOf(formaSugerida) }
    var cartao by rememberSaveable { mutableStateOf(cartoes.firstOrNull() ?: "") }
    // Vencimento da fatura: segue o cartão escolhido até o usuário mudar a data à mão.
    var vencimentoManual by rememberSaveable { mutableStateOf<Long?>(null) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var calendarioAberto by remember { mutableStateOf(false) }
    var calendarioFatura by remember { mutableStateOf(false) }

    val noCartao = categoria == Categoria.CARTAO
    val vencimento = vencimentoManual?.let(LocalDate::ofEpochDay)
        ?: vencimentoDaFatura(cartao.trim())
        ?: hoje.plusMonths(1)

    val erroDescricao = descricao.isBlank()
    val erroValor = valor <= 0

    fun salvar() {
        tentouSalvar = true
        if (erroDescricao || erroValor) return
        escopo.launch { sheetState.hide() }.invokeOnCompletion {
            if (noCartao) {
                onSalvarNoCartao(descricao.trim(), valor, cartao.trim(), vencimento)
            } else {
                onSalvar(descricao.trim(), valor, LocalDate.ofEpochDay(data), categoria, forma)
            }
            onFechar()
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Gasto rápido", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (noCartao) {
                        "Compra no cartão: entra na fatura e fica paga junto com ela, no “Pagar fatura”."
                    } else {
                        "Para o que já foi pago: entra direto no histórico do mês."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CampoValor(centavos = valor, onChange = { valor = it }, rotulo = "Valor", erro = tentouSalvar && erroValor)

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it.take(60) },
                label = { Text("Descrição") },
                placeholder = { Text("Ex.: Mercado, gasolina, iFood") },
                singleLine = true,
                isError = tentouSalvar && erroDescricao,
                supportingText = if (tentouSalvar && erroDescricao) {
                    { Text("Informe uma descrição") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
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
                            onClick = { categoria = c },
                            label = { Text(c.rotulo) },
                            leadingIcon = { Icon(c.icone, null, Modifier.size(FilterChipDefaults.IconSize)) },
                            shape = CircleShape,
                        )
                    }
                }
            }

            if (noCartao) {
                OutlinedTextField(
                    value = cartao,
                    onValueChange = { cartao = it.take(40) },
                    label = { Text("Cartão") },
                    placeholder = { Text("Ex.: Nubank, Inter, Itaú") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (cartoes.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cartoes.forEach { nome ->
                            FilterChip(
                                selected = cartao.trim().equals(nome, ignoreCase = true),
                                onClick = {
                                    cartao = nome
                                    vencimentoManual = null
                                },
                                label = { Text(nome) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = CircleShape,
                            )
                        }
                    }
                }
                CampoData(rotulo = "Vencimento da fatura", data = vencimento, onClick = { calendarioFatura = true })
            } else {
                CampoData(rotulo = "Data do pagamento", data = LocalDate.ofEpochDay(data), onClick = { calendarioAberto = true })

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
                    SeletorForma(forma) { forma = it }
                }
            }

            Button(
                onClick = ::salvar,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(if (noCartao) "Adicionar à fatura" else "Registrar gasto", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (calendarioFatura) {
        SeletorDataPassada(
            inicial = vencimento,
            ateHoje = false,
            onEscolher = { vencimentoManual = it.toEpochDay() },
            onFechar = { calendarioFatura = false },
        )
    }
    if (calendarioAberto) {
        SeletorDataPassada(
            inicial = LocalDate.ofEpochDay(data),
            onEscolher = { data = it.toEpochDay() },
            onFechar = { calendarioAberto = false },
        )
    }
}

/** Calendário; com [ateHoje], só deixa escolher até hoje (datas de pagamento não podem estar no futuro). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeletorDataPassada(
    inicial: LocalDate,
    onEscolher: (LocalDate) -> Unit,
    onFechar: () -> Unit,
    ateHoje: Boolean = true,
) {
    val hoje = LocalDate.now().toEpochDay()
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = inicial.toEpochDay() * MILIS_POR_DIA,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !ateHoje || Math.floorDiv(utcTimeMillis, MILIS_POR_DIA) <= hoje
        },
    )
    DatePickerDialog(
        onDismissRequest = onFechar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { onEscolher(LocalDate.ofEpochDay(Math.floorDiv(it, MILIS_POR_DIA))) }
                onFechar()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    ) {
        // O título padrão em português ("23 de out. de 2026") não cabe e corta o ano.
        DatePicker(
            state = estado,
            headline = {
                Text(
                    estado.selectedDateMillis?.let { formatarData(LocalDate.ofEpochDay(Math.floorDiv(it, MILIS_POR_DIA))) } ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                )
            },
        )
    }
}
