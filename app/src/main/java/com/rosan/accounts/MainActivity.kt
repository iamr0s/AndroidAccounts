package com.rosan.accounts

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Face
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.accounts.data.common.utils.contentCopy
import com.rosan.accounts.data.common.utils.requireShizukuPermissionGranted
import com.rosan.accounts.data.common.utils.toast
import com.rosan.accounts.ui.theme.AccountsTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    private val context = this

    private var jobOrNull: Job? = null

    private val refreshState = mutableStateOf(true)

    private val users = mutableStateMapOf<UserInfo, List<AccountType>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        refreshState.value = true
        shizukuJob {
            // Wait for the system cache be refreshed
            delay(1500)
            getUsers()
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class
    )
    private fun setContent() {
        setContent {
            AccountsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val refreshing by refreshState
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = refreshing, onRefresh = ::refresh
                    )

                    var currentIndex by rememberSaveable {
                        mutableStateOf(0)
                    }
                    val userKeys = (if (users.isNotEmpty()) users.keys else listOf(
                        UserInfo(
                            0,
                            "Default",
                            0
                        )
                    )).sortedBy { it.id }
                    if (currentIndex >= userKeys.size)
                        currentIndex = 0
                    val currentUser = userKeys[currentIndex]
                    val accountTypes = users[currentUser] ?: emptyList()

                    Scaffold(topBar = {
                        TopAppBar(navigationIcon = {
                            IconButton(onClick = { this.finish() }) {
                                Icon(
                                    imageVector = Icons.TwoTone.Close, contentDescription = null
                                )
                            }
                        }, title = {
                            Text(stringResource(id = R.string.app_name))
                        }, actions = {
                            AnimatedVisibility(currentUser.id != 0) {
                                val show = remember {
                                    mutableStateOf(false)
                                }
                                IconButton(onClick = {
                                    show.value = true
                                }) {
                                    Icon(
                                        imageVector = Icons.TwoTone.Delete,
                                        contentDescription = "delete user"
                                    )
                                }
                                if (!show.value) return@AnimatedVisibility
                                DeleteDialog(show, currentUser)
                            }
                            IconButton(onClick = {
                                val json = JSONArray()
                                accountTypes.map { it.packageName }
                                    .distinct()
                                    .forEach {
                                        json.put(it)
                                    }
                                context.contentCopy(json.toString())
                                context.toast("copied, import it in Hail!")
                            }) {
                                Icon(
                                    imageVector = Icons.TwoTone.ContentCopy,
                                    contentDescription = "copy"
                                )
                            }
                        })
                    }) {
                        Box(
                            modifier = Modifier
                                .padding(it)
                                .pullRefresh(pullRefreshState)
                        ) {
                            UsersPage(
                                currentUser = currentUser,
                                users = userKeys,
                                accountTypes = accountTypes,
                                onSelected = {
                                    currentIndex = userKeys.indexOf(it)
                                }
                            )

                            PullRefreshIndicator(
                                modifier = Modifier.align(Alignment.TopCenter),
                                refreshing = refreshing,
                                state = pullRefreshState
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    @Composable
    private fun UsersPage(
        currentUser: UserInfo,
        users: List<UserInfo>,
        accountTypes: List<AccountType>,
        onSelected: (UserInfo) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                accountTypes, modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) { accountTypes ->
                if (accountTypes.isEmpty()) Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "No Application For Account",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium
                    )
                } else LazyVerticalStaggeredGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = if (accountTypes.isEmpty()) StaggeredGridCells.Fixed(1) else StaggeredGridCells.Adaptive(
                        200.dp
                    ),
                    contentPadding = PaddingValues(16.dp),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(accountTypes, key = {
                        "${it.userId}:${it.type}"
                    }) {
                        ItemWidget(it)
                    }
                }
            }
            AnimatedContent(users) { users ->
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    users.forEach { user ->
                        NavigationBarItem(
                            selected = currentUser.id == user.id,
                            onClick = { onSelected(user) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Face,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text("${user.name} (${user.id})")
                            },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    @Composable
    private fun ItemWidget(accountType: AccountType) {
        ElevatedCard {
            var showAccounts by remember {
                mutableStateOf(false)
            }
            Row(modifier = Modifier
                .clickable {
                    showAccounts = !showAccounts
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Image(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterVertically),
                    painter = rememberDrawablePainter(accountType.icon),
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    @Composable
                    fun MyText(
                        text: String, style: TextStyle = MaterialTheme.typography.bodyMedium
                    ) {
                        Text(text, style = style)
                    }
                    MyText(accountType.label, style = MaterialTheme.typography.titleMedium)
                    MyText("userId: ${accountType.userId}")
                    MyText("package: ${accountType.packageName}")
                    MyText("type: ${accountType.type}")
                    AnimatedVisibility(visible = showAccounts && accountType.values.isNotEmpty()) {
                        MyText("accounts: ${accountType.values.joinToString()}")
                    }
                }
            }
        }
    }

    @Composable
    private fun DeleteDialog(show: MutableState<Boolean>, user: UserInfo) {
        AlertDialog(onDismissRequest = { show.value = false }, title = {
            Text("${user.name} (${user.id})")
        }, text = {
            Text(
                """Are you sure you want to delete this user space? 
All applications and data in this user space will be lost"""
            )
        }, confirmButton = {
            TextButton(onClick = {
                shizukuJob {
                    show.value = false
                    removeUser(user.id)
                    refresh()
                }
            }) {
                Text("Sure")
            }
        }, dismissButton = {
            TextButton(onClick = { show.value = false }) {
                Text("Cancel")
            }
        })
    }

    private fun shizukuJob(action: suspend () -> Unit) {
        jobOrNull?.cancel()
        jobOrNull = lifecycleScope.launch(Dispatchers.IO) {
            val result = kotlin.runCatching {
                requireShizukuPermissionGranted(context) {
                    action.invoke()
                }
            }
            withContext(Dispatchers.Main) {
                result.onFailure {
                    if (it is CancellationException) return@onFailure
                    it.printStackTrace()
                    this@MainActivity.toast("$it ${it.localizedMessage}")
                }
                refreshState.value = false
            }
        }
    }

    private suspend fun getUsers() {
        val result = UserService.getUsers(this@MainActivity)
        withContext(Dispatchers.Main) {
            users.clear()
            users.putAll(result)
            refreshState.value = false
        }
    }

    private suspend fun removeUser(userId: Int): Boolean = UserService.removeUser(userId)
}