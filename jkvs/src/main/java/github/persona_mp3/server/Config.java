package github.persona_mp3.server;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Commandline arguments used to parse configuration options used to start the
 * database server
 * By default, the <addr> is localhost and <port> is 9090
 */
@Command(name = "ServerConfig")
public class Config {
	@Option(names = "--addr", description = "ip address to run the server", defaultValue = "localhost")
	public String addr;

	@Option(names = "--port", description = "port to listen on", defaultValue = "9090")
	public int port;
}
