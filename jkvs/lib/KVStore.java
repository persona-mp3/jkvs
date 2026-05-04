package jkvs.lib;

import java.io.*;
import java.nio.file.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import jkvs.Std;

public class KVStore {

	private HashMap<String, String> values;
	private final Path LOG_DIR = Paths.get("logs");
	private final Path INDEX_FILE = LOG_DIR.resolve("index.txt");
	private final Path WAL_FILE = LOG_DIR.resolve("log.wal");

	public static final String GET_COMMAND = "get";
	public static final String SET_COMMAND = "set";
	public static final String REMOVE_COMMAND = "rm";
	public static final String VERSION_COMMAND = "-V";

	Std std = new Std();
	KVLib kvlib = new KVLib();

	public void init() throws IOException {
		this.values = new HashMap<String, String>();

		std.println("initialising log directory..\n");
		Files.createDirectories(LOG_DIR);
	}

	public String set(String key, String value) throws IOException {
		// RandomAccessFile wal_file = new RandomAccessFile(WAL_FILE.toString(), "rw");
		//
		// // Start at the end of the file to append the new log
		// wal_file.seek(wal_file.length());
		//
		// // <set> <key> <value> $\r\n
		// byte[] content = std.encoder(SET_COMMAND, key, value);
		// wal_file.write(content);
		//
		// long log_pointer = wal_file.getFilePointer();
		// wal_file.close();
		long log_pointer = kvlib.append_to_log(WAL_FILE, SET_COMMAND, key, value);
		kvlib.append_to_index(INDEX_FILE, key, log_pointer);

		// Append to the index file <key> <log-pointer>
		// RandomAccessFile index_file = new RandomAccessFile(INDEX_FILE.toString(),
		// "rw");
		// index_file.seek(index_file.length());
		//
		// content = String.format("%s %s\n", key, log_pointer).getBytes();
		// index_file.write(content);
		// index_file.close();

		std.println("written to index file");
		return values.put(key, value);
	}

	/// Returns null when record for key is not found
	public String get(String key) {
		return values.get(key);
	}

	public String remove(String key) throws IOException {
		// Step 1. Rebuild index file to check if the key exists
		// Step 2. Append to wal file to record the command -> log_pointer
		// Step 3. Append to index file to record key and offset
		try {
			HashMap<String, Long> index = kvlib.rebuild_index(INDEX_FILE, " ");
			std.println("  rebuilt index -> ");
			std.println(index);

			if (!index.containsKey(key)) {
				std.printf("%s doesn't exist\n", key);
				return null;
			}

			long log_pointer = kvlib.append_to_log(WAL_FILE, REMOVE_COMMAND, key, "");
			kvlib.append_to_index(INDEX_FILE, REMOVE_COMMAND, log_pointer);

		} catch (Exception err) {
			throw new RuntimeException(err);
		}

		return values.remove(key);
	}

	public void debug() {
		for (String key : values.keySet()) {
			System.out.println("  kv_store debug");
			System.out.printf("   %s :: %s\n", key, values.get(key));
		}
	}
}
