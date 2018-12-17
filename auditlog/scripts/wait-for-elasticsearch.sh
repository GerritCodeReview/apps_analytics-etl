#!/bin/sh

wait_for() {

  ELASTIC_SEARCH_HOST=$1
  ELASTIC_SEARCH_PORT=$2

  ELASTIC_SEARCH_URL="http://$ELASTIC_SEARCH_HOST:$ELASTIC_SEARCH_PORT"

  for i in `seq 30` ; do
    curl -f ${ELASTIC_SEARCH_URL}/_cluster/health > /dev/null 2>&1

    result=$?
    if [ $result -eq 0 ] ; then
          exit 0
    fi
    echo "* Waiting for Elasticsearch at $ELASTIC_SEARCH_URL ($i/30)"
    sleep 2
  done
  echo "Operation timed out" >&2
  exit 1
}

wait_for "$@"
