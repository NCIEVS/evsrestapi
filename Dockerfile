# Base image 
FROM centos:7
MAINTAINER Frankie Parks <frankie.parks@bioappdev.org>

ENV TZ=America/New_York
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

# Update all packages installed for security
RUN yum makecache && yum update -y

# Create a user and group used to launch processes
# The user ID 1000 is the default for the first "regular" user on Fedora/RHEL,
# so there is a high chance that this ID will be equal to the current user
# making it easier to use volumes (no permission issues)
RUN groupadd -r evsapi -g 1000 && useradd -u 1000 -r -g evsapi -m -d /opt/evsapi -s /sbin/nologin -c "EVSAPI user" evsapi && \
    chmod 755 /opt/evsapi

# Set the working directory to evsapi' user home directory
WORKDIR /opt/evsapi

# Install necessary packages
RUN yum -y install java-1.8.0-openjdk-devel unzip && yum clean all

# Set ENV variable for EVS_SERVER_PORT
ENV EVS_SERVER_PORT="5830"
ENV JAVA_OPTIONS="-Xmx2048m -XX:PermSize=1024m -XX:MaxPermSize=512m"

# Add file files to image
ADD build/distributions/evsrestapi-1.0.0-SNAPSHOT.zip /opt/evsapi/
RUN unzip evsrestapi-1.0.0-SNAPSHOT.zip
RUN ln -s evsrestapi-1.0.0-SNAPSHOT evsrestapi

RUN ls -l

EXPOSE 5830 
USER evsapi
CMD java -jar ./evsrestapi/lib/evsrestapi.war 

