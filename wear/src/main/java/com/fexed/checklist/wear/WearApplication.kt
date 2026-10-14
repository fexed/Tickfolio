package com.fexed.checklist.wear

import android.app.Application
import com.fexed.checklist.wear.sync.WearChecklistStore

class WearApplication : Application() {
    val checklistStore by lazy { WearChecklistStore(this) }
}
