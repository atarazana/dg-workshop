# Deploying the operator

You have to be administrator to run this script successfully!

```sh
./run-admin.sh
```

## Deploying a PostgreSQL, a DG cluster and other needed artifacts...

You have to be logged in your OpenShift cluster before running this script, check if you are running this command. It should return a string containing the user or an error if you're not logged in.

```sh
oc whoami
```

If you are correctly logged in, please run this script to deploy

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
