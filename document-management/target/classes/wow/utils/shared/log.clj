(ns wow.utils.shared.log
  (:require [clojure.tools.logging :as cl]))

(defn fatal
  ([msg]
   {:version "0.9.0"}
   (cl/fatal msg))
  ([msg data]
   (cl/fatal msg data)))

(defn error
  ([msg]
   {:version "0.9.0"}
   (cl/error msg))
  ([msg data]
   (cl/error msg data)))

(defn info
  ([msg]
   {:version "0.9.0"}
   (cl/info msg))
  ([msg data]
   (cl/info msg data)))

(defn warning
  ([msg]
   {:version "0.9.0"}
   (cl/warn msg))
  ([msg data]
   (cl/warn msg data)))

(defn debug
  ([msg]
   {:version "0.9.0"}
   (cl/debug msg))
  ([msg data]
   (cl/debug msg data)))

(defn trace
  ([msg]
   {:version "0.9.0"}
   (cl/trace msg))
  ([msg data]
   (cl/trace msg data)))

(defn warn
  ([msg]
   {:version "0.9.0"}
   (cl/warn msg))
  ([msg data]
   (cl/warn msg data)))
