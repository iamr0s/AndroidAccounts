package com.rosan.accounts

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.accounts.data.common.utils.contentCopy
import com.rosan.accounts.data.common.utils.toast
import com.rosan.accounts.ui.theme.AccountsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    private val context = this

    private var jobOrNull: Job? = null

    private val refreshState = mutableStateOf(true)

    private val accountTypesState = mutableStateOf<Array<AccountType>>(emptyArray())

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
        getAccountTypes(refreshState, accountTypesState)
        Toast.makeText(this,"",0).show()
    }

    @OptIn(
        ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class,
        ExperimentalFoundationApi::class
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
                            IconButton(onClick = {
                                val json = JSONArray()
                                accountTypesState.value.map { it.packageName }.distinct().forEach {
                                    json.put(it)
                                }
                                context.contentCopy(json.toString())
                                context.toast("copied, import it in Hail!")
                            }) {
                                Icon(
                                    imageVector = Icons.TwoTone.ContentCopy,
                                    contentDescription = null
                                )
                            }
                        })
                    }) {
                        Box(
                            modifier = Modifier
                                .padding(it)
                                .pullRefresh(pullRefreshState)
                        ) {
                            val accountTypes by accountTypesState

                            LazyVerticalStaggeredGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = if (accountTypes.isEmpty()) StaggeredGridCells.Fixed(1) else StaggeredGridCells.Adaptive(
                                    300.dp
                                ),
                                contentPadding = PaddingValues(16.dp),
                                verticalItemSpacing = 16.dp,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (accountTypes.isEmpty()) item {
                                    EmptyItemWidget()
                                } else items(accountTypes) {
                                    ItemWidget(it)
                                }
                            }
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

    @Composable
    private fun EmptyItemWidget() {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "No Application For Account",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("InlinedApi")
    @Composable
    private fun ItemWidget(accountType: AccountType) {
        ElevatedCard {
            Row(modifier = Modifier
                .clickable { }
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
                        Text(
                            modifier = Modifier.basicMarquee(), text = text, style = style
                        )
                    }
                    MyText(text = accountType.label, style = MaterialTheme.typography.titleMedium)
                    MyText("package: ${accountType.packageName}")
                    MyText("type: ${accountType.type}")
                }
            }
        }
    }

    private fun getAccountTypes(
        refreshState: MutableState<Boolean>, accountTypesState: MutableState<Array<AccountType>>
    ) {
        jobOrNull?.cancel()
        jobOrNull = lifecycleScope.launch(Dispatchers.IO) {
            val result = kotlin.runCatching {
                UserService(this@MainActivity).getAccountTypes()
            }
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    accountTypesState.value = it
                }.onFailure {
                    it.printStackTrace()
                    Toast.makeText(
                        this@MainActivity, "$it ${it.localizedMessage}", Toast.LENGTH_SHORT
                    ).show()
                }
                refreshState.value = false
            }
        }
    }
}