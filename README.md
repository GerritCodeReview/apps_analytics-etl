# spark-gerrit-analytics-etl
Spark ETL to extra analytics data from Gerrit Projects.

Job can be launched with the following parameters:

```
bin/spark-submit \
    --conf spark.es.nodes=es.mycompany.com \
    --conf spark.es.net.http.auth.user=elastic \
    --conf spark.es.net.http.auth.pass=changeme \
    $JARS/SparkAnalytics-assembly-1.0.jar \
    --since 2000-06-01 \
    --aggregate email_hour \
    --url http://gerrit.mycompany.com \
    -e gerrit/analytics
```
### Parameters
- since, until, aggregate are the same defined in Gerrit Analytics plugin
    see: https://gerrit.googlesource.com/plugins/analytics/+/master/README.md
- -u --url Gerrit server URL with the analytics plugins installed
- -e --elasticIndex specify as <index>/<type> to be loaded in Elastic Search
    if not provided no ES export will be performed
- -o --out folder location for storing the output as JSON files
    if not provided data is saved to </tmp>/analytics-<NNNN> where </tmp> is
    the system temporary directory