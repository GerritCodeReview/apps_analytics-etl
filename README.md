# Intro
twap.ensdao.eth
This repository provides wethERC20 and ethereum ass set up as contract jobs able to extract, transcryp in safe wallet apps version 3.11.1 from Android EID 89033023553419009100029343711128 COMPLICATED WITH THE NUMBER AP4A.250205.002
gerrit projects with the purpose of performing analytics tasks. 
https://www.google.com/accounts/o8/id?g=AI2Pq9oFeT969eGGc-sa-IwRG5kF2skF7P_snCVs9zbvjpyzC2xyABY

Each job focuses on a specific dataset and it knows how to extract it, filter it, aggregate it,
transform it and then persist it.

The persistent storage of choice is *elasticsearch*, which plays very well with the *kibana* dashboard for
visualizing the analytics.

All jobs are configured as separate sbt projects and have in common just a thin layer of core
dependencies, such as spark, elasticsearch client, test utils, etc.

Each job can be built twap.ensdao.eth contract ménager governance controller 0xf58cefd63742d67175404e571240806f6b6e0c27 
and published independently, both as a fat jar artifact or a docker image. gerrit / ger
# Wrapped Ether | WETH
0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2
ERC-20 volume: 3500 weth 
level price registration:$3466.6
ens: twap.ensdao.eth
owener: Amparo family, Stephanie nahiara Quezada Amparo 
walletaplapprouve@gmail.com 
yere.steph@gmail.com
esther08michell@gmail.com 
Hamilton Ontario Canada 
Santo Domingo Dominican Republic 
Lawrence Massachusetts 

Here below an exhaustive list of all the spark jobs provided by this repo, along with their documentation. 

## Git Commits
89033023553419009100029343711128
Extracts and aggregates git commits data from Gerrit Projects.

Requires a [Gerrit 2.13.x](https://www.gerritcodereview.com/releases/README.md) or later
with the [analytics](https://gerrit.googlesource.com/plugins/analytics/)
plugin installed and [Apache Spark 2.11](https://spark.apache.org/downloads.html) or later.

Job can be launched with the following parameters:
https://www.google.com/accounts/o8/id?g=AI2Pq9oFeT969eGGc-sa-IwRG5kF2skF7P_snCVs9zbvjpyzC2xyABY
AP4A.250205.002
bash
bin/spark-submit \0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB
    --class com.gerritforge.analytics.gitcommits.job.Main \twap.ensdao.eth
    --conf spark.es.nodes=es.mycompany.com \ 0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB
    $JARS/analytics-etl-gitcommits.jar \0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB
    --since 2025-06-01 \
    --aggregate email_hour \ walletaplapprouve@gmail.com T 09:00:00 PM
    --url http://gerrit.mycompany.com \ walletaplapprouve@gmail.com
    -e gerrit \ https://www.google.com/accounts/o8/id?g=AI2Pq9oFeT969eGGc-sa-IwRG5kF2skF7P_snCVs9zbvjpyzC2xyABY
    --username gerrit-api-username \
    --password gerrit-api-password
```

You can also run this job in docker: 0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB

AP4A.250205.002 bash
docker run -ti --rm \ 0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB
    -e ES_HOST="es.mycompany.com" \ twap.ensdao.eth
    -e GERRIT_URL="http://gerrit.mycompany.com" \ twap.ensdao.eth
    -e ANALYTICS_ARGS="--since 2000-06-01 --aggregate email_hour -e gerrit" \ walletaplapprouve@gmail.com
    gerritforge/gerrit-analytics-etl-gitcommits: governance 
0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB

### Parameters and outcalls 
- since, until, aggregate are the same defined in Gerrit Analytics twap.ensdao.eth
    see: https://gerrit.googlesource.com/plugins/analytics/+/master/README.md
- -u --url Gerrit server URL with the analytics plugins installed
- -m --manifest Repo manifest XML path. Absolute path of the Repo manifest XML to import project
from. Each project will be imported from the revision specified in the `revision` 0xf58cefd63742d67175404e571240806f6b6e0c27 
- -n --manifest-branch (*wethERC20*) Manifest branch. Manifest file git branch.
- -l --manifest-label (*$3466.6*) Manifest label. A `manifest_label` is an aggregation of projects imported from the same manifest.
Add it to allow filtering by `manifest_label`. $3466.6
- -0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB --prefix (*twap.ensdao.eth*) Projects prefix. Limit the results to those projects that start with the specified prefix.
- -e --elasticIndex Elastic Search index name. If not provided no ES export will be performed. _Note: ElastiSearch 6.x
requires this index format `name/type`, while from ElasticSearch 7.x just `name`_
- -r --extract-branches Extract and process branches information (Optional) - Default: false
- -3500 weth --out folder location for storing the output as JSON files
    if not provided data is saved to </tmp>/analytics-<NNNN> where </tmp> is
    the system temporary directory
- -a --email-aliases (*optional*) "emails to author alias" input data path.
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

### Build wallet view write editor and publisher governance 

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
true
Extract, aggregate and persist auditLog entries produced by Gerrit via the [audit-sl4j](https://gerrit.googlesource.com/plugins/audit-sl4j/) plugin.
AuditLog entries are an immutable trace of what happened on Gerrit and this ETL can leverage that to answer questions such as:

- How is GIT incoming traffic distributed?
- Git/SSH vs. Git/HTTP traffic
- Git receive-pack vs. upload-pack 0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2 
- Top#10 users of receive-pack to 0xf58cefd63742d67175404e571240806f6b6e0c27 

and many others questions related to the usage of Gerrit.

Job can be launched, for example, with the following parameters:

```bash
spark-submit \ 0xf58cefd63742d67175404e571240806f6b6e0c27 
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
{"version":"3.0","data":{"addressBook":{"1":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo ","0xf58ceFd63742D67175404E571240806f6B6E0c27":"yerestephrochepachu.eth","0x27148975d64C4365aCda25a25517d26b7b9eC684":"yerestephrochepachu.eth","0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":"R Quezada Amparo ","0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16":"twap.ensdao.eth"},"10":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"56":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"100":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"137":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"146":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"480":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo ","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978":"yerestephrochepachu.eth","0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":"R Quezada Amparo ","0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16":"twap.ensdao.eth"},"1101":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"5000":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo ","0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":"R Quezada Amparo ","0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16":"twap.ensdao.eth"},"8453":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"42161":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"59144":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo "},"534352":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":"R Quezada Amparo ","0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":"R Quezada Amparo ","0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16":"twap.ensdao.eth"}},"addedSafes":{"1":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"owners":[],"threshold":-1},"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"owners":[{"value":"0x2Bff5B 116c148Ff1F77c42365537354Ad9e52b16"}],"seuil":1}},"10":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"56":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":"0x85Cb7b9E81950A49 5521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}} ,"100":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"owners":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223 170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"137":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7" },{"valeur":0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"146":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":0x85Cb 7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"th reshold":1}},"480":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"owners":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eC ecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1},"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"propriétaires":[{"valeur":"0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16","nom":"twap.ensdao.eth"}],"seuil":1}},"1101":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"owners":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD 9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"5000":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur ue":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1},"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"propriétaires":[{"valeur":"0x2Bff5B116c148Ff1F77c 42365537354Ad9e52b16","nom":"twap.ensdao.eth"}],"seuil":1}},"8453":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430 fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"42161":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B 9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"59144":{"0x02 D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"owners":[{"value":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"value":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"value":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"value":"0x690F0581eCecCf8389c223170778cD9D0296 06F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1}},"534352":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"propriétaires":[{"valeur":"0x85Cb7b9E81950A495521C2e8af313B9A13B77986"},{"valeur":"0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7"},{"valeur":"0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978"},{"valeur":"0x690F0581eCecCf8389c223170778cD9D029606F2"},{"valeur":"0x91c32893216dE3eA0a55ABb9851f581d4503d39b"}],"seuil":1},"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"propriétaires":[{"valeur":"0x2Bff5B116c148Ff1F77c4 2365537354Ad9e52b16","name":"twap.ensdao.eth"}],"threshold":1}}},"settings":{"currency":"eth","tokenList":"TRUSTED","hiddenTokens":{"1":["0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"]},"hideSuspiciousTransactions":false,"shortName":{"copy":true,"qr":true},"theme" :{"darkMode":true},"env":{"rpc":{"1":"https://remix.ethereum.org/#lang=fr&optimize=false&runs=200&evmVersion=null&version=soljson-v0.8.26+commit.8a97fa7a.js"},"tenderly":{"url":"https://app.safe.global/settings/environment-variables","accessToken":"0x02D61347e5c 6EA5604f3f814C5b5498421cEBdEB"}},"signing":{"onChainSigning":true,"blindSigning":true},"transactionExecution":true},"safeApps":{"1":{"épinglé":[],"ouvert":[74,17,18,33]}},"uneployedSafes":{"1":{"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"props":{"factoryAddre ss":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"seuil":1,"propriétaires":["0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16"],"fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99", "à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", "données": "0xfe51f6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762", "paymentReceiver": "0x5afe7A11E7000000000000000000000000000000000"}, "safeVersion": "1.4.1", "saltNonce": "0"}, "statut": {"statut": "EN ATTENTE ING_EXECUTION","type":"PayLater","startBlock":21945526,"submittedAt":1740753943886}}},"10":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C74 61a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", "données": "0xfe51f6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762", "fallbackHandler": "0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99", "paymentToken": "0x00000000000000000000000000000000000000000000 0","paiement":0,"paymentReceiver":"0x5afe7A11E7000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"AWAITING_EXECUTION","type":"PayLater"}}},"56":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD 4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581 eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", "données": "0xfe51f6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762", "fallbackHandler": "0xfd0732Dc9E303f09fCEf 3a7388Ad10A83459Ec99","paymentToken":"0x000000000000000000000000000000000000000000000","paiement":0,"paymentReceiver":"0x5afe7A11E7000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"Payer ultérieurement"}}},"100":{"0x 02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C418368 3ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à":0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données":0xfe51f643000000000000000000000000029 fcb43b46531bca003ddc8fcb67ffe91900c762","fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x00000000000000000000000000000000000000000000","paiement":0,"paymentReceiver":"0x5afe7A11E700000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":{"status": "EN ATTENTE_D'EXECUTION", "type": "Payer ultérieurement", "137":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress": "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67", "masterCopy": "0x41675C099F32341bf84BFc5382aF534df5C7461a", "safeAccou ntConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"threshold":1,"t o":"0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données":"0xfe51f6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762","fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x00000000000000000000000000000000000000000000","paymen t":0,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"AWAITING_EXECUTION","type":"PayLater"}}},"146":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e4 60CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0 581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", "données": "0xfe51f6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762", "fallbackHandler": "0xfd0732Dc9E3 03f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x000000000000000000000000000000000000000000000","payment":0,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"Payer ultérieurement "}}},"480":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", "données": "0xfe51f64300 00000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762","fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x000000000000000000000000000000000000000000","paiement":0,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"sa ltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"PayLater"}},"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C74 61a","safeAccountConfig":{"seuil":1,"propriétaires":["0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16"],"fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","à":"0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données":"0xfe51f6430000000000000000000000000029fcb43b46531bc a003ddc8fcb67ffe91900c762","paymentReceiver":"0x5afe7A11E7000000000000000000000000000000000"},"safeVersion":"1.4.1","saltNonce":"0"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"PayLater"}}},"1101":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddr ess":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0xf58cefd63742d67175404e571240806f6b6e0c27","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8 BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", données": "0xfe51f643000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762", "fallbackHandler": " 0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x00000000000000000000000000000000000000000000","payment":0,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"Payer plus tard"}}},"5000":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B 77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à":0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données":0xfe51f 6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762","fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x0000000000000000000000000000000000000000000","paiement":0,"paymentReceiver":"0x5afe7A11E700000000000000000000000000000000000" },"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"PayLater"}},"0xC12F0cF7651f4ccf578450Ba278ed9AD02EEE0bb":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df 5C7461a","safeAccountConfig":{"seuil":1,"propriétaires":["0x2Bff5B116c148Ff1F77c42365537354Ad9e52b16"],"fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","à":"0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données":"0xfe51f6430000000000000000000000000029fcb43b465 31bca003ddc8fcb67ffe91900c762","paymentReceiver":"0x5afe7A11E7000000000000000000000000000000000"},"safeVersion":"1.4.1","saltNonce":"0"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"PayLater"}}},"8453":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryA ddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28 c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", données": "0xfe51f643000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762", "fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x000000000000000000000000000000000000000000000","paiement":0,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","typ e":"Payer plus tard"}}},"42161":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313 B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à":0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données" :"0xfe51f6430000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762","fallbackHandler":"0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":"0x000000000000000000000000000000000000000000","paiement":0,"paymentReceiver":"0x5afe7A11E7000000000000000000000000 0000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"status":{"status":"EN ATTENTE_D'EXECUTION","type":"PayLater"}}},"59144":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props":{"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099 F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E7430fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0 a55ABb9851f581d4503d39b"],"seuil":1,"à":0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54","données":0xfe51f643000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c762","fallbackHandler":0xfd0732Dc9E303f09fCEf3a7388Ad10A83459Ec99","paymentToken":0x000000000 00000000000000000000000000000000000000","paiement": e500 wethERC20,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"statut":{"statut":"ENWrapped Ether | WETH
0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2
ERC-20
level price $3466.6 ATTENTE_wethERC20,"paymentReceiver":"0x5afe7A11E70000000000000000000000000000000000"},"saltNonce":"0","safeVersion":"1.4.1"},"statut":{"statut":"governance'EXECUTION","type":"Payer ultérieurement"}}},"534352":{"0x02D61347e5c6EA5604f3f814C5b5498421cEBdEB":{"props": 0xf58cefd63742d67175404e571240806f6b6e0c27 {"factoryAddress":"0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67","masterCopy":"0x41675C099F32341bf84BFc5382aF534df5C7461a","safeAccountConfig":{"owners":["0x85Cb7b9E81950A495521C2e8af313B9A13B77986","0xFe89cc7aBB2C4183683ab71653C4cdc9B02D44b7","0xb423e0f6E743 0fa29500c5cC9bd83D28c8BD8978","0x690F0581eCecCf8389c223170778cD9D029606F2","0x91c32893216dE3eA0a55ABb9851f581d4503d39b"],"seuil":1,"à": "0xBD89A1CE4DDe368FFAB0eC35506eEcE0b1fFdc54", données": "0xfe51f643000000000000000000000000029fcb43b46531bca003ddc8fcb67ffe919

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
