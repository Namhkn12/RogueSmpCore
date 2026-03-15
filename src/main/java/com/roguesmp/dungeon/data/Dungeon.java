package com.roguesmp.dungeon.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Dungeon {
    private String dgId;
    private String dgName;
    private String dgDescription;
    private int minRoomToEnd;
    private Map<String, Node> nodes;

    public Dungeon() {
    }

    public Dungeon(String dgId, String dgName, String dgDescription, int minRoomToEnd) {
        this.dgId = dgId;
        this.dgName = dgName;
        this.dgDescription = dgDescription;
        this.nodes = createDefaultNodes();
        this.minRoomToEnd = minRoomToEnd;
    }

    private Map<String, Node> createDefaultNodes() {
        Map<String, Node> defaultNodes = new LinkedHashMap<>();

        defaultNodes.put("start", new Node(
                "start", "Start Node", "BEACON",
                1.0, 1, List.of()
        ));
        defaultNodes.put("end", new Node(
                "end", "End Node", "END_PORTAL_FRAME",
                1.0, 1, List.of()
        ));

        return defaultNodes;
    }

    public String getDgId() {
        return dgId;
    }

    public void setDgId(String dgId) {
        this.dgId = dgId;
    }

    public String getDgName() {
        return dgName;
    }

    public void setDgName(String dgName) {
        this.dgName = dgName;
    }

    public String getDgDescription() {
        return dgDescription;
    }

    public void setDgDescription(String dgDescription) {
        this.dgDescription = dgDescription;
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, Node> nodes) {
        this.nodes = nodes;
    }

    public int getMinRoomToEnd() {
        return minRoomToEnd;
    }

    public void setMinRoomToEnd(int minRoomToEnd) {
        this.minRoomToEnd = minRoomToEnd;
    }
}
