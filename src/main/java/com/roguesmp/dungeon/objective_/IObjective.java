package com.roguesmp.dungeon.objective_;

import com.roguesmp.dungeon.objective_.param.ObjectiveData;

import java.util.List;
import java.util.Map;

public interface IObjective {
    void callBack(ObjCallBack callBack);
    void start();
    void process();
    boolean isCompleted();

    ObjectiveData getData();
    int getScore();

    String getMessage();
    List<String> getMessageScoreBoard();

    Map<String, Object> exportProgress();
    void importProgress(Map<String, Object> progress);
}
