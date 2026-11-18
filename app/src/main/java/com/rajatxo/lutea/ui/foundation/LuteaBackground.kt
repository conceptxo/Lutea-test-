package com.rajatxo.lutea.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Lutea — pure black base background.
 *
 * The intro jelly scene and text sit on top of this canvas. Keeping the base
 * black means any live effect's bottom fade blends out invisibly.
 */
@Composable
fun LuteaBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        content()
    }
}
