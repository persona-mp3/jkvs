package jkvs.lib;

import java.io.*;
import java.nio.file.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jkvs.Std;

public class JKVStore {
	private HashMap<String, Long> memoryIndex;
	private final Path LOG_DIR = Paths.get("logs");
	private final Path INDEX_FILE = LOG_DIR.resolve("index.txt");
	private final String INDEX_FILE_DELIM = " ";
	private final Path WAL_FILE = LOG_DIR.resolve("log.wal");

	/// Usage: jkvs GET <KEY>
	public static final String GET_COMMAND = "get";

	/// Usage: jkvs SET <KEY> <VALUE>
	public static final String SET_COMMAND = "set";

	/// Usage: jkvs RM <KEY>
	public static final String REMOVE_COMMAND = "rm";

	/// Usage: jkvs -V
	public static final String VERSION_COMMAND = "-V";

	Std std = new Std();
	KVLib kvlib = new KVLib();

	public void init() throws IOException {
		this.memoryIndex = new HashMap<>();
		try {
			if (Files.exists(LOG_DIR)
					&& Files.exists(INDEX_FILE)
					&& Files.exists(WAL_FILE)) {
				resumeStore();
				System.out.flush();
				return;

			}

			std.println("creating jkvs log directories...");

			Files.createDirectories(LOG_DIR);
		} catch (Exception err) {
			throw new RuntimeException(err);
		}

	}

	private void resumeStore() throws IOException {
		try {
			std.println("Resuming jkvs store...");
			memoryIndex = kvlib.rebuild_index(INDEX_FILE, INDEX_FILE_DELIM);
			std.println("Rebuilding index logs...\n\n");
		} catch (Exception err) {
			std.eprintln("KVStore::resumeStore:: Unexpected error occured");
			throw new RuntimeException(err);
		}

	}

	public String set(String key, String value) throws IOException {
		try {
			long log_pointer = kvlib.append_to_log(WAL_FILE, SET_COMMAND, key, value);
			kvlib.append_to_index(INDEX_FILE, key, log_pointer);
			memoryIndex.put(key, log_pointer);
			return value;

		} catch (Exception err) {
			std.eprintln("KVS::set:: Unexpected error");
			throw new RuntimeException(err);
		}
	}

	public String get(String key) throws IOException {
		if (!memoryIndex.containsKey(key)) {
			std.printf("%s not found\n", key);
			return null;
		}

		long logPointer = memoryIndex.get(key);
		// Open the .wal file to read it
		try (
				RandomAccessFile raf = new RandomAccessFile(WAL_FILE.toString(), "r")) {
			raf.seek(logPointer);

			String record = raf.readLine();

			if (record.contains(REMOVE_COMMAND)) {
				std.println("key not found");
				return null;
			}

			String[] parsedLog = record.replaceAll("$\r\n", "").split(" ");

			if (parsedLog.length < 4) {
				std.eprintf("log record parsed to array has unexpected format: %d\n ", parsedLog.length);
				std.eprintf("Log: %s\n", record);
				for (String value : parsedLog) {
					std.eprintf(" %s\n", value);
				}
				throw new RuntimeException("KVStore::get::Unexpected log format");
			}

			String value = String.join(" ", Arrays.copyOfRange(parsedLog, 2, parsedLog.length - 1));

			return value.replaceAll("\"", "");

		} catch (IOException err) {
			std.println("KVStore::get::IOException occured");
			throw new RuntimeException(err);
		} catch (Exception err) {
			std.println("KVStore::get::Unexpected errror occured");
			throw new RuntimeException(err);
		}

	}

	public void remove(String key) throws IOException {
		try {
			if (!memoryIndex.containsKey(key)) {
				std.println("key not found");
				return;
			}

			long log_pointer = kvlib.append_to_log(WAL_FILE, REMOVE_COMMAND, key, "");
			kvlib.append_to_index(INDEX_FILE, key, log_pointer);

			memoryIndex.put(key, log_pointer);
		} catch (Exception err) {
			std.eprintln("JKVS::remove:: Unexpected error");
			throw new RuntimeException(err);
		}
	}
}
