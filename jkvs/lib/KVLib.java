package jkvs.lib;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.nio.file.*;
import java.io.*;

import jkvs.Std;
import jkvs.utils.*;

public class KVLib {
	Std std = new Std();

	private Path PAST_LOGS_DIR = Path.of("past_logs");

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

	public void log_compaction(Path src, Path src_index_file, Path dest) throws IOException {
		// Step 1. Rebuild logs and eliminate duplicates using a hashmap and reading in
		// reverse
		final int MIN_LENGTH_OF_PARSED_LOG = 4;

		std.debug("dest_file -> " + src.toString());
		HashMap<String, String> compacted_logs = new HashMap<>();
		String log = "";
		std.debug("startinng loop -> ");
		if (Files.exists(src)) {
			std.debug("normal file exists ");
		} else {
			std.debug("normal file  doesnt exists ");
		}
		try (
				SimpleReversedReader srr = new SimpleReversedReader(src.toString());) {

			std.debug("constructed srr ");
			while ((log = srr.readLine()) != null) {
				if (log.isEmpty() || log.isBlank()) {
					std.debug("empty log");
					continue;
				}

				String[] parsed_log = log.split(" ");
				if (parsed_log.length < MIN_LENGTH_OF_PARSED_LOG) {
					throw new RuntimeException(String.format(
							"JKVSLib::compact_log:: Length of parsed log is less than the MIN %d\n, ",
							"%s\n %s\n %s\n %s\n",
							" Possible Reason",
							"1. Logs may have been corrupted",
							"2. Codec may have changed",
							"3. Distributed systems behaving like distributed systems (we don't know the reason)"));
				}

				String key = parsed_log[1];
				compacted_logs.putIfAbsent(key, log);
			}

			std.debug("done compacting");
			std.debug(compacted_logs);

			// Write each entry to the dest file, and create new index
			// So on renaming, we should a folder called, past_logs/
			// and inside past_logs/ we can have subdirs that contain previous logs
			// past_logs/
			// log_2008-09-13/ - log.wal, index.txt
			// log_2008-09-21/ - log.wal, index.txt
			// log_2008-10-01/ - log.wal, index.txt

			if (!Files.exists(PAST_LOGS_DIR)) {
				Files.createDirectory(PAST_LOGS_DIR);
				std.println("creating past_logs_dir to store original logs...");
			}

			// using ISO Date format with time
			LocalDateTime current_date = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
			String parent_dir_name = String.format("%s_%s", "log", current_date.format(formatter));
			std.println("saving log under name:: " + parent_dir_name);
			Path parent_path = PAST_LOGS_DIR.resolve(parent_dir_name);
			// NOTE: If a folder w the exact same name already exists, then theres a
			// problem, we might have to use nano-seconds in the filename then because
			// collissions could be possble

			if (Files.exists(parent_path)) {
				std.printf("JKVS::compact_logs:: [WARN] %s already exists!\n", parent_path);
				Files.createDirectory(parent_path);
				std.println("creating past_logs_dir to store original logs...");
			}

			Files.createDirectory(parent_path);

			// 1. Move the wal file first
			Path src_move_destination = parent_path.resolve(src.getFileName());
			Files.createFile(src_move_destination);
			std.debug("creating -> " + src_move_destination.toString());

			src_move_destination = Files.move(src, src_move_destination, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING);

			// 2. Move its index there
			Path index_move_destination = parent_path.resolve(src_index_file.getFileName());
			Files.createFile(index_move_destination);
			std.debug("creating -> " + index_move_destination.toString());
			index_move_destination = Files.move(src_index_file, index_move_destination, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING);

			// 3. We can begin writing to the src
			RandomAccessFile raf = new RandomAccessFile(src.toString(), "rw");
			long log_pointer = raf.length();
			std.printf("size of log pointer after compaction %d\n", log_pointer);

			// 4. Create the new index file
			Files.createFile(src_index_file);

			for (String key : compacted_logs.keySet()) {
				String cmd = compacted_logs.get(key);
				// Later on might want to refactor to avoid closoing and opening it on every
				// write
				raf.write(cmd.getBytes());
				log_pointer = raf.length();
				append_to_index(src_index_file, key, log_pointer);
			}

		} catch (Exception err) {
			std.eprintln("JKVSLib::log_compaction:: Unexpected error");
			throw new RuntimeException(err);
		}
	}

}
