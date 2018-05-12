#!/bin/sh
/wait-for-elasticsearch

echo "** Creating Gerrit index"
curl -XPUT "http://elasticsearch:9200/gerrit?pretty" -H 'Content-Type: application/json'

echo "** Creating gerrit_changes index and deploy change mapping"
for file in `ls -v /elasticsearch-mappings/*.json`;
    do
        echo "--> $file";
        index_name=$(echo ${file} | cut -d'.' -f1 | cut -d'/' -f2)
        echo "Index Name: $index_name"
        /usr/lib/node_modules/elasticdump/bin/elasticdump \
        --output=http://elasticsearch:9200/$index_name \
        --input=$file \
        --type=mapping \
        --headers '{"Content-Type": "application/json"}';
done;

echo "** Importing Kibana setting from: "
for file in `ls -v /kibana-config/*.json`;
    do  echo "--> $file";
        /usr/lib/node_modules/elasticdump/bin/elasticdump \
        --output=http://elasticsearch:9200/.kibana \
        --input=$file \
        --type=data \
        --headers '{"Content-Type": "application/json"}';
done;