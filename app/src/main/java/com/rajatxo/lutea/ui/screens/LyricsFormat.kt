package com.rajatxo.lutea.ui.screens

/**
 * LyricsFormat — auto-detects the format of a lyrics blob and reports a short
 * label. Supports the formats Mochy handles (LRC, TTML, SRT, VTT, KRC, YRC, QRC,
 * ASS/SSA, plain text). We do NOT copy Mochy's decoder; this is a lightweight
 * detector + normalizer that turns any of these into a plain-text-with-LRC-tags
 * string the app can store and render later.
 *
 * Detection is by signature, not extension: the user can import any text file.
 */
object LyricsFormat {

    /** All formats the editor can import. Used to label the picker. */
    val supportedLabels = listOf(
        "LRC", "TTML", "SRT", "VTT", "KRC", "YRC", "QRC", "ASS", "Plain",
    )

    /** Detect the format of [text]. Never returns null — unknown falls back to Plain. */
    fun detect(text: String): String {
        val t = text.trimStart()
        if (t.isEmpty()) return "Plain"
        // KRC: Kugou, starts with [ti:] and often has [karma] or binary after decode;
        //      raw .krc is gzip+xor binary, but a text form has [offset:] + word tags.
        if (t.startsWith("[ti:") || t.contains("[offset:") && t.contains("<")) {
            if (t.contains("[krc:") || Regex("""\[\d+,\d+\]>""").containsMatchIn(t)) return "KRC"
        }
        // TTML: <tt> or <ttm:tt> or <p begin=
        if (t.startsWith("<?xml") || t.startsWith("<tt") || t.contains("<p begin=")) return "TTML"
        // QRC: QQ Music, looks like [offset:0] then lines "word<100,200>word<...>"
        if (Regex("""\d+,\d+,\d+""").containsMatchIn(t) && t.contains("LyricContent=")) return "QRC"
        // YRC: Netease word-timed {n:...} or [1000,2000](word)
        if (t.contains("{n:") || Regex("""\[\d+,\d+\]\(""").containsMatchIn(t)) return "YRC"
        // SRT: "1\n00:00:01,000 --> 00:00:03,000"
        if (Regex("""^\d+\s*\n\d{2}:\d{2}:\d{2},\d{3}\s*-->""", RegexOption.MULTILINE).containsMatchIn(t)) return "SRT"
        // VTT: WEBVTT header or "00:00:01.000 --> 00:00:03.000"
        if (t.startsWith("WEBVTT") || Regex("""\d{2}:\d{2}:\d{2}\.\d{3}\s*-->""").containsMatchIn(t)) return "VTT"
        // ASS/SSA: [Script Info] or Dialogue: ...
        if (t.startsWith("[Script Info]") || t.startsWith("Dialogue:") || t.startsWith("[Events]")) return "ASS"
        // LRC: [mm:ss.xx] timestamps
        if (Regex("""\[\d{1,2}:\d{2}([.:]\d{1,3})?\]""").containsMatchIn(t)) return "LRC"
        return "Plain"
    }

    /**
     * Normalize any supported format to a plain LRC-style string: lines like
     * "[mm:ss.xx] lyric". Plain text is kept as-is (no timestamps). This is a
     * pragmatic converter — it strips formatting/tags it doesn't need and keeps
     * the readable words + timing it can parse. Good enough to store + render.
     */
    fun normalize(text: String): String {
        val format = detect(text)
        return when (format) {
            "LRC", "Plain" -> text
            "SRT", "VTT" -> convertSubtitles(text, format)
            "ASS" -> convertAss(text)
            "TTML" -> convertTtml(text)
            "KRC", "YRC", "QRC" -> convertWordTimed(text, format)
            else -> text
        }.trim() + "\n"
    }

    private fun convertSubtitles(text: String, format: String): String {
        val out = StringBuilder()
        val timeRegex = if (format == "SRT") {
            Regex("""(\d{2}):(\d{2}):(\d{2}),(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2}),(\d{3})""")
        } else {
            Regex("""(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})""")
        }
        val lines = text.lines()
        var i = 0
        while (i < lines.size) {
            val m = timeRegex.find(lines[i]) ?: lines.getOrNull(i + 1)?.let { timeRegex.find(it) }
            if (m != null) {
                val min = m.groupValues[1].toInt()
                val sec = m.groupValues[2].toInt()
                val ms = (m.groupValues[3].toInt() / 10).coerceIn(0, 99)
                val stamp = "[%02d:%02d.%02d]".format(min, sec, ms)
                // Collect caption lines until the next blank.
                val captionStart = if (timeRegex.containsMatchIn(lines[i])) i + 1 else i + 2
                var j = captionStart
                val words = StringBuilder()
                while (j < lines.size && lines[j].isNotBlank()) { words.append(lines[j]).append(' '); j++ }
                if (words.isNotBlank()) out.append(stamp).append(' ').append(words.trim()).append('\n')
                i = j + 1
            } else {
                i++
            }
        }
        return if (out.isEmpty()) text else out.toString()
    }

    private fun convertAss(text: String): String {
        val out = StringBuilder()
        val dialogueRegex = Regex("""^Dialogue:\s*\d+,(\d):(\d{2}):(\d{2})\.(\d{2}),.*?,(.*?)$""")
        for (line in text.lines()) {
            val m = dialogueRegex.find(line) ?: continue
            val min = m.groupValues[1].toInt()
            val sec = m.groupValues[2].toInt()
            val cs = m.groupValues[3].toInt()
            val stamp = "[%02d:%02d.%02d]".format(min, sec, cs)
            // Strip ASS override tags {\\...} and karaoke {\\k...}.
            val cleaned = m.groupValues[5].replace(Regex("""\{\\[^}]*\}"""), "")
            if (cleaned.isNotBlank()) out.append(stamp).append(' ').append(cleaned.trim()).append('\n')
        }
        return if (out.isEmpty()) text else out.toString()
    }

    private fun convertTtml(text: String): String {
        val out = StringBuilder()
        // <p begin="mm:ss.xxx" ...>text</p>
        val pRegex = Regex("""<p\b[^>]*\bbegin="(\d{1,2}):(\d{2})[.:](\d{1,3})"[^>]*>(.*?)</p>""")
        for (m in pRegex.findAll(text)) {
            val min = m.groupValues[1].toInt()
            val sec = m.groupValues[2].toInt()
            val cs = (m.groupValues[3].padEnd(3, '0').take(2).toIntOrNull() ?: 0)
            val stamp = "[%02d:%02d.%02d]".format(min, sec, cs)
            val cleaned = m.groupValues[4].replace(Regex("<[^>]+>"), "").trim()
            if (cleaned.isNotBlank()) out.append(stamp).append(' ').append(cleaned).append('\n')
        }
        return if (out.isEmpty()) text else out.toString()
    }

    private fun convertWordTimed(text: String, format: String): String {
        // Strip the word-timing markers each format uses, keep LRC line stamps.
        var cleaned = text
        cleaned = cleaned.replace(Regex("""\{[^}]*\}"""), "")     // YRC {n:...}
        cleaned = cleaned.replace(Regex("""<[^>]*>"""), " ")       // QRC/KRC word tags
        cleaned = cleaned.replace(Regex("""\(\d+,\d+\)"""), "")    // YRC [t,d](word)
        return cleaned
    }
}
