package com.robbdeeze.nuviotv.domain.model

import kotlinx.serialization.Serializable

/**
 * A user-defined Quick Channel — a named search pattern that, when clicked,
 * searches every IPTV source for channels whose name matches the pattern.
 *
 * Unlike the curated [QuickChannel] list, user quick channels are persisted
 * locally and can be added/removed by the user from the home screen.
 */
@Serializable
data class UserQuickChannel(
    val id: String,
    val displayName: String,
    /** Substring(s) matched against channel names (case-insensitive). */
    val aliases: List<String>,
)