package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.behavior.IBehavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnerInstance {
        private String iid;
        private String templateId;
        private List<IBehavior> behaviors;

        public SpawnerInstance() {
        }

        public SpawnerInstance(String iid, String templateId, List<IBehavior> behaviors) {
                this.iid = iid;
                this.templateId = templateId;
                this.behaviors = behaviors;
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

        public List<IBehavior> getBehaviors() {
                return behaviors;
        }

        public void setBehaviors(List<IBehavior> behaviors) {
                this.behaviors = behaviors;
        }
}
