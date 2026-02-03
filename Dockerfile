# Base image 
FROM eclipse-temurin:21-jre
LABEL maintainer="Frankie Parks <frankie.parks@bioappdev.org>"

ENV TZ=America/New_York
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

# Update all packages installed for security and install unzip
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

# Create a user and group used to launch processes
RUN groupadd -r evsapi -g 1000 && useradd -u 1000 -r -g evsapi -m -d /opt/evsapi -s /sbin/nologin -c "EVSAPI user" evsapi && \
    chmod 755 /opt/evsapi

# Set the working directory to evsapi' user home directory
WORKDIR /opt/evsapi

# Set ENV variable for EVS_SERVER_PORT
ENV EVS_SERVER_PORT="5830"
ENV JAVA_OPTIONS="-Xmx2048m"

# Add file files to image
ADD build/distributions/evsrestapi-*.zip /opt/evsapi/
RUN unzip evsrestapi-*.zip && rm evsrestapi-*.zip
RUN ln -s evsrestapi-* evsrestapi

EXPOSE 5830 
USER evsapi
CMD java $JAVA_OPTIONS -jar ./evsrestapi/lib/evsrestapi.war 

