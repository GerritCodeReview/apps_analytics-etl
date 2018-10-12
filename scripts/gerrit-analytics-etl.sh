#!/bin/sh

set -o errexit

test -z "$ES_HOST" && ( echo "ES_HOST is not set; exiting" ; exit 1 )
test -z "$ANALYTICS_ARGS" && ( echo "ANALYTICS_ARGS is not set; exiting" ; exit 1 )
test -z "$GERRIT_URL" && ( echo "GERRIT_URL is not set; exiting" ; exit 1 )

echo "Elastic Search Host: $ES_HOST"
echo "Gerrit URL: $GERRIT_URL"
echo "Analytics arguments: $ANALYTICS_ARGS"
echo "Spark jar class: $SPARK_JAR_CLASS"
echo "Spark jar path: $SPARK_JAR_PATH"

spark-submit \
    --conf spark.es.nodes="$ES_HOST" \
    --class $SPARK_JAR_CLASS $SPARK_JAR_PATH \
    --url $GERRIT_URL \
    $ANALYTICS_ARGS