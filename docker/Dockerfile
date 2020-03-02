########################################################
# Build the project first
########################################################
ARG jar_version=1.12.16
ARG jar_name=mailproxy-$jar_version.jar

FROM gradle:6.0.1-jdk13 AS mailproxy-build-image
ARG jar_version
ARG jar_name

# build the executable jar
WORKDIR /usr/local/src
COPY . .
RUN gradle fatJar -Djar_version=$jar_version --no-daemon

########################################################
# Prepare the runtime image
########################################################
FROM openjdk:12-alpine

# set the jar name and final destination
ARG jar_version
ARG jar_name
ARG jar_root=/root
ARG jar_path=$jar_root+$jar_name
ENV env_jar_path=$jar_path

COPY --from=mailproxy-build-image /usr/local/src/docker/$jar_name $jar_path

COPY docker/mailproxy.properties $jar_root/mailproxy.properties
COPY docker/user.properties $jar_root/user.properties

EXPOSE 80/tcp
EXPOSE 25/tcp
EXPOSE 4777/tcp
EXPOSE 2500/tcp

ENTRYPOINT java -jar $env_jar_path
