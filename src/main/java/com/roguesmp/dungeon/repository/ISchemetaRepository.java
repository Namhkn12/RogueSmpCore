package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.definition.Schemeta;

import java.util.List;
import java.util.Optional;

public interface ISchemetaRepository {
    List<Schemeta> loadAll();
    Optional<Schemeta> findById(String id);
    void save(Schemeta schemeta);
    void delete(String id);
    boolean exists(String id);
}
