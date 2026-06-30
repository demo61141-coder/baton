package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadTask(
    val downloadId: Long,
    val title: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: Int = DownloadManager.STATUS_PENDING,
    val speedString: String = "0 KB/s",
    val progress: Float = 0f,
    val fileType: String = "video", // "music" or "video"
    val quality: String = "360P",
    val timestamp: Long = System.currentTimeMillis()
)

object DownloadTracker {
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Keep track of the last downloaded bytes to calculate speed
    private val lastDownloadedBytesMap = mutableMapOf<Long, Long>()
    private val lastUpdateTimeMap = mutableMapOf<Long, Long>()

    fun addDownload(
        context: Context,
        downloadId: Long,
        title: String,
        quality: String,
        fileType: String
    ) {
        val newTask = DownloadTask(
            downloadId = downloadId,
            title = title,
            quality = quality,
            fileType = fileType
        )
        synchronized(_tasks) {
            val currentList = _tasks.value.toMutableList()
            // Avoid duplicates
            if (currentList.none { it.downloadId == downloadId }) {
                currentList.add(0, newTask) // Add at top
                _tasks.value = currentList
            }
        }
        startTrackingIfNeeded(context)
    }

    private fun startTrackingIfNeeded(context: Context) {
        if (trackingJob == null || trackingJob?.isActive == false) {
            trackingJob = scope.launch {
                val appContext = context.applicationContext
                val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                
                while (isActive) {
                    var hasActiveDownloads = false
                    val currentTasks = _tasks.value
                    
                    if (currentTasks.isEmpty()) {
                        delay(2000)
                        continue
                    }

                    val updatedTasks = currentTasks.map { task ->
                        if (task.status == DownloadManager.STATUS_SUCCESSFUL || task.status == DownloadManager.STATUS_FAILED) {
                            task
                        } else {
                            hasActiveDownloads = true
                            queryDownloadStatus(downloadManager, task)
                        }
                    }

                    _tasks.value = updatedTasks

                    // If no active downloads, we can sleep longer or stop tracking
                    if (!hasActiveDownloads) {
                        delay(3000)
                    } else {
                        delay(1000)
                    }
                }
            }
        }
    }

    private fun queryDownloadStatus(downloadManager: DownloadManager, task: DownloadTask): DownloadTask {
        val query = DownloadManager.Query().setFilterById(task.downloadId)
        var cursor: Cursor? = null
        try {
            cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else task.status
                val downloaded = if (downloadedIndex != -1) cursor.getLong(downloadedIndex) else task.downloadedBytes
                val total = if (totalIndex != -1) cursor.getLong(totalIndex) else task.totalBytes

                val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f

                // Calculate Speed
                val now = System.currentTimeMillis()
                val lastBytes = lastDownloadedBytesMap[task.downloadId] ?: 0L
                val lastTime = lastUpdateTimeMap[task.downloadId] ?: now

                val speedString = if (status == DownloadManager.STATUS_RUNNING) {
                    val timeDiffSec = (now - lastTime) / 1000.0
                    val bytesDiff = downloaded - lastBytes
                    if (timeDiffSec > 0.1 && bytesDiff >= 0) {
                        val speedBytesPerSec = (bytesDiff / timeDiffSec).toLong()
                        formatSpeed(speedBytesPerSec)
                    } else {
                        task.speedString
                    }
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    "Completed"
                } else if (status == DownloadManager.STATUS_FAILED) {
                    "Failed"
                } else {
                    "Pending..."
                }

                // Update maps
                lastDownloadedBytesMap[task.downloadId] = downloaded
                lastUpdateTimeMap[task.downloadId] = now

                return task.copy(
                    status = status,
                    downloadedBytes = downloaded,
                    totalBytes = total,
                    progress = progress.coerceIn(0f, 1f),
                    speedString = speedString
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return task
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    fun removeTask(downloadId: Long) {
        synchronized(_tasks) {
            val currentList = _tasks.value.toMutableList()
            currentList.removeAll { it.downloadId == downloadId }
            _tasks.value = currentList
        }
        lastDownloadedBytesMap.remove(downloadId)
        lastUpdateTimeMap.remove(downloadId)
    }

    fun clearAll() {
        synchronized(_tasks) {
            _tasks.value = emptyList()
        }
        lastDownloadedBytesMap.clear()
        lastUpdateTimeMap.clear()
    }
}
