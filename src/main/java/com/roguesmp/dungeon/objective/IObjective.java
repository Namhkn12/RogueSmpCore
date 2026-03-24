package com.roguesmp.dungeon.objective;

import java.util.List;
import java.util.Map;

public interface IObjective {
    void start(ObjectiveCompleteCallBack onComplete);
    void process();
    boolean isCompleted();
    ObjectiveData getData();
    int getScore();
    String getProgressMessage();
    String getStartMessage();
    List<String> getLineForUI();
    public Map<String, Object> exportProgress();
    public void importProgress(Map<String, Object> progress);
}