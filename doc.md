## in-memory-index
* map of keys to log pointers,

So when a command like `set` is received, we can write to the `.wal` file as 
```
set   username        "persona-mp3"
set   specialization  "distributed_systems"
```
And then append to the `index` file as 
```
username 0
specialization 90
```


So when command is `get` <key>
1. Rebuild `in-memory index` with `index` file
2. check if the key-exists, if exists, use the log pointer to retrive value


So when command is `rm` <key>
1. Rebuild `in-memory index` with `index` file
2. check if the key-exists, if not exists, exit(0)
3. append `rm` command to log as 

```
rm username
```

### Log Compaction
I'm thinking right, the log-compaction should be straight foward, at least within the scope 
I have in mind, 

If we ever wanted to do that, we could just reload the index file, 
and then just copy those offsets into a new log-file, leaving the redundants and stuff.
Why?
1. HashMaps, by default only allow a single value, so the last key entry will always be the 
   fist in a hashmap. This eliminates duplicates for us.

Concern:: Reading a whole index file into memory isn't always the best thing, so the previous approach isn't the best


2. We can go ahead of this, by 
    1. Keeping another log_file, where for each set, we write to both to these. If 
    the set is simply an update, we don't append to the second file, but we just overwrite it.
    2. If there's a rm command, we simply remove it from the second file. 
    But this requires that we have two-files to read and write from, which at anytime could fail. 
    While the main.log is fine and possibly crrect, the append.log might not be.


3. Or when we want to compact, we can read the main.log file in reverse, and
   append them to the new logfile but instead we'll use a HashMap. 
   
   1. For each line we read, we append it to the HashMap. And then write
   everything to the
   new index file

    Scenarios:
    1. If we encounter a <rm> <key> log before the log itself, we'll have an
       ignore_list that we can compare each log_line against. 
    2. If we encounter a <rm> <key> log after the log iteself, we ignore it



In all of these scenarions, we have to maintain two-files at a time. And
copying and reading could fail at anytime If we were to do it inplace, this
would pose more risk, as the original log file is corrupted and data can't be
replayed/rebuilt


### Concurrency Design
SO heres the thing, Reads in the db are primarily hashMapLookUps, so we don't or want to limit 
the locks in this section.

So we'll end up favouring writes, because IO operations to files or even networks are slower in general

So we can have a writer or multiple writerrs locking the lookUpMap, but a single one writing to the file
That way, we don't manage file concurrency. 

I'm mapping how I would do this in Go to Java, but we'd want x amount of virtual-threads/go-routines, (we'll call them level-1)that write to the 
map, and a single go-routine/thread (0-level) that writes to these files. The level-1 threads will all drop their values into the channel/queue
<insert java equivalent> that 0-level owns. Once the level-1s have dropped it in the queue, they send their response back to the client. The queue 0level
has and level1s interact with are non-blocking queues so they can just drop there and leave
But the other issue is that if we have x amount of threads  nah  THOSE WERE WRONG, I got the order mixed up

- Writes -> toLogFile ---lptr---> lookUpTable ---lptr---> indexFile

So the bottleneck here for writes is at the LogFile, because discIO is slow
Or could we 
1. Every level-1 thread just write to the queue
2. Level-0 thread does the fileIO first, locks the Map, writes to it, and then to the indexFile?

But this looks like (JVM's Young Normal Generation Pause) because when this dude wants to write to the map, all threads must wait in place
Idk if this is a good idea?

Also we really dont know how much percentage of writes::read that we can balance so we can design the conccurency

In this talk Jon Gjenset talks about [Concurrency Coordination](https://www.youtube.com/watch?v=tND-wBBZ8RY) as he goes to talk about 
`cache-lines`, `mutexes` and `reader-writer-locks`
One in particular that piques interest is the `left-right pad` coordination. The design is simple and performance seems reliable (i've not
impl or used this before), , but idk if  thats the right tool here 
and if Java gives access to that, or there's any up-to-date library there

1. It's not yet clear how much of readers::writers the system will in GENERAL have, the concurrency design is important here

SECONDLY is that for writes, how do we handle responses? Do we just say `YES WE SAVED IT` or `YES WE RMED IT`? because we 
still need to reply to the clients, 


