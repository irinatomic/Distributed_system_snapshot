package com.kids.cli.command;

import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BitcakeInfoCommand implements CLICommand {

	private final SnapshotCollector collector;
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("START BITCAKE INFO");
		collector.startCollecting();
	}
}
