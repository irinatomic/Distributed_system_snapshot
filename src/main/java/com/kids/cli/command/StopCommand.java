package com.kids.cli.command;

import com.kids.communication.message.util.FifoMessageSender;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import com.kids.cli.CLIParser;
import com.kids.communication.SimpleServentListener;

import java.util.List;

public class StopCommand implements CLICommand {

	private final CLIParser parser;
	private final SimpleServentListener listener;
	private final SnapshotCollector snapshotCollector;
	private final List<FifoMessageSender> senderThreads;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener,
					   SnapshotCollector snapshotCollector,
					   List<FifoMessageSender> senderThreads) {
		this.parser = parser;
		this.listener = listener;
		this.snapshotCollector = snapshotCollector;
		this.senderThreads = senderThreads;
	}
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		parser.stop();
		listener.stop();
		for (FifoMessageSender senderWorker : senderThreads) {
			senderWorker.stop();
		}
		snapshotCollector.stop();
		AppConfig.timestampedStandardPrint("Stopping...");
	}
}
