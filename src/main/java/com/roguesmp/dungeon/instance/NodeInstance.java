package com.roguesmp.dungeon.instance;

import java.util.List;

public class NodeInstance {
    private String key;
    private String name;
    private String icon;
    private List<String> schemeta;

    public NodeInstance(String key, String name, String icon, List<String> schemeta) {
        this.key = key;
        this.name = name;
        this.icon = icon;
        this.schemeta = schemeta;
    }

    public NodeInstance() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public List<String> getSchemeta() {
        return schemeta;
    }

    public void setSchemeta(List<String> schemeta) {
        this.schemeta = schemeta;
    }
}
