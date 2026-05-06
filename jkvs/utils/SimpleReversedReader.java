/*
 * Simplified version of Apache Commons IO ReversedLinesFileReader
 *
 * Original work licensed under Apache License 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Modifications:
 * - Removed builder pattern
 * - Removed Apache Commons dependencies
 * - Simplified encoding handling (UTF-8 only)
 * - Reduced complexity for learning purposes
 */

package jkvs.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SimpleReversedReader implements Closeable {

	private static final int DEFAULT_BLOCK_SIZE = 4096;

	private final RandomAccessFile file;
	private final int blockSize;
	private final long fileLength;

	private long currentBlock = -1;
	private byte[] leftover = null;
	private byte[] buffer;

	private int bufferPos;

	public SimpleReversedReader(String path) throws IOException {
		this.file = new RandomAccessFile(path, "r");
		this.blockSize = DEFAULT_BLOCK_SIZE;
		this.fileLength = file.length();

		this.currentBlock = (fileLength + blockSize - 1) / blockSize;
		loadBlock();
	}

	private void loadBlock() throws IOException {
		if (currentBlock <= 0) {
			buffer = null;
			return;
		}

		long start = (currentBlock - 1) * blockSize;
		int length = (int) Math.min(blockSize, fileLength - start);

		byte[] newBuffer = new byte[length];

		file.seek(start);
		file.readFully(newBuffer);

		// attach leftover from previous block
		if (leftover != null) {
			byte[] combined = new byte[newBuffer.length + leftover.length];
			System.arraycopy(newBuffer, 0, combined, 0, newBuffer.length);
			System.arraycopy(leftover, 0, combined, newBuffer.length, leftover.length);
			buffer = combined;
		} else {
			buffer = newBuffer;
		}

		bufferPos = buffer.length - 1;
		leftover = null;
		currentBlock--;
	}

	public String readLine() throws IOException {
		if (buffer == null)
			return null;

		StringBuilder line = new StringBuilder();

		while (true) {
			while (bufferPos >= 0) {
				byte b = buffer[bufferPos--];

				if (b == '\n') {
					return line.reverse().toString();
				}

				if (b != '\r') { // ignore Windows CR
					line.append((char) b);
				}
			}

			// reached start of buffer
			leftover = line.reverse().toString().getBytes(StandardCharsets.UTF_8);
			line.setLength(0);

			loadBlock();

			if (buffer == null) {
				// start of file
				if (leftover != null && leftover.length > 0) {
					return new String(leftover, StandardCharsets.UTF_8);
				}
				return null;
			}

			// restore line content
			line.append(new String(leftover, StandardCharsets.UTF_8)).reverse();
		}
	}

	@Override
	public void close() throws IOException {
		file.close();
	}
}
