#!/bin/sh

if [ "$#" -lt 1 ]; then
    echo "Usage:\n  $0 [user-id] [delete?]\n\nExamples:\n  $0 user1\n  $0 \$(oc whoami)"
    exit 1
fi

TEMPLATE_NAME=dg-workshop
DEBUG="true"
USER_ID=$1
NAMESPACE="${TEMPLATE_NAME}-${USER_ID}"

if [ "delete" == "$2" ]; then
    COMMAND=delete
else
    oc new-project ${NAMESPACE}
    COMMAND=apply
fi

helm template ./helm_base --name-template $TEMPLATE_NAME --set debug=${DEBUG},namespace=${NAMESPACE} --include-crds > ./helm_base/all.yml && kustomize build ./kustomize | kubectl ${COMMAND} -f -

if [ "delete" == "$2" ]; then
    oc delete project ${NAMESPACE}
fi