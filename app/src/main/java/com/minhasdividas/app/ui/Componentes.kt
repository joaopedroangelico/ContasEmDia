package com.minhasdividas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.Categoria
import com.minhasdividas.app.ui.theme.corDaCategoria

val Categoria.icone: ImageVector
    get() = when (this) {
        Categoria.CARTAO -> Icons.Rounded.CreditCard
        Categoria.CONTA_MENSAL -> Icons.Rounded.Receipt
        Categoria.EMPRESTIMO -> Icons.Rounded.AccountBalance
        Categoria.FINANCIAMENTO -> Icons.Rounded.Home
        Categoria.OUTROS -> Icons.Rounded.MoreHoriz
    }

@Composable
fun IconeCategoria(categoria: Categoria, modifier: Modifier = Modifier, tamanho: Dp = 44.dp) {
    val cor = MaterialTheme.colorScheme.corDaCategoria(categoria)
    Box(
        modifier = modifier
            .size(tamanho)
            .background(cor.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoria.icone, contentDescription = categoria.rotulo, tint = cor, modifier = Modifier.size(tamanho * 0.5f))
    }
}
