# EVSRESTAPI - STARDOG SETUP

Information on downloading and using stardog with EVSRESTAPI.

## Running Stardog Locally

### Initial setup (create a volume to store data/license)

      docker pull stardog/stardog:latest
      
      # Windows volume seems to have an "fsync" issue, try using a "local docker volume"
      docker volume create --name stardog-home2 -d local

* Using an existing stardog license (in $dir). Make sure your license file is called `stardog-license-key.bin` and is in the $dir directory on your local machine.

      # dir will be the path you are storing your project files in on your local machine
      dir=c:/Users/carlsenbr/eclipse-workspace/data/  
      docker run -it --entrypoint "/bin/bash" -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# cp /data/stardog-license-key.bin /var/opt/stardog
      [root@0b9fbb0b90ba bin]# exit
  * You may have to share a license with a fellow co-work as a temporary workaround.

* Obtaining Stardog Licence - [Request License](https://www.stardog.com/license-request/) from Stardog directly.
  * This isn't a guaranteed solution unfortunately and you could be denied. 

* Download the [ThesaurusInferred.owl](https://drive.google.com/drive/u/0/folders/11RcXLTsbOZ34_7ofKdVxLKHp_8aJGgTI) and save it in $dir

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

### Running Stardog Locally (after data is loaded)
* You'll be running this regularly when working with the proejct, so save this command somewhere easily accessible to you.

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -d --name=stardog_evs --rm -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog -e STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g" stardog/stardog

### Log into a Running Stardog Container
* Make sure to add your running container id to log in.  e.g. 3c29d72babc2

      docker exec -it <container_id> /bin/bash
