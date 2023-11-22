(ns application.state.syncer
  (:require [application.state.core.company :refer [save-company]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [wow.utils.shared.http :as http]
            [wow.utils.shared.log :as log]
            [wow.utils.shared.common :refer [keys->uuid]]))

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


(defn safe-invoke
  [service ds items]
  (doall
   (map
    (fn [item]
      (try
        (service ds item)
        (catch Exception e
          (log/error (str "Exeption occured during inserting" item " : " e))))
      item)
    items)))

(defn init-company [ctx]
  (let [{{:keys [cdb]} :ds} ctx
        companies (call-api
                "http://company.wow.local:8080/companies"
                ;; "http://localhost:8000/companies"
                )]
    (safe-invoke save-company cdb (mapv keys->uuid companies))))

(defn initiate-state
  [ctx params]
  (let [selected-source (not-empty (:source params))]

    (cond
      (= selected-source "company") (init-company ctx)
      :else (init-company ctx))
  {}))

(defn handler
  [request]
  (let [{:keys [ctx json-params]} request 
        response-model #(http/successful %)
        result (initiate-state ctx json-params)]
    (http/response-builder result response-model)))

