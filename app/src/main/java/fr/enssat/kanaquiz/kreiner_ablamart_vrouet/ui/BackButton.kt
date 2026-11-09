package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * La flèche de retour des barres de titre.
 *
 * L'icône est *auto-mirrored* : elle se retourne d'elle-même dans une langue
 * écrite de droite à gauche.
 */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Retour",
        )
    }
}
