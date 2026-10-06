package com.hajun.layar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.hajun.layar.model.Show

@Composable
fun ShowPoster(show: Show, modifier: Modifier = Modifier, large: Boolean = false) {
    val imageUrl = if (large) {
        show.imageOriginalUrl ?: show.imageMediumUrl
    } else {
        show.imageMediumUrl ?: show.imageOriginalUrl
    }
    var isLoaded by remember(imageUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (large) 16.dp else 10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (!isLoaded) {
            InitialsTile(show = show, modifier = Modifier.matchParentSize(), large = large)
        }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                // The adjacent title already identifies the show for accessibility.
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (large) ContentScale.Fit else ContentScale.Crop,
                onState = { isLoaded = it is AsyncImagePainter.State.Success },
            )
        }
    }
}
