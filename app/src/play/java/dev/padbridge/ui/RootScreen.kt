package dev.padbridge.ui

import androidx.compose.runtime.Composable

/**
 * Play Store builds don't include root mode: no `su`, no system-property changes.
 * Settings never opens this (BuildConfig.ROOT_FEATURES is false); it exists so shared code compiles.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun RootScreen(onBack: () -> Unit) = Unit
