package com.minhasdividas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minhasdividas.app.data.LocaleBR
import com.minhasdividas.app.data.MesPrevisto
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.nomeDoMes
import com.minhasdividas.app.ui.theme.aviso
import java.text.NumberFormat
import java.time.format.TextStyle as EstiloData
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

/** Chave da gaveta "Próximos meses" no conjunto de gavetas abertas. */
const val GAVETA_PREVISAO = "PREVISAO"

/**
 * Gaveta "Próximos meses": despesas previstas mês a mês, em colunas, com o salário como referência.
 * Tocar numa coluna mostra o mês e as contas que pesam nele.
 */
@Composable
internal fun GavetaPrevisao(
    meses: List<MesPrevisto>,
    salarioCentavos: Long,
    aberta: Boolean,
    onAlternar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    var escolhido by rememberSaveable { mutableIntStateOf(1) }
    val proximo = meses.getOrNull(1)

    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = false,
        icone = {
            Box(
                Modifier
                    .size(44.dp)
                    .background(cores.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = cores.primary, modifier = Modifier.size(22.dp))
            }
        },
        titulo = "Próximos meses",
        subtitulo = "Despesas previstas",
        etiqueta = null,
        lateral = {
            if (proximo != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatarMoeda(proximo.total), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "em ${nomeAbreviado(proximo)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        if (meses.all { it.total == 0L }) {
            Text(
                "Nenhuma despesa prevista. Cadastre contas para ver os próximos meses.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
            return@GavetaBase
        }
        val indice = escolhido.coerceIn(meses.indices)
        GraficoMeses(
            meses = meses,
            salarioCentavos = salarioCentavos,
            escolhido = indice,
            onEscolher = { escolhido = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(start = 8.dp, end = 12.dp, top = 12.dp),
        )
        DetalheMes(meses[indice], primeiro = indice == 0, salarioCentavos = salarioCentavos)
    }
}

/** "nov", "dez", "jan" */
private fun nomeAbreviado(m: MesPrevisto): String =
    m.mes.month.getDisplayName(EstiloData.SHORT_STANDALONE, LocaleBR).lowercase().removeSuffix(".")

/** Eixo em números redondos: "0", "500", "1 mil", "2,5 mil". */
private fun rotuloEixo(centavos: Long): String {
    val reais = centavos / 100.0
    if (reais < 1000) return NumberFormat.getIntegerInstance(LocaleBR).format(reais)
    val formato = NumberFormat.getNumberInstance(LocaleBR).apply { maximumFractionDigits = 1 }
    return "${formato.format(reais / 1000)} mil"
}

/** Passo "redondo" (1, 2, 2,5 ou 5 × 10ⁿ) para dividir o eixo em até [partes] faixas. */
private fun passoRedondo(maximo: Double, partes: Int): Double {
    val bruto = maximo / partes
    val potencia = 10.0.pow(kotlin.math.floor(log10(bruto)))
    val fator = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * potencia >= bruto }
    return fator * potencia
}

/**
 * Colunas de uma série só (o total do mês), na cor primária do tema; a escolhida em cheio e as demais
 * mais claras. A linha do salário é a referência para ver em que mês as contas passam dele.
 */
@Composable
private fun GraficoMeses(
    meses: List<MesPrevisto>,
    salarioCentavos: Long,
    escolhido: Int,
    onEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val medidor = rememberTextMeasurer()
    val estiloEixo = MaterialTheme.typography.labelSmall.copy(color = cores.onSurfaceVariant)
    val estiloMes = MaterialTheme.typography.labelSmall.copy(color = cores.onSurfaceVariant)
    val estiloMesEscolhido = MaterialTheme.typography.labelSmall.copy(color = cores.onSurface)

    val maior = maxOf(meses.maxOf { it.total }, salarioCentavos).coerceAtLeast(100L).toDouble()
    val passo = passoRedondo(maior, 4)
    val topo = ceil(maior / passo) * passo
    val ticks = (0..(topo / passo).toInt()).map { (it * passo).toLong() }
    val descricao = meses.joinToString("; ") { "${nomeDoMes(it.mes)}: ${formatarMoeda(it.total)}" }

    Canvas(
        modifier
            .semantics { contentDescription = "Despesas previstas por mês. $descricao" }
            .pointerInput(meses.size) {
                detectTapGestures { toque ->
                    val esquerda = 44.dp.toPx()
                    val largura = (size.width - esquerda) / meses.size
                    val i = ((toque.x - esquerda) / largura).toInt()
                    if (i in meses.indices) onEscolher(i)
                }
            },
    ) {
        val esquerda = 44.dp.toPx()
        val baixo = 22.dp.toPx()
        val alturaPlot = size.height - baixo
        val largura = (size.width - esquerda) / meses.size
        val barra = minOf(24.dp.toPx(), largura * 0.62f)
        val raio = 4.dp.toPx()
        fun y(valor: Long) = alturaPlot - (valor / topo * alturaPlot).toFloat()

        // Grade e eixo: linhas finas e discretas, só nos valores redondos.
        ticks.forEach { t ->
            val yy = y(t)
            drawLine(cores.outlineVariant, Offset(esquerda, yy), Offset(size.width, yy), strokeWidth = 1.dp.toPx())
            val texto = medidor.measure(rotuloEixo(t), estiloEixo)
            drawText(texto, topLeft = Offset(esquerda - texto.size.width - 6.dp.toPx(), yy - texto.size.height / 2f))
        }

        // Colunas: canto arredondado só no topo; base reta, apoiada na linha do zero.
        meses.forEachIndexed { i, m ->
            val centro = esquerda + largura * i + largura / 2
            if (m.total > 0) {
                val topoBarra = y(m.total)
                val cor = if (i == escolhido) cores.primary else cores.primary.copy(alpha = 0.4f)
                val caminho = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = centro - barra / 2,
                            top = topoBarra,
                            right = centro + barra / 2,
                            bottom = alturaPlot,
                            topLeftCornerRadius = CornerRadius(raio),
                            topRightCornerRadius = CornerRadius(raio),
                        ),
                    )
                }
                drawPath(caminho, cor)
            }
            val rotulo = medidor.measure(
                nomeAbreviado(m),
                if (i == escolhido) estiloMesEscolhido else estiloMes,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            drawText(rotulo, topLeft = Offset(centro - rotulo.size.width / 2f, alturaPlot + 4.dp.toPx()))
        }

        // Salário: linha de referência sólida, com o nome na ponta direita.
        if (salarioCentavos > 0) {
            val ys = y(salarioCentavos)
            drawLine(cores.onSurfaceVariant, Offset(esquerda, ys), Offset(size.width, ys), strokeWidth = 1.5.dp.toPx())
            val texto = medidor.measure("Salário", estiloEixo)
            val fundo = Size(texto.size.width + 8.dp.toPx(), texto.size.height.toFloat())
            val canto = Offset(size.width - fundo.width, ys - fundo.height - 2.dp.toPx())
            drawRect(cores.surface, topLeft = canto, size = fundo)
            drawText(texto, topLeft = canto + Offset(4.dp.toPx(), 0f))
        }
    }
}

/** O mês escolhido: total, aviso se passa do salário e as contas que pesam nele. */
@Composable
private fun DetalheMes(m: MesPrevisto, primeiro: Boolean, salarioCentavos: Long) {
    val cores = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nomeDoMes(m.mes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(formatarMoeda(m.total), style = MaterialTheme.typography.titleSmall)
        }
        if (primeiro && m.jaPago > 0) {
            Text(
                "${formatarMoeda(m.jaPago)} já pago, ${formatarMoeda(m.total - m.jaPago)} a pagar",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
            )
        }
        if (salarioCentavos > 0 && m.total > salarioCentavos) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = cores.aviso, modifier = Modifier.size(16.dp))
                Text(
                    "Passa do salário em ${formatarMoeda(m.total - salarioCentavos)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurface,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        val visiveis = m.contas.take(6)
        visiveis.forEach { (divida, valor) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeCategoria(divida.categoria, tamanho = 22.dp)
                Text(
                    divida.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(formatarMoeda(valor), style = MaterialTheme.typography.bodySmall, color = cores.onSurfaceVariant)
            }
        }
        if (m.contas.size > visiveis.size) {
            val resto = m.contas.drop(visiveis.size)
            Text(
                "+ ${contar(resto.size, "outra conta", "outras contas")} · ${formatarMoeda(resto.sumOf { it.second })}",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
            )
        }
        if (m.contas.isEmpty()) {
            Text("Nenhuma conta prevista.", style = MaterialTheme.typography.bodySmall, color = cores.onSurfaceVariant)
        }
    }
}
