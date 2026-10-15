package com.roguesmp.server.goal;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.bukkit.craftbukkit.entity.CraftSpider;
import org.bukkit.entity.Spider;


public class GoalModifiers {

    public static void modifySpiderGoal(Spider spider) {
        //SpiderAttackGoal extend MeleeAttackGoal
        net.minecraft.world.entity.monster.spider.Spider nmsSpider = ((CraftSpider) spider).getHandle();
        nmsSpider.removeAllGoals(goal -> goal instanceof MeleeAttackGoal);
        nmsSpider.getGoalSelector().addGoal(4, new CustomSpiderAttackGoal(nmsSpider));
    }

    private static class CustomSpiderAttackGoal extends MeleeAttackGoal {
        public CustomSpiderAttackGoal(net.minecraft.world.entity.monster.spider.Spider mob) {
            super(mob, (double)1.0F, true);
        }

        public boolean canUse() {
            net.minecraft.world.entity.Entity entity = mob.getFirstPassenger();
            boolean displayPassenger = entity != null && entity.getType() == EntityTypes.TEXT_DISPLAY;
            return super.canUse() && displayPassenger;
        }

        public boolean canContinueToUse() {
            float br = this.mob.getLightLevelDependentMagicValue();
            if (br >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
                this.mob.setTarget((LivingEntity)null);
                return false;
            } else {
                return super.canContinueToUse();
            }
        }
    }
}
