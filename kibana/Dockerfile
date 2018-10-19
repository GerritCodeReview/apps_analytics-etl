FROM docker.elastic.co/kibana/kibana:5.5.2
RUN kibana-plugin remove x-pack
RUN /usr/local/bin/kibana-docker 2>&1 | grep -m 1 "Optimization of .* complete"
