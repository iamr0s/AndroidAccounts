package com.rosan.accounts.ui.page.account_manager

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.accounts.R
import com.rosan.accounts.data.common.utils.copy
import com.rosan.accounts.data.common.utils.toast
import org.json.JSONArray
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AccountManagerPage(
    userId: Int,
    navController: NavController,
    viewModel: AccountManagerViewModel = getViewModel {
        parametersOf(userId)
    }
) {
    SideEffect {
        viewModel.dispatch(AccountManagerViewAction.Load)
    }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.account_manager))
                },
                actions = {
                    IconButton(onClick = {
                        val array = JSONArray()
                        viewModel.state.authenticators.forEach {
                            array.put(it.auth.packageName)
                        }
                        context.copy(array.toString())
                        context.toast(R.string.copied_format_hail)
                    }) {
                        Icon(
                            imageVector = Icons.TwoTone.ContentCopy,
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) {
        AnimatedContent(
            viewModel.state.authenticators.isEmpty(),
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (it) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        stringResource(R.string.account_empty),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.state.authenticators, key = {
                        it.auth.type
                    }) {
                        var alpha by remember {
                            mutableStateOf(0f)
                        }
                        ItemWidget(
                            modifier = Modifier
                                .fillMaxWidth()
//                                .clip(RoundedCornerShape(8.dp))
                                .animateItemPlacement()
                                .graphicsLayer(
                                    alpha = animateFloatAsState(
                                        targetValue = alpha,
                                        animationSpec = spring(stiffness = 100f)
                                    ).value
                                ),
                            authenticator = it
                        )
                        SideEffect {
                            alpha = 1f
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemWidget(
    modifier: Modifier = Modifier,
    authenticator: AccountManagerViewState.Authenticator
) {
    OutlinedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterVertically),
                painter = rememberDrawablePainter(authenticator.auth.icon),
                contentDescription = null
            )
            Column {
                Text(authenticator.auth.label, style = MaterialTheme.typography.titleMedium)
                Text(authenticator.auth.type, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}