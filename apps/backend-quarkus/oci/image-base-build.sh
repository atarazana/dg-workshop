#!/bin/sh

. ./image-base-env.sh

docker build -f ./Dockerfile.native.base -t $IMAGE_NAME:$IMAGE_VERSION .