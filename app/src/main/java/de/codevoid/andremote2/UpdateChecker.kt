package de.codevoid.andremote2

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_API_BASE = "https://api.github.com/repos/c0dev0id/andRemote2/releases"

data class ReleaseInfo(
    val tagName: String,
    val downloadUrl: String,
    val fileName: String
)

object UpdateChecker {

    /**
     * Checks whether a newer version is available on GitHub.
     *
     * For release builds (DEBUG=false): queries /releases?per_page=1 (includes pre-releases,
     * sorted newest first) and compares tag_name against BuildConfig.VERSION_NAME.
     *
     * For prerelease/dev builds (DEBUG=true): queries /releases/tags/dev and compares the
     * short SHA embedded in the APK asset name against BuildConfig.BUILD_COMMIT.
     *
     * Returns a [ReleaseInfo] if an update is available, or null if already up to date.
     * Throws an exception on network or parse errors.
     *
     * Must be called from a background thread.
     */
    fun checkForUpdate(): ReleaseInfo? {
        return if (BuildConfig.DEBUG) {
            checkPrerelease()
        } else {
            checkRelease()
        }
    }

    private fun checkRelease(): ReleaseInfo? {
        val json = fetchJsonArray("$GITHUB_API_BASE?per_page=1").getJSONObject(0)
        val tagName = json.getString("tag_name")          // e.g. "v1.2.3"
        val remoteVersion = tagName.trimStart('v')         // e.g. "1.2.3"
        val currentVersion = BuildConfig.VERSION_NAME      // e.g. "1.0"

        if (remoteVersion == currentVersion) return null

        val asset = findApkAsset(json) ?: return null
        return ReleaseInfo(
            tagName = tagName,
            downloadUrl = asset.getString("browser_download_url"),
            fileName = asset.getString("name")
        )
    }

    private fun checkPrerelease(): ReleaseInfo? {
        val json = fetchJson("$GITHUB_API_BASE/tags/dev")
        val asset = findApkAsset(json) ?: return null
        val fileName = asset.getString("name") // e.g. "andRemote2-dev-abc1234.apk"

        // Extract short SHA from filename: andRemote2-dev-<sha>.apk
        val remoteCommit = Regex("andRemote2-dev-([a-f0-9]+)\\.apk")
            .find(fileName)
            ?.groupValues
            ?.get(1)

        val currentCommit = BuildConfig.BUILD_COMMIT
        // "local" means a local dev build — always offer the latest dev APK
        if (remoteCommit != null && remoteCommit == currentCommit) return null

        return ReleaseInfo(
            tagName = json.getString("tag_name"),
            downloadUrl = asset.getString("browser_download_url"),
            fileName = fileName
        )
    }

    private fun findApkAsset(releaseJson: JSONObject): JSONObject? {
        val assets = releaseJson.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                return asset
            }
        }
        return null
    }

    private fun fetchJson(urlString: String): JSONObject {
        return JSONObject(fetch(urlString))
    }

    private fun fetchJsonArray(urlString: String): JSONArray {
        return JSONArray(fetch(urlString))
    }

    private fun fetch(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        try {
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}
