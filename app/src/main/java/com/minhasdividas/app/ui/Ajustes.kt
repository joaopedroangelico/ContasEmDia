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
import com.minhasdividas.app.data.Preferencias
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
) {
    val context = LocalContext.current
    var temPermissao by remember { mutableStateOf(Lembretes.temPermissao(context)) }
    // Revalida ao voltar das configurações do sistema.
    LifecycleResumeEffect(Unit) {
        temPermissao = Lembretes.temPermissao(context)
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
                        "Notificação diária às ${Lembretes.HORA_DO_AVISO}h",
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
        }
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
