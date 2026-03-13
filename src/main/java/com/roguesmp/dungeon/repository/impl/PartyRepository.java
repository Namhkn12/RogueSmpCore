package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.adapter.UUIDTypeAdapter;
import com.roguesmp.dungeon.constraint.FolderConfig;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.repository.IPartyRepository;

import java.io.*;
import java.util.*;

public class PartyRepository implements IPartyRepository {

    private final File partyFolder;
    private final Gson gson;

    public PartyRepository() {
        this.partyFolder = new File(
                RogueSmpCore.getInstance().getDataFolder(),
                FolderConfig.getPartyFolder()
        );
        this.partyFolder.mkdirs();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .setPrettyPrinting()
                .create();
    }

    @Override
    public void save(Party party) {
        File file = getFile(party.getPartyId());
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(party, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID partyId) {
        File file = getFile(partyId);
        if (file.exists()) file.delete();
    }

    @Override
    public Collection<Party> loadAll() {
        File[] files = partyFolder.listFiles(
                (dir, name) -> name.endsWith(FolderConfig.JSON_TYPE)
        );
        if (files == null) return Collections.emptyList();

        List<Party> result = new ArrayList<>();
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                Party party = gson.fromJson(reader, Party.class);
                if (party != null) result.add(party);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private File getFile(UUID partyId) {
        return new File(partyFolder, partyId + FolderConfig.JSON_TYPE);
    }
}
