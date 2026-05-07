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
package github.persona_mp3.lib.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class ReversedReader implements Closeable {

	private static final int DEFAULT_BLOCK_SIZE = 4096;

	private final RandomAccessFile file;
	private final int blockSize;
	private final long fileLength;

	private long position; // current file pointer (moving backwards)
	private byte[] buffer;
	private int bufferPos;

	public ReversedReader(String path) throws IOException {
		this.file = new RandomAccessFile(path, "r");
		this.blockSize = DEFAULT_BLOCK_SIZE;
		this.fileLength = file.length();

		this.position = fileLength;
		loadBlock();
	}

	private void loadBlock() throws IOException {
		if (position <= 0) {
			buffer = null;
			return;
		}

		int size = (int) Math.min(blockSize, position);
		long start = position - size;

		buffer = new byte[size];
		file.seek(start);
		file.readFully(buffer);

		bufferPos = size - 1;
		position = start;
	}

	public String readLine() throws IOException {
		if (buffer == null)
			return null;

		byte[] lineBuffer = new byte[128];
		int lineLen = 0;

		while (true) {

			if (buffer == null) {
				if (lineLen == 0)
					return null;

				return reverseToString(lineBuffer, lineLen);
			}

			while (bufferPos >= 0) {
				byte b = buffer[bufferPos--];

				if (b == '\n') {
					if (bufferPos >= 0 && buffer[bufferPos] == '\r') {
						bufferPos--;
					} else if (bufferPos < 0) {
						loadBlock();
						if (buffer != null && bufferPos >= 0 && buffer[bufferPos] == '\r') {
							bufferPos--;
						}
					}
					return reverseToString(lineBuffer, lineLen);
				}

				// if (b == '\n') {
				// // skip '\r' if present before '\n'
				// if (bufferPos >= 0 && buffer[bufferPos] == '\r') {
				// bufferPos--;
				// }
				// return reverseToString(lineBuffer, lineLen);
				// }

				// grow buffer if needed
				if (lineLen == lineBuffer.length) {
					byte[] newBuf = new byte[lineBuffer.length * 2];
					System.arraycopy(lineBuffer, 0, newBuf, 0, lineBuffer.length);
					lineBuffer = newBuf;
				}

				lineBuffer[lineLen++] = b;
			}

			loadBlock();
		}
	}

	private String reverseToString(byte[] data, int len) {
		byte[] result = new byte[len];

		for (int i = 0; i < len; i++) {
			result[i] = data[len - 1 - i];
		}

		return new String(result, StandardCharsets.UTF_8);
	}

	@Override
	public void close() throws IOException {
		file.close();
	}
}
