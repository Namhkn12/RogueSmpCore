package com.roguesmp.npc;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import com.roguesmp.registry.npc.NpcRegistry;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manage spawned npc's lifecycle
 */
public class NpcManager {

    private static NpcManager INSTANCE;

    private final Map<UUID, SmpNpc> npcs = new HashMap<>();

    /**
     * Wrap the entity and process its entity data
     * @return null if entity is already wrapped, or if it is not an npc
     */
    public @Nullable SmpNpc wrap(Entity entity) {
        if (npcs.containsKey(entity.getUniqueId())) return null; //If already registered, won't process entity
        String id = entity.getPersistentDataContainer().get(Keys.NPC_ID, PersistentDataType.STRING);
        if (id == null) return null;
        BaseNpc baseNpc = NpcRegistry.getInstance().getBase(id);
        if (baseNpc == null) return null;
        return new SmpNpc(baseNpc, entity);
    }

    public void register(SmpNpc npc) {
        npcs.put(npc.getUuid(), npc);
    }

    public void remove(SmpNpc npc) {
        remove(npc.getUuid());
    }

    public void remove(UUID uuid) {
        npcs.remove(uuid);
    }

    public @Nullable SmpNpc getNpc(Entity entity) {
        return getNpc(entity.getUniqueId());
    }

    public @Nullable SmpNpc getNpc(UUID uuid) {
        return npcs.get(uuid);
    }

    public static void init() {
        INSTANCE = new NpcManager();
    }

    public static NpcManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NpcManager is null!");
        }
        return INSTANCE;
    }

    public void registerCommand() {
        new CommandAPICommand("smpnpc")
                .withArguments(new StringArgument("id"))
                .executesPlayer((sender, args) -> {
                    BaseNpc npc = NpcRegistry.getInstance().getBase((String) args.get("id"));
                    if (npc != null) {
                        npc.spawn(sender.getLocation());
                        sender.sendMessage(Component.text("Spawned NPC", NamedTextColor.GREEN));
                    }
                }).register(RogueSmpCore.getInstance());
    }
}
