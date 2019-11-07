# EVSRESTAPI - STARDOG SETUP AND CONFIGURATION

Information on downloading and using stardog.

## Running Stardog Locally

* Obtaining Stardog Licence Locally - [see Quick Start Guide](https://www.stardog.com/docs/#_quick_start_guide)
 
 <br/><br/><pre><code>
docker pull stardog/stardog:latest

# Windows volume seems to have an "fsync" issue, try using a "local docker volume"
docker volume create --name stardog-home -d local

# get license
docker run -it --entrypoint "/bin/bash" -v stardog-home:/var/opt/stardog stardog/stardog
[root@0b9fbb0b90ba bin]# cd /opt/stardog/bin
[root@0b9fbb0b90ba bin]# ./stardog-admin server start
... answer questions, provide email - bcarlsen+stardog@westcoastinformatics.com ...
</code></pre>

    * At this point, the license should be in docker volume "stardog-home" and be properly remounted with the license intact
    * NOTE: this step only needs to be run once (until license expires)

* Loading NCIt Thesaurus.owl (after license is obtained)
    * In a terminal/Cygwin window, run the following to have a stardog instance running.  Keep this window open to keep the server running.
 
        docker run -it --entrypoint "/bin/bash" -p 5820:5820 -v c:/evsrestapi:/data -v stardog-home:/var/opt/stardog stardog/stardog
        [root@0b9fbb0b90ba bin]# export STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g"
        [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin server start
        
        # Check if the db already exists
        [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db list
        
        # If not, create it
        [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db create -n NCIT2
        [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog data add --named-graph http://NCI_T NCIT2 /data/Thesaurus.owl
 
* Running Stardog Locally (after data is loaded)

        docker run -p 5820:5820 -v stardog-home/:/var/opt/stardog -e STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g" stardog/stardog

