package com.actiary.domain

enum class FreeformFocusPart(val key: String) {
    CHEST("chest"),
    BACK("back"),
    SHOULDERS("shoulders"),
    ARMS("arms"),
    LEGS("legs"),
    ABS("abs"),
}

/**
 * Maintains per-body-part lines in a workout note (e.g. `胸：` / `Chest: `).
 * Removing a part only strips empty placeholder lines so user-typed details are never lost.
 */
object FreeformFocusNote {
    private fun lineMatchesLabel(line: String, label: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("$label：") || trimmed.startsWith("$label:")
    }

    private fun lineIsEmptyPlaceholder(line: String, label: String): Boolean {
        val trimmed = line.trim()
        return trimmed == "$label：" || trimmed == "$label:"
    }

    fun detectSelected(note: String, labels: Map<FreeformFocusPart, String>): Set<FreeformFocusPart> {
        if (note.isBlank()) return emptySet()
        val lines = note.lines()
        return buildSet {
            for ((part, label) in labels) {
                if (lines.any { lineMatchesLabel(it, label) }) add(part)
            }
        }
    }

    fun togglePartInNote(note: String, label: String, selected: Boolean, colon: String): String {
        if (selected) {
            if (note.lines().any { lineMatchesLabel(it, label) }) return note
            val entry = "$label$colon"
            return when {
                note.isEmpty() -> entry
                note.endsWith("\n") -> "$note$entry"
                else -> "$note\n$entry"
            }
        } else {
            if (note.isEmpty()) return note
            val lines = note.lines()
            val kept = lines.filterNot { lineIsEmptyPlaceholder(it, label) }
            if (kept.size == lines.size) return note
            return kept.joinToString("\n").trimEnd('\n')
        }
    }
}
