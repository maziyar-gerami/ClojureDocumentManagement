(ns ci
  (:require 
            [clojure.tools.logging :as log]
            [dev.eastwood :as es]
            [org.corfield.build :as bb]))

(def lib 'wow/document-management)
(def version "0.1.0-SNAPSHOT")
(def -prod 'application.system)
(def -dev 'dev.app)

(defn lint "Run the linter." [opts]
  (es/run opts))

(defn run-tests "Run all app tests." [opts]
  (bb/run-tests opts))


(defn build-dev
  "Run the CI pipeline of tests (and build the uberjar)." [opts]
  (log/info (str "CI pipe running for '" lib "' version: " version))
  (-> opts
      (assoc :lib lib :version "SNAPSHOT" :main -dev 
             :transitive true :resource-dirs ["resources/dev/builds"] )
      lint
      run-tests
      (bb/clean)
      (bb/uber)))


(defn build-prod
  "Run the CI pipeline of tests (and build the uberjar)."
  [opts]
  (log/info (str "CI pipe running for '" lib "' version: " version))
  (-> opts
      (assoc :lib lib :version version :main -prod 
             :transitive true :resource-dirs ["resources/prod/builds"])
      (bb/clean)
      (bb/uber)))