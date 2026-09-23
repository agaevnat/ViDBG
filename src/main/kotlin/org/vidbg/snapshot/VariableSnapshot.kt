package org.vidbg.snapshot

import org.vidbg.enrich.ObjectIdentity

data class VariableSnapshot(
    val name: String,
    val type: String?,
    val value: String,
    val hasChildren: Boolean,
    /** Null for the container's own locals; the group name (e.g. "Static") when pulled from an XValueGroup. */
    val scope: String?,
    val identity: ObjectIdentity?,
)
