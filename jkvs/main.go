package main

import (
	"log"
	"net"
)


func handleConn(conn net.Conn){
	defer conn.Close()
	buffer := make([]byte, 1024);

	for {
		n, err := conn.Read(buffer);
		if (err != nil) {
			log.Printf("error reading from client:: %s", err);
			continue;
		}
		log.Printf("request:: %s\n", string(buffer[:n]));
	}
}



// Bug is that the server or the thread somehow drops the connection silently? 
// Errors and logs are swallowed because we dont know when the client has disconnected
func main(){
	listener, err := net.Listen("tcp", "localhost:9090" )
	if err != nil {
		log.Fatalf("could not create listener: %s", err)
	}
	log.Printf("server listening on tcp[::]9090");

	for {
		conn, err := listener.Accept();
		if err != nil {
			log.Printf("could  not accept connection: %s\n", err);
			continue
		}

		go handleConn(conn)
	}
}
