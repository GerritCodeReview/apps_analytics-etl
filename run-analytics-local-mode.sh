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
		echo "--repos              https://review.gerrithub.io/spdk/spdk,https://github.com/apache/spark.git"
                echo "[--gerrit-version]   2.15.12"
                echo "[--gerrit-address]   http://localhost:8080"
                echo "[--aggregate-since]  2010-01-01"
                echo "[--aggregate-by]     email|email_hour|email_day|email_month|email_year"
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
  "--aggregate-since")
		AGGREGATE_SINCE=$2
		shift
		shift
  ;;
  "--aggregate-by")
		AGGREGATE_BY=$2
		shift
		shift
  ;;


  *	   ) 
		echo "Unknown option argument: $2"
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
if [ -z "$AGGREGATE_SINCE" ]; then AGGREGATE_SINCE="2010-01-01"; fi
if [ -z "$AGGREGATE_BY" ];    then AGGREGATE_BY="email_hour"; fi

echo "================================="
echo "Parameters"
echo "================================="
echo "Gerrit Version:  $GERRIT_VERSION"
echo "Gerrit Address:  $GERRIT_ADDRESS"
echo "---"
echo "Aggregate By:    $AGGREGATE_BY"
echo "Aggregate since: $AGGREGATE_SINCE"
echo "Repositories:    $REPOSITORIES"
echo "================================="


echo "Starting gerrit..."
GERRIT_DOCKER=$(docker run -d -p 8080:8080 -p 29418:29418 -e CANONICAL_WEB_URL=$GERRIT_ADDRESS gerritcodereview/gerrit:$GERRIT_VERSION)
sleep 10m
echo "Gerrit docker id $GERRIT_DOCKER"

echo "Cloning repositories"
for repository in $REPOSITORIES_ARRAY
do
	echo "Cloning $repository"
	docker exec $GERRIT_DOCKER bash -c "cd /var/gerrit/git&&git clone --mirror $repository"
done

echo "Flushing gerrit caches"
docker exec $GERRIT_DOCKER bash -c "ssh -p 29418 admin@localhost gerrit flush-caches --all"


echo "Starting elasticsearch reports..."
#docker-compose up &

#sleep 1m

echo "Building analytics project..."
#sbt clean assembly

echo "Running Spark Job..."
#spark-submit --conf spark.es.nodes=localhost target/scala-2.11/analytics-etl.jar --since $AGGREGATE_SINCE --aggregate $AGGREGATE_BY --url $GERRIT_ADDRESS -e gerrit/analytics

exit $?
