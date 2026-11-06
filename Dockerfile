FROM scratch AS artifact
COPY build/libs/kant-api.jar /kant-api.jar

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
WORKDIR /app
ENV TZ="Europe/Oslo"
COPY --from=artifact /kant-api.jar /app/kant-api.jar
EXPOSE 8080
CMD ["-jar", "kant-api.jar"]
