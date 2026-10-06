package com.hajun.layar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hajun.layar.R
import com.hajun.layar.model.Show
import com.hajun.layar.ui.theme.CinemaGold
import com.hajun.layar.ui.theme.InkNavy
import com.hajun.layar.ui.theme.NightTeal
import com.hajun.layar.ui.theme.Teal
import com.hajun.layar.ui.theme.WarmPaper
import java.util.Locale

@Composable
fun LayarTopBar(onBack: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back_action),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_layar_mark),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = stringResource(R.string.brand_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "TVmaze",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun SectionEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
fun LayarAttribution(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.attribution_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.attribution_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RatingBadge(rating: Double?, modifier: Modifier = Modifier) {
    val formatted = rating?.takeIf { it.isFinite() }?.let {
        String.format(Locale.forLanguageTag("id-ID"), "%.1f", it)
    }
    val description = if (formatted == null) {
        stringResource(R.string.rating_unavailable)
    } else {
        stringResource(R.string.rating_accessibility, formatted)
    }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (formatted != null) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = if (formatted == null) stringResource(R.string.rating_unavailable)
            else stringResource(R.string.rating_value, formatted),
            style = MaterialTheme.typography.labelMedium,
            color = if (formatted == null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowGenres(genres: List<String>, modifier: Modifier = Modifier) {
    val available = genres.filter { it.isNotBlank() }
    if (available.isEmpty()) {
        Text(
            text = stringResource(R.string.genres_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            available.forEach { genre ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowCard(show: Show, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = show.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.title_unavailable)
    val actionLabel = stringResource(R.string.open_show_details, title)
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp)
            .semantics {
                role = Role.Button
                onClick(label = actionLabel, action = null)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ShowPoster(show = show, modifier = Modifier.width(62.dp).height(87.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = show.year?.takeIf { it.isNotBlank() } ?: stringResource(R.string.year_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RatingBadge(show.rating)
                }
                Text(
                    text = show.genres.filter { it.isNotBlank() }.joinToString(" · ")
                        .ifBlank { stringResource(R.string.genres_unavailable) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun InitialsTile(show: Show, modifier: Modifier = Modifier, large: Boolean = false) {
    val initials = remember(show.title) {
        show.title.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { word ->
                String(Character.toChars(word.codePointAt(0)))
            }
            .uppercase(Locale.ROOT)
            .ifBlank { "L" }
    }
    val variant = Math.floorMod(show.id, 3)
    val start = when (variant) {
        0 -> InkNavy
        1 -> Teal
        else -> Color(0xFF614C3E)
    }
    val end = when (variant) {
        0 -> Color(0xFF385860)
        1 -> Color(0xFF183F44)
        else -> InkNavy
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(if (large) 20.dp else 10.dp)).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(brush = Brush.linearGradient(listOf(start, end)))
            val stroke = if (large) 1.3.dp.toPx() else 0.8.dp.toPx()
            val inset = size.minDimension * 0.12f
            val arm = size.minDimension * 0.18f
            val line = WarmPaper.copy(alpha = 0.35f)
            drawLine(line, Offset(inset, inset + arm), Offset(inset, inset), stroke)
            drawLine(line, Offset(inset, inset), Offset(inset + arm, inset), stroke)
            drawLine(line, Offset(size.width - inset - arm, inset), Offset(size.width - inset, inset), stroke)
            drawLine(line, Offset(size.width - inset, inset), Offset(size.width - inset, inset + arm), stroke)
            drawLine(line, Offset(inset, size.height - inset - arm), Offset(inset, size.height - inset), stroke)
            drawLine(line, Offset(inset, size.height - inset), Offset(inset + arm, size.height - inset), stroke)
            drawLine(line, Offset(size.width - inset - arm, size.height - inset), Offset(size.width - inset, size.height - inset), stroke)
            drawLine(line, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - arm), stroke)
            drawLine(
                color = NightTeal.copy(alpha = 0.12f),
                start = Offset(size.width * 0.5f, 0f),
                end = Offset(size.width, size.height * 0.72f),
                strokeWidth = size.width * 0.15f,
            )
            drawCircle(
                color = CinemaGold,
                radius = if (large) 3.dp.toPx() else 1.8.dp.toPx(),
                center = Offset(size.width - inset * 1.7f, size.height - inset * 1.7f),
            )
        }
        Text(
            text = initials,
            color = WarmPaper,
            fontFamily = FontFamily.Serif,
            fontSize = if (large) 68.sp else 26.sp,
            lineHeight = if (large) 76.sp else 32.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoadingPanel(description: String, modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.loading_accessibility)
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 36.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp).semantics { contentDescription = loadingDescription },
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.loading_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatePanel(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(if (isError) R.drawable.ic_connection else R.drawable.ic_search),
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = actionLabel)
            }
        }
    }
}
