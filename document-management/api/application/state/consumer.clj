(ns application.state.consumer
  (:require 
   [application.state.core.company :refer [modify-info
                                           save-company
                                           modify-avatar
                                           remove-avatar
                                           ]]
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [wow.utils.shared.log :as log]
   [wow.utils.shared.common :refer [keys->uuid]]))

(defn- json-parser ;;TODO util it and depricate response-body-parser
  [msg]
  (let [->json #(json/read-str % :key-fn keyword)]
    (try (->json msg)
         (catch Exception e
           (log/error "Exception occurred while parsing JSON: " e)
           (log/debug "Failed parsing the json body: " msg)
           (assoc msg :error :internal-error)))))

(defn- call-api
  ;;TODO this should return either nil or response,
  ;;     depricate the last version in util
  [url]
  (log/debug "Asking for resource: " url)
  (try
    (:data
     (json/read-str
      (:body (client/get url))
      :key-fn keyword))
    (catch Exception e (do (log/error "failed to get url: " e)
                           (log/debug "failed to get url: " url)))))

;;--

(defmulti group-dispatcher (fn [_ body] (:group_id body)))

(defmethod group-dispatcher  :default  [_ _] true)

(defmethod group-dispatcher "company.profile" ;;TODO check convention
  [ctx msg]

  (let [{:keys [cdb]} ctx
        {:keys [event]} msg 
              company (-> msg
                          :ref
                          call-api
                          keys->uuid
                          )] 

    (cond
      (= event :company-created) (save-company cdb company)
      (= event :avatar-updated ) (modify-avatar cdb company)
      (= event :avatar-deleted) (remove-avatar cdb company)
      (= event :info-updated) (modify-info cdb company)
      :else true)))

(defn consumer
  [ctx messages acker]
  (when (not-empty messages)
    (let [{:keys [ack nack]} acker
          message (update (first messages) :body json-parser)
          message-body (read-string (:Message (:body message)))]
      (if (group-dispatcher ctx message-body)
        (ack message)
        (nack message)))))
