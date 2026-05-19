package com.example.railticket

import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearWebViewData() // Clear data on launch
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearWebViewData() // Also clear data on exit
    }

    private fun clearWebViewData() {
        Log.d("MainActivity", "Clearing WebView data.")
        try {
            // Clear cookies
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            Log.d("MainActivity", "WebView cookies cleared.")

            // Delete WebView cache directory's contents
            val cacheDir = File(cacheDir, "web_cache")
            if (deleteDirectoryContents(cacheDir)) {
                Log.d("MainActivity", "WebView cache directory contents deleted successfully.")
            } else {
                Log.w("MainActivity", "Failed to delete WebView cache directory contents.")
            }

            // Delete WebView app data directories' contents
            val filesDirParent = filesDir.parentFile
            if (filesDirParent != null) {
                val appWebViewDir = File(filesDirParent, "app_webview")
                if (deleteDirectoryContents(appWebViewDir)) {
                    Log.d("MainActivity", "app_webview directory contents deleted successfully.")
                } else {
                    Log.w("MainActivity", "Failed to delete app_webview directory contents.")
                }

                val databasesDir = File(filesDirParent, "databases")
                if (deleteDirectoryContents(databasesDir)) {
                    Log.d("MainActivity", "databases directory contents deleted successfully.")
                } else {
                    Log.w("MainActivity", "Failed to delete databases directory contents.")
                }
            } else {
                Log.w("MainActivity", "Could not find parent directory of filesDir to clear app_webview and databases.")
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error clearing WebView data", e)
        }
    }

    private fun deleteDirectoryContents(directory: File?): Boolean {
        if (directory == null || !directory.exists() || !directory.isDirectory) {
            return false
        }
        for (child in directory.listFiles() ?: emptyArray()) {
            if (child.isDirectory) {
                deleteDirectoryContents(child)
            }
            child.delete()
        }
        return true
    }
}
