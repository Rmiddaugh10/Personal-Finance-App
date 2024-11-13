package com.example.myapplication.data

sealed class UiEvent {
    data class ShowUndoDelete(val message: String, val type: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class ShowSuccess(val message: String) : UiEvent()
}