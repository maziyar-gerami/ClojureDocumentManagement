(ns application.system
  (:gen-class)
  (:require [application.state.consumer :as consumer]
            [application.system-map :as system-map] 
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [wow.utils.shared.ds :as ds]
            [wow.utils.shared.http :as utils-http]
            [wow.utils.shared.log :as log]
            [wow.utils.sns.producer :as sns]
            [wow.utils.sqs.consumer :as sqs]
            [wow.utils.sqs.core :as sqs-core]))



;;;;-system states
(defmethod ig/init-key :sys/ds
  [_ ds]
  (log/info "Creating W/R data source connections...")
  (let [{:keys [env qdb cdb]} ds
        ds (if (= env :prod) (-> {}
                                 (assoc :qdb (ds/create-connection-pool qdb))
                                 (assoc :cdb (ds/create-connection-pool cdb)))
               (select-keys ds [:qdb :cdb]))]
    (log/info "Data source attached: " ds)
    ds))

(defmethod ig/halt-key! :sys/ds
  [_ ds]
  (do
    (log/info "Closing DB connections...")
    (ds/close-ds-connection (:qdb ds))
    (ds/close-ds-connection (:cdb ds))
    (log/info "Data source detached: " ds)))

(defmulti server-env-manager (fn [env _] env))
(defmethod server-env-manager :test [_ server] server)
(defmethod server-env-manager :default [_ server]
  (let [s (http/start server)] (log/info "Server started: " server) s))

(defmethod ig/init-key :sys/notification-from-all-queue
  [_ notification-from-all-queue]
  (let [{:keys [queue-name env]} notification-from-all-queue]
    (when (= env :prod)
          (sqs-core/get-connection queue-name))))

(defmethod ig/halt-key! :sys/notification-from-all-queue
  [_ conn]
  (assoc conn :queue-name nil)) ;;TODO

(defmethod ig/init-key :sys/company-desk-from-all-queue
  [_ company-desk-from-all-queue]
  (let [{:keys [queue-name env]} company-desk-from-all-queue]
    (when (= env :prod)
          (sqs-core/get-connection queue-name))))

(defmethod ig/halt-key! :sys/company-desk-from-all-queue
  [_ conn]
  (assoc conn :queue-name nil)) ;;TODO

(defmethod ig/init-key :sys/worker-desk-from-all-queue
  [_ worker-desk-from-all-queue]
  (let [{:keys [queue-name env]} worker-desk-from-all-queue]
    (when (= env :prod)
      (sqs-core/get-connection queue-name))))

(defmethod ig/halt-key! :sys/worker-desk-from-all-queue
  [_ conn]
  (assoc conn :queue-name nil)) ;;TODO



(defmethod ig/init-key :sys/server [_ server] server)

(defmethod ig/init-key :sys/topic [_ t]
  (let [{:keys [env]} t]
    (if (= env :prod)
      (assoc t :client (sns/client t))
      t)))

(defmethod ig/init-key :sys/ctx [_ ctx] ctx)

(defmethod ig/init-key :sys/document-management-from-company-queue
  [_ document-management-from-company-queue]
  (let [{:keys [queue-name ds env]} document-management-from-company-queue
        consume<- (partial consumer/consumer ds)]
    (cond
      (empty? queue-name)
      (log/error (str "Queue name not found: " queue-name))
      (= env :prod) (do
                      (log/info "Start Consuming: " queue-name)
                      (future (sqs/consume! queue-name consume<-)))
      :else (log/info "Skip consumer on: " queue-name))))


(defmethod ig/init-key :sys/system
  [_ {:keys [server ctx env] :as sys}]
  (let [start-in-env (partial server-env-manager env)]
    (log/info "Starting web server in profile: " env)
    (-> server
        (http/default-interceptors)
        (update ::http/interceptors conj (utils-http/with-ctx ctx))
        http/create-server
        start-in-env
        ((partial assoc sys :server)))))

(defmethod ig/halt-key! :sys/system
  [_ {:keys [server] :as sys}]
  (log/info "Stoping web-server...")
  (http/stop server)
  (assoc sys :server nil))

(defn init-sys [profile] (system-map/init profile))

(defn -main []
  (log/info "Starting system in PROD mode:")
  (ig/init (ig/prep (init-sys :prod))))
