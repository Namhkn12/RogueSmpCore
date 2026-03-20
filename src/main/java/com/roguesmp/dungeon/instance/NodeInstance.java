package com.roguesmp.dungeon.instance;

import java.util.List;
import java.util.UUID;

public class NodeInstance {
    private UUID id;
    private String nodeKey;
    private String name;
    private String icon;
    private String schemetas;

    public NodeInstance() {}

    public NodeInstance(UUID id, String nodeKey, String name, String icon, String schemetas) {
        this.id = id;
        this.nodeKey = nodeKey;
        this.name = name;
        this.icon = icon;
        this.schemetas = schemetas;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSchemetas() {
        return schemetas;
    }

    public void setSchemetas(String schemetas) {
        this.schemetas = schemetas;
    }
}