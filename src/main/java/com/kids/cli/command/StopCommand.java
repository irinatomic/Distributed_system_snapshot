package com.kids.cli.command;

import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import com.kids.cli.CLIParser;
import com.kids.communication.SimpleServentListener;

public class StopCommand implements CLICommand {

	private final CLIParser parser;
	private final SimpleServentListener listener;
	private final SnapshotCollector snapshotCollector;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener, SnapshotCollector snapshotCollector) {
		this.parser = parser;
		this.listener = listener;
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		snapshotCollector.stop();
	}
}
