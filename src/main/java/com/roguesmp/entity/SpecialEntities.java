package com.roguesmp.entity;

import com.roguesmp.entity.boss.blackveinhorror.BlackveinHorror;
import com.roguesmp.entity.boss.cursedforeman.CursedForeman;
import com.roguesmp.entity.boss.hollowheart.CrystalMinion;
import com.roguesmp.entity.boss.hollowheart.HollowheartCrystal;
import com.roguesmp.entity.boss.veinstalker.VeinStalker;
import com.roguesmp.registry.Registries;

/**
 * Every special (boss/minion) {@link EntityFactory}. Each constant registers itself into
 * {@link Registries#ENTITY_FACTORY} as it's initialized - call {@link #loadClass()} to force that
 * to happen.
 */
public class SpecialEntities {

//    public static final EntityFactory<PrimordialSlime> PRIMORDIAL_SLIME = register("primordial_slime", PrimordialSlime::new);
//    public static final EntityFactory<HellKnightCompanion> HELL_KNIGHT_COMPANION = register(HellKnightCompanion.ID, HellKnightCompanion::new);
//    public static final EntityFactory<HellKnightHordes> HELL_KNIGHT_HORDES = register("hell_knight_hordes", HellKnightHordes::new);
//    public static final EntityFactory<HellKnightMinionMelee> HELL_KNIGHT_MINION_MELEE = register("hell_knight_minion_melee", HellKnightMinionMelee::new);
//    public static final EntityFactory<HellKnightMinionRanged> HELL_KNIGHT_MINION_RANGED = register("hell_knight_minion_ranged", HellKnightMinionRanged::new);
//    public static final EntityFactory<HellKnightEvoker> HELL_KNIGHT_EVOKER = register("hell_knight_evoker", HellKnightEvoker::new);
//    public static final EntityFactory<HellKnightStray> HELL_KNIGHT_STRAY = register("hell_knight_stray", HellKnightStray::new);
//    public static final EntityFactory<HellKnightBlaze> HELL_KNIGHT_BLAZE = register("hell_knight_blaze", HellKnightBlaze::new);
//    public static final EntityFactory<HellKnightGolem> HELL_KNIGHT_GOLEM = register("hell_knight_golem", HellKnightGolem::new);

    public static final EntityFactory<CursedForeman> CURSED_FOREMAN = register(CursedForeman.ID, CursedForeman::new);
    public static final EntityFactory<VeinStalker> VEIN_STALKER = register(VeinStalker.ID, VeinStalker::new);
    public static final EntityFactory<BlackveinHorror> BLACKVEIN_HORROR = register(BlackveinHorror.ID, BlackveinHorror::new);
    public static final EntityFactory<HollowheartCrystal> HOLLOWHEART_CRYSTAL = register(HollowheartCrystal.ID, HollowheartCrystal::new);
    public static final EntityFactory<CrystalMinion> CRYSTAL_SHARD = register(CrystalMinion.SHARD_ID, CrystalMinion::shard);
    public static final EntityFactory<CrystalMinion> CRYSTAL_ORB = register(CrystalMinion.ORB_ID, CrystalMinion::orb);
    public static final EntityFactory<CrystalMinion> CRYSTAL_CONDUIT = register(CrystalMinion.CONDUIT_ID, CrystalMinion::conduit);

    public static void loadClass() {

    }

    private static <T extends SmpEntity> EntityFactory<T> register(String id, EntityFactory<T> factory) {
        return Registries.ENTITY_FACTORY.register(id, factory);
    }
}
