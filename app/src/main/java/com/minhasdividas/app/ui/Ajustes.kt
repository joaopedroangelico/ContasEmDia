package com.minhasdividas.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.minhasdividas.app.BuildConfig
import com.minhasdividas.app.R
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.PaddingValues
import com.minhasdividas.app.ui.theme.sucesso
import com.minhasdividas.app.data.Preferencias
import com.minhasdividas.app.data.formatarHorario
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import com.minhasdividas.app.data.Tema
import com.minhasdividas.app.lembretes.Lembretes
import com.minhasdividas.app.ui.theme.semente

private val OPCOES_DIAS = listOf(0, 1, 3, 5, 7)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolhaAjustes(
    preferencias: Preferencias,
    onFechar: () -> Unit,
    onTema: (Tema) -> Unit,
    onLembretes: (Boolean) -> Unit,
    onDias: (Int) -> Unit,
    onHorario: (Int) -> Unit,
) {
    val context = LocalContext.current
    var escolhendoHorario by remember { mutableStateOf(false) }
    var temPermissao by remember { mutableStateOf(Lembretes.temPermissao(context)) }
    var nomeDoSom by remember { mutableStateOf(Lembretes.nomeDoSom(context)) }
    // Revalida ao voltar das configurações do sistema.
    LifecycleResumeEffect(Unit) {
        temPermissao = Lembretes.temPermissao(context)
        nomeDoSom = Lembretes.nomeDoSom(context)
        onPauseOrDispose {}
    }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        temPermissao = it
    }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Todos os temas acompanham o modo claro/escuro do celular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Tema.entries.chunked(3).forEach { linha ->
                Row(Modifier.fillMaxWidth()) {
                    linha.forEach { tema ->
                        AmostraTema(
                            tema = tema,
                            selecionado = preferencias.tema == tema,
                            onClick = { onTema(tema) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lembretes de vencimento", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notificação diária às ${formatarHorario(preferencias.minutosAviso)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preferencias.lembretesAtivos,
                    onCheckedChange = { ativo ->
                        onLembretes(ativo)
                        if (ativo && !temPermissao && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }

            AnimatedVisibility(visible = preferencias.lembretesAtivos) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Horário do aviso", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { escolhendoHorario = true }) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(formatarHorario(preferencias.minutosAviso))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Som da notificação", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                nomeDoSom,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { Lembretes.abrirConfiguracaoDoSom(context) }) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Escolher")
                        }
                    }
                    Text("Avisar a partir de", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OPCOES_DIAS.forEach { dias ->
                            FilterChip(
                                selected = preferencias.diasAntecedencia == dias,
                                onClick = { onDias(dias) },
                                label = {
                                    Text(
                                        when (dias) {
                                            0 -> "No dia"
                                            1 -> "1 dia"
                                            else -> "$dias dias"
                                        },
                                    )
                                },
                                shape = CircleShape,
                            )
                        }
                    }
                    Text(
                        "Dívidas vencidas continuam sendo lembradas até serem pagas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!temPermissao) {
                        AvisoPermissao(onAbrirConfiguracoes = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        })
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SecaoPrivacidade()

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SecaoSobre()
        }
    }

    if (escolhendoHorario) {
        DialogoHorario(
            minutos = preferencias.minutosAviso,
            onConfirmar = onHorario,
            onFechar = { escolhendoHorario = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoHorario(minutos: Int, onConfirmar: (Int) -> Unit, onFechar: () -> Unit) {
    val estado = rememberTimePickerState(initialHour = minutos / 60, initialMinute = minutos % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Horário do aviso") },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = {
                onConfirmar(estado.hour * 60 + estado.minute)
                onFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

/**
 * Resumo da política de privacidade (texto completo em PRIVACIDADE.md no repositório).
 * Cada frase aqui precisa continuar verdadeira: o app não tem permissão de internet e
 * os dados ficam fora do backup na nuvem (res/xml/regras_*.xml).
 */
@Composable
private fun SecaoPrivacidade() {
    val cores = MaterialTheme.colorScheme
    var completa by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = cores.sucesso, modifier = Modifier.size(20.dp))
            Text("Privacidade", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(cores.sucesso.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            ItemPrivacidade("Seus dados ficam apenas neste celular.")
            ItemPrivacidade("Nada é enviado ou compartilhado com ninguém, nem com o desenvolvedor.")
            ItemPrivacidade("O app não tem acesso à internet, anúncios ou rastreamento.")
            ItemPrivacidade("Sem cadastro, login ou coleta de dados pessoais.")
        }
        TextButton(onClick = { completa = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("Ler política de privacidade completa")
        }
    }
    if (completa) {
        AlertDialog(
            onDismissRequest = { completa = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text("Política de privacidade") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    TopicoPolitica(
                        "O que fica guardado",
                        "Somente o que você digita: dívidas, valores a receber, salário (se informado) e " +
                            "preferências. Tudo fica armazenado localmente, neste aparelho. O app não acessa nome, " +
                            "e-mail, contatos, localização, fotos ou dados bancários.",
                    )
                    TopicoPolitica(
                        "Nada sai do celular",
                        "O app não tem permissão de acesso à internet, então o próprio Android impede o envio de " +
                            "qualquer informação. Não há anúncios, estatísticas de uso nem serviços de terceiros. " +
                            "O desenvolvedor não tem acesso aos seus dados.",
                    )
                    TopicoPolitica(
                        "Backup e troca de celular",
                        "Os dados não entram no backup automático na nuvem. No Android 12 ou superior, uma " +
                            "transferência direta entre aparelhos feita por você pode levar os dados para o novo celular.",
                    )
                    TopicoPolitica(
                        "Permissões",
                        "Notificações, para os lembretes de vencimento. As demais (iniciar com o aparelho, manter " +
                            "ativo e estado da rede) são usadas apenas pelo agendador de lembretes do Android.",
                    )
                    TopicoPolitica(
                        "Apagar seus dados",
                        "Exclua itens dentro do app, ou desinstale/limpe os dados do app nas configurações do Android. " +
                            "Como não existe cópia fora do aparelho, dados apagados não podem ser recuperados.",
                    )
                    TopicoPolitica(
                        "LGPD",
                        "Como o app não coleta nem transmite dados pessoais, não há tratamento de dados pelo " +
                            "desenvolvedor (Lei nº 13.709/2018).",
                    )
                    Text(
                        "Última atualização: 7 de outubro de 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { completa = false }) { Text("Entendi") } },
        )
    }
}

@Composable
private fun ItemPrivacidade(texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.sucesso,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TopicoPolitica(titulo: String, texto: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Autoria e direitos do app, visíveis para quem instala o APK. */
@Composable
private fun SecaoSobre() {
    val cores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sobre", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            // O ícone do launcher é adaptativo (não suportado pelo painterResource): monta fundo + desenho.
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.icone_fundo)),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    // A área visível de um ícone adaptativo é ~2/3 do desenho: amplia para enquadrar igual.
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.5f),
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("Minhas Dívidas", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
        }
        Text(
            "© 2026 João Pedro Angélico. Todos os direitos reservados.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "Uso pessoal permitido. É proibido copiar, modificar, redistribuir ou vender este aplicativo, " +
                "no todo ou em parte, sem autorização do autor.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.onSurfaceVariant,
        )
    }
}

@Composable
private fun AmostraTema(tema: Tema, selecionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val pincel = if (tema == Tema.SISTEMA) {
        // Metade clara, metade escura: indica que segue o modo do celular.
        Brush.linearGradient(
            0f to Color(0xFFF1F1F4),
            0.5f to Color(0xFFF1F1F4),
            0.5f to Color(0xFF2A2C33),
            1f to Color(0xFF2A2C33),
        )
    } else {
        Brush.linearGradient(listOf(lerp(tema.semente, Color.White, 0.45f), tema.semente))
    }
    val borda by animateDpAsState(if (selecionado) 3.dp else 0.dp, label = "borda")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .selectable(selected = selecionado, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(56.dp)
                    .border(borda, cores.onSurface, CircleShape)
                    .padding(if (selecionado) 5.dp else 0.dp)
                    .background(pincel, CircleShape),
            )
            if (selecionado) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(cores.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = cores.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tema.rotulo,
            style = MaterialTheme.typography.labelLarge,
            color = if (selecionado) cores.onSurface else cores.onSurfaceVariant,
        )
    }
}

@Composable
private fun AvisoPermissao(onAbrirConfiguracoes: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Icon(
            Icons.Rounded.NotificationsOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(
            "Notificações bloqueadas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        TextButton(onClick = onAbrirConfiguracoes) { Text("Permitir") }
    }
}
