FROM bellsoft/liberica-openjdk-alpine:17

EXPOSE 8080
#ARG JAVA_OPTS

WORKDIR /app
ENV TZ "UTC"

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

COPY . .

RUN ls

RUN ./gradlew clean build kotlinSourcesJar

ENTRYPOINT exec java \
# для сертификатов минцифры. https://wiki.sberned.ru/pages/viewpage.action?pageId=168767227
    -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
    -Duser.language=ru \
    -Duser.region=RU \
    -Duser.timezone=${TZ} \
    -Djava.security.egd=file:/dev/./urandom  \
    -jar app.jar

COPY build/libs/intelligence-*.jar app.jar