package com.fexed.checklist.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fexed.checklist.MainActivity
import com.fexed.checklist.R

class AllChecklistsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val checklists = loadAllChecklists(context)
        val colors = widgetColors(context)

        provideContent {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(12.dp)
            ) {
                item {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = TextStyle(
                            color = colors.accent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(6.dp))
                }
                if (checklists.isEmpty()) {
                    item {
                        Text(
                            text = context.getString(R.string.widget_no_checklists),
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity<MainActivity>()),
                            style = TextStyle(color = colors.mutedInk, fontSize = 13.sp)
                        )
                    }
                } else {
                    items(checklists) { checklist ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(
                                    actionStartActivity<MainActivity>(
                                        parameters = checklistOpenParameters(checklist.checklist.id)
                                    )
                                )
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                Text(
                                    text = checklist.checklist.title,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = colors.ink,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = context.getString(
                                        R.string.widget_completed_format,
                                        checklist.completedCount,
                                        checklist.totalCount
                                    ),
                                    style = TextStyle(color = colors.mutedInk, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class AllChecklistsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AllChecklistsWidget()
}
