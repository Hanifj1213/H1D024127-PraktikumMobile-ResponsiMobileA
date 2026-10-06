package com.hajun.layar.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hajun.layar.R
import com.hajun.layar.ui.components.LayarAttribution
import com.hajun.layar.ui.components.LayarTopBar
import com.hajun.layar.ui.components.LoadingPanel
import com.hajun.layar.ui.components.SectionEyebrow
import com.hajun.layar.ui.components.ShowCard
import com.hajun.layar.ui.components.StatePanel

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onGenreSelected: (String?) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onRetryPage: () -> Unit,
    onShuffleSuggestions: () -> Unit,
    onShowClick: (Int) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val resultsScrollOffset = with(LocalDensity.current) { 28.dp.roundToPx() }
    var lastPageChange by rememberSaveable { mutableIntStateOf(uiState.pageChangeToken) }
    LaunchedEffect(uiState.pageChangeToken) {
        // Do not reset a restored scroll position when returning from Detail.
        if (lastPageChange != uiState.pageChangeToken) {
            lastPageChange = uiState.pageChangeToken
            listState.scrollToItem(2, resultsScrollOffset)
        }
    }
    val nextPage = {
        focusManager.clearFocus()
        onNextPage()
        scope.launch { listState.scrollToItem(2, resultsScrollOffset) }
        Unit
    }
    val submittedQuery = uiState.submittedQuery.trim()
    val hasSearch = submittedQuery.isNotEmpty()
    val submit = {
        focusManager.clearFocus()
        onSearch()
    }
    val resetSearch = {
        focusManager.clearFocus()
        onQueryChanged("")
        onSearch()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
        topBar = { LayarTopBar() },

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                // Let the initial header spacing scroll away with the content.
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "intro", contentType = "intro") {
                    HomeIntroduction()
                }
                item(key = "search", contentType = "search") {
                    SearchSection(
                        query = uiState.query,
                        suggestions = uiState.suggestions,
                        onShuffle = onShuffleSuggestions,
                        onQueryChanged = onQueryChanged,
                        onSearch = submit,
                        onClear = { onQueryChanged("") },
                        onQuickQuery = { query ->
                            onQueryChanged(query)
                            submit()
                        },
                    )
                }
                item(key = "results-heading", contentType = "heading") {
                    Column(
                        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionEyebrow(
                            text = stringResource(if (hasSearch) R.string.search_eyebrow else R.string.browse_eyebrow),
                        )
                        Text(
                            text = if (hasSearch) stringResource(R.string.search_results_title, submittedQuery)
                            else stringResource(R.string.browse_title),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() },
                        )

                    }
                }
                item(key = "results-controls", contentType = "controls") {
                    if (!uiState.isLoading && uiState.error == null) {
                        ResultsControls(uiState, onGenreSelected, onRetryPage)
                    }
                }
                when {
                    uiState.isLoading -> item(key = "loading", contentType = "state") {
                        LoadingPanel(
                            description = if (hasSearch) stringResource(R.string.loading_search_description, submittedQuery)
                            else stringResource(R.string.loading_description),
                        )
                    }
                    uiState.error != null -> item(key = "error", contentType = "state") {
                        StatePanel(
                            title = stringResource(R.string.home_error_title),
                            description = uiState.error.ifBlank { stringResource(R.string.generic_error) },
                            actionLabel = stringResource(R.string.retry_action),
                            onAction = onRetry,
                            isError = true,
                        )
                    }
                    uiState.shows.isEmpty() -> item(key = "empty", contentType = "state") {
                        val hasGenre = uiState.selectedGenre != null
                        StatePanel(
                            title = stringResource(if (hasGenre) R.string.genre_empty_title else R.string.empty_title),
                            description = when {
                                hasGenre -> stringResource(R.string.genre_empty_description)
                                hasSearch -> stringResource(R.string.empty_search_description, submittedQuery)
                                uiState.isCatalogComplete -> stringResource(R.string.catalog_end)
                                else -> stringResource(R.string.empty_loaded_catalog)
                            },
                            actionLabel = stringResource(when {
                                hasGenre -> R.string.reset_genre
                                hasSearch -> R.string.reset_search_action
                                uiState.hasNextPage -> R.string.next_page
                                else -> R.string.retry_action
                            }),
                            onAction = when {
                                hasGenre -> { { onGenreSelected(null) } }
                                hasSearch -> resetSearch
                                uiState.hasNextPage -> nextPage
                                else -> onRetry
                            },
                        )
                    }
                    else -> items(
                        items = uiState.shows,
                        key = { show -> show.id },
                        contentType = { "show" },
                    ) { show ->
                        ShowCard(show = show, onClick = { onShowClick(show.id) })
                    }
                }
                if (!uiState.isLoading && uiState.error == null) {
                    item(key = "pagination", contentType = "pagination") {
                        PageNavigation(uiState, onPrevious = onPreviousPage, onNext = nextPage)
                    }
                }
                item(key = "attribution", contentType = "footer") {
                    LayarAttribution(modifier = Modifier.padding(top = 18.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeIntroduction() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionEyebrow(text = stringResource(R.string.home_eyebrow))
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun SearchSection(
    query: String,
    suggestions: List<String>,
    onShuffle: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onQuickQuery: (String) -> Unit,
) {
    val suggestionsScroll = rememberScrollState()
    LaunchedEffect(suggestions) {
        suggestionsScroll.scrollTo(0)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search_label)) },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            supportingText = { Text(stringResource(R.string.search_supporting)) },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.clear_search),
                        )
                    }
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.search_action),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        ),
                    )
                    // Balance the 16.dp icon + 8.dp gap so the label itself stays centered.
                    Spacer(Modifier.width(24.dp))
                }
            }
        }
        if (query.isBlank() && suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.quick_query_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onShuffle, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.shuffle_suggestions))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(suggestionsScroll)
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    suggestions.forEach { querySuggestion ->
                        AssistChip(
                            onClick = { onQuickQuery(querySuggestion) },
                            label = {
                                Text(
                                    text = querySuggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                            ),
                            // Keep Material's compact visual height and 48.dp touch target.
                        )
                    }
                }
            }
        }
    }
}
