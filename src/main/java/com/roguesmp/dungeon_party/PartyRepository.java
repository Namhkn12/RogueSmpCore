package com.roguesmp.dungeon_party;

import com.google.gson.JsonSyntaxException;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PartyRepository {

    private final File dataFolder;

    public PartyRepository(RogueSmpCore plugin) {
        this.dataFolder = new File(plugin.getDataFolder(), "dungeon_v2/runtime/party");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public void save(Party party) {
        File file = getFile(party.getPartyId());
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            Utils.GSON.toJson(party, writer);
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to save party file: {}", file.getName(), e);
        }
    }

    public void delete(String partyId) {
        File file = getFile(partyId);
        if (file.exists() && !file.delete()) {
            RogueSmpCore.LOGGER.error("Failed to delete party file: {}", file.getName());
        }
    }

    public List<Party> loadAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        List<Party> result = new ArrayList<>();
        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Party party = Utils.GSON.fromJson(reader, Party.class);
                if (party != null) result.add(party);
            } catch (JsonSyntaxException | IOException e) {
                RogueSmpCore.LOGGER.error("Corrupt party file, skipping: {}", file.getName(), e);
                file.renameTo(new File(file.getParent(), file.getName() + ".corrupt"));
            }
        }
        return result;
    }

    private File getFile(String partyId) {
        return new File(dataFolder, "party" + partyId + ".json");
    }
}
