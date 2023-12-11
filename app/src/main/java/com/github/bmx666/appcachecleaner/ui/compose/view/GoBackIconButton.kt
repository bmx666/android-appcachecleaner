package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.const.Constant

internal fun goBack(navController: NavHostController) {
    navController.popBackStack()
    navController.navigate(Constant.Navigation.HOME.name) {
        popUpTo(navController.graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

@Composable
internal fun GoBackIconButton(
    navController: NavHostController,
    onClick: () -> Unit = { goBack(navController) }
) {
    androidx.compose.material3.IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            contentDescription = "Back"
        )
    }
}