package com.rosan.accounts.ui.page.user_manager

import android.os.Process
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.accounts.R
import com.rosan.accounts.data.common.utils.help
import com.rosan.accounts.data.common.utils.id
import com.rosan.accounts.data.service.entity.UserEntity
import com.rosan.accounts.ui.page.main.MainScreen
import org.koin.androidx.compose.getViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class, ExperimentalAnimationApi::class
)
@Composable
fun UserManagerPage(
    navController: NavController,
    viewModel: UserManagerViewModel = getViewModel()
) {
    SideEffect {
        viewModel.dispatch(UserManagerViewAction.Load)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.user_manager))
                }
            )
        },
    ) {
        AnimatedContent(
            viewModel.state.cause != null,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (it) Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    viewModel.state.cause.let {
                        it?.help() ?: it?.localizedMessage ?: it?.toString() ?: ""
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.state.users, key = {
                    it.id
                }) {
                    var alpha by remember {
                        mutableStateOf(0f)
                    }
                    ItemWidget(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .graphicsLayer(
                                alpha = animateFloatAsState(
                                    targetValue = alpha,
                                    animationSpec = spring(stiffness = 100f)
                                ).value
                            ),
                        viewModel = viewModel,
                        navController = navController,
                        user = it
                    )
                    SideEffect {
                        alpha = 1f
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemWidget(
    modifier: Modifier = Modifier,
    viewModel: UserManagerViewModel,
    navController: NavController,
    user: UserEntity
) {
    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    user.id.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    user.name ?: stringResource(R.string.user_name_default),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    navController.navigate(MainScreen.AccountManager.builder(user.id))
                }) {
                    Text(stringResource(R.string.account_manager))
                }
                val curUserId = Process.myUserHandle().id
                if (curUserId != user.id) {
                    var showing by remember {
                        mutableStateOf(false)
                    }
                    TextButton(onClick = { showing = true }) {
                        Text(stringResource(R.string.remove))
                    }
                    DeleteUserDialog(
                        viewModel = viewModel,
                        showing = showing,
                        onDismissRequest = {
                            showing = false
                        },
                        user = user
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteUserDialog(
    viewModel: UserManagerViewModel,
    showing: Boolean,
    onDismissRequest: () -> Unit,
    user: UserEntity
) {
    if (!showing) return
    AlertDialog(onDismissRequest = onDismissRequest, icon = {
        Icon(imageVector = Icons.TwoTone.Warning, contentDescription = null)
    }, title = {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                user.id.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(user.name ?: stringResource(R.string.user_name_default))
        }
    }, text = {
        Text(stringResource(R.string.delete_user_warning))
    }, confirmButton = {
        TextButton(onClick = {
            viewModel.dispatch(UserManagerViewAction.Remove(user))
            onDismissRequest()
        }) {
            Text(stringResource(R.string.delete_user_confirm))
        }
    }, dismissButton = {
        TextButton(onClick = onDismissRequest) {
            Text(stringResource(R.string.delete_user_cancel))
        }
    })
}
