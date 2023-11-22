FROM eclipse-temurin:11-jre-alpine
WORKDIR /srv
RUN mkdir -p ./resources

ENV READ_DB_USER="wowuser"
ENV READ_DB_PASS="123"
ENV READ_DB_NAME="wowdb"
ENV READ_DB_SOURCE="read-localhost"
ENV READ_DB_TIMEOUT=30
ENV READ_DB_POOL_SIZE=10
ENV READ_LEAK_THRESHOLD=1000

ENV WRITE_DB_USER="wowuser"
ENV WRITE_DB_PASS="123"
ENV WRITE_DB_NAME="wowdb"
ENV WRITE_DB_SOURCE="write-localhost"
ENV WRITE_DB_TIMEOUT=30
ENV WRITE_DB_POOL_SIZE=10
ENV WRITE_LEAK_THRESHOLD=1000

ENV SERVER_TYPE=jetty
ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=8080

ENV QUEUES=document-management-from-company-queue.fifo,company-desk-from-all-queue.fifo,worker-desk-from-all-queue.fifo,notification-from-all-queue.fifo
ENV TOPIC=""

ENV AWS_REGION=""
ENV TZ=GMT

ENV JAR_FILE=document-management-0.1.0-SNAPSHOT.jar
ENV LOG_FILE=""

EXPOSE 8080

COPY ./target/$JAR_FILE .

CMD java \
	-Dlog4j2.configurationFile=prod/$LOG_FILE \
	-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory \
	-jar $JAR_FILE
