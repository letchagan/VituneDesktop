package app.vitune.desktop.player

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolves playable audio stream URLs with yt-dlp, the same tool the Android app
 * runs internally. The Windows build of yt-dlp is downloaded on first launch and
 * kept up to date, because YouTube changes often and old versions stop working.
 */
class YtDlp(private val dataDir: Path) {
    private val exe: Path = dataDir.resolve("yt-dlp.exe")
    private val urlCache = ConcurrentHashMap<String, CachedUrl>()
    private val ready = CompletableDeferred<Unit>()

    private data class CachedUrl(val url: String, val createdAt: Long)

    /** Downloads (if needed) and updates yt-dlp. Call once at startup. */
    suspend fun prepare(onStatus: (String) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                if (!Files.exists(exe)) {
                    onStatus("Downloading yt-dlp (first launch only)…")
                    download()
                } else {
                    onStatus("Checking for yt-dlp updates…")
                    // Failing to update isn't fatal: the installed version may still work.
                    runCatching { exec(listOf("-U"), timeoutSeconds = 90) }
                }
            }
            ready.complete(Unit)
        } catch (e: Exception) {
            ready.completeExceptionally(e)
            throw e
        }
    }

    suspend fun streamUrl(videoId: String): String {
        ready.await()

        urlCache[videoId]?.let { cached ->
            if (System.currentTimeMillis() - cached.createdAt < URL_CACHE_MS) return cached.url
        }

        val args = buildList {
            add("-f"); add("bestaudio")
            add("-g")
            add("--no-playlist")
            add("--no-warnings")
            addAll(jsRuntimeArgs)
            add("https://music.youtube.com/watch?v=$videoId")
        }

        val output = runInterruptible(Dispatchers.IO) { exec(args, timeoutSeconds = 90) }
        val url = output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("http") }
            ?: error("yt-dlp didn't return a stream URL")

        urlCache[videoId] = CachedUrl(url, System.currentTimeMillis())
        return url
    }

    private fun download() {
        Files.createDirectories(dataDir)
        val partial = dataDir.resolve("yt-dlp.exe.part")
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
        val request = HttpRequest.newBuilder(URI.create(DOWNLOAD_URL)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(partial))
        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial)
            error("Couldn't download yt-dlp (HTTP ${response.statusCode()})")
        }
        Files.move(partial, exe, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun exec(args: List<String>, timeoutSeconds: Long): String {
        val process = ProcessBuilder(listOf(exe.toString()) + args).start()
        // Read both streams concurrently so a full pipe can't block the process.
        val stdout = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readText() }
        val stderr = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().readText() }

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("yt-dlp timed out")
            }
        } catch (e: InterruptedException) {
            // The coroutine was cancelled (e.g. the user skipped the song).
            process.destroyForcibly()
            throw e
        }

        if (process.exitValue() != 0) {
            val message = stderr.get().lineSequence().lastOrNull { it.isNotBlank() }
            error(message ?: "yt-dlp failed with exit code ${process.exitValue()}")
        }
        return stdout.get()
    }

    /**
     * yt-dlp needs a JavaScript runtime for YouTube. It uses Deno by default;
     * if only Node.js or Bun is installed, tell yt-dlp to use that instead.
     */
    private val jsRuntimeArgs: List<String> by lazy {
        fun onPath(name: String) = System.getenv("PATH").orEmpty()
            .split(File.pathSeparator)
            .any { dir -> dir.isNotBlank() && File(dir, name).isFile }

        when {
            onPath("deno.exe") -> emptyList()
            onPath("node.exe") -> listOf("--js-runtimes", "node")
            onPath("bun.exe") -> listOf("--js-runtimes", "bun")
            else -> emptyList()
        }
    }

    private companion object {
        const val DOWNLOAD_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"

        // YouTube stream URLs expire after roughly six hours
        const val URL_CACHE_MS = 4 * 60 * 60 * 1000L
    }
}
