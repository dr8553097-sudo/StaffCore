package com.tuservidor.staffcore.storage;

public record StorageModeSelection(StorageMode requested, StorageMode active, String note) {

    public boolean fallbackInUse() {
        return requested != active;
    }
}
