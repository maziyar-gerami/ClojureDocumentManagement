(ns wow.utils.sns.producer
  (:require[wow.utils.shared.log :as log])
  (:import [software.amazon.awssdk.regions Region]
           (software.amazon.awssdk.services.sns SnsClient)
           (software.amazon.awssdk.services.sns.model MessageAttributeValue PublishRequest PublishResponse)))

(defn- build-msg-attributes
  "A helper function to turn a Clojure keyword map into a Map<String,MessageAttributeValue>"
  [message-attribute-map]
  (when message-attribute-map
    (into {}
          (map (fn [[k v]] [(name k) (.. MessageAttributeValue builder
                                         (stringValue (str v))
                                         (dataType "String")
                                         build)])
               message-attribute-map))))

(defn pub!
  "publishing message to a topic, since we are using
   fifo topics and content de-duplication, then **:group_id** is mandatory.
   
    **these are opts:**
   
   - :message-attributes
   - :message-deduplication-id
   - :subject
   - :serialization-fn
   - :env
   
   default serialization is String.
   in case of missing :_id value for correlation,it will be generated."
  [topic msg & opts]
  (let [{:keys [client arn env]} topic
        {:keys [message-attributes
                message-deduplication-id
                subject
                serialization-fn] :or {serialization-fn pr-str}} opts
        {:keys [group_id _id] :or {_id (java.util.UUID/randomUUID)}} msg
        message-attr (build-msg-attributes (assoc message-attributes :_id _id))]
    (if-not (= env :prod)
      (do (log/trace (str "Publishing message from group: " group_id
                          " to: " arn
                          " message:" msg))
          (:_id msg))
      (try
        (let [^PublishRequest req
              (cond-> (.. PublishRequest builder
                          (message (serialization-fn msg))
                          (topicArn arn)
                          (messageGroupId group_id))
                message-attr (.messageAttributes message-attr)
                subject (.subject subject)
                message-deduplication-id (.messageDeduplicationId
                                          message-deduplication-id)
                :always .build)
              ^PublishResponse result (.publish client req)]
          (log/trace "Requesting to publish: " req)
          (.messageId result))
        (catch Exception e
          (log/error "Encountered exception while publishing message: " e)
          (log/debug (str "Failed to publish: " msg " to " topic) opts))))))

(defn
  ^SnsClient client
  ([] (try
        (SnsClient/create)
        (catch Exception e (log/error "Error in creating client: ") e)))
  ([conf]
   (let [{:keys [region]} conf]
     (try (.. SnsClient
         builder
         (region (Region/of region))
         build)
          (catch Exception e (log/error "Error in creating client: ") e)))))