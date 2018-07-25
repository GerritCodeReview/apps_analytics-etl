#!/bin/bash


## Handle input parameters
if [ $# -eq 0 ]
then 
	echo "No arguments provided. For more information --help"
	exit -1
fi

while [ $# -ne 0 ]
do
case "$1" in
  "--help" ) 
	 	echo "Usage: $0 --repos repo1,repo2 [--options...]"
	 	echo
		echo "--repos              https://github.com/spdk/spdk.git,https://github.com/apache/spark.git"
                echo "[--gerrit-version]   2.15.12"
                echo "[--gerrit-address]   http://localhost:8080"
		echo
		exit 0
  ;;

  "--repos")
                REPOSITORIES=$2
		shift
		shift
  ;;

  "--gerrit-version")
		GERRIT_VERSION=$2
		shift
		shift
  ;;

  "--gerrit-address" )
       		GERRIT_ADDRESS=$2
		shift
		shift
  ;;

  *	   ) 
		echo "Unknown option argument: $1"
		shift
		shift
  ;;
esac
done

if [ -z "$REPOSITORIES" ]
then 
	echo "No repositories defined" 
	exit -1 
else 
	REPOSITORIES_ARRAY=$(echo $REPOSITORIES | tr "," "\n")
fi


## Optional Parameters
if [ -z "$GERRIT_ADDRESS" ];  then GERRIT_ADDRESS=http://localhost:8080; fi
if [ -z "$GERRIT_VERSION" ];  then GERRIT_VERSION="2.15.1"; fi

echo "===================================="
echo "Parameters"
echo "===================================="
echo "Gerrit Version:      $GERRIT_VERSION"
echo "Gerrit Address:      $GERRIT_ADDRESS"
echo "---"
echo "CloningRepositories: $REPOSITORIES"
echo "===================================="


echo "Starting gerrit..."
GERRIT_DOCKER=$(docker run -d -p 8080:8080 -p 29418:29418 -e CANONICAL_WEB_URL=$GERRIT_ADDRESS gerritcodereview/gerrit:$GERRIT_VERSION)
sleep 1m
echo "Gerrit docker id $GERRIT_DOCKER"

for repository in $REPOSITORIES_ARRAY
do
	echo "Cloning $repository"
	docker exec $GERRIT_DOCKER bash -c "cd /var/gerrit/git&&git clone --mirror $repository"
done

echo "Flushing gerrit caches"
docker exec $GERRIT_DOCKER bash -c "ssh -p 29418 admin@localhost gerrit flush-caches --all"


echo "Starting elasticsearch and kibana..."
docker-compose up &


exit $?
