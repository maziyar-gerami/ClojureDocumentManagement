(ns dev.app
  (:gen-class)
  (:require [application.system :as sys]
            [integrant.core :as ig]
            [wow.utils.shared.log :as log]))

(defn -main
  "This is an entry-point but in dev mode"
  []
  (log/info "Starting system in DEV mode:")
  (ig/init (ig/prep (sys/init-sys :dev))))