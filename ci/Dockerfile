FROM openjdk:11

ENV HTTP_PORT=8666

COPY ci/procaas.jar /app/

EXPOSE ${HTTP_PORT}

HEALTHCHECK CMD curl --fail -s http://localhost:${HTTP_PORT}/about || exit 1

ENTRYPOINT java -jar /app/procaas.jar