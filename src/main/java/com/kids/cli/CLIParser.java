package com.kids.cli;

import com.kids.communication.message.util.FifoMessageSender;
import com.kids.servent.config.AppConfig;
import com.kids.servent.Cancellable;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import com.kids.cli.command.*;
import com.kids.communication.SimpleServentListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A simple CLI parser. Each command has a name and arbitrary arguments.
 * <p>
 * 
 * <ul>
 * <li><code>info</code> - prints information about the current node</li>
 * <li><code>pause [ms]</code> - pauses execution given number of ms - useful when scripting</li>
 * <li><code>ping [id]</code> - sends a PING message to node [id] </li>
 * <li><code>broadcast [text]</code> - broadcasts the given text to all nodes</li>
 * <li><code>causal_broadcast [text]</code> - causally broadcasts the given text to all nodes</li>
 * <li><code>print_causal</code> - prints all received causal broadcast messages</li>
 * <li><code>stop</code> - stops the servent and program finishes</li>
 * </ul>
 */
public class CLIParser implements Runnable, Cancellable {

	private volatile boolean working = true;
	private final List<CLICommand> commandList;
	
	public CLIParser(SimpleServentListener listener, SnapshotCollector snapshotCollector, List<FifoMessageSender> senderThreads) {
		this.commandList = new ArrayList<>();
		
		commandList.add(new InfoCommand());
		commandList.add(new PauseCommand());
		commandList.add(new TransactionBurstCommand());
		commandList.add(new BitcakeInfoCommand(snapshotCollector));
		commandList.add(new StopCommand(this, listener, snapshotCollector, senderThreads));
	}
	
	@Override
	public void run() {
		Scanner sc = new Scanner(System.in);
		
		while (working) {
			String commandLine = sc.nextLine();
			int spacePos = commandLine.indexOf(" ");
			String commandName;
			String commandArgs = null;

			if (spacePos != -1) {
				commandName = commandLine.substring(0, spacePos);
				commandArgs = commandLine.substring(spacePos+1);
			} else {
				commandName = commandLine;
			}
			
			boolean found = false;
			
			for (CLICommand cliCommand : commandList) {
				if (cliCommand.commandName().equals(commandName)) {
					cliCommand.execute(commandArgs);
					found = true;
					break;
				}
			}
			
			if (!found) {
				AppConfig.timestampedErrorPrint("Unknown command: " + commandName);
			}
		}
		
		sc.close();
	}
	
	@Override
	public void stop() {
		this.working = false;
		
	}
}
