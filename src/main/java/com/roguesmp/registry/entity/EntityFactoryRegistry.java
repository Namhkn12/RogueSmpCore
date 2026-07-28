package com.roguesmp.registry.entity;

import com.roguesmp.entity.EntityFactory;
import com.roguesmp.entity.boss.hellknight.HellKnight;
import com.roguesmp.entity.boss.hellknight.minion.*;
import com.roguesmp.entity.boss.hellknight.minion.companion.HellKnightCompanion;
import com.roguesmp.entity.boss.primordialslime.PrimordialSlime;
import com.roguesmp.registry.Registries;

public class EntityFactoryRegistry {

    public static void bootstrap() {
        register("primordial_slime", PrimordialSlime::new);

        register("hell_knight", HellKnight::new);
        register(HellKnightCompanion.ID, HellKnightCompanion::new);
        register("hell_knight_hordes", HellKnightHordes::new);
        register("hell_knight_minion_melee", HellKnightMinionMelee::new);
        register("hell_knight_minion_ranged", HellKnightMinionRanged::new);
        register("hell_knight_evoker", HellKnightEvoker::new);
        register("hell_knight_stray", HellKnightStray::new);
        register("hell_knight_blaze", HellKnightBlaze::new);
        register("hell_knight_golem", HellKnightGolem::new);
    }

    private static void register(String id, EntityFactory factory) {
        Registries.ENTITY_FACTORY.register(id, factory);
    }
}
