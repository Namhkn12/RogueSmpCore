package com.roguesmp.dungeon_v2.data.runtime;


import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorPipeline;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.IBehavior;
import org.bukkit.Location;

import java.util.List;

public class SpawnerInstance {
    private String iid;
    private String templateId;
    private Location location;
    private BehaviorPipeline pipeline;

    public SpawnerInstance() {
    }

    public SpawnerInstance(String iid, String templateId, Location location, List<IBehavior> behaviors) {
        this.iid = iid;
        this.templateId = templateId;
        this.location = location;
        this.pipeline = new BehaviorPipeline(behaviors);
    }

    public String getId() {
        return iid;
    }

    public void setId(String iid) {
        this.iid = iid;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public BehaviorPipeline getPipeline() {
        return pipeline;
    }

    public List<IBehavior> getBehaviors() {
        return pipeline.getBehaviors();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
