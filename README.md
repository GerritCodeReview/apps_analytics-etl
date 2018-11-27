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

Extracts and aggregate git commits data from Gerrit Projects.

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
    --events file:///tmp/gerrit-events-export.json \
    --writeNotProcessedEventsTo file:///tmp/failed-events \
    -e gerrit \
    --username gerrit-api-username \
    --password gerrit-api-password
```

You can also run this job in docker:

```bash
docker run -ti --rm \
    -e ES_HOST="es.mycompany.com" \
    -e GERRIT_URL="http://gerrit.mycompany.com" \
    -e ANALYTICS_ARGS="--since 2000-06-01 --aggregate email_hour --writeNotProcessedEventsTo file:///tmp/failed-events -e gerrit" \
    gerritforge/gerrit-analytics-etl-gitcommits:latest
```

### Parameters
- since, until, aggregate are the same defined in Gerrit Analytics plugin
    see: https://gerrit.googlesource.com/plugins/analytics/+/master/README.md
- -u --url Gerrit server URL with the analytics plugins installed
- -p --prefix (*optional*) Projects prefix. Limit the results to those projects that start with the specified prefix.
- -e --elasticIndex Elastic Search index name. If not provided no ES export will be performed
- -o --out folder location for storing the output as JSON files
    if not provided data is saved to </tmp>/analytics-<NNNN> where </tmp> is
    the system temporary directory
- -a --email-aliases (*optional*) "emails to author alias" input data path.

- --events location where to load the Gerrit Events
    If not specified events will be ignored
- --writeNotProcessedEventsTo location where to write a TSV file containing the events we couldn't process
    with a description fo the reason why
- -k --ignore-ssl-cert allows to proceed even for server connections otherwise considered insecure.


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

### Caveats
* If you want to run the git commits ETL job from within docker you need to make elasticsearch and gerrit available to it.
  You can do this by:

    * spinning the container within the same network used by your elasticsearch container (`analytics-etl_ek` if you used the docker-compose provided by this repo)
    * provide routing to the docker host machine (via `--add-host="gerrit:<your_host_ip_address>"`)

  For example:

  ```bash
  HOST_IP=`ifconfig en0 | grep "inet " | awk '{print $2}'` \
      docker run -ti --rm \
           --add-host="gerrit:$HOST_IP" \
          --network analytics-etl_ek \
          -e ES_HOST="elasticsearch" \
          -e GERRIT_URL="http://$HOST_IP:8080" \
          -e ANALYTICS_ARGS="--since 2000-06-01 --aggregate email_hour --writeNotProcessedEventsTo file:///tmp/failed-events -e gerrit" \
          gerritforge/gerrit-analytics-etl-gitcommits:latest
  ```

# Development environment

A docker compose file is provided to spin up an instance of Elastisearch with Kibana locally.
Just run `docker-compose up`.

## Caveats

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