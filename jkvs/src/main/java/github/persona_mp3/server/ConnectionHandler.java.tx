package github.persona_mp3.server;

import github.persona_mp3.lib.protocol.Request;
import github.persona_mp3.lib.JKVStore;
import github.persona_mp3.lib.types.WriteRequest;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
	BlockingQueue<WriteRequest> queue;
	Socket conn;
	JKVStore store;
	private static Logger logger = LogManager.getLogger(ConnectionHandler.class);

	public ConnectionHandler(BlockingQueue<WriteRequest> queue, Socket conn) {
		this.queue = queue;
		this.conn = conn;
	}

	public void main() {
	}

	public String processRequest(Request req) throws Exception {
		if (req.command.equals(JKVStore.SET_COMMAND) || req.command.equals(JKVStore.REMOVE_COMMAND)) {
			WriteRequest wq = new WriteRequest(req.command, req.key, req.value);
			// todo: implement a poision_pill to tell the thread to stop reading
			// will need to do some sort of Future/await thing here, not sure yet?
			queue.offer(wq);
			store.dropItem(wq);
		} else if (req.command.equals(JKVStore.GET_COMMAND)) {
			return store.get(req.key);
		}

		return "";
	}

	@Override
	public void run() {
	}
}
