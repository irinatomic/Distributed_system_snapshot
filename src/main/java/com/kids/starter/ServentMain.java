package com.kids.starter;

import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import com.kids.servent.snapshot.collector.SnapshotCollectorWorker;
import com.kids.servent.snapshot.SnapshotType;
import com.kids.cli.CLIParser;
import com.kids.communication.SimpleServentListener;
import com.kids.communication.message.util.MessageUtil;

/**
 * Describes the procedure for starting a single servent
 */
public class ServentMain {

	/**
	 * args[0] - path to servent list file
	 * args[1] - this servent's id
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
		}

		if (AppConfig.SNAPSHOT_TYPE == SnapshotType.NONE) {
			AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
			System.exit(0);
		}

		String serventListFile = args[0];
		AppConfig.readConfig(serventListFile);
		
		int serventId = getServentId(args);
		int portNumber = gerServentPortNumber(serventId);
		
		MessageUtil.initializePendingMessages();
		
		AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);

		// BitcakeManager
		BitcakeManagerInstance.initialize(AppConfig.SNAPSHOT_TYPE);

		// SnapshotCollector
		SnapshotCollector snapshotCollector = new SnapshotCollectorWorker(AppConfig.SNAPSHOT_TYPE);

		CausalBroadcast.injectSnapshotCollector(snapshotCollector);
		Thread snapshotCollectorThread = new Thread(snapshotCollector);
		snapshotCollectorThread.start();
		
		SimpleServentListener simpleListener = new SimpleServentListener();
		Thread listenerThread = new Thread(simpleListener);
		listenerThread.start();
		
		CLIParser cliParser = new CLIParser(simpleListener, snapshotCollector);
		Thread cliThread = new Thread(cliParser);
		cliThread.start();
	}

	private static int getServentId(String[] args) {
		int serventId = -1;

		try {
			serventId = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
			System.exit(0);
		}

		if (serventId >= AppConfig.getServentCount()) {
			AppConfig.timestampedErrorPrint("Invalid servent id provided");
			System.exit(0);
		}

		return serventId;
	}

	private static int gerServentPortNumber(int serventId) {
		int portNumber = -1;

		AppConfig.myServentInfo = AppConfig.getInfoById(serventId);
		try {
			portNumber = AppConfig.myServentInfo.listenerPort();

			if (portNumber < 1000 || portNumber > 2000) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
			System.exit(0);
		}

		return portNumber;
	}
}
