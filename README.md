### jkvs - Key Value Store in Java
Building a distributed key-value store database following [Pingcap's Talent Plan](https://github.com/pingcap/talent-plan), which
is intentionally meant for Rust. I'll be porting all the tests too later on down the project as correctness will be more favoured than 
speed

### Clone the repo
```bash
git clone https://github.com/persona-mp3/jkvs.git ~/jkvs
cd ~/jkvs
```

### Requirements
1. At least Java 21 - 25. Run `java --version` in your terminal to see what version you have

2. SDKMAN to manage and install GraalVM and native image for building to binary. [SDKMAN installation](https://sdkman.io/install/)
    To install GraalVM visit [GraalVM](https://www.graalvm.org/latest/getting-started/). If you're on a linux distro, you can manage all these with 
    SDKMAN
3. Maven to manage dependencies and run the application. You can find maven at [Apache Maven](https://maven.apache.org/install.html)

At the end, `java --version` should look similar to this. I'm on a linux distro, so 
this might slightly differ
```bash
    dev::jkvs (main) | java --version 
    openjdk 25.0.2 2026-01-20
    OpenJDK Runtime Environment (Red_Hat-25.0.2.0.10-3) (build 25.0.2+10)
    OpenJDK 64-Bit Server VM (Red_Hat-25.0.2.0.10-3) (build 25.0.2+10, mixed mode, sharing)
```


### Run the application using the build script
The custom  build script `compiler.sh` automates running the Maven command and using 
GraalVM to build the Java Project. By default, GraalVM and native-image consume alot 
of resources and are slow in building, mostly because it's trying to bake the JVM into 
the binary, so it will take at least 3secs
```
chmod u+x ./compiler.sh

./compiler.sh
```

That runs the `mvn package` command which bundles the project into a .jar file

And then executes the native-image to build to the project, along with it's dependencies.
As of now, the only dependency is [log4j2](https://logging.apache.org/log4j/2.x/manual/getting-started.html)
```
native-image --no-fallback \
  -H:IncludeResources="log4j2.xml" \
  -jar target/jkvs-1.0-SNAPSHOT.jar \
  jkvs
```

### Execute the binary
To set a value to the database, `./jkvs set <key> <value>`
```
./jkvs set username persona-mp3 
```

To retreive a value of a key, `./jkvs get <key>`. If  the key exists or hasn't 
be removed, it returns the value, otherwise null
```
./jkvs get username  # retreives the value of `username`
```

To delete a key, `./jkvs rm <key>`. If  the key exists or hasn't 
be removed, it returns the key, otherwise null
```
./jkvs rm username 
```

To show the version
```
./jkvs -V # prints out the version
```

