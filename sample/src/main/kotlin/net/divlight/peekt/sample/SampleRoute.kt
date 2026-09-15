package net.divlight.peekt.sample

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute : NavKey

@Serializable
data class TransactionRoute(val id: Long) : NavKey
