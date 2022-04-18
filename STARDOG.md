# EVSRESTAPI - STARDOG SETUP

Information on downloading and using stardog with EVSRESTAPI.

## Running Stardog Locally

* Initial setup (create a volume to store data/license)

      docker pull stardog/stardog:latest
      
      # Windows volume seems to have an "fsync" issue, try using a "local docker volume"
      docker volume create --name stardog-home2 -d local

* Using an existing stardog license (in $dir)

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -it --entrypoint "/bin/bash" -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# cp /data/stardog-license-key.bin /var/opt/data
      [root@0b9fbb0b90ba bin]# exit


* Obtaining Stardog Licence Locally - [see Quick Start Guide](https://www.stardog.com/docs/#_quick_start_guide)

      # get license
      docker run -it --entrypoint "/bin/bash" -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin license request --force
      ... answer questions, provide email - bcarlsen+stardog@westcoastinformatics.com ...
      [root@0b9fbb0b90ba bin]# exit

  * At this point, the license should be in docker volume "stardog-home2" and be properly remounted with the license intact
  * NOTE: this step only needs to be run once (until license expires)

* Loading NCIt ThesaurusInferred.owl (after license is setup).  Make sure the local volume being mounted is the one that contains the ThesaurusInferred.owl file.

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -it --entrypoint "/bin/bash" -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# export STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g"
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin server start
      
      # Check if the db already exists
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db list
      
      # If not, create it
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db create -n NCIT2
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog data add --named-graph http://NCI_T NCIT2 /data/ThesaurusInferred.owl
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db optimize -n NCIT2

* Running Stardog Locally (after data is loaded)

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -d --name=stardog_evs --rm -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog -e STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g" stardog/stardog

* Log into a Running Stardog Container

      docker exec -it <container_id, e.g. 3c29d72babc2> /bin/bash

