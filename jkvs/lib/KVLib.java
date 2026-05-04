package jkvs.lib;

import java.util.HashMap;
import java.nio.file.*;
import java.io.*;
import java.io.IOException;
import jkvs.Std;

public class KVLib {

	Std std = new Std();

	/// For now, I'd want us to stream the whole file into memory, But later on I'd
	/// want to experiement with using it like a search, ie as we're reading each
	/// line we do a search on it, But this solely depends on the operation For
	/// example, a get command, might only see the first occurence of its key, which
	/// could've been updated 12 lines below it
	public HashMap<String, Long> rebuild_index(Path index_file, String delimiter) throws Exception {
		HashMap<String, Long> memory_index = new HashMap<>();

		try (BufferedReader br = Files.newBufferedReader(index_file)) {
			String log = "";
			while ((log = br.readLine()) != null) {

				String[] pair = log.split(delimiter);
				// More checks can be done here:
				// ie -> if any of the pair is empty
				if (pair.length != 2) {
					std.eprintf("Unexpected log format:: Expected two values in pair, got %d\n", pair.length);
					std.eprintln(pair);
					throw new RuntimeException("Unexpected log format");
				}

				String key = pair[0];
				Long log_pointer = Long.parseLong(pair[1]);

				memory_index.put(key, log_pointer);
			}

		} catch (Exception err) {
			std.eprintln("Error:: could not rebuild_index");
			throw err;
		}
		return memory_index;
	}

	/// Appends to the wal file provided and returns the file offset or log pointer
	/// corresponding to the last written position
	public long append_to_log(Path wal, String command, String key, String value) throws IOException {
		RandomAccessFile wal_file = new RandomAccessFile(wal.toString(), "rw");
		// Start at the end of the file to append the new log
		wal_file.seek(wal_file.length());

		// <set> <key> <value> $\r\n
		byte[] content = std.encoder(command, key, value);
		wal_file.write(content);

		long log_pointer = wal_file.getFilePointer();
		wal_file.close();
		std.println("written to log file");

		return log_pointer;

	}

	public void append_to_index(Path index_file, String key, long log_pointer) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(index_file.toString(), "rw");
		raf.seek(raf.length());

		byte[] content = String.format("%s %s\n", key, log_pointer).getBytes();
		raf.write(content);
		raf.close();
		std.printf("written to %s sucessfully\n", index_file.toString());
	}
}
