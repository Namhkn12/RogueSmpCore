package com.roguesmp.dungeon.objective.obj;

import com.roguesmp.dungeon.objective.IObjective;
import com.roguesmp.dungeon.objective.ObjectiveCompleteCallBack;
import com.roguesmp.dungeon.objective.ObjectiveData;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;
import java.util.Map;

public class SpawnerBreakObj implements IObjective {
    private int requiredCount = 0;
    private int brokenCount = 0;
    private boolean completed = false;
    private transient ObjectiveCompleteCallBack onComplete;

    public SpawnerBreakObj() {
        this.requiredCount = 0;
    }

    public SpawnerBreakObj(ObjectiveData data) {
        Object raw = data.getParams().get("count");
        this.requiredCount = ((Number) raw).intValue();
    }

    @Override
    public void start(ObjectiveCompleteCallBack onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public void process() {
        // không cần tick
    }

    public void onSpawnerBreak(BlockBreakEvent e) {
        if (completed) return;

        brokenCount++;

        if (brokenCount >= requiredCount) {
            completed = true;
            onComplete.onComplete(this);
        }
    }

    @Override
    public boolean isCompleted() { return completed; }

    @Override
    public ObjectiveData getData() {
        return new ObjectiveData("spawner_break", Map.of("count", requiredCount));
    }

    @Override
    public int getScore() { return 20; }

    @Override
    public String getProgressMessage() {
        return "Đã phá spawner: " + brokenCount + "/" + requiredCount;
    }

    @Override
    public String getStartMessage() {
        return "Mục tiêu hãy phá " + requiredCount + " spawner";
    }

    @Override
    public List<String> getLineForUI() {
        return List.of(
                "☠ Phá spawner",
                brokenCount + "/" + requiredCount + (completed ? " ✓" : "")
        );
    }

    @Override
    public Map<String, Object> exportProgress() {
        return Map.of("brokenCount", brokenCount, "completed", completed);
    }

    @Override
    public void importProgress(Map<String, Object> progress) {
        this.brokenCount = ((Number) progress.get("brokenCount")).intValue();
        this.completed = (Boolean) progress.get("completed");
    }
}