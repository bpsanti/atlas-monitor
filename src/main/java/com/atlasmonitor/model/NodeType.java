package com.atlasmonitor.model;

/**
 * Desired Atlas node role.
 * Matches Atlas typeName values that contain the enum name
 * (e.g. REPLICA_PRIMARY, SHARD_PRIMARY → PRIMARY).
 */
public enum NodeType {
    PRIMARY,
    SECONDARY;

    public boolean matches(String atlasTypeName) {
        return atlasTypeName != null && atlasTypeName.contains(this.name());
    }
}
