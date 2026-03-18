package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.behavior.IBehavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnerInstance {
        private UUID id;
        private String templateId;
        private List<IBehavior> behaviors;

        public SpawnerInstance() {
        }

        public SpawnerInstance(UUID id, String templateId, List<IBehavior> behaviors) {
                this.id = id;
                this.templateId = templateId;
                this.behaviors = behaviors;
        }

        public UUID getId() {
                return id;
        }

        public void setId(UUID id) {
                this.id = id;
        }

        public String getTemplateId() {
                return templateId;
        }

        public void setTemplateId(String templateId) {
                this.templateId = templateId;
        }

        public List<IBehavior> getBehaviors() {
                return behaviors;
        }

        public void setBehaviors(List<IBehavior> behaviors) {
                this.behaviors = behaviors;
        }
}
