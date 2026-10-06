package com.hajun.layar.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.hajun.layar.R
import com.hajun.layar.model.Show
import com.hajun.layar.ui.components.LayarAttribution
import com.hajun.layar.ui.components.LayarTopBar
import com.hajun.layar.ui.components.LoadingPanel
import com.hajun.layar.ui.components.RatingBadge
import com.hajun.layar.ui.components.SectionEyebrow
import com.hajun.layar.ui.components.ShowGenres
import com.hajun.layar.ui.components.ShowPoster
import com.hajun.layar.ui.components.StatePanel

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val show = uiState.show
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
        topBar = { LayarTopBar(onBack = onBack) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                when {
                    uiState.isLoading -> item(key = "loading", contentType = "state") {
                        LoadingPanel(description = stringResource(R.string.loading_detail_description))
                    }
                    uiState.error != null -> item(key = "error", contentType = "state") {
                        StatePanel(
                            title = stringResource(R.string.detail_error_title),
                            description = uiState.error.ifBlank { stringResource(R.string.generic_error) },
                            actionLabel = stringResource(R.string.retry_action),
                            onAction = onRetry,
                            isError = true,
                        )
                    }
                    show != null -> {
                        item(key = "identity-${show.id}", contentType = "identity") {
                            DetailIdentity(show = show)
                        }
                        item(key = "summary-${show.id}", contentType = "summary") {
                            SummarySection(summary = show.summary)
                        }
                        item(key = "metadata-${show.id}", contentType = "metadata") {
                            MetadataSection(show = show)
                        }
                    }
                    else -> item(key = "unavailable", contentType = "state") {
                        StatePanel(
                            title = stringResource(R.string.detail_error_title),
                            description = stringResource(R.string.generic_error),
                            actionLabel = stringResource(R.string.retry_action),
                            onAction = onRetry,
                            isError = true,
                        )
                    }
                }
                item(key = "attribution", contentType = "footer") {
                    LayarAttribution()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailIdentity(show: Show) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionEyebrow(text = stringResource(R.string.detail_eyebrow))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                ShowPoster(
                    show = show,
                    modifier = Modifier.widthIn(max = 196.dp).fillMaxWidth().aspectRatio(210f / 295f),
                    large = true,
                )
            }
        }
        Text(
            text = stringResource(R.string.detail_art_caption),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = show.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.title_unavailable),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = show.year?.takeIf { it.isNotBlank() } ?: stringResource(R.string.year_unavailable),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RatingBadge(show.rating)
        }
        ShowGenres(genres = show.genres)
    }
}

@Composable
private fun SummarySection(summary: String?) {
    val plainSummary = remember(summary) {
        summary?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace('\u00a0', ' ')
                .trim()
        }.orEmpty()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.detail_summary_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Text(
            text = plainSummary.ifBlank { stringResource(R.string.summary_unavailable) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (plainSummary.isNotBlank()) {
            Text(
                text = stringResource(R.string.summary_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetadataSection(show: Show) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.detail_metadata_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        MetadataRow(label = stringResource(R.string.metadata_year), value = show.year)
        MetadataRow(label = stringResource(R.string.metadata_language), value = show.language)
        MetadataRow(label = stringResource(R.string.metadata_status), value = localizedStatus(show.status))
        MetadataRow(label = stringResource(R.string.metadata_type), value = localizedType(show.type))
        MetadataRow(
            label = stringResource(R.string.metadata_runtime),
            value = show.runtime?.takeIf { it > 0 }?.let { stringResource(R.string.runtime_minutes, it) },
        )
        MetadataRow(label = stringResource(R.string.metadata_network), value = show.network)
    }
}

@Composable
private fun MetadataRow(label: String, value: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.metadata_unavailable),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun localizedStatus(status: String?): String? = when (status) {
    "Running" -> stringResource(R.string.status_running)
    "Ended" -> stringResource(R.string.status_ended)
    "To Be Determined" -> stringResource(R.string.status_undetermined)
    else -> status
}

@Composable
private fun localizedType(type: String?): String? = when (type) {
    "Scripted" -> stringResource(R.string.type_scripted)
    "Reality" -> stringResource(R.string.type_reality)
    "Documentary" -> stringResource(R.string.type_documentary)
    "Animation" -> stringResource(R.string.type_animation)
    "Talk Show" -> stringResource(R.string.type_talk_show)
    "Game Show" -> stringResource(R.string.type_game_show)
    "Sports" -> stringResource(R.string.type_sports)
    "News" -> stringResource(R.string.type_news)
    "Panel Show" -> stringResource(R.string.type_panel_show)
    "Variety" -> stringResource(R.string.type_variety)
    "Award Show" -> stringResource(R.string.type_award_show)
    else -> type
}
