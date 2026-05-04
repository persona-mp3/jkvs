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
