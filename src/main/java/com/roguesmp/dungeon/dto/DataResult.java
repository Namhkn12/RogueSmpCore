package com.roguesmp.dungeon.dto;

import java.util.List;

public class DataResult<C> {
    private final C result;
    private final List<String> logs;

    public DataResult(C success, List<String> logs) {
        this.result = success;
        this.logs = logs;
    }

    public C getResult() {
        return result;
    }

    public List<String> getLogs() {
        return logs;
    }

    public boolean hasLogs() {return !logs.isEmpty();}
}
