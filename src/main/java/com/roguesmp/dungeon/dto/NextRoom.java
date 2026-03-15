package com.roguesmp.dungeon.dto;

import com.roguesmp.dungeon.instance.NodeInstance;

public class NextRoom {
    public NodeInstance node;
    public int index;

    public NextRoom() {
    }

    public NextRoom(NodeInstance node, int index) {
        this.node = node;
        this.index = index;
    }

    public NodeInstance getNode() {
        return node;
    }

    public void setNode(NodeInstance node) {
        this.node = node;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
