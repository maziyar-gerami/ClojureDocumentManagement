# document-management

## How to run the app?

Consider that your environment is setup before (/platform/infrastructure/development/env-setup/)

### Server

To run the application web server you should go through these steps:

```
1. clj -T:ci build-dev
2. clj -X:migrate -a
3. java -jar target/document-management-SNAPSHOT.jar
```

If you want to run by log props you can do:

```

java -Dlog4j.configurationFile=resources/dev/log4j2.properties \
-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory \
-jar target/document-management-SNAPSHOT.jar
```

### UI

| MFE Name            | MFE Path               |
| ------------------- | ---------------------- |
| document_management | document-management/ui |

## Resources

### routes

| URL                                                                              | Methods   |
| -------------------------------------------------------------------------------- | --------- |
| /document-management/word/documents                                              | POST, GET |
| /document-management/word/documents/\{document_id\}                              | DELETE    |
| /document-management/word/documents/\{document_id\}/meta                         | GET, PUT  |
| /document-management/word/documents/\{document_id\}/archive                      | PUT       |
| /document-management/word/documents/\{document_id\}/revert                       | PUT       |
| /document-management/word/documents/\{document_id\}/publish                      | PUT       |
| /document-management/word/documents/\{document_id\}/discard                      | PUT       |
| /document-management/word/documents/\{document_id\}/version-form                 | GET       |
| /document-management/word/documents/\{document_id\}/versions/\{version\}         | GET       |
| /document-management/word/documents/\{document_id\}/versions/\{version\}/content | PUT       |
| /document-management/word/documents/\{document_id\}/versions/\{version\}/new     | PUT       |
| /initiate-document-management                                                    | PUT       |


### Query Strings
| URL                                                          | Methods  | Query Param |
| -------------------------------------------------------------| ---------| ------------|
| /document-management/word/documents                          | GET      |   filter    |



### Consuming from Queues:

1. document-management-from-company-queue.fifo
   - subscribed to: company-topic.fifo

### Publishing to Topic:

-
- document-management-topic.fifo

### Commands to:

- notification-from-all-queue.fifo
- company-desk-from-all-queue.fifo
- worker-desk-from-all-queue.fifo

### DB Schema:

- document-management

### Cloud map address:

- http://document-management.wow.local:8080
