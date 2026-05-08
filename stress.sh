 #!/bin/env bash
 set -e
 LIMIT=100000
 for ((i=LIMIT; i >=0; i--))
 do
	 if (( $i  ==  999888 )); then
	  ./app rm key_$i 
	  ./app set key_$i value_$i 
		echo "mark done"
	  break
	 fi
	 ./jkvs/jkvs set key_$i value_$i


 done
