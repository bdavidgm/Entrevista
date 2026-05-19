package com.bdavidgm.entrevista.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bdavidgm.entrevista.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface MainDestination {
    data object Saved : MainDestination
    data object QuestionList : MainDestination
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    currentDestination: MainDestination,
    canContinue: Boolean,
    scope: CoroutineScope,
    onContinueClick: () -> Unit,
    onDestinationSelected: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            AppDrawerContent(
                currentDestination = currentDestination,
                canContinue = canContinue,
                drawerState = drawerState,
                scope = scope,
                onContinueClick = onContinueClick,
                onDestinationSelected = onDestinationSelected
            )
        },
        content = content
    )
}

@Composable
private fun AppDrawerContent(
    currentDestination: MainDestination,
    canContinue: Boolean,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onContinueClick: () -> Unit,
    onDestinationSelected: (MainDestination) -> Unit
) {
    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
        NavigationDrawerItem(
            icon = {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            },
            label = {
                Text(
                    text = stringResource(R.string.continue_menu),
                    color = if (canContinue) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            },
            selected = false,
            onClick = {
                if (canContinue) {
                    scope.launch { drawerState.close() }
                    onContinueClick()
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            label = { Text(stringResource(R.string.saved_menu)) },
            selected = currentDestination is MainDestination.Saved,
            onClick = {
                scope.launch { drawerState.close() }
                onDestinationSelected(MainDestination.Saved)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            label = { Text(stringResource(R.string.all_questions)) },
            selected = currentDestination is MainDestination.QuestionList,
            onClick = {
                scope.launch { drawerState.close() }
                onDestinationSelected(MainDestination.QuestionList)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
