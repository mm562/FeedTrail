package com.example.cameraswitch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    fun saveCurrentStage(stage: String) {
        savedStateHandle.set("current_stage", stage)
    }

    fun getCurrentStage(): String? {
        return savedStateHandle.get("current_stage")
    }
}
