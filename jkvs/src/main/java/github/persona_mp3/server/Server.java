package github.persona_mp3.server;

import github.persona_mp3.server.models.Request;
import github.persona_mp3.server.models.Response;
import github.persona_mp3.lib.JKVStore;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import picocli.CommandLine;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Server {
	static JKVStore store = new JKVStore();
	private static Logger logger = LogManager.getLogger(Server.class);

	public static void main(String[] args) throws JsonProcessingException {
		Config config = new Config();
		CommandLine cmd = new CommandLine(config);

		cmd.parseArgs(args);

		String addr = config.addr;
		int port = config.port;
		logger.info("Initialising database");

		try (
				ServerSocket listener = new ServerSocket(port);) {

			store.init();
			logger.info("tcp-server listening tcp://{}:{}", addr, port);

			while (true) {
				Socket conn = listener.accept();
				logger.info("accpeted connection from localAddr={}", conn.getClass());

				handleConn(conn);
			}
		} catch (Exception err) {
			logger.error("An error occured: {}", err.getMessage());
			err.printStackTrace();
		}
	}

	// We're catching errors here so the server doesn't stop because of one
	// conection's error
	static void handleConn(Socket conn) {
		try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				PrintWriter pr = new PrintWriter(conn.getOutputStream());) {
			ObjectMapper mapper = new ObjectMapper();
			String line;

			while ((line = reader.readLine()) != null) {
				Request req = mapper.readValue(line, Request.class);
				logger.info("processing request= {} from conn: {}", req, conn.getRemoteSocketAddress());

				Response response = processRequest(req);
				pr.println(mapper.writeValueAsString(response));
				logger.info("wrote response={} to client", response);
			}

			logger.info("client {} has disconnected", conn.getRemoteSocketAddress());
		} catch (IOException err) {
			logger.error("IOException occured in handleConn. Summary: {}", err.getMessage());
			System.err.println("\n");
			err.printStackTrace();
		} catch (Exception err) {
			logger.error("Unexpected exception occured in handleConn. Summary: {}", err.getMessage());
			System.err.println("\n");
			err.printStackTrace();
		}
	}

	static Response processRequest(Request req) throws IOException {
		Response response = new Response();
		if (req.command == null) {
			response.response = "invalid command";
			return response;
		}
		switch (req.command) {
			case JKVStore.GET_COMMAND:
				response.response = store.get(req.key);
				return response;

			case JKVStore.SET_COMMAND:
				response.response = store.set(req.key, req.value);
				return response;

			case JKVStore.REMOVE_COMMAND:
				response.response = store.remove(req.key);
				return response;
		}

		response.response = "unknown command";
		return response;
	}
}
