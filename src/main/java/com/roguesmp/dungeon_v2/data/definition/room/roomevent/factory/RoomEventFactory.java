package com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory;

import com.roguesmp.dungeon_v2.data.definition.room.roomevent.PersistableRoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.impl.*;

import java.util.Map;

public final class RoomEventFactory {

    private RoomEventFactory() {
    }

    public static RoomEvent create(RoomEventConfig config) {
        if (config == null || config.getType() == null || config.getType().isBlank()) {
            throw new IllegalArgumentException("Room event config type is missing.");
        }

        return switch (config.getType()) {

            case NoopRoomEvent.TYPE -> new NoopRoomEvent();

            case PartyStrengthBuffRoomEvent.TYPE -> {
                PartyStrengthBuffRoomEvent event = new PartyStrengthBuffRoomEvent();
                if (config.getParams().get("durationTicks") instanceof Number n) {
                    event.setDurationTicks(n.intValue());
                }
                if (config.getParams().get("amplifier") instanceof Number n) {
                    event.setAmplifier(n.intValue());
                }
                yield event;
            }

            case DarknessEyesEvent.TYPE -> {
                DarknessEyesEvent event = new DarknessEyesEvent();
                if (config.getParams().get("isDisrupt") instanceof Boolean b) {
                    event.setDisrupt(b);
                }
                yield event;
            }

            case CreepingDreadEvent.TYPE -> {
                CreepingDreadEvent event = new CreepingDreadEvent();

                if (config.getParams().get("maxTriggers") instanceof Number n) {
                    event.setMaxTriggers(n.intValue());
                }
                if (config.getParams().get("spawnChance") instanceof Number n) {
                    event.setSpawnChance(n.doubleValue());
                }
                if (config.getParams().get("minInterval") instanceof Number n) {
                    event.setMinInterval(n.intValue());
                }
                if (config.getParams().get("maxInterval") instanceof Number n) {
                    event.setMaxInterval(n.intValue());
                }

                yield event;
            }

            default -> throw new IllegalArgumentException("Unknown room event type: " + config.getType());
        };
    }

    public static RoomEvent restore(Map<String, Object> data) {
        String type = data.get("type") instanceof String value ? value : null;
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Missing room event type in serialized data.");
        }

        RoomEvent event = create(new RoomEventConfig(type, data));
        if (event instanceof PersistableRoomEvent persistable) {
            persistable.deserialize(data);
        }
        return event;
    }
}