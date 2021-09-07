#!/bin/sh

#docker run -it --rm -p 11222:11222 -v $(pwd)/infinispan:/user-config --name=infinispan --entrypoint "/opt/infinispan/bin/server.sh" quay.io/infinispan/server:12.1 -b SITE_LOCAL -c /user-config/custom-infinispan.xml
docker run -it --rm -p 11222:11222 -e USER="developer" -e PASS="developer" -v $(pwd)/infinispan:/opt/infinispan/server/conf --name=infinispan --entrypoint "/opt/infinispan/bin/server.sh" quay.io/infinispan/server:12.1 -b SITE_LOCAL -c /opt/infinispan/server/conf/custom-infinispan.xml

#docker run -it --rm -p 11222:11222 -e USER="developer" -e PASS="developer" -e EXTERNAL_HOST="localhost" -e EXTERNAL_PORT=11222 -v $(pwd)/infinispan:/user-config --name=infinispan  quay.io/infinispan/server:12.1 
#docker run -it --rm -p 11222:11222 -e USER="developer" -e PASS="developer" -e EXTERNAL_HOST="localhost" -e EXTERNAL_PORT=11222 -v $(pwd)/infinispan:/user-config --name=infinispan  --entrypoint "/opt/infinispan/bin/server.sh" quay.io/infinispan/server:12.1 -b $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' infinispan) 


