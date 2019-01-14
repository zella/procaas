FROM openjdk:11

ENV DOCKERVERSION=18.03.1-ce

ENV HTTP_PORT=8666

HEALTHCHECK CMD curl --fail http://localhost:${HTTP_PORT}/health_check || exit 1

#RUN apt-get update \
#  && apt-get install -y python3 \
#  && wget https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKERVERSION}.tgz \
#  && tar xzvf docker-${DOCKERVERSION}.tgz --strip 1 \
#                 -C /usr/local/bin docker/docker \
#  && rm docker-${DOCKERVERSION}.tgz


COPY pyaas-assembly-0.1-SNAPSHOT.jar /app/
#COPY {{JAR_FILE}} /app/

ENTRYPOINT java -jar /app/pyaas-assembly-0.1-SNAPSHOT.jar