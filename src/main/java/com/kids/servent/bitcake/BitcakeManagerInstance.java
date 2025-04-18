package com.kids.servent.bitcake;

import com.kids.servent.snapshot.SnapshotType;

public class BitcakeManagerInstance {

    private static BitcakeManager instance;

    private BitcakeManagerInstance() { }

    public static void initialize(SnapshotType snapshotType) {
        if (instance != null) {
            throw new IllegalStateException("BitcakeManager already exists");
        }

        switch (snapshotType) {
            case ACHARYA_BADRINATH:
                instance = new ABBitcakeManager(); break;
            default:
                throw new IllegalArgumentException("Unsupported snapshot type: " + snapshotType);
        }
    }

    public static BitcakeManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BitcakeManager is not initialized.");
        }
        return instance;
    }
}
