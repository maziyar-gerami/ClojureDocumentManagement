(ns wow.utils.sqs.core
  (:import
   (com.amazonaws.services.sqs.model
    Message
    MessageAttributeValue
    ReceiveMessageRequest)
   (com.amazonaws.services.sqs
    AmazonSQS
    AmazonSQSClientBuilder))
  (:require [wow.utils.shared.log :as log]))

(defn- parse-queue-name
  [queue-name]
  (let [[_ queue-name fifo?] (re-matches #"([A-Za-z0-9_-]+)(\.fifo)?" queue-name)]
    (when queue-name
      {:name queue-name
       :fifo? (boolean fifo?)})))

(defn- valid-queue-name?
  "Returns true if an SQS queue name is valid, false otherwise"
  [queue-name]
  (and (some? (parse-queue-name queue-name))
       (<= (count queue-name) 80)))

(defn validate-queue-name!
  [queue-name]
  (when-not (valid-queue-name? queue-name)
    (log/error "Ecountered error by invalid queue name: " queue-name)
    (throw (IllegalArgumentException. "Invalid QUEUE Name: " queue-name))))

(defn default-client [] (AmazonSQSClientBuilder/defaultClient))

(defn get-connection
  "Make an SQS connection to a queue identified by string queue-name. This
  should be done once at startup time.
  Will throw QueueDoesNotExistException if the queue does not exist."
  [^String queue-name & {:keys [client] :as options}]
  (validate-queue-name! queue-name)
  (try
    (let [^AmazonSQS client (or client (default-client))
          queue-url (.getQueueUrl (.getQueueUrl client queue-name))]

      (log/info "Getting connection to SQS queue:" queue-url)
      {:client client
       :queue-name queue-name
       :queue-url queue-url})
    (catch Exception e
      (do (log/error "Caught exception on creating SQS client: " e)
          (log/debug "Failed to connect: " queue-name)))))

;;;;;;;;;;
;; message

(defn- clojurify-message-attributes
  [^Message msg]
  (let [javafied-message-attributes (.getMessageAttributes msg)
        parser (fn [[k ^MessageAttributeValue mav]]
                 [(keyword k) (case (.getDataType mav)
                                "String" (.getStringValue mav)
                                "Number" (.getStringValue mav)
                                "Binary" (.getBinaryValue mav)
                                (.getStringValue mav))])]
    (into {} (map parser javafied-message-attributes))))

(defn- message-map
  [queue-url ^Message msg]
  {:attributes (.getAttributes msg)
   :message-attributes (clojurify-message-attributes msg)
   :body (.getBody msg)
   :body-md5 (.getMD5OfBody msg)
   :id (.getMessageId msg)
   :receipt-handle (.getReceiptHandle msg)
   :source-queue queue-url})

(defn- receive
  "Receives one or more messages from the queue specified by the given URL.
  Input:
    client - AmazonSQS
    queue-url - url to the SQS resource
    Optional keyword arguments:
    :limit - between 1 (default) and 10, the maximum number of messages to receive
    :visibility - seconds the received messages should not be delivered to other
              receivers; defaults to the queue's visibility attribute
    :attributes - a collection of string names of :attributes to include in
              received messages; e.g. #{\"All\"} will include all attributes,
              #{\"SentTimestamp\"} will include only the SentTimestamp attribute, etc.
              Defaults to the empty set (i.e. no attributes will be included in
              received messages).
              See the SQS documentation for all support message attributes.
    :wait-time-seconds - enables long poll support. time is in seconds, bewteen
              0 (default - no long polling) and 20.
              Allows Amazon SQS service to wait until a message is available
              in the queue before sending a response.
              See the SQS documentation at (http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html)
  Returns a seq of maps with these slots:
    :attributes - message attributes
    :body - the string body of the message
    :body-md5 - the MD5 checksum of :body
    :id - the message's ID
    :receipt-handle - the ID used to delete the message from the queue after
              it has been fully processed.
    :source-queue - the URL of the queue from which the message was received"
  [^AmazonSQS client queue-url & {:keys [limit
                                         visibility
                                         wait-time-seconds
                                         ^java.util.Collection attributes
                                         ^java.util.Collection message-attribute-names]}]
  (let [limit (Integer/valueOf (int (max (min limit 10) 1)))
        req (.withMessageAttributeNames
             (.withAttributeNames
              (.withMaxNumberOfMessages
               (ReceiveMessageRequest. queue-url) limit)
              attributes)
             message-attribute-names)
        req
        (if wait-time-seconds
          (.withWaitTimeSeconds req (Integer/valueOf (int wait-time-seconds)))
          req)
        req
        (if visibility
          (.withVisibilityTimeout req (Integer/valueOf (int visibility)))
          req)]

    (map (partial message-map queue-url)
         (.getMessages (.receiveMessage client req)))))

(defn dequeue*
  "Read messages from a queue. If there is nothing to read before poll-timeout-seconds, return [].
  This does *not* remove the messages from the queue! For that, see ack.
  Does not catch Exceptions that might be thrown while attempting to receive messages from the queue."
  [{:keys [client queue-name queue-url]}
   & {:keys [limit poll-time attributes message-attributes]
      :or   {limit              1
             poll-time          20
             attributes         #{"All"}
             message-attributes #{"All"}}}]
  (let [fin
        (fn [m]
          (log/trace  (pr-str "Dequeued from queue %s message %s"
                              queue-name m))
          (assoc m :queue-name queue-name))]

    (log/trace (pr-str "Attempting dequeue from %s" queue-name))
    (map fin (receive client
                      queue-url
                      :wait-time-seconds poll-time
                      :limit limit
                      :attributes attributes
                      :message-attribute-names message-attributes))))

(defn dequeue
  "Read messages from a queue. If there is nothing to read before
  poll-timeout-seconds, return [].
  This does *not* remove the messages from the queue! For that, see ack.
  In case of exception, logs the exception and returns []."
  [& args]
  (try
    (apply dequeue* args)
    (catch Exception e
      (log/error "Encountered exception while dequeueing." e)
      (log/debug "Dequeueing info: " args)
      [])))

(defn ack
  "Remove the message from the queue.
  Input:
    connection - as created by mk-connection
    message - the message to ack"
  [{:keys [^AmazonSQS client
           ^String queue-url]}
   {:keys [^String receipt-handle]}]
  (try
    (log/trace "Acking message: " receipt-handle)
    (.deleteMessage client queue-url receipt-handle)
    (catch Exception e
      (do
        (log/error "Caught exception while acking message:" e)
        (log/debug (str "Client" client
                        "has failed to ack: " receipt-handle
                        "on queue: " queue-url))))))

(defn nack
  "Put the message back on the queue.
  Input:
    connection - as created by mk-connection
    message - the message to nack
    nack-visibility-seconds (optional) - How long to wait before retrying a failed compute request.Defaults to 0."
  ([connection message]
   (try
     (nack connection message 0)
     (catch Exception e
       (do (log/error "Nacking message failed due to an exception: " e)
           (log/debug "Nacking failed for: " message)))))
  ([{:keys [^AmazonSQS client
            ^String queue-url]}
    {:keys [^String receipt-handle]}
    nack-visibility-seconds]
   (log/warning "Nacking message: " receipt-handle)
   (.changeMessageVisibility client
                             queue-url
                             receipt-handle
                             (int nack-visibility-seconds))))


