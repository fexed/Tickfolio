package com.fexed.checklist.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material3.Text as WearText
import com.fexed.checklist.wear.sync.WearChecklistSnapshot
import com.fexed.checklist.wear.sync.WearItemSnapshot
import com.fexed.checklist.wear.sync.WearChecklistStore
import com.fexed.checklist.wear.sync.WearSyncCoordinator
import com.fexed.checklist.wear.ui.theme.TickfolioWearTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = (application as WearApplication).checklistStore
        setContent {
            TickfolioWearTheme {
                TickfolioWearApp(store)
            }
        }
    }
}

@Composable
private fun TickfolioWearApp(store: WearChecklistStore) {
    val checklists by store.checklists.collectAsState()
    var selectedSyncId by remember { mutableStateOf<String?>(null) }
    val selected = checklists.firstOrNull { it.syncId == selectedSyncId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selected != null) {
        selectedSyncId = null
    }

    LaunchedEffect(context, store) {
        store.load()
        try {
            WearSyncCoordinator.refreshSnapshots(context)
        } catch (error: Exception) {
            Log.e("TickfolioWearSync", "Unable to load existing Wear Data Layer snapshots", error)
        }
        WearSyncCoordinator.flushPending(context)
    }

    WearScreenSurface {
        if (selected == null) {
            ChecklistList(
                checklists = checklists,
                onSelect = { selectedSyncId = it.syncId }
            )
        } else {
            ChecklistItems(
                checklist = selected,
                onBack = { selectedSyncId = null },
                onItemClick = { itemSyncId ->
                    scope.launch {
                        val update = store.incrementProgress(itemSyncId)
                        if (update != null) {
                            WearSyncCoordinator.flushPending(context)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ChecklistList(
    checklists: List<WearChecklistSnapshot>,
    onSelect: (WearChecklistSnapshot) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            WearText(
                text = stringResource(R.string.checklists_title),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                color = WearMaterialTheme.colorScheme.onBackground
            )
        }
        if (checklists.isEmpty()) {
            item {
                WearText(
                    text = stringResource(R.string.empty_checklists),
                    modifier = Modifier.padding(vertical = 12.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = WearMaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            items(checklists, key = { it.syncId }) { checklist ->
                androidx.wear.compose.material3.Button(
                    onClick = { onSelect(checklist) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        WearText(
                            text = checklist.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        val completed = checklist.items.sumOf {
                            it.completedChecks.coerceIn(0, it.requiredChecks)
                        }
                        val required = checklist.items.sumOf { it.requiredChecks }
                        WearText(stringResource(R.string.checks_progress, completed, required))
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistItems(
    checklist: WearChecklistSnapshot,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val itemsByPosition = checklist.items.sortedWith(compareBy({ it.position }, { it.title }))
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
    ) {
        androidx.wear.compose.material3.Button(
            onClick = onBack,
            label = {
                WearText(
                    text = stringResource(R.string.back),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        )
        WearText(
            text = checklist.title,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = WearMaterialTheme.colorScheme.onBackground
        )
        if (itemsByPosition.isEmpty()) {
            WearText(
                text = stringResource(R.string.empty_items),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = WearMaterialTheme.colorScheme.onBackground
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(itemsByPosition, key = { it.syncId }) { item ->
                    val completed = item.completedChecks.coerceIn(0, item.requiredChecks)
                    androidx.wear.compose.material3.Button(
                        onClick = { onItemClick(item.syncId) },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            WearText(
                                text = item.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            val status = stringResource(
                                if (completed >= item.requiredChecks) {
                                    R.string.done
                                } else {
                                    R.string.tap_to_check
                                }
                            )
                            val progress = stringResource(
                                R.string.checks_progress,
                                completed,
                                item.requiredChecks
                            )
                            WearText(
                                text = listOfNotNull(
                                    progress,
                                    item.tag.takeIf { it.isNotBlank() },
                                    status
                                ).joinToString(" · "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WearScreenSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WearMaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

private val previewChecklists = listOf(
    WearChecklistSnapshot(
        syncId = "weekend-trip",
        title = "Weekend trip",
        items = listOf(
            WearItemSnapshot("clothes", "Pack clothes", "Essentials", requiredChecks = 1, completedChecks = 1),
            WearItemSnapshot("charger", "Phone charger", "Electronics", position = 1),
            WearItemSnapshot("toiletries", "Toiletries", position = 2)
        )
    ),
    WearChecklistSnapshot(
        syncId = "car-maintenance",
        title = "Car maintenance",
        items = listOf(
            WearItemSnapshot("oil", "Check oil level", requiredChecks = 2, completedChecks = 1),
            WearItemSnapshot("tires", "Inspect tires", position = 1)
        )
    )
)

@Preview(name = "Checklist list", device = "id:wearos_large_round")
@Composable
private fun ChecklistListPreview() {
    TickfolioWearTheme {
        WearScreenSurface {
            ChecklistList(checklists = previewChecklists, onSelect = {})
        }
    }
}

@Preview(name = "Checklist list - empty", device = "id:wearos_large_round")
@Composable
private fun EmptyChecklistListPreview() {
    TickfolioWearTheme {
        WearScreenSurface {
            ChecklistList(checklists = emptyList(), onSelect = {})
        }
    }
}

@Preview(name = "Checklist items", device = "id:wearos_large_round")
@Composable
private fun ChecklistItemsPreview() {
    TickfolioWearTheme {
        WearScreenSurface {
            ChecklistItems(
                checklist = previewChecklists.first(),
                onBack = {},
                onItemClick = {}
            )
        }
    }
}

@Preview(name = "Checklist items - empty", device = "id:wearos_large_round")
@Composable
private fun EmptyChecklistItemsPreview() {
    TickfolioWearTheme {
        WearScreenSurface {
            ChecklistItems(
                checklist = WearChecklistSnapshot(syncId = "empty", title = "Empty checklist"),
                onBack = {},
                onItemClick = {}
            )
        }
    }
}
