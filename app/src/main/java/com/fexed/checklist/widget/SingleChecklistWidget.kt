package com.fexed.checklist.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fexed.checklist.MainActivity
import com.fexed.checklist.R

private val ItemIdParameter = ActionParameters.Key<Long>("checklist_item_id")
private val CompletedParameter = ActionParameters.Key<Boolean>("is_completed")

class SingleChecklistWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val checklistId = ChecklistWidgetPreferences.getSelectedChecklist(context, appWidgetId)
        val checklist = checklistId?.let { loadChecklist(context, it) }
        val colors = widgetColors(context)

        provideContent {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(12.dp)
            ) {
                if (checklist == null) {
                    item {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(
                                    actionStartActivity<SingleChecklistWidgetConfigActivity>(
                                        parameters = actionParametersOf(
                                            ActionParameters.Key<Int>(SingleChecklistWidgetConfigActivity.EXTRA_APPWIDGET_ID) to appWidgetId
                                        )
                                    )
                                )
                                .padding(4.dp)
                        ) {
                            Text(
                                text = context.getString(R.string.widget_loading_checklist),
                                style = TextStyle(
                                    color = colors.accent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                text = context.getString(R.string.widget_choose_checklist_hint),
                                style = TextStyle(color = colors.mutedInk, fontSize = 13.sp)
                            )
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(
                                    actionStartActivity<MainActivity>(
                                        parameters = checklistOpenParameters(checklist.checklist.id)
                                    )
                                )
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = checklist.checklist.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = colors.ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val progress = context.getString(
                                R.string.widget_completed_format,
                                checklist.completedCount,
                                checklist.totalCount
                            )
                            Text(
                                text = progress,
                                style = TextStyle(color = colors.mutedInk, fontSize = 12.sp)
                            )
                        }
                    }
                    if (checklist.items.isEmpty()) {
                        item {
                            Text(
                                text = context.getString(R.string.widget_no_items),
                                style = TextStyle(color = colors.mutedInk, fontSize = 13.sp)
                            )
                        }
                    } else {
                        items(checklist.items) { item ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .clickable(
                                        actionRunCallback<ToggleChecklistWidgetItemAction>(
                                            parameters = actionParametersOf(
                                                ItemIdParameter to item.id,
                                                CompletedParameter to !item.isCompleted
                                            )
                                        )
                                    )
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.isCompleted) "\u2713" else "\u25cb",
                                    style = TextStyle(
                                        color = if (item.isCompleted) colors.completed else colors.accent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = if (item.isCompleted) colors.mutedInk else colors.ink,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class SingleChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleChecklistWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        ChecklistWidgetPreferences.clearSelectedChecklists(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class ToggleChecklistWidgetItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val itemId = parameters[ItemIdParameter] ?: return
        val completed = parameters[CompletedParameter] ?: return
        toggleChecklistItem(context, itemId, completed)
    }
}
