set OCP=%CLASSPATH%
set lib1=.
set lib2=lib
set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;%lib1%\*
set CLASSPATH=%CLASSPATH%;%lib2%\*

java -d64 -Xms512m -Xmx4g -classpath %CLASSPATH% gov.nih.nci.evs.restapi.appl.WiKiHomeStatistics ThesaurusInferred_forTS.owl

set CLASSPATH=%OCP%