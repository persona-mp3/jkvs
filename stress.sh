 #!/bin/env bash

 LIMIT=100000
 for i in $(seq 1 $LIMIT)
 do
	 ./app set $i $i*2
 done
