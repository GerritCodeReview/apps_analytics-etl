# Intro

This repository provides a set of spark ETL jobs able to extract, transform and persist data from
gerrit projects with the purpose of performing analytics tasks. 

Each job focuses on a specific dataset and it knows how to extract it, filter it, aggregate it,
transform it and then persist it.

The persistent storage of choice is *elasticsearch*, which plays very well with the *kibana* dashboard for
visualizing the analytics.

All jobs are configured as separate sbt projects and have in common just a thin layer of core
dependencies, such as spark, elasticsearch client, test utils, etc.

Each job can be built and published independently, both as a fat jar artifact or a docker image.  

# Spark ETL jobs

Here below an exhaustive list of all the spark jobs provided by this repo, along with their documentation. 

## Git Commits

Extracts and aggregates git commits data from Gerrit Projects.

Requires a [Gerrit 2.13.x](https://www.gerritcodereview.com/releases/README.md) or later
with the [analytics](https://gerrit.googlesource.com/plugins/analytics/)
plugin installed and [Apache Spark 2.11](https://spark.apache.org/downloads.html) or later.

Job can be launched with the following parameters:

```bash
bin/spark-submit \
    --class com.gerritforge.analytics.gitcommits.job.Main \
    --conf spark.es.nodes=es.mycompany.com \
    $JARS/analytics-etl-gitcommits.jar \
    --since 2000-06-01 \
    --aggregate email_hour \
    --url http://gerrit.mycompany.com \
    --botlike-filename-regexps='.+\.xml,.+\.bzl,BUILD,WORKSPACE,\.gitignore,plugins/,\.settings' \
    --ignore-binary-files=true \
    -e gerrit \
    --username gerrit-api-username \
    --password gerrit-api-password
```

You can also run this job in docker:

```bash
docker run -ti --rm \
    -e ES_HOST="es.mycompany.com" \
    -e GERRIT_URL="http://gerrit.mycompany.com" \
    -e ANALYTICS_ARGS="--since 2000-06-01 --aggregate email_hour -e gerrit" \
    gerritforge/gerrit-analytics-etl-gitcommits:latest
```

### Parameters
- since, until, aggregate are the same defined in Gerrit Analytics plugin
    see: https://gerrit.googlesource.com/plugins/analytics/+/master/README.md
- -u --url Gerrit server URL with the analytics plugins installed
- -p --prefix (*optional*) Projects prefix. Limit the results to those projects that start with the specified prefix.
- -e --elasticIndex Elastic Search index name. If not provided no ES export will be performed
- -r --extract-branches Extract and process branches information (Optional) - Default: false
- -o --out folder location for storing the output as JSON files
    if not provided data is saved to </tmp>/analytics-<NNNN> where </tmp> is
    the system temporary directory
- -a --email-aliases (*optional*) "emails to author alias" input data path.
- -k --ignore-ssl-cert allows to proceed even for server connections otherwise considered insecure.
- -n --botlike-filename-regexps comma separated list of regexps that identify a bot-like commit, commits that modify only files whose name is a match will be flagged as bot-like
- -I --ignore-binary-files boolean value to indicate whether binary files should be ignored from the analytics.
 This means that binary files will not be accounted for in "num_files", "num_distinct_files", "added_lines" and "deleted_lines" fields
 and they will not be listed in the "commits.files" field either. Default false.

  CSVs with 3 columns are expected in input.

  Here an example of the required files structure:
  ```csv
  author,email,organization
  John Smith,john@email.com,John's Company
  John Smith,john@anotheremail.com,John's Company
  David Smith,david.smith@email.com,Indipendent
  David Smith,david@myemail.com,Indipendent
  ```

  You can use the following command to quickly extract the list of authors and emails to create part of an input CSV file:
  ```bash
  echo -e "author,email\n$(git log --pretty="%an,%ae%n%cn,%ce"|sort |uniq )" > /tmp/my_aliases.csv
  ```
  Once you have it, you just have to add the organization column.

  *NOTE:*
  * **organization** will be extracted from the committer email if not specified
  * **author** will be defaulted to the committer name if not specified

### Build

#### JAR
To build the jar file, simply use

`sbt analyticsETLGitCommits/assembly`

#### Docker

To build the *gerritforge/gerrit-analytics-etl-gitcommits* docker container just run:

`sbt analyticsETLGitCommits/docker`.

If you want to distribute use:

`sbt analyticsETLGitCommits/dockerBuildAndPush`.

The build and distribution override the `latest` image tag too
Remember to create an annotated tag for a release. The tag is used to define the docker image tag too

## Audit Logs

Extract, aggregate and persist auditLog entries produced by Gerrit via the [audit-sl4j](https://gerrit.googlesource.com/plugins/audit-sl4j/) plugin.
AuditLog entries are an immutable trace of what happened on Gerrit and this ETL can leverage that to answer questions such as:

- How is GIT incoming traffic distributed?
- Git/SSH vs. Git/HTTP traffic
- Git receive-pack vs. upload-pack
- Top#10 users of receive-pack

and many others questions related to the usage of Gerrit.

Job can be launched, for example, with the following parameters:

```bash
spark-submit \
    --class com.gerritforge.analytics.auditlog.job.Main \
    --conf spark.es.nodes=es.mycompany.com \
    --conf spark.es.port=9200 \
    --conf spark.es.index.auto.create=true \
    $JARS/analytics-etl-auditlog.jar \
        --gerritUrl https://gerrit.mycompany.com \
        --elasticSearchIndex gerrit \
        --eventsPath /path/to/auditlogs \
        --ignoreSSLCert false \
        --since 2000-06-01 \
        --until 2020-12-01
```

You can also run this job in docker:

```bash
docker run \
    --volume <source>/audit_log:/app/events/audit_log -ti --rm \
    -e ES_HOST="<elasticsearch_url>" \
    -e GERRIT_URL="http://<gerrit_url>:<gerrit_port>" \
    -e ANALYTICS_ARGS="--elasticSearchIndex gerrit --eventsPath /app/events/audit_log --ignoreSSLCert false --since 2000-06-01 --until 2020-12-01 -a hour" \
    gerritforge/gerrit-analytics-etl-auditlog:latest
```

## Parameters

* -u, --gerritUrl              - gerrit server URL (Required)
* --username                   - Gerrit API Username (Optional)
* --password                   - Gerrit API Password (Optional)
* -i, --elasticSearchIndex     - elasticSearch index to persist data into (Required)
* -p, --eventsPath             - path to a directory (or a file) containing auditLogs events. Supports also _.gz_ files. (Required)
* -a, --eventsTimeAggregation  - Events of the same type, produced by the same user will be aggregated with this time granularity: 'second', 'minute', 'hour', 'week', 'month', 'quarter'. (Optional) - Default: 'hour'
* -k, --ignoreSSLCert          - Ignore SSL certificate validation (Optional) - Default: false
* -s, --since                  - process only auditLogs occurred after (and including) this date (Optional)
* -u, --until                  - process only auditLogs occurred before (and including) this date (Optional)
* -a, --additionalUserInfoPath - path to a CSV file containing additional user information (Optional). Currently it is only possible to add user `type` (i.e.: _bot_, _human_).
If the type is not specified the user will be considered _human_.

  Here an additional user information CSV file example:
  ```csv
    id,type
    123,"bot"
    456,"bot"
    789,"human"
  ```

### Build

#### JAR
To build the jar file, simply use

`sbt analyticsETLAuditLog/assembly`

#### Docker

To build the *gerritforge/gerrit-analytics-etl-auditlog* docker image just run:

`sbt analyticsETLAuditLog/docker`.

If you want to distribute it use:

`sbt analyticsETLAuditLog/dockerBuildAndPush`.

The build and distribution override the `latest` image tag too.


# Development environment

A docker compose file is provided to spin up an instance of Elastisearch with Kibana locally.
Just run `docker-compose up`.

## Caveats

* If you want to run the git ETL job from within docker against containerized elasticsearch and/or gerrit instances, you need
  to make them reachable by the ETL container. You can do this by spinning the ETL within the same network used by your elasticsearch/gerrit container (use `--network` argument)

* If elasticsearch or gerrit run on your host machine, then you need to make _that_ reachable by the ETL container.
  You can do this by providing routing to the docker host machine (i.e. `--add-host="gerrit:<your_host_ip_address>"` `--add-host="elasticsearch:<your_host_ip_address>"`)

  For example:

  * Run gitcommits ETL:
  ```bash
  HOST_IP=`ifconfig en0 | grep "inet " | awk '{print $2}'` \
      docker run -ti --rm \
          --add-host="gerrit:$HOST_IP" \
          --network analytics-etl_ek \
          -e ES_HOST="elasticsearch" \
          -e GERRIT_URL="http://$HOST_IP:8080" \
          -e ANALYTICS_ARGS="--since 2000-06-01 --aggregate email_hour -e gerrit" \
          gerritforge/gerrit-analytics-etl-gitcommits:latest
  ```

  * Run auditlog ETL:
    ```bash
    HOST_IP=`ifconfig en0 | grep "inet " | awk '{print $2}'` \
        docker run -ti --rm --volume <source>/audit_log:/app/events/audit_log \
        --add-host="gerrit:$HOST_IP" \
        --network analytics-wizard_ek \
        -e ES_HOST="elasticsearch" \
        -e GERRIT_URL="http://$HOST_IP:8181" \
        -e ANALYTICS_ARGS="--elasticSearchIndex gerrit --eventsPath /app/events/audit_log --ignoreSSLCert true --since 2000-06-01 --until 2020-12-01 -a hour" \
        gerritforge/gerrit-analytics-etl-auditlog:latest
    ```

* If Elastisearch dies with `exit code 137` you might have to give Docker more memory ([check this article for more details](https://github.com/moby/moby/issues/22211))

* Should ElasticSearch need authentication (i.e.: if X-Pack is enabled), credentials can be passed through the *spark.es.net.http.auth.pass* and *spark.es.net.http.auth.user* parameters.

* If the dockerized spark job cannot connect to elasticsearch (also, running on docker) you might need to tell elasticsearch to publish
the host to the cluster using the \_site\_ address.

```
elasticsearch:
    ...
    environment:
       ...
      - http.host=0.0.0.0
      - network.host=_site_
      - http.publish_host=_site_
      ...
```

See [here](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-network.html#network-interface-values) for more info

## Build all

To perform actions across all jobs simply run the relevant *sbt* task without specifying the job name. For example:

* Test all jobs: `sbt test`
* Build jar for all jobs: `sbt assembly`
* Build docker for all jobs: `sbt docker`