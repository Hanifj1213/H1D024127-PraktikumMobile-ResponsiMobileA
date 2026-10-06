package com.hajun.layar.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hajun.layar.R

@Composable
internal fun ResultsControls(
    state: HomeUiState,
    onGenreSelected: (String?) -> Unit,
    onRetryPage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.genre_filter_label, state.selectedGenre ?: stringResource(R.string.all_genres)),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(90f),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp).widthIn(min = 220.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.all_genres)) },
                    onClick = { expanded = false; onGenreSelected(null) },
                )
                state.availableGenres.forEach { genre ->
                    DropdownMenuItem(
                        text = { Text(genre) },
                        onClick = { expanded = false; onGenreSelected(genre) },
                    )
                }
            }
        }
        Text(
            text = stringResource(if (state.submittedQuery.isBlank()) R.string.filter_scope_browse else R.string.filter_scope_search),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.loaded_show_count, state.loadedCount, state.filteredCount),
            style = MaterialTheme.typography.bodySmall,
        )
        if (state.shows.isNotEmpty()) {
            val first = state.pageIndex * HomeViewModel.PAGE_SIZE + 1
            Text(
                text = stringResource(R.string.visible_show_range, first, first + state.shows.size - 1, state.filteredCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.isPageLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.loading_page), style = MaterialTheme.typography.bodySmall)
        }
        state.pageError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onRetryPage, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.retry_action))
            }
        }
        state.pageNotice?.let { notice ->
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (state.submittedQuery.isBlank() && state.hasNextPage && state.pageIndex == state.pageCount - 1) {
            Text(
                text = stringResource(R.string.next_batch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PageNavigation(state: HomeUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val pageDescription = stringResource(
        if (state.submittedQuery.isBlank()) R.string.pagination_loaded else R.string.pagination_search,
        state.pageIndex + 1,
        state.pageCount,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = state.hasPreviousPage && !state.isPageLoading,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.previous_page),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 4.dp).weight(1f, fill = false),
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Text(
                text = stringResource(R.string.pagination_compact, state.pageIndex + 1, state.pageCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    .clearAndSetSemantics {
                        contentDescription = pageDescription
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
        TextButton(
            onClick = onNext,
            enabled = state.hasNextPage && !state.isPageLoading,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.next_page),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(end = 4.dp).weight(1f, fill = false),
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
