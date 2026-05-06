package jkvs.lib;

import java.util.HashMap;
import java.nio.file.*;
import java.io.*;
import java.io.IOException;
import jkvs.Std;
import jkvs.utils.*;

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

		long log_pointer = wal_file.length();
		wal_file.seek(wal_file.length());

		// <set> <key> <value> $\r\n
		byte[] content = std.encoder(command, key, value);
		wal_file.write(content);

		wal_file.close();

		return log_pointer;

	}

	public void append_to_index(Path index_file, String key, long log_pointer) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(index_file.toString(), "rw");
		raf.seek(raf.length());

		byte[] content = String.format("%s %s\n", key, log_pointer).getBytes();
		raf.write(content);
		raf.close();
	}

	// Step 1. We need to rebuild the log file, by reading from the bottom,
	// and append each entry to a hashMap
	// Step 2. If an entry we find already exists backUp and we ignore it
	// Step 3 -> for (rm) commands,
	// Step 4 -> Write each entry to the new_log_file, and create a new index file
	// 1. Rebuild wal into map
	public void compact_log(Path src, Path dest) throws IOException {
		try (SimpleReversedReader srr = new SimpleReversedReader(src.toString())) {

			String log = null;
			HashMap<String, String> temp_log = new HashMap<>();

			while ((log = srr.readLine()) != null) {
				if (log.isEmpty() || log.isBlank()) {
					continue;
				}

				String[] parsed_logs = log.split(" ");

				// Yeah this isn't good, at somepoint using JSON as the codec would have been
				// way more easier
				// I wasn't sure JSON would be the best pick, but couldn't think of a better
				// encoding yet
				final int MIN_LENGTH_OF_PARSED_LOG = 4;

				// Throwing an error here might seem like overkill, but at some point
				// skipping past broken records means our database accuracy has dropped by over
				// 80%. And that is bad. This way, we could recover from the error, and defer
				// compacting and just continue with the database. We log out the error,
				// hopefully
				// someone sees it and fixes it
				if (parsed_logs.length < MIN_LENGTH_OF_PARSED_LOG) {
					throw new RuntimeException(String.format(
							"JKVSLib::compact_log:: Length of parsed log is less than the MIN %d\n, ",
							"%s\n %s\n %s\n %s\n",
							" Possible Reason",
							"1. Logs may have been corrupted",
							"2. Codec may have changed",
							"3. Distributed systems behaving like distributed systems (we don't know the reason)"));
				}

				// <cmd> <key> <value>?
				String key = parsed_logs[1];
				temp_log.putIfAbsent(key, log);
			}

			std.println("done compacting");
			std.println(temp_log);

			// Now we can iter through the map, and append each line to the src

			// Write compacted log to file
		} catch (Exception err) {
			std.eprintf("JKVS::comapact_log:: Unexpected error");
			throw new RuntimeException(err);
		}
	}

}
