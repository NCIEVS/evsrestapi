set OCP=%CLASSPATH%
set lib1=.
set lib2=lib
rem set lib3=extlib

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;%lib1%\*
set CLASSPATH=%CLASSPATH%;%lib2%\*
rem set CLASSPATH=%CLASSPATH%;%lib3%\*

java -d64 -Xms512m -Xmx4g -classpath %CLASSPATH% gov.nih.nci.evs.restapi.appl.UNIIProcessor https://sparql-evs.nci.nih.gov/sparql http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl UNII_Names_27Mar2020.txt UNII_Records_27Mar2020.txt  

set CLASSPATH=%OCP%