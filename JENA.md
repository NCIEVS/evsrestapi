# Jena setup
Run the following command to build the Jena image:

```bash
cd docker/fuseki
docker build -t fuseki:5.1.0  .
```

Start the container with the following command. Note: you would need the src mount path to a local directory of your choice to persist the data.
```bash
cd docker/fuseki
docker run -d -p "3030:3030" --mount type=bind,src=/Users/squareroot/data,dst=/opt/fuseki/run/databases fuseki:5.1.0
```