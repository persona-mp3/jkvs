### Client, with logging on
  jkvs::jkvs (main) | time ./tester.sh  >> perf_client
________________________________________________________
Executed in  491.18 secs    fish           external
   usr time  147.66 secs    0.39 millis  147.66 secs
   sys time  346.61 secs    1.10 millis  346.61 secs

### Client, with logging off on server

  jkvs::jkvs (main) | time ./tester.sh  >> perf_client
________________________________________________________
Executed in  480.79 secs    fish           external
   usr time  145.71 secs    1.41 millis  145.71 secs
   sys time  342.67 secs    0.00 millis  342.67 secs



### Server and Client, without shell redirect and logging off
  jkvs::jkvs (main) | time ./tester.sh  >> perf_client
________________________________________________________
│Executed in  322.69 secs    fish           external
│   usr time  104.29 secs    0.00 millis  104.29 secs
│   sys time  222.29 secs    1.16 millis  222.29 secs
