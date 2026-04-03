package com.roguesmp.dungeon_v2.service;


import com.roguesmp.dungeon_v2.data.runtime.Region;
import java.util.Optional;

/**
 * Quản lý dữ liệu về thông tin thế giới và cấp phát các slot
 * dungeon bên trong thế giới
 * */
public interface IRegionService {

    /**
     * Cung cấp 1 region available để tạo dungeon instance.
     * Nếu tất cả world đầy slot → tạo world mới rồi cấp region.
     * Trả empty nếu đạt MAX_WORLD_PER_SERVER và tất cả các world đều full slot.
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

}