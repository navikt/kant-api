FROM scratch AS artifact
COPY build/libs/kant-api.jar /kant-api.jar

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:fac3b7b14c36abad6aa4be856f00dc5fd3be176ce66cd45b3cfa45ab23b76d1b
WORKDIR /app
ENV TZ="Europe/Oslo"
COPY --from=artifact /kant-api.jar /app/kant-api.jar
EXPOSE 8080
CMD ["-jar", "kant-api.jar"]
