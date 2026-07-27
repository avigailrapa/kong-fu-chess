FROM eclipse-temurin:26-jdk AS build
WORKDIR /kongfu
COPY lib lib
COPY src src
COPY app app
RUN CP=$(find lib -name '*.jar' | tr '\n' ':') && \
    PROC=$(find lib -name 'lombok*.jar') && \
    find src app -name '*.java' > sources.txt && \
    javac -d out -encoding UTF-8 -cp "$CP" -processorpath "$PROC" @sources.txt

FROM eclipse-temurin:26-jre
WORKDIR /kongfu
COPY --from=build /kongfu/out ./out
COPY --from=build /kongfu/lib ./lib
COPY assets assets
