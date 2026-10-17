package com.roguesmp.block.storage;

/** Implemented by an {@code SmpBlock} holding items - its storage is then saved, dropped on break and ticked for side transfers automatically. */
public interface ItemStorageHolder {

    ItemStorage itemStorage();
}
