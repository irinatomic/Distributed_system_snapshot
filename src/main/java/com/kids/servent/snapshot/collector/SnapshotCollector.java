package com.kids.servent.snapshot.collector;

import com.kids.servent.Cancellable;
import com.kids.servent.bitcake.BitcakeManager;
import com.kids.servent.snapshot.ABSnapshot;
import com.kids.servent.snapshot.strategy.SnapshotStrategy;

import java.util.Map;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	void startCollecting();

	// BitcakeManager getBitcakeManager();

	SnapshotStrategy getSnapshotStrategy();
}