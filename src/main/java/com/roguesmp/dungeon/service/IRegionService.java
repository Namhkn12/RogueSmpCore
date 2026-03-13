package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Region;

import java.util.Optional;

public interface IRegionService {

    /**
     * Cung cấp 1 region available để tạo dungeon instance.
     * Nếu tất cả world đầy slot → tạo world mới rồi cấp region.
     * Trả empty nếu đạt MAX_WORLD_PER_SERVER và tất cả đều full.
     */
    Optional<Region> acquireRegion();

    /**
     * Trả region về pool sau khi dungeon kết thúc
     */
    void releaseRegion(Region region);

    /**
     * Ghi toàn bộ state xuống file — gọi khi server close
     */
    void saveDungeonRegion();

    void onServerStart();
}