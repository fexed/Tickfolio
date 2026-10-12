package com.fexed.checklist.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.R
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme
import kotlinx.coroutines.launch

class SingleChecklistWidgetConfigActivity : ComponentActivity() {
    companion object {
        const val EXTRA_APPWIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val launchedByWidgetHost =
            intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        val app = application as ChecklistApplication
        setContent {
            val checklists by app.repository.allChecklists.collectAsState(initial = emptyList())
            val userPreferences by app.repository.userPreferences.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            ChecklistTheme(themeMode = userPreferences?.themeMode ?: ThemeMode.SYSTEM) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChecklistWidgetPicker(
                        checklists = checklists,
                        onSelect = { checklist ->
                            ChecklistWidgetPreferences.setSelectedChecklist(
                                this,
                                appWidgetId,
                                checklist.checklist.id
                            )
                            if (launchedByWidgetHost) {
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                )
                                finish()
                            } else {
                                scope.launch {
                                    val glanceId = GlanceAppWidgetManager(
                                        this@SingleChecklistWidgetConfigActivity
                                    ).getGlanceIdBy(appWidgetId)
                                    SingleChecklistWidget().update(
                                        this@SingleChecklistWidgetConfigActivity,
                                        glanceId
                                    )
                                    finish()
                                }
                            }
                        },
                        onOpenApp = {
                            startActivity(Intent(this, com.fexed.checklist.MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistWidgetPicker(
    checklists: List<ChecklistWithItems>,
    onSelect: (ChecklistWithItems) -> Unit,
    onOpenApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.widget_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (checklists.isEmpty()) {
            Text(
                text = stringResource(R.string.widget_picker_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenApp)
                    .padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(checklists, key = { it.checklist.id }) { checklist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(checklist) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = checklist.checklist.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (checklist.checklist.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = checklist.checklist.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Checklist widget picker", showBackground = true)
@Composable
private fun ChecklistWidgetPickerPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChecklistWidgetPicker(
                checklists = listOf(
                    ChecklistWithItems(
                        checklist = Checklist(
                            id = 1,
                            title = "Work trip",
                            description = "Packing and preparation",
                            iconName = "trip"
                        ),
                        items = emptyList()
                    ),
                    ChecklistWithItems(
                        checklist = Checklist(
                            id = 2,
                            title = "Weekly car maintenance",
                            description = "Quick checks before the commute"
                        ),
                        items = emptyList()
                    )
                ),
                onSelect = {},
                onOpenApp = {}
            )
        }
    }
}

@Preview(name = "Checklist widget picker - empty", showBackground = true)
@Composable
private fun ChecklistWidgetPickerEmptyPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChecklistWidgetPicker(
                checklists = emptyList(),
                onSelect = {},
                onOpenApp = {}
            )
        }
    }
}
