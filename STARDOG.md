# EVSRESTAPI - STARDOG SETUP

Information on downloading and using stardog with EVSRESTAPI.

## Running Stardog Locally

### Initial setup (create a volume to store data/license)
* Verify your $dir points to the correct path we set in the previous steps. If not, set it now: </br>`export set 
  dir=c:/Users/carlsenbr/eclipse-workspace/data/`

      docker pull stardog/stardog:latest
      
      # Windows volume seems to have an "fsync" issue, try using a "local docker volume"
      docker volume create --name stardog-home2 -d local

* Using an existing stardog license (in $dir). Make sure your license file is called `stardog-license-key.bin` and 
  is in the $dir directory on your local machine.

      docker run -it --entrypoint "/bin/bash" -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# cp /data/stardog-license-key.bin /var/opt/stardog
      [root@0b9fbb0b90ba bin]# exit


* Obtaining Stardog Licence - [Request License](https://www.stardog.com/license-request/) from Stardog directly.
  * This isn't a guaranteed solution unfortunately and you could be denied.

* Download the [ThesaurusInferred.owl](https://drive.google.com/drive/u/0/folders/11RcXLTsbOZ34_7ofKdVxLKHp_8aJGgTI) and save it in $dir

* Loading NCIt ThesaurusInferred.owl (after license is setup).  Make sure the local volume being mounted is the one that contains the ThesaurusInferred.owl file.

      docker run -it --entrypoint "/bin/bash" -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# export STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g"
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin server start
      
      # Check if the db already exists
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db list
      
      # If not, create it
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db create -n NCIT2
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog data add --named-graph http://NCI_T NCIT2 /data/ThesaurusInferred.owl
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db optimize -n NCIT2

### Running Stardog Locally (after data is loaded)

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -d --name=stardog_evs --rm -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog -e STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g" stardog/stardog

### Log into a Running Stardog Container

      docker exec -it <container_id, e.g. 3c29d72babc2> /bin/bash

