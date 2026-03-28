package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.adapter.UUIDTypeAdapter;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon.repository.IPartyRepository;
import com.roguesmp.dungeon.utils.Log4Craft;

import java.io.*;
import java.util.*;

public class PartyRepository implements IPartyRepository {

    private final File partyFolder;
    private final Gson gson;

    public PartyRepository(Gson gson) {
        this.partyFolder = new File(
                RogueSmpCore.getInstance().getDataFolder(),
                DataConfig.getPartyFolder()
        );
        this.partyFolder.mkdirs();
        this.gson = gson;
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
        File[] files = partyFolder.listFiles(
                (dir, name) -> name.endsWith(DataConfig.JSON_TYPE)
        );
        if (files == null) return Collections.emptyList();

        List<Party> result = new ArrayList<>();
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                Party party = gson.fromJson(reader, Party.class);
                if (party != null) result.add(party);
            } catch (IOException e) {
                Log4Craft.fire("Failed to load party file: " + file.getName(), e);
            }
        }
        return result;
    }

    private File getFile(UUID partyId) {
        return new File(partyFolder, DataConfig.PARTY_FILE + partyId + DataConfig.JSON_TYPE);
    }
}
