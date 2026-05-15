package main

import (
	"encoding/binary"
	"errors"
	"io"
	"log"
	"net"
	"sync"
)

const max_threads = 100 * 1000

func main() {
	log.Println("max_connections::", max_threads);
	wg := sync.WaitGroup{}
	wg.Add(max_threads)
	for i := range max_threads {
		go func() {
			defer wg.Done()
			conn, err := net.Dial("tcp", "localhost:9090")
			if err != nil {
				log.Println("could not connect for conn->", i, err)
				return
			}

			defer conn.Close()

			buffer := make([]byte, 1024)
			if _, err := conn.Write(toPacket("set\r\nresults\r\nodd")); err != nil {
				log.Println("could not write to server:: ", err)
				return
			}

			if _, err := conn.Read(buffer); err != nil  && !errors.Is(err, io.EOF){
				log.Printf("Error: %s, At:: %d\n", err, i)
				return
			}

			// log.Printf("response:: %s\n", string(buffer))

		}()
	}

	wg.Wait()
	println("done with bombing server")

}

func toPacket(msg string) []byte {
	size := len([]byte(msg))
	buffer := make([]byte, 4)
	binary.BigEndian.PutUint32(buffer, uint32(size))
	buffer = append(buffer, []byte(msg)...)

	return buffer
}

