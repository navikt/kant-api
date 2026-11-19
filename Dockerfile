FROM scratch AS artifact
COPY build/libs/kant-api.jar /kant-api.jar

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:734fe4196c703f8f1f332dadde4fee56c1f09971ececa775cb2578d8475d730f
WORKDIR /app
ENV TZ="Europe/Oslo"
COPY --from=artifact /kant-api.jar /app/kant-api.jar
EXPOSE 8080
CMD ["-jar", "kant-api.jar"]
