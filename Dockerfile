FROM bellsoft/liberica-openjdk-alpine:17

EXPOSE 8080
ARG JAVA_OPTS
RUN ls

WORKDIR /app
ENV TZ "UTC"
ENV BOT_TOKEN "1220716581:AAFwCqgGdZy4TPfmOu4-Em6nw2Aw-Xhh8vw"
ENV CREATOR_ID "429854045"

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

COPY . /app

RUN ./gradlew clean build --no-daemon
RUN ls

ENTRYPOINT [ \
    "java", \
    "-Duser.language=ru", \
    "-Duser.timezone=${TZ}", \
    "-jar", "build/libs/intelligence-1.0-SNAPSHOT.jar" \
]