(ns user
  (:require
   [integrant.repl :as ig-repl]
   [integrant.repl.state :as state]
   [application.system :as sys]))

(integrant.repl/set-prep! #(sys/init-sys :dev))

(defn system [] (or state/system (throw (ex-info "System not running" {}))))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment
  (go) 
  (halt) 
  (reset)
  (reset-all))