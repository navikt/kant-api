FROM scratch AS artifact
COPY build/libs/kant-api.jar /kant-api.jar

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:7afde2c565ed2664bbaf507c2ff02e050cfcb3e5c414ee69e24a8f80e9d1e31c
WORKDIR /app
ENV TZ="Europe/Oslo"
COPY --from=artifact /kant-api.jar /app/kant-api.jar
EXPOSE 8080
CMD ["-jar", "kant-api.jar"]
