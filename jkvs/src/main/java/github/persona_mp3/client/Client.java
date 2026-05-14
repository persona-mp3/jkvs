package github.persona_mp3.client;

import github.persona_mp3.lib.protocol.Protocol;

import java.io.*;
import java.net.Socket;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public class Client {
	@Command(name = "ClientConfig")
	static class Config {
		@Option(names = "--addr", description = "ip address contact the server", defaultValue = "localhost")
		public String addr;

		@Option(names = "--port", description = "port the server is listnening on", defaultValue = "9090")
		public int port;

		@Override
		public String toString() {
			return String.format("ClientConfig: {addr: %s, port:%d}", this.addr, this.port);
		}
	}

	private static Logger logger = LogManager.getLogger(Client.class);
	private static Protocol protocol = new Protocol();
	private static int MAX_PAYLOAD_SIZE = 1 * 1024 * 1024;

	public static void main(String[] args) {
		Config config = new Config();
		CommandLine cmd = new CommandLine(config);
		String response = "";

		cmd.parseArgs(args);

		try (
				Socket conn = new Socket(config.addr, config.port);
				OutputStream outputStream = conn.getOutputStream();) {
			logger.info("connected to tcp::{}:{}", config.addr, config.port);

			Writer writer = new Writer(outputStream, conn);
			Thread writerThread = new Thread(writer);
			writerThread.start();

			while (conn.isConnected() && !conn.isClosed()) {
				response = protocol.readPacket(MAX_PAYLOAD_SIZE, conn);
				if (response == null) {
					logger.debug("response returned null");
					return;
				}
				logger.debug("Response from server:: {}", response);
				println(response);
			}

		} catch (Exception err) {
			logger.error("Could not connect to server. Reason: {}", err.getMessage());
			err.printStackTrace();
		}
	}

	private static void println(Object s) {
		System.out.println(s);
	}
}
