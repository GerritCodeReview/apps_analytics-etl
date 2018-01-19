FROM gerritforge/jw2017-spark

RUN apk add --no-cache wget
RUN mkdir -p /app

COPY ./target/scala-2.11/GerritAnalytics-assembly-1.0.jar /app
