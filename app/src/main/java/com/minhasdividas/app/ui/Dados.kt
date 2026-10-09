package com.minhasdividas.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Armazenamento
import com.minhasdividas.app.data.BACKUP_DESDE_DATA
import com.minhasdividas.app.data.BACKUP_TUDO
import com.minhasdividas.app.data.DadosBackup
import com.minhasdividas.app.data.LeituraBackup
import com.minhasdividas.app.data.LimpezaMes
import com.minhasdividas.app.data.Preferencias
import com.minhasdividas.app.data.formatarData
import com.minhasdividas.app.data.formatarGB
import com.minhasdividas.app.data.formatarHora
import com.minhasdividas.app.data.formatarMB
import com.minhasdividas.app.data.formatarPorcentagem
import com.minhasdividas.app.data.inicioBackup
import com.minhasdividas.app.data.nomeDoMes
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

private const val MILIS_POR_DIA = 86_400_000L
private val OPCOES_MESES = listOf(3, 6, 12)
private const val SENHA_MINIMA = 4

/** Ajustes → Backup e dados: exportar, importar, apagar um mês e ver o espaço ocupado. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SecaoDados(vm: DividasViewModel, preferencias: Preferencias) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val cores = MaterialTheme.colorScheme
    val hoje = LocalDate.now()

    var armazenamento by remember { mutableStateOf<Armazenamento?>(null) }
    // Muda a cada exportação/importação/limpeza, para medir o espaço de novo.
    var versaoDados by remember { mutableIntStateOf(0) }
    LaunchedEffect(versaoDados) { armazenamento = vm.armazenamento() }

    // --- Exportar ---
    var senhaExportar by remember { mutableStateOf<String?>(null) }
    var pedindoSenhaExportar by remember { mutableStateOf(false) }
    var escolhendoData by remember { mutableStateOf(false) }
    val criarArquivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val senha = senhaExportar
        senhaExportar = null
        if (uri != null) {
            escopo.launch {
                vm.exportar(uri, senha).fold(
                    onSuccess = { bytes ->
                        avisar(context, "Backup salvo (${formatarMB(bytes.toLong())})")
                        versaoDados++
                    },
                    onFailure = { avisar(context, "Não foi possível salvar o backup") },
                )
            }
        }
    }
    fun exportar() {
        if (preferencias.backupComSenha) {
            pedindoSenhaExportar = true
        } else {
            criarArquivo.launch("ContasEmDia-backup-$hoje.json")
        }
    }

    // --- Importar ---
    var uriImportar by remember { mutableStateOf<Uri?>(null) }
    var pedindoSenhaImportar by remember { mutableStateOf(false) }
    var senhaIncorreta by remember { mutableStateOf(false) }
    var backupLido by remember { mutableStateOf<DadosBackup?>(null) }
    fun tratar(leitura: LeituraBackup) {
        when (leitura) {
            is LeituraBackup.Ok -> {
                pedindoSenhaImportar = false
                backupLido = leitura.dados
            }
            LeituraBackup.PrecisaSenha -> pedindoSenhaImportar = true
            LeituraBackup.SenhaIncorreta -> senhaIncorreta = true
            LeituraBackup.Invalido -> {
                pedindoSenhaImportar = false
                avisar(context, "Este arquivo não é um backup do Contas em Dia")
            }
        }
    }
    // Alguns gerenciadores de arquivo não reconhecem .json: aceita qualquer tipo e valida o conteúdo.
    val abrirArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            uriImportar = uri
            escopo.launch { tratar(vm.lerBackup(uri, null)) }
        }
    }

    // --- Apagar mês ---
    var mesesDisponiveis by remember { mutableStateOf<List<YearMonth>?>(null) }
    var limpeza by remember { mutableStateOf<Pair<YearMonth, LimpezaMes>?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Backup, contentDescription = null, tint = cores.primary, modifier = Modifier.size(20.dp))
            Text("Backup e dados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "Salve uma cópia das suas informações num arquivo (em Downloads, no Google Drive, onde preferir) " +
                "e restaure quando precisar, inclusive em outro celular.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.onSurfaceVariant,
        )

        Text("Período do backup", style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OPCOES_MESES.forEach { meses ->
                FilterChip(
                    selected = preferencias.mesesBackup == meses,
                    onClick = { vm.definirPeriodoBackup(meses) },
                    label = { Text("$meses meses") },
                    shape = CircleShape,
                )
            }
            FilterChip(
                selected = preferencias.mesesBackup == BACKUP_TUDO,
                onClick = { vm.definirPeriodoBackup(BACKUP_TUDO) },
                label = { Text("Tudo") },
                shape = CircleShape,
            )
            val porData = preferencias.mesesBackup == BACKUP_DESDE_DATA
            FilterChip(
                selected = porData,
                onClick = { escolhendoData = true },
                label = {
                    Text(if (porData) "Desde ${formatarData(LocalDate.ofEpochDay(preferencias.backupDesdeEpochDay))}" else "Escolher data")
                },
                shape = CircleShape,
            )
        }
        val inicio = preferencias.inicioBackup(hoje)
        Text(
            if (inicio == null) {
                "Leva todo o histórico."
            } else {
                "Leva os pagamentos e as contas quitadas desde ${formatarData(inicio)}. " +
                    "Contas e valores a receber em aberto sempre entram."
            },
            style = MaterialTheme.typography.bodySmall,
            color = cores.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Proteger com senha", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Opcional. Sem a senha, o arquivo não pode ser aberto, nem por você.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
            Switch(checked = preferencias.backupComSenha, onCheckedChange = vm::definirBackupComSenha)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = ::exportar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Exportar")
            }
            OutlinedButton(onClick = { abrirArquivo.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Importar")
            }
        }
        TextButton(
            onClick = { escopo.launch { mesesDisponiveis = vm.mesesParaApagar() } },
            colors = ButtonDefaults.textButtonColors(contentColor = cores.error),
        ) {
            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Apagar dados de um mês")
        }

        armazenamento?.let { BlocoArmazenamento(it) }
    }

    if (escolhendoData) {
        val atual = if (preferencias.mesesBackup == BACKUP_DESDE_DATA) {
            preferencias.backupDesdeEpochDay
        } else {
            (preferencias.inicioBackup(hoje) ?: hoje).toEpochDay()
        }
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = atual * MILIS_POR_DIA,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / MILIS_POR_DIA <= hoje.toEpochDay()
            },
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        vm.definirPeriodoBackup(BACKUP_DESDE_DATA, Math.floorDiv(it, MILIS_POR_DIA))
                    }
                    escolhendoData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado, title = { Text("Backup desde", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }

    if (pedindoSenhaExportar) {
        DialogoSenha(
            titulo = "Senha do backup",
            texto = "Anote a senha: ela será pedida para importar este backup e não há como recuperá-la.",
            repetir = true,
            erro = null,
            textoConfirmar = "Exportar",
            onConfirmar = { senha ->
                pedindoSenhaExportar = false
                senhaExportar = senha
                criarArquivo.launch("ContasEmDia-backup-$hoje.json")
            },
            onFechar = { pedindoSenhaExportar = false },
        )
    }

    if (pedindoSenhaImportar) {
        DialogoSenha(
            titulo = "Backup protegido",
            texto = "Digite a senha usada ao exportar este backup.",
            repetir = false,
            erro = if (senhaIncorreta) "Senha incorreta" else null,
            textoConfirmar = "Abrir",
            onConfirmar = { senha ->
                senhaIncorreta = false
                uriImportar?.let { uri -> escopo.launch { tratar(vm.lerBackup(uri, senha)) } }
            },
            onFechar = {
                pedindoSenhaImportar = false
                senhaIncorreta = false
            },
        )
    }

    backupLido?.let { dados ->
        val criado = LocalDateTime.ofInstant(Instant.ofEpochMilli(dados.criadoEmMillis), ZoneId.systemDefault())
        AlertDialog(
            onDismissRequest = { backupLido = null },
            icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
            title = { Text("Restaurar backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Criado em ${formatarData(criado.toLocalDate())} às ${formatarHora(criado)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        dados.desdeEpochDay?.let { "Histórico desde ${formatarData(LocalDate.ofEpochDay(it))}" }
                            ?: "Histórico completo",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        listOf(
                            contar(dados.dividas.size, "conta", "contas"),
                            contar(dados.pagamentos.size, "pagamento", "pagamentos"),
                            contar(dados.recebimentos.size, "valor a receber", "valores a receber"),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.onSurfaceVariant,
                    )
                    Text(
                        "Os dados atuais do app serão substituídos pelos do backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.error,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        backupLido = null
                        escopo.launch {
                            vm.importar(dados)
                            avisar(context, "Backup restaurado")
                            versaoDados++
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cores.error),
                ) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { backupLido = null }) { Text("Cancelar") } },
        )
    }

    mesesDisponiveis?.let { meses ->
        var escolhido by remember { mutableStateOf<YearMonth?>(null) }
        AlertDialog(
            onDismissRequest = { mesesDisponiveis = null },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text("Apagar dados de um mês") },
            text = {
                if (meses.isEmpty()) {
                    Text("Ainda não há pagamentos registrados para apagar.")
                } else {
                    Column(
                        Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        meses.forEach { mes ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(selected = escolhido == mes, role = Role.RadioButton, onClick = { escolhido = mes })
                                    .padding(vertical = 2.dp),
                            ) {
                                RadioButton(selected = escolhido == mes, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(nomeDoMes(mes), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = escolhido != null,
                    onClick = {
                        val mes = escolhido ?: return@TextButton
                        mesesDisponiveis = null
                        escopo.launch { limpeza = mes to vm.planejarApagarMes(mes) }
                    },
                ) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { mesesDisponiveis = null }) { Text("Cancelar") } },
        )
    }

    limpeza?.let { (mes, plano) ->
        AlertDialog(
            onDismissRequest = { limpeza = null },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = cores.error) },
            title = { Text("Apagar ${nomeDoMes(mes).replaceFirstChar { it.lowercase() }}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Será apagado:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        buildList {
                            if (plano.pagamentos.isNotEmpty()) {
                                add(contar(plano.pagamentos.size, "pagamento registrado", "pagamentos registrados"))
                            }
                            if (plano.dividas.isNotEmpty()) add(contar(plano.dividas.size, "conta quitada", "contas quitadas"))
                            if (plano.recebimentos.isNotEmpty()) {
                                add(contar(plano.recebimentos.size, "valor recebido", "valores recebidos"))
                            }
                        }.joinToString("\n") { "• $it" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Contas em aberto, parceladas e mensais fixas continuam no app. " +
                            "Sem um backup, o que for apagado não pode ser recuperado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        limpeza = null
                        escopo.launch {
                            vm.apagarMes(mes)
                            avisar(context, "${nomeDoMes(mes)} apagado")
                            versaoDados++
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cores.error),
                ) { Text("Apagar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = ::exportar) { Text("Fazer backup") }
                    TextButton(onClick = { limpeza = null }) { Text("Cancelar") }
                }
            },
        )
    }
}

/** Espaço livre do celular, quanto o app ocupa e quanto disso são as informações cadastradas. */
@Composable
private fun BlocoArmazenamento(a: Armazenamento) {
    val cores = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(cores.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Storage, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text("Armazenamento", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinhaEspaco(
                "Espaço disponível no celular",
                formatarGB(a.livreCelular),
                "${formatarPorcentagem(a.livreCelular, a.totalCelular)} livre de ${formatarGB(a.totalCelular)}",
            )
            LinearProgressIndicator(
                progress = { if (a.totalCelular > 0) 1f - a.livreCelular.toFloat() / a.totalCelular else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                drawStopIndicator = {},
            )
        }
        LinhaEspaco(
            "O app ocupa",
            formatarMB(a.app),
            "${formatarPorcentagem(a.app, a.totalCelular)} do celular",
        )
        LinhaEspaco(
            "Suas informações ocupam",
            formatarMB(a.dados),
            "${formatarPorcentagem(a.dados, a.app)} do app",
        )
    }
}

@Composable
private fun LinhaEspaco(rotulo: String, valor: String, detalhe: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(valor, style = MaterialTheme.typography.titleSmall)
            Text(detalhe, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DialogoSenha(
    titulo: String,
    texto: String,
    repetir: Boolean,
    erro: String?,
    textoConfirmar: String,
    onConfirmar: (String) -> Unit,
    onFechar: () -> Unit,
) {
    var senha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var tentou by remember { mutableStateOf(false) }
    val curta = repetir && senha.length < SENHA_MINIMA
    val diferente = repetir && senha != confirmacao
    val problema = when {
        senha.isEmpty() -> "Digite a senha"
        curta -> "Use pelo menos $SENHA_MINIMA caracteres"
        diferente -> "As senhas não são iguais"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { Icon(Icons.Rounded.Key, contentDescription = null) },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(texto, style = MaterialTheme.typography.bodyMedium)
                CampoSenha(senha, { senha = it }, "Senha", isError = erro != null || (tentou && problema != null && !diferente))
                if (repetir) {
                    CampoSenha(confirmacao, { confirmacao = it }, "Repita a senha", isError = tentou && diferente)
                }
                val mensagem = erro ?: if (tentou) problema else null
                if (mensagem != null) {
                    Text(mensagem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tentou = true
                if (problema == null) onConfirmar(senha)
            }) { Text(textoConfirmar) }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun CampoSenha(valor: String, onChange: (String) -> Unit, rotulo: String, isError: Boolean) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onChange(it.take(64)) },
        label = { Text(rotulo) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun avisar(context: Context, texto: String) = Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
