package com.vagdroid.diag

import android.content.Context

/** Парсер autoscan.txt: кузов → список адресов блоков. */
object Autoscan {

    class Chassis(val code: String, val name: String, val addresses: List<Int>)

    fun load(ctx: Context): List<Chassis> {
        val result = mutableListOf<Chassis>()
        val text = ctx.assets.open("autoscan.txt").bufferedReader().readText()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(";")) continue
            val parts = line.split(",")
            if (parts.size < 3) continue
            val code = parts[0].trim()
            val name = parts[1].trim()
            val addresses = parts.drop(2).mapNotNull { it.trim().toIntOrNull(16) }
            if (addresses.isNotEmpty()) result.add(Chassis(code, name, addresses))
        }
        return result
    }

    fun defaultAllLong(ctx: Context): Chassis =
        load(ctx).firstOrNull { it.code == "XX" } ?: Chassis("XX", "All Long", (1..0x7F).toList())
}
