#!/bin/sh

export PROJECT_NAME=$(oc project -q)
export DEPLOYMENT_NAME=atomic-fruit-service

export TELEPRESENCE_USE_OCP_IMAGE=NO
oc project ${PROJECT_NAME}
telepresence --swap-deployment ${DEPLOYMENT_NAME} --expose 8080
