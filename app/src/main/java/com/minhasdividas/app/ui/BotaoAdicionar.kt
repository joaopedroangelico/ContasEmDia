package com.minhasdividas.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.ui.theme.sucesso

/**
 * Botão "+" expansível: ao tocar, gira para "×" e revela as opções
 * "A receber" e "Nova dívida" logo acima.
 */
@Composable
fun BotaoAdicionar(
    aberto: Boolean,
    rotuloVisivel: Boolean,
    onAlternar: () -> Unit,
    onNovaDivida: () -> Unit,
    onAReceber: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val rotacao by animateFloatAsState(if (aberto) 135f else 0f, label = "rotacao")

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AnimatedVisibility(
            visible = aberto,
            enter = fadeIn() + slideInVertically { it / 3 } + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + slideOutVertically { it / 3 } + scaleOut(targetScale = 0.85f),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OpcaoAdicionar(
                    texto = "A receber",
                    icone = Icons.AutoMirrored.Rounded.TrendingUp,
                    fundo = lerp(cores.surface, cores.sucesso, 0.2f),
                    conteudo = cores.sucesso,
                    onClick = onAReceber,
                )
                OpcaoAdicionar(
                    texto = "Nova dívida",
                    icone = Icons.AutoMirrored.Rounded.TrendingDown,
                    fundo = cores.primaryContainer,
                    conteudo = cores.onPrimaryContainer,
                    onClick = onNovaDivida,
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAlternar,
            expanded = rotuloVisivel && !aberto,
            icon = {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = if (aberto) "Fechar" else null,
                    modifier = Modifier.rotate(rotacao),
                )
            },
            text = { Text("Adicionar") },
            containerColor = cores.primary,
            contentColor = cores.onPrimary,
        )
    }
}

@Composable
private fun OpcaoAdicionar(texto: String, icone: ImageVector, fundo: Color, conteudo: Color, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icone, contentDescription = null) },
        text = { Text(texto, style = MaterialTheme.typography.titleSmall) },
        containerColor = fundo,
        contentColor = conteudo,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
    )
}
