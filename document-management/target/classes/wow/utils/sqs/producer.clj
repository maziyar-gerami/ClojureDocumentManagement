(ns wow.utils.sqs.producer
  (:require [wow.utils.shared.log :as log])
  (:import
   (com.amazonaws.services.sqs.model
    MessageAttributeValue
    SendMessageRequest)
   (com.amazonaws.services.sqs AmazonSQS)))

(defn- build-msg-attributes
  "A helper function to turn a Clojure keyword map into a Map<String,MessageAttributeValue>"
  [message-attribute-map]
  (when message-attribute-map
    (into {}
          (map (fn [[k v]] [(name k) (-> (MessageAttributeValue.)
                                         (.withStringValue (str v))
                                         (.withDataType "String"))])
               message-attribute-map))))

(defn- send-message
  "Sends a new message with the given string body to the queue specified
  by the string URL.  Returns a map with :id and :body-md5 slots."
  [^AmazonSQS client queue-url message opts]
  (let [{:keys [message-attributes
                message-group-id
                message-deduplication-id]} opts
        message-attribute-value-map (build-msg-attributes message-attributes)
        send-message-request
        (cond-> (SendMessageRequest. queue-url message)
          message-attribute-value-map
          (.withMessageAttributes message-attribute-value-map)
          message-group-id
          (.withMessageGroupId message-group-id)
          message-deduplication-id
          (.withMessageDeduplicationId message-deduplication-id))
        resp (.sendMessage client send-message-request)]

    {:id (.getMessageId resp)
     :body-md5 (.getMD5OfMessageBody resp)}))

(defn enqueue!
  "Enqueues a message to a queue.
   
  Input:
   
    1. queue-connection - A connection to a queue made with mk-connection
    2. message - The message to be placed on the queue
   
    **Optional arguments:**

    - :message-attributes - A map of SQS Attributes to apply to this message.
    - :serialization-fn - A function that serializes the message you want to enqueue to a string
    - :message-group-id - This parameter applies to FIFO queues only
    - :message-deduplication-id - This parameter applies to FIFO queues only
    By default, pr-str will be used"
  [queue-connection message opts]
  (let [{:keys [client queue-name queue-url]} queue-connection
        {:keys [serialization-fn]
         :or {serialization-fn pr-str}} opts]
    (log/trace (str "Enqueueing: " message " to: " queue-name))

    (try
      (send-message client queue-url
                    (serialization-fn message)
                    (dissoc opts :serialization-fn))
      (catch Exception e
        (do (log/error "Enqueueing message failed, Exception accured: " e)
            (log/debug "Enqueue failed for: " message))))))