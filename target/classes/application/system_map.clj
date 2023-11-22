(ns application.system-map
  (:require [application.routes :refer [routes]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.string :as str]
            [wow.utils.shared.log :as log]))

(defn- conf<-edn-file
  "source: filename or io/resource"
  [source]
  (with-open [r (io/reader (io/resource source))]
    (edn/read (java.io.PushbackReader. r))))

(defn- with-data-source
  [config-map]
  (let [ds (try
             {:qdb {:dbtype "postgres"
                    :host (System/getenv "READ_DB_SOURCE")
                    :dbname (System/getenv "READ_DB_NAME")
                    :username (System/getenv "READ_DB_USER")
                    :password (System/getenv "READ_DB_PASS")
                    :dataSourceProperties
                    {:read-only? true
                     :socketTimeout (-> "READ_DB_TIMEOUT"
                                        System/getenv
                                        Integer/parseInt)
                     :maximumPoolSize (-> "READ_DB_POOL_SIZE"
                                          System/getenv
                                          Integer/parseInt)
                     :leakDetectionThreshold (-> "READ_LEAK_THRESHOLD"
                                                 System/getenv
                                                 Integer/parseInt)}}
              :cdb {:dbtype "postgres"
                    :host (System/getenv "WRITE_DB_SOURCE")
                    :dbname (System/getenv "WRITE_DB_NAME")
                    :username (System/getenv "WRITE_DB_USER")
                    :password (System/getenv "WRITE_DB_PASS")
                    :dataSourceProperties
                    {:socketTimeout (-> "WRITE_DB_TIMEOUT"
                                        System/getenv
                                        Integer/parseInt)
                     :maximumPoolSize (-> "WRITE_DB_POOL_SIZE"
                                          System/getenv
                                          Integer/parseInt)
                     :leakDetectionThreshold (-> "WRITE_LEAK_THRESHOLD"
                                                 System/getenv
                                                 Integer/parseInt)}}}
             (catch Exception e
               (log/fatal "Failed to read data source properties: " e)))]

    (assoc config-map :ds ds)))

(defn- with-topic
  [config-map]
  (if (not-empty (System/getenv "TOPIC"))
    (assoc config-map :topic {:arn (System/getenv "TOPIC")
                              :region (System/getenv "AWS_REGION")})
    config-map))

(defn- with-server
  [config-map]
  (assoc config-map :server
         {:http/type (keyword (System/getenv "SERVER_TYPE"))
          :http/host (System/getenv "SERVER_HOST")
          :http/port (-> "SERVER_PORT"
                         System/getenv
                         Integer/parseInt)
          :http/allowed-origins ["*"]
          :http/join? false}))

(defn- with-queues
  [config-map]
  (if (not-empty (System/getenv "QUEUES"))
    (let [split-trim #(mapv str/trim (str/split % #","))
          queue-values   (split-trim (System/getenv "QUEUES"))
          queue-keys (mapv #(first (str/split % #".fifo")) queue-values)]
      (conj config-map (into {} (map #(assoc {} (keyword %1) {:queue-name %2}) queue-keys queue-values))))
    config-map))

(defn- conf<-sys-env
  []
  (let [config-map {}]
    (-> config-map
        with-data-source
        with-topic
        with-server
        with-queues)))

(defmulti load-conf identity)
(defmethod load-conf :dev [_] (conf<-edn-file "app_configs.edn"))
(defmethod load-conf :prod [_] (conf<-sys-env))
(defmethod load-conf :default [_] (conf<-edn-file "app_configs.edn"))

(defmulti build-server (fn [env _] env))

(defmethod build-server :prod
  [_ conf]
  {:env :prod
   ::http/routes routes
   ::http/type (:http/type conf)
   ::http/host (:http/host conf)
   ::http/port (:http/port conf)
   ::http/join? false})

(defmethod build-server :default
  [env conf]
  {:env env
   ::http/routes #(route/expand-routes (deref #'routes))
   ::http/type (:http/type conf)
   ::http/host (:http/host conf)
   ::http/port (:http/port conf)
   ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
   ::http/join? (:http/join? conf)})

(defn init
  [env]
  (let [conf (load-conf env)
        {:keys [topic
                ds
                server
                document-management-from-company-queue
                notification-from-all-queue
                company-desk-from-all-queue
                worker-desk-from-all-queue]} conf
        server (build-server env server)]

    (log/trace "Loaded System Config: " conf)
    {:sys/ds (-> ds
                 (select-keys [:qdb :cdb])
                 (assoc :env env))

     :sys/topic (assoc topic :env env)

     :sys/notification-from-all-queue (assoc notification-from-all-queue :env env)

     :sys/company-desk-from-all-queue (assoc company-desk-from-all-queue :env env)

     :sys/worker-desk-from-all-queue (assoc worker-desk-from-all-queue :env env)

     :sys/document-management-from-company-queue (assoc document-management-from-company-queue
                                                     :env env
                                                     :ds (ig/ref :sys/ds))
     
     

     :sys/ctx
     {:ds (ig/ref :sys/ds)
      :topic (ig/ref :sys/topic)
      :notification-from-all-queue (ig/ref :sys/notification-from-all-queue)
      :company-desk-from-all-queue (ig/ref :sys/company-desk-from-all-queue)
      :worker-desk-from-all-queue (ig/ref :sys/company-desk-from-all-queue)}

     :sys/server server
     :sys/system {:env env
                  :server (ig/ref :sys/server)
                  :ctx (ig/ref :sys/ctx)}}))