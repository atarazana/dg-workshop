# Deploying the operator

You have to be administrator to run this script successfully!

```sh
./run-admin.sh
```

## Deploying a PostgreSQL and 

You have to be logged in your OpenShift cluster before running this script

```sh
./run.sh $(oc whoami)
```

## If Spring Boot

Deploy code with no cache involved

```sh
mvn clean oc:deploy -Popenshift -DskipTests
```

## If Quarkus

Deploy code with no cache involved

```sh
export PROJECT_NAME=$(oc project -q)
./mvnw clean package -Dquarkus.kubernetes.deploy=true -DskipTests
```
