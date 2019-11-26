#!/bin/sh

set -o errexit

# Required
test -z "$ANALYTICS_ARGS" && ( echo "ANALYTICS_ARGS is not set; exiting" ; exit 1 )
test -z "$GERRIT_URL" && ( echo "GERRIT_URL is not set; exiting" ; exit 1 )

# Optional
SAVE_MODE="${SAVE_MODE:-saveToEs}"
ES_PORT="${ES_PORT:-9200}"
DB_PORT="${DB_PORT:-5432}"
SPARK_JAR_PATH="${SPARK_JAR_PATH:-/app/analytics-etl-auditlog-assembly.jar}"
SPARK_JAR_CLASS="${SPARK_JAR_CLASS:-com.gerritforge.analytics.auditlog.job.Main}"

echo "* Elastic Search Host: $ES_HOST:$ES_PORT"
echo "* Gerrit URL: $GERRIT_URL"
echo "* Analytics arguments: $ANALYTICS_ARGS"
echo "* Spark jar class: $SPARK_JAR_CLASS"
echo "* Spark jar path: $SPARK_JAR_PATH"
echo "* Save mode: $SAVE_MODE"

if [ "$SAVE_MODE" = 'saveToEs' ]; then
  test -z "$ES_HOST" && ( echo "ES_HOST is not set; exiting" ; exit 1 )
  echo "* Elastic Search Host: $ES_HOST:$ES_PORT"
  $(dirname $0)/wait-for-elasticsearch.sh ${ES_HOST} ${ES_PORT}

  echo "Elasticsearch is up, now running spark job..."

  spark-submit \
    --conf spark.es.nodes="$ES_HOST" \
    --class ${SPARK_JAR_CLASS} ${SPARK_JAR_PATH} \
    ${SAVE_MODE} \
    --gerritUrl ${GERRIT_URL} \
    ${ANALYTICS_ARGS}
fi

if [ "$SAVE_MODE" = 'saveToDb' ]; then
  test -z "$DB_HOST" && ( echo "DB_HOST is not set; exiting" ; exit 1 )
  test -z "$DB_DRIVER_JAR" && ( echo "DB_DRIVER_JAR is not set; exiting" ; exit 1 )

  echo "* Database Host: $DB_HOST:$DB_PORT"
  $(dirname $0)/wait-for-it.sh $DB_HOST:$DB_PORT -t 60 -- echo "Database is up"

  echo "Database is up, now running spark job..."

  spark-submit \
    --conf spark.driver.extraClassPath="/app/additional_jars/$DB_DRIVER_JAR" \
    --conf spark.jars="/app/additional_jars/$DB_DRIVER_JAR" \
    --class ${SPARK_JAR_CLASS} ${SPARK_JAR_PATH} \
    ${SAVE_MODE} \
    --gerritUrl ${GERRIT_URL} \
    ${ANALYTICS_ARGS}
fi