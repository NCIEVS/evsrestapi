FROM bellsoft/liberica-openjre-alpine:17.0.13

# Set environment variables
ENV FUSEKI_VERSION=5.1.0
ENV JENA_HOME=/opt/jena
ENV FUSEKI_HOME=/opt/fuseki
ENV FUSEKI_BASE=/opt/fuseki/run

# Install necessary packages
RUN apk add --no-cache curl unzip bash

# Create directories
RUN mkdir -p ${FUSEKI_HOME} ${FUSEKI_BASE} ${JENA_HOME}

# Download and unzip Fuseki
RUN curl -L -o /tmp/apache-jena-fuseki-${FUSEKI_VERSION}.zip \
    https://archive.apache.org/dist/jena/binaries/apache-jena-fuseki-${FUSEKI_VERSION}.zip && \
    unzip /tmp/apache-jena-fuseki-${FUSEKI_VERSION}.zip -d /tmp && \
    mv /tmp/apache-jena-fuseki-${FUSEKI_VERSION}/* ${FUSEKI_HOME} && \
    rm /tmp/apache-jena-fuseki-${FUSEKI_VERSION}.zip

RUN curl -L -o /tmp/apache-jena-${FUSEKI_VERSION}.zip \
    https://archive.apache.org/dist/jena/binaries/apache-jena-${FUSEKI_VERSION}.zip && \
    unzip /tmp/apache-jena-${FUSEKI_VERSION}.zip -d /tmp && \
    mv /tmp/apache-jena-${FUSEKI_VERSION}/* ${JENA_HOME} && \
    rm /tmp/apache-jena-${FUSEKI_VERSION}.zip

## Expose the default Fuseki port
#EXPOSE 3030

# Set the working directory
WORKDIR ${FUSEKI_HOME}
COPY ./shiro.ini ${FUSEKI_HOME}/run/shiro.ini
# Start Fuseki
#CMD ["./fuseki-server", "--update", "--loc=/opt/fuseki/run/databases","/NCIT2"]
#CMD ["./fuseki", "start"]
#CMD ["./fuseki-server", "--update"]
COPY entrypoint.sh /opt/fuseki/entrypoint.sh
RUN chmod +x /opt/fuseki/entrypoint.sh
ENTRYPOINT ["/opt/fuseki/entrypoint.sh"]