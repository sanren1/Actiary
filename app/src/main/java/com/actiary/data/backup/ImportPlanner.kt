package com.actiary.data.backup

import com.actiary.data.db.ExerciseTypeEntity

data class LocalSnapshot(val types: List<ExerciseTypeEntity>, val dayEpochs: Set<Long>)

data class ImportPlan(val typeMap: Map<Long, TypeTarget>, val daysToWrite: List<Long>, val skippedDays: Int)

sealed interface TypeTarget {
    data class Existing(val id: Long) : TypeTarget
    data class Create(val spec: BackupType) : TypeTarget
}

fun planMerge(local: LocalSnapshot, backup: BackupData): ImportPlan = planImport(local, backup, overwrite = false)

fun planOverwrite(local: LocalSnapshot, backup: BackupData): ImportPlan = planImport(local, backup, overwrite = true)

private fun planImport(local: LocalSnapshot, backup: BackupData, overwrite: Boolean): ImportPlan {
    val builtin = local.types.filter { it.builtinKey != null }.associateBy { it.builtinKey }
    val custom = mutableMapOf<String, TypeTarget>()
    if (!overwrite) {
        for (type in local.types.sortedBy { it.archived }) {
            if (type.builtinKey == null && type.name != null) {
                custom.putIfAbsent(type.name.trim(), TypeTarget.Existing(type.id))
            }
        }
    }
    val targets = backup.types.associate { type ->
        val target = when {
            type.builtinKey != null -> builtin[type.builtinKey]?.let { TypeTarget.Existing(it.id) } ?: TypeTarget.Create(type)
            overwrite -> TypeTarget.Create(type)
            else -> custom.getOrPut(requireNotNull(type.name).trim()) { TypeTarget.Create(type) }
        }
        type.id to target
    }
    val days = backup.records.map { it.epochDay }.filter { overwrite || it !in local.dayEpochs }
    return ImportPlan(targets, days, backup.records.size - days.size)
}
