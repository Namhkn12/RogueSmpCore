package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon_v2.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon_v2.repository.IPartyRepository;
import com.roguesmp.dungeon_v2.utils_.Log4Craft_;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

public class PartyRepository implements IPartyRepository {

    private final File dataFolder;
    private final Gson gson;
    private final Log4Craft_ logger;

    public PartyRepository(Plugin plugin, Gson gson, Log4Craft_ logger) {
        this.dataFolder = new File(
                plugin.getDataFolder(),
                DataFolderConfig.getPartyFolder()
        );
        this.gson = gson;
        this.logger = logger;
        if (!dataFolder.exists()) dataFolder.mkdirs();

    }

    @Override
    public void save(Party party) {
        File file = getFile(party.getPartyId());
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(party, writer);
        } catch (IOException e) {
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public void delete(UUID partyId) {
        File file = getFile(partyId);
        if (file.exists() && !file.delete()) {
            throw new DataDeleteException(file.getName(), null);
        }
    }

    @Override
    public Collection<Party> loadAll() {
        File[] files = dataFolder.listFiles(
                (dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE)
        );
        if (files == null) return Collections.emptyList();

        List<Party> result = new ArrayList<>();
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                Party party = gson.fromJson(reader, Party.class);
                if (party != null) result.add(party);
            } catch (IOException e) {
                logger.fire(this.getClass(), "Failed to load party file: " + file.getName(), e);
            }
        }
        return result;
    }

    private File getFile(UUID partyId) {
        return new File(dataFolder, DataFolderConfig.PARTY_FILE + partyId + DataFolderConfig.JSON_TYPE);
    }
}
