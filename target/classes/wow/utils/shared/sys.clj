(ns wow.utils.shared.sys
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- load-edn
  "source: filename or io/resource"
  {:version "0.9.0"}
  [source]
  (with-open [r (io/reader source)]
    (edn/read (java.io.PushbackReader. r))))

(defn- add-ds
  "Adds database configurations to the system environment.
   It takes a 'sys-env' map as input and adds the database configurations to it.
   The resulting map with added configurations is returned."
  {:version "0.9.0"}
  [sys-env]
  (assoc sys-env
         :ds
         {:qdb {:dbtype "postgres"
                :host (System/getenv "READ_DB_SOURCE")
                :dbname (System/getenv "READ_DB_NAME")
                :username (System/getenv "READ_DB_USER")
                :password (System/getenv "READ_DB_PASS")
                :read-only? true
                :dataSourceProperties {:socketTimeout 30
                                       :maximumPoolSize 5}}
          :cdb {:dbtype "postgres"
                :host (System/getenv "WRITE_DB_SOURCE")
                :dbname (System/getenv "WRITE_DB_NAME")
                :username (System/getenv "WRITE_DB_USER")
                :password (System/getenv "WRITE_DB_PASS")
                :dataSourceProperties {:socketTimeout 30
                                       :maximumPoolSize 5}}}))

(defn- add-topic
  {:version "0.9.0"}
  [sys-env]
  (if (not-empty (System/getenv "TOPIC"))
    (assoc sys-env :topic {:arn (System/getenv "TOPIC")})
    sys-env))

(defn- add-server
  {:version "0.9.0"}
  [sys-env]
  (assoc sys-env :server
         {:http/type :jetty
          :http/host "0.0.0.0"
          :http/port 8080
          :http/allowed-origins ["*"]
          :http/join? false}))

(defn- split-trim [inp] (mapv str/trim (str/split inp #",")))

(defn- add-queue
  {:version "0.9.0"}
  [sys-env]
  (if (not-empty (System/getenv "QUEUES"))
    (let [queue-values   (split-trim (System/getenv "QUEUES"))
          queue-keys (mapv #(first (str/split % #".fifo")) queue-values)]
      (conj sys-env (into {} (map #(assoc {} (keyword %1) %2) queue-keys queue-values))))
    sys-env))