package github.persona_mp3.server.models;

public class Response {
	public String response;

	@Override
	public String toString() {
		return String.format("Response {response: %s}", this.response);
	}
}
