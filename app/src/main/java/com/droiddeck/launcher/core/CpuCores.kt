package com.droiddeck.launcher.core

import java.io.File

object CpuCores {
    val all: List<Int> by lazy { (0 until Runtime.getRuntime().availableProcessors()).toList() }

    fun maxGhz(core: Int): Double? =
        FileUtils.readString(File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq"))
            ?.trim()?.toLongOrNull()?.let { it / 1_000_000.0 }

    /**
     * The big cluster on a big.LITTLE SoC: every core whose cpufreq max frequency is at
     * least 2.45 GHz. Little clusters top out below that (A55 at 2.0-2.4 GHz), while every
     * current big core (A7x/X1-X4) boosts past it. Empty when the SoC is homogeneous.
     */
    fun bigCores(): Set<Int> =
        all.filter { (maxGhz(it) ?: 0.0) >= 2.45 }.toSet()

    /**
     * A default game core list that keeps the emulated (FEX) threads off the little cores:
     * an A55 runs translated code several times slower, and the scheduler otherwise parks
     * JIT and game threads there under load. Empty when the SoC has no distinct big cluster
     * or every core qualifies, which keeps the unrestricted behaviour.
     */
    fun bigClusterDefault(): String {
        val big = bigCores()
        return if (big.isEmpty() || big.size >= all.size) "" else format(big)
    }

    fun parse(list: String): Set<Int> =
        list.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in all }.toSet()

    fun format(cores: Set<Int>): String = cores.sorted().joinToString(",")

    fun listOrAll(list: String): String {
        val cores = parse(list)
        return format(if (cores.isEmpty()) all.toSet() else cores)
    }

    fun gameLaunchList(list: String, clientPinned: Boolean): String =
        restrictionOrEmpty(list).ifEmpty { if (clientPinned) listOrAll("") else "" }

    fun restrictionOrEmpty(list: String): String {
        val cores = parse(list)
        return if (cores.isEmpty() || cores.size >= all.size) "" else format(cores)
    }
}
