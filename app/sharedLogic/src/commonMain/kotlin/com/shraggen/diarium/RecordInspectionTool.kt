package com.shraggen.diarium

/**
 * A domain-specific tool. Notice how this class knows about "Hives" and "Queens",
 * but the Kernel has no idea what this is.
 */
class RecordInspectionTool : DiariumTool {
    override val name = "record_inspection"
    override val description = "Records the status of a beehive."

    override fun execute(arguments: Map<String, String>): String {
        val hiveId = arguments["hive_id"] ?: throw IllegalArgumentException("hive_id is required")
        val queenSeen = arguments["queen_seen"]?.toBoolean() ?: false

        // This is where Room DB / SQLDelight logic will go later.
        val status = if (queenSeen) "Queen was spotted." else "Queen was NOT seen."

        return "Success: Logged inspection for Hive $hiveId. $status"
    }
}