#!/bin/sh

TEMPLATE_NAME=dg-workshop-prep

helm template ./admin --name-template $TEMPLATE_NAME --include-crds | kubectl apply -f -