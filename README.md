# Distributed System: Global Snapshot Algorithms
This is a 2nd homework for a University course on Concurrent and Distributed Systems. 

Each node holds a certain amount of bitcakes and engage in transactions that change their internal state. CLI Commands:
- `info` – Print this node's ID and neighbors.
- `pause <ms>` – Pause execution for given milliseconds.
- `transaction_burst` – Spawn 5 threads, each sending 5 random transactions (1–5 bitcakes) to other nodes.
- `stop` – Gracefully stop the node.
- `bitcake_info` – Initiates the global snapshot.

Algorithms:
1. **Coordinated Checkpoint** – Custom algorithm
2. **Acharya-Badrinath** – Lazy evaluation using vector clocks.
3. **Alagar-Venkatesan** – On-demand snapshot with control messages.

|                         | Coordinated Checkpoint | Acharya-Badrinath                   | Alagar-Venkatesan                   |
|-------------------------|------------------------|-------------------------------------|-------------------------------------|
| FIFO system             | Yes                    | No                                  | No                                  |
| Snapshot included       | Amount of bitcakes     | Amount of bitcakes + channels state | Amount of bitcakes + channels state |
| Who prints the snapshot | Every node             | Node initiator                      | Every node                          |

## Coordinated Checkpoint
**Each node keeps:**
- Number of bitcakes

**Message Types:**
- `TRANSACTION`: number of bitcakes
- `SNAPSHOT_REQ`: node id of initiator
- `ACK`: state of node
- `RESUME`: empty message

**Initiator's Perspective**
1. Sends a `SNAPSHOT_REQ` message and records its state
2. Waits for `ACK` messages from all other nodes. White waiting:
    - **Receiving**: puts incoming messages in a buffer
    - **Sending**: pauses sending
3. Sends a `RESUME` message to everyone 
4. Prints all the states

**Non-initiator's Perspective**
1. Upon receiving a `SNAPSHOT_REQ` message:
   - Sends an `ACK` message to the initiator with its state
   - Forwards the snapshot request to all neighbours
2. Waits for the `RESUME` message from the initiator. While waiting
    - **Receiving**: buffers all incoming messages
    - **Sending**: pauses sending

## Acharya-Badrinath
**Each node keeps:**
- Number of bitcakes
- Its vector clock
- `sentHistory` = list of <transaction_message>
- `recdHistory` = list of <transaction_message>

**Message Types:**
- `TRANSACTION`: number of bitcakes, sender’s vector clock
- `SNAPSHOT_REQ`: sender’s vector clock
- `SNAPSHOT_REPLY`: nodeId, number of bitcakes, sent, recd

**Initiator's Perspective**
1. The `bitcake_info` command is called
2. It sends a `SNAPSHOT_REQ` message with its vector clock `V` (via causal broadcast)
3. Records its local state (bitcakes, sent/recd history)
4. Waits for `SNAPSHOT_REPLY` messages from all other nodes. During this:
   - **Receiving**: records incoming messages where timestamp ≤ `V`
   - **Sending**: pauses further message sending
5. Once all replies are received, it calculates the channel states: 
   - `ChannelState(pj→pi) = (number of bitcakes SENTj[i]) - (number of bitcakes RECDi[j])`
6. Prints the snapshot

**Non-initiator's Perspective**
1. Receives `SNAPSHOT_REQ` message
2. Sends `SNAPSHOT_REPLY` message with its local state (bitcakes, sent and recd history)

## Alagar-Venkatesan 
**Each node keeps:**
- Number of bitcakes
- Its vector clock

**Message Types:**
- `TRANSACTION`: number of bitcakes, sender’s vector clock
- `SNAPSHOT_REQ`: sender’s vector clock
- `DONE`: empty
- `TERMINATE`: empty  

**Initiator's Perspective**
1. The `bitcake_info` command is called
2. Sends a `SNAPSHOT_REQ` message with its vector clock `V` (via causal broadcast)
3. Records its own state (bitcake count)
4. Wait for `DONE` messages from all. While waiting:
   - **Receiving**: records incoming messages where timestamp ≤ `V`
   - **Sending**: continues as normal
5. Sends `TERMINATE` message (broadcast)
6. Prints its state and received messages between `DONE` and `TERMINATE` 

**Non-initiator's Perspective**
1. Upon receiving `SNAPSHOT_REQ`:
   - Records its own state (bitcake count)
   - Sends a `DONE` (empty) message to the initiator
2. Wait for the `TERMINATE` message. White waiting:
   - **Receiving**: records incoming messages where timestamp ≤ `V`
   - **Sending**: continues as normal
3. Prints its state and received messages between `DONE` and `TERMINATE`
