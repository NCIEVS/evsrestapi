#
# Build like this
# % docker build . -t evsrestapi:elasticsearch
#
# Run like this
# % 
#

# Start with the python3 anaconda image
FROM continuumio/anaconda3

RUN mkdir /elasticsearch
COPY Mapping/ /elasticsearch/Mapping
COPY scripts /elasticsearch/scripts
WORKDIR /elasticsearch

# Prep project

#RUN conda init bash
#RUN conda create -y -n BulkLoadES python=3.7
#RUN conda activate BulkLoadES
RUN pip install requests python-dotenv elasticsearch

# default is to log into the container
CMD /bin/bash
