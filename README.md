# spark-gerrit-analytics-etl
Spark ETL to extra analytics data from Gerrit Projects.

Job can be launched with the following parameters:

```
bin/spark-submit \
    --conf spark.es.nodes=company.com \
    $JARS/SparkAnalytics-assembly-1.0.jar \
    --since 2000-06-01 \
    --aggregate email_hour \ 
    --url http://localhost:8080 \ 
    -e gerrit/analytics
```
### Parameters
- since, until, aggregate are the same defined in Gerrit Analytics plugin
    see: https://gerrit.googlesource.com/plugins/analytics/+/master/README.md
- -u --url location/port of Gerrit server for extracting the analytics data
- -e --elasticIndex specify as <index>/<type> to be loaded in Elastic Search
    if not provided no ES export will be performed
- -o --out folder location for storing the output as JSON files
    if not provided data is saved to </tmp>/analytics-<NNNN> where </tmp> is
    the system temporary directory