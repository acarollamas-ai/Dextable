package com.example.ui.windowmanager

data class WindowState(
  val id: String,
  val appId: String,
  val title: String,
  val iconEmoji: String,
  val offsetX: Float = 60f,
  val offsetY: Float = 40f,
  val width: Float = 680f,
  val height: Float = 480f,
  val isMinimized: Boolean = false,
  val isMaximized: Boolean = false,
  val isFullscreen: Boolean = false,
  val zIndex: Float = 1f,
  val initialParam: String? = null
)
