package com.kids.servent.snapshot.strategy;

public interface SnapshotStrategy {

    void initiateSnapshot();
    boolean isSnapshotComplete();
    void processSnapshotEnding();
}
