package com.kids.servent.snapshot.collector;

import com.kids.servent.bitcake.AVBitcakeManager;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.bitcake.CCBitcakeManager;
import com.kids.servent.config.AppConfig;
import com.kids.servent.bitcake.ABBitcakeManager;
import com.kids.servent.snapshot.SnapshotType;
import com.kids.servent.snapshot.strategy.ABSnapshotStrategy;
import com.kids.servent.snapshot.strategy.AVSnapshotStrategy;
import com.kids.servent.snapshot.strategy.CCSnapshotStrategy;
import com.kids.servent.snapshot.strategy.SnapshotStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The snapshot collection consists of three main stages:
 * <ol>
 *   <li>Sending snapshot request messages to neighboring nodes.</li>
 *   <li>Waiting for snapshot responses.</li>
 *   <li>Aggregating and printing the snapshot results.</li>
 * </ol>
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	private final AtomicBoolean collecting = new AtomicBoolean(false);
	private SnapshotStrategy snapshotStrategy;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		switch(snapshotType) {
			case COORD_CHECKPOINT -> this.snapshotStrategy = new CCSnapshotStrategy(
					(CCBitcakeManager) BitcakeManagerInstance.getInstance()
			);
			case ACHARYA_BADRINATH -> this.snapshotStrategy = new ABSnapshotStrategy(
					(ABBitcakeManager) BitcakeManagerInstance.getInstance()
			);
			case ALAGAR_VENKATESAN -> this.snapshotStrategy = new AVSnapshotStrategy(
					(AVBitcakeManager) BitcakeManagerInstance.getInstance()
			);
			case NONE -> this.snapshotStrategy = null;
		}
	}

	@Override
	public void run() {
		while(true) {
			
			// Not collecting yet - just sleep until we start actual work, or finish
			while (!collecting.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!working) return;
			}

			snapshotStrategy.initiateSnapshot();

			AppConfig.timestampedStandardPrint("START SNAPSHOT");
			while (!snapshotStrategy.isSnapshotComplete()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!working) return;
			}

			AppConfig.timestampedStandardPrint("END SNAPSHOT");
			snapshotStrategy.processSnapshotEnding();
			collecting.set(false);
		}
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}

	@Override
	public SnapshotStrategy getSnapshotStrategy() {
		return snapshotStrategy;
	}

	@Override
	public void stop() {
		working = false;
	}

}
