#!/bin/sh

set -o errexit

# Required
test -z "$ES_HOST" && ( echo "ES_HOST is not set; exiting" ; exit 1 )
test -z "$ANALYTICS_ARGS" && ( echo "ANALYTICS_ARGS is not set; exiting" ; exit 1 )
test -z "$GERRIT_URL" && ( echo "GERRIT_URL is not set; exiting" ; exit 1 )

# Optional
ES_PORT="${ES_PORT:-9200}"
SPARK_JAR_PATH="${SPARK_JAR_PATH:-/app/analytics-etl-gitcommits-assembly.jar}"
SPARK_JAR_CLASS="${SPARK_JAR_CLASS:-com.gerritforge.analytics.gitcommits.job.Main}"

echo "* Elastic Search Host: $ES_HOST:$ES_PORT"
echo "* Gerrit URL: $GERRIT_URL"
echo "* Analytics arguments: $ANALYTICS_ARGS"
echo "* Spark jar class: $SPARK_JAR_CLASS"
echo "* Spark jar path: $SPARK_JAR_PATH"

$(dirname $0)/wait-for-elasticsearch.sh ${ES_HOST} ${ES_PORT}

echo "Elasticsearch is up, now running spark job..."

spark-submit \
    --conf spark.es.nodes="$ES_HOST" \
    --class ${SPARK_JAR_CLASS} ${SPARK_JAR_PATH} \
    --url ${GERRIT_URL} \
    ${ANALYTICS_ARGS}