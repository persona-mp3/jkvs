package github.persona_mp3.lib;

import github.persona_mp3.Std;
import github.persona_mp3.lib.types.WriteRequest;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import java.nio.file.Paths;
import java.nio.file.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class JKVStore {
	Std std = new Std();

	public String VERSION = "0.0.1";

	/// Usage: jkvs get <key>
	public static final String GET_COMMAND = "get";

	/// Usage: jkvs set <key> <value>
	public static final String SET_COMMAND = "set";

	/// Usage: jkvs -V
	public static final String VERSION_COMMAND = "-V";

	/// Usage: jkvs rm <key>
	public static final String REMOVE_COMMAND = "rm";

	private ConcurrentHashMap<String, Long> memoryIndex = new ConcurrentHashMap<>();
	private BlockingQueue<WriteRequest> queue;
	private ExecutorService writerThread;

	// public JKVStore(BlockingQueue<WriteRequest>) {
	// }

	private final Path LOG_DIR = Paths.get("logs");
	private final Path LOG_FILE = LOG_DIR.resolve("log.wal");
	private final Path INDEX_FILE = LOG_DIR.resolve("index");
	private long MAX_SIZE_MB = 1 * 1024;

	private Logger logger = LogManager.getLogger(JKVStore.class);

	JKVLib jkvlib = new JKVLib();

	public void init() throws IOException {
		if (Files.exists(LOG_DIR)
				&& Files.exists(LOG_FILE)
				&& Files.exists(INDEX_FILE)) {
			rebuildStore();
			return;
		} else if (!Files.exists(LOG_DIR)) {
			std.println("Creating log directories");
			Files.createDirectory(LOG_DIR);
		}

	}

	/**
	 * Rebuilds the database, by reading the the index file into the in-memory
	 * hashmap, containting keys and log-pointer offsets<br/>
	 *
	 *
	 * Before each rebuild, it checks the size of the log file, if over a certain
	 * threshold, ie 1MB, it compacts the logs, saves the old logs to the
	 * <bold>past_logs</bold> directory
	 *
	 */
	private void rebuildStore() throws IOException {
		logger.info("Rebuliding logs");

		long fileSize = LOG_FILE.toFile().length();
		if (fileSize >= MAX_SIZE_MB) {
			logger.info("Log compaction triggered, log-size: {}", fileSize);
			jkvlib.compactLogs(LOG_FILE, INDEX_FILE);
		}

		memoryIndex = jkvlib.rebuildIndex(INDEX_FILE, " ");
		logger.info("Logs rebuilt successfully. IndexSize={}", memoryIndex.size());
	}

	public String set(String key, String value) throws IOException {
		logger.debug("set-command: {}::{}", key, value);
		long logPointer = jkvlib.appendToLogFile(LOG_FILE, SET_COMMAND, key, value);
		jkvlib.appendToIndexFile(INDEX_FILE, key, logPointer);
		memoryIndex.put(key, logPointer);
		return value;
	}

	public String get(String key) throws IOException {
		logger.debug("get-command: {}, inMemSize: {}", key, memoryIndex.size());
		if (!memoryIndex.containsKey(key)) {
			logger.debug("memory index does not contain key={}", key);
			return null;
		}

		long logPointer = memoryIndex.get(key);
		logger.debug("log_pointer of key = {}, logPointer={}", key, logPointer);

		RandomAccessFile raf = null;

		try {
			raf = new RandomAccessFile(LOG_FILE.toString(), "r");
			raf.seek(logPointer);
			String record = raf.readLine();
			// could have been a nasty but with record.contains(RM_COMMAND)
			// if you did set env terminal, this record would be deleted, because terminal
			// contains rm
			if (record.split(" ")[0].equals(REMOVE_COMMAND)) {
				std.printf("%s not found\n", key);
				logger.debug("key {} contains tombstone rm", record);
				return null;
			}

			String[] parsedLog = record.replaceAll("$\r\n", "").split(" ");

			if (parsedLog.length < 4) {
				logger.error("log record parsed to array has unexpected format: {} ", parsedLog.length);
				logger.error("Log: {}", record);
				for (String log : parsedLog) {
					logger.error("{}", log);
				}
				throw new RuntimeException("JKVStore.get: Unexpected log format");
			}

			String value = String.join(" ", Arrays.copyOfRange(parsedLog, 2, parsedLog.length - 1));
			logger.debug("key: {}, value: {}", key, value);
			return value.replaceAll("\"", "");

		} finally {
			if (raf != null) {
				raf.close();
			}
		}

	}

	public String remove(String key) throws IOException {
		if (!memoryIndex.containsKey(key)) {
			std.printf("%s not found\n", key);
			return null;
		}

		long logPointer = jkvlib.appendToLogFile(LOG_FILE, REMOVE_COMMAND, key, "");
		jkvlib.appendToIndexFile(INDEX_FILE, key, logPointer);

		memoryIndex.put(key, logPointer);
		return key;
	}

	public void async_init(BlockingQueue<WriteRequest> queue, ExecutorService writerThread) throws IOException {
		logger.info("async_init:: starting");
		this.queue = queue;
		this.writerThread = writerThread;
		init();
		async_writer();
	}

	// todo: remove it from the core engine or make it into a seperate module so the
	// thread lives, THis is just to see if we can impl the single-writer
	// as long as the server
	//
	// Problem, theres no way of communicating the result back to the caller thread
	public void async_writer() throws IOException {
		logger.info("async writer active");
		writerThread.submit(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					WriteRequest req = queue.take();
					if (req.command.equals(SET_COMMAND)) {
						logger.info("async_writer:: writing set command {}:{}", req.key, req.value);
						req.result.complete(set(req.key, req.value));
						// req.result.complete("set_key_resolved");
					} else if (req.command.equals(REMOVE_COMMAND)) {
						logger.info("async_writer:: writing rming command {}:{}", req.key);
						req.result.complete(remove(req.key));
						// req.result.complete("remove_key_resolved");
					}
				} catch (InterruptedException err) {
					logger.error("writer interrupted");
					return;
				} catch (Exception err) {
					logger.error("writer error. Reason: {}", err.getMessage());
					err.printStackTrace();
					return;
				}
			}
		});
	}

	public void dropItem(WriteRequest req) {
		// todo: use timeouts
		// And we cant call async_writer() here again, because why? we'd have two thread
		// instances runnning
		// async_writer is called during startup
		queue.offer(req);
	}

}
