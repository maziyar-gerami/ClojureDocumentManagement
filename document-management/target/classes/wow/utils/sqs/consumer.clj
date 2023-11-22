(ns wow.utils.sqs.consumer
  (:require [wow.utils.sqs.core :as sqs]
            [wow.utils.shared.log :as log]))

(defn- options->map
  "If options are provided as a map, return it as-is; otherwise,
   the options are provided as varargs and must be converted to a map"
  [options]
  (if (= 1 (count options))
    ; if only 1 option, assume it is a map, otherwise options are provided in varargs pairs
    (first options)
    ; convert the varags list into a map
    (apply hash-map options)))

(defn- start!
  [connection consumer poll-size poll-time retry-dely]
  (let [acker {:ack (partial sqs/ack connection)
               :nack (partial sqs/nack connection)}]
    (try
      (while true
        (let [messages (sqs/dequeue connection
                                    :limit poll-size
                                    :wait-time-seconds poll-time)]

          (when (not-empty messages)
            (log/trace "Message recieved: " messages)
            (consumer messages acker))))

      (catch Throwable t
        (log/error (str "Encountered exception or error while dequeueing. 
                     Waiting before trying again: " retry-dely "ms") t)
        (log/debug "Exception caught on consuming: " connection)
        (Thread/sleep retry-dely)))))

(defn consume!
  "Start consuming sqs queue and process the messages by a consumer.
   
   A consumer should take two args, first as a vec of messages &
   the second arg as an acker, **acker** is a map consist of *:ack* and *:nack* helpers for consumer.
   
   - :poll-time milliseconds of polling duration, default is MAX.
   - :poll-size number of messages will be retrieve in one poll. default is 1.
   - :retry-delay duration waiting between next poll request after a failure.
   - :client either will be passed in opts or created by Queue."
  [q consumer & opts]
  (let [options (options->map opts)
        connection (sqs/get-connection q :client (:client options))
        conn-poll-time (or (:poll-time options) 20) ;;ms
        msg-poll-size (or (:poll-size options) 1) ;;count
        retry-delay (or (:retry-delay options) 10000)] ;;ms

    (start! connection
            consumer
            msg-poll-size
            conn-poll-time
            retry-delay)))