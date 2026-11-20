package com.vagdroid.diag

import android.content.Context
import java.io.IOException

object LabelStore {

    class GroupLabels(
        val group: Int,
        var comment: String,
        val names: Array<String?>,
        val specs: Array<String?>
    )

    private var index: List<Pair<String, String>>? = null

    @Synchronized
    private fun buildIndex(ctx: Context) {
        if (index != null) return
        val files = ctx.assets.list("labels") ?: emptyArray()
        index = files.map { f ->
            val norm = f.substringBeforeLast('.').uppercase().replace("-", "")
            norm to f
        }
    }

    fun findForComponent(ctx: Context, componentId: String): String? {
        if (componentId.isBlank()) return null
        buildIndex(ctx)
        val id = componentId.uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (id.isBlank()) return null
        val idx = index!!
        idx.firstOrNull { it.first == id }?.let { return it.second }
        var best: String? = null
        var bestLen = 0
        for ((norm, file) in idx) {
            val minLen = minOf(norm.length, id.length)
            var prefix = 0
            while (prefix < minLen && norm[prefix] == id[prefix]) prefix++
            if (prefix >= 7 && prefix > bestLen) {
                bestLen = prefix
                best = file
            }
        }
        return best
    }

    fun load(ctx: Context, fileName: String): Map<Int, GroupLabels> {
        val result = mutableMapOf<Int, GroupLabels>()
        try {
            val text = ctx.assets.open("labels/$fileName").bufferedReader(Charsets.ISO_8859_1).readText()
            for (raw in text.lines()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith(";") || line.startsWith(":")) continue
                val parts = line.split(",", limit = 4)
                if (parts.size < 3) continue
                val group = parts[0].trim().toIntOrNull() ?: continue
                val field = parts[1].trim().toIntOrNull() ?: continue
                val name = parts[2].trim()
                val spec = if (parts.size > 3) parts[3].trim() else ""
                val gl = result.getOrPut(group) {
                    GroupLabels(group, "", arrayOfNulls(4), arrayOfNulls(4))
                }
                when (field) {
                    0 -> gl.comment = name
                    in 1..4 -> {
                        gl.names[field - 1] = name
                        gl.specs[field - 1] = spec
                    }
                }
            }
        } catch (e: IOException) {
        }
        return result
    }
}
