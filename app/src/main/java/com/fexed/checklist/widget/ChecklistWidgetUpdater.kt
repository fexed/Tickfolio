package com.fexed.checklist.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object ChecklistWidgetUpdater {
    suspend fun updateAll(context: Context) {
        SingleChecklistWidget().updateAll(context)
        AllChecklistsWidget().updateAll(context)
    }
}
