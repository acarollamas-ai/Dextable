package com.example.core.filesystem

import android.content.Context
import com.example.backends.LinuxBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileEntry(
  val id: String,
  val name: String,
  val path: String,
  val isDirectory: Boolean,
  val size: Long = 0,
  val modifiedDate: Long = System.currentTimeMillis(),
  val content: String = "",
  val extension: String = "",
  val iconEmoji: String = if (isDirectory) "📁" else "📄"
) {
  val formattedSize: String get() {
    if (isDirectory) return "--"
    return when {
      size < 1024 -> "$size B"
      size < 1024 * 1024 -> "${size / 1024} KB"
      else -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    }
  }

  val formattedDate: String get() {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(modifiedDate))
  }
}

class FileSystemEngine(private val context: Context? = null) : LinuxBackend.LinuxFilesystemBridge {

  private val _currentPath = MutableStateFlow("/home/user")
  val currentPath: StateFlow<String> = _currentPath.asStateFlow()

  private val _items = MutableStateFlow<List<FileEntry>>(emptyList())
  val items: StateFlow<List<FileEntry>> = _items.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _clipboard = MutableStateFlow<Pair<FileEntry, Boolean>?>(null) // entry, isCut
  val clipboard: StateFlow<Pair<FileEntry, Boolean>?> = _clipboard.asStateFlow()

  // Base virtual/local directory tree
  private val entriesMap = mutableMapOf<String, FileEntry>()

  init {
    initializeDefaultStructure()
  }

  private fun initializeDefaultStructure() {
    // Root directories
    createDirInternal("/home", "user")
    createDirInternal("/home/user", "Desktop")
    createDirInternal("/home/user", "Documents")
    createDirInternal("/home/user", "Downloads")
    createDirInternal("/home/user", "Pictures")
    createDirInternal("/home/user", "Videos")
    createDirInternal("/home/user", "Music")
    createDirInternal("/home/user", "Projects")

    // Default sample files for desktop feel
    createFileInternal(
      "/home/user/Documents",
      "Welcome_to_Desktable.txt",
      "Welcome to Desktable OS!\n\nThis is a desktop operating system tailored for touchscreen ARM64 tablets.\nClean, responsive, and ready for multitasking."
    )
    createFileInternal(
      "/home/user/Projects",
      "desktable-core.kt",
      "// Desktable OS Core Architecture\nfun main() {\n    println(\"Desktable OS running on ARM64\")\n}"
    )
    createFileInternal(
      "/home/user/Downloads",
      "linux_arm64_specs.md",
      "# Desktable Linux Specs\n- Kernel: Linux 6.8 ARM64\n- Display Server: Wayland\n- Window Manager: Desktable Window Compositor\n- Primary Browsers: Firefox Desktop & Chrome Desktop"
    )

    refreshDirectory()
  }

  fun navigateTo(path: String) {
    _currentPath.value = path
    refreshDirectory()
  }

  fun navigateUp() {
    val cur = _currentPath.value
    if (cur != "/home/user" && cur != "/home" && cur != "/") {
      val lastSlash = cur.lastIndexOf('/')
      if (lastSlash > 0) {
        _currentPath.value = cur.substring(0, lastSlash)
      } else {
        _currentPath.value = "/home/user"
      }
      refreshDirectory()
    }
  }

  fun setSearch(query: String) {
    _searchQuery.value = query
  }

  override fun createDirectory(path: String, name: String): Boolean {
    val cleanName = name.trim().replace("/", "")
    if (cleanName.isEmpty()) return false
    val success = createDirInternal(path, cleanName)
    if (success) refreshDirectory()
    return success
  }

  override fun createFile(path: String, name: String, content: String): Boolean {
    val cleanName = name.trim().replace("/", "")
    if (cleanName.isEmpty()) return false
    val success = createFileInternal(path, cleanName, content)
    if (success) refreshDirectory()
    return success
  }

  fun rename(oldPath: String, newName: String): Boolean {
    val cleanName = newName.trim().replace("/", "")
    val old = entriesMap[oldPath] ?: return false
    val parent = oldPath.substringBeforeLast('/', "")
    val newPath = if (parent.isEmpty()) "/$cleanName" else "$parent/$cleanName"
    if (entriesMap.containsKey(newPath)) return false

    entriesMap.remove(oldPath)
    entriesMap[newPath] = old.copy(name = cleanName, path = newPath)
    refreshDirectory()
    return true
  }

  override fun delete(path: String): Boolean {
    val removed = entriesMap.remove(path) != null
    // Also remove any children if directory
    val toRemove = entriesMap.keys.filter { it.startsWith("$path/") }
    toRemove.forEach { entriesMap.remove(it) }
    refreshDirectory()
    return removed
  }

  fun copy(entry: FileEntry) {
    _clipboard.value = Pair(entry, false)
  }

  fun cut(entry: FileEntry) {
    _clipboard.value = Pair(entry, true)
  }

  fun paste(destinationPath: String): Boolean {
    val clip = _clipboard.value ?: return false
    val (source, isCut) = clip
    val newPath = "$destinationPath/${source.name}"
    if (source.isDirectory) {
      createDirInternal(destinationPath, source.name)
    } else {
      createFileInternal(destinationPath, source.name, source.content)
    }
    if (isCut) {
      delete(source.path)
      _clipboard.value = null
    }
    refreshDirectory()
    return true
  }

  fun getFileContent(path: String): String? {
    return entriesMap[path]?.content
  }

  fun updateFileContent(path: String, content: String): Boolean {
    val old = entriesMap[path] ?: return false
    entriesMap[path] = old.copy(content = content, size = content.toByteArray().size.toLong(), modifiedDate = System.currentTimeMillis())
    refreshDirectory()
    return true
  }

  private fun createDirInternal(parent: String, name: String): Boolean {
    val path = if (parent == "/") "/$name" else "$parent/$name"
    if (entriesMap.containsKey(path)) return false
    entriesMap[path] = FileEntry(
      id = path,
      name = name,
      path = path,
      isDirectory = true,
      size = 0,
      iconEmoji = getFolderEmoji(name)
    )
    return true
  }

  private fun createFileInternal(parent: String, name: String, content: String): Boolean {
    val path = if (parent == "/") "/$name" else "$parent/$name"
    val ext = name.substringAfterLast('.', "")
    val emoji = when (ext.lowercase(Locale.ROOT)) {
      "txt", "md" -> "📝"
      "kt", "py", "sh", "js", "html" -> "💻"
      "png", "jpg", "jpeg", "webp" -> "🖼"
      "mp3", "wav", "ogg" -> "🎵"
      "mp4", "mkv" -> "🎬"
      "zip", "tar", "gz" -> "📦"
      else -> "📄"
    }
    entriesMap[path] = FileEntry(
      id = path,
      name = name,
      path = path,
      isDirectory = false,
      size = content.toByteArray().size.toLong(),
      content = content,
      extension = ext,
      iconEmoji = emoji
    )
    return true
  }

  private fun getFolderEmoji(name: String): String {
    return when (name) {
      "Desktop" -> "🖥"
      "Documents" -> "📑"
      "Downloads" -> "📥"
      "Pictures" -> "🖼"
      "Videos" -> "🎬"
      "Music" -> "🎵"
      "Projects" -> "🔨"
      else -> "📁"
    }
  }

  private fun refreshDirectory() {
    val cur = _currentPath.value
    val list = entriesMap.values.filter { entry ->
      val parent = entry.path.substringBeforeLast('/', "")
      val effectiveParent = if (parent.isEmpty()) "/" else parent
      effectiveParent == cur
    }.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })

    _items.value = list
  }

  fun registerDownloadedFile(parentPath: String, name: String, content: String, size: Long = 0L) {
    val path = if (parentPath == "/") "/$name" else "$parentPath/$name"
    val ext = name.substringAfterLast('.', "")
    val emoji = when (ext.lowercase(Locale.ROOT)) {
      "txt", "md" -> "📝"
      "kt", "py", "sh", "js", "html" -> "💻"
      "png", "jpg", "jpeg", "webp" -> "🖼"
      "mp3", "wav", "ogg" -> "🎵"
      "mp4", "mkv" -> "🎬"
      "zip", "tar", "gz" -> "📦"
      else -> "📄"
    }
    entriesMap[path] = FileEntry(
      id = path,
      name = name,
      path = path,
      isDirectory = false,
      size = if (size > 0) size else content.toByteArray().size.toLong(),
      content = content,
      extension = ext,
      iconEmoji = emoji,
      modifiedDate = System.currentTimeMillis()
    )
    refreshDirectory()
  }

  fun getLocalFileForEntry(entry: FileEntry): File {
    val dir = File(context?.filesDir ?: File("/tmp"), "storage").apply { if (!exists()) mkdirs() }
    val f = File(dir, entry.name)
    if (!f.exists() || f.length() == 0L) {
      f.writeText(entry.content.ifEmpty { "// File: ${entry.name}\n" })
    }
    return f
  }

  // Linux backend bridge implementation
  override fun listFiles(path: String): List<String> {
    val effective = if (path.isEmpty()) _currentPath.value else path
    return entriesMap.values.filter {
      val parent = it.path.substringBeforeLast('/', "")
      val checkParent = if (parent.isEmpty()) "/" else parent
      checkParent == effective
    }.map { if (it.isDirectory) "${it.name}/" else it.name }
  }

  override fun readFile(path: String): String? = getFileContent(path)
}
