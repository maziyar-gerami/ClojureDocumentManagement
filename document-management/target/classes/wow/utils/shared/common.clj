(ns wow.utils.shared.common
  (:require [clojure.instant :refer [read-instant-date]]
            [clojure.string :as str]
            [wow.utils.shared.log :as log])
  (:import (java.util UUID)))

(defmacro -->
  "A very similar macro to the thread-first in Clojure core;
   includes a relay to cut the thread in case of error. 
   **example:**
   ```clojure
   (--> 0
        inc
        str) ;;==>\"1\"
   ```
**example:**
   ```clojure
   (--> {:k 0}
        (update :k inc)
        (assoc :error :conflict) ;; <-- stops here.
        #(update % :k dec)
        (update : dec)) ;;==> {:k 1 :error :conflict}
   ```"
  {:vesion "0.9.0"}
  [x & forms]
  (let [g (gensym)
        steps (map (fn [step]
                     `(if (and (map? ~g) (contains? ~g :error))
                        ~g
                        (-> ~g ~step)))
                   forms)]
    `(let [~g ~x
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(defmacro -->>
  "A very similar macro to the thread-last in Clojure core;
   includes a relay to cut the thread in case of error. "
  {:version "0.9.0"}
  [expr & forms]
  (let [g (gensym)
        steps (map (fn [step] `(if (and (map? ~g) (contains? ~g :error))
                                 ~g
                                 (->> ~g ~step)))
                   forms)]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(defn presented?
  "True if value is presented for the key.
   
   **example:**
   ```clojure
   (presented? :b {:a \"a\"}) ;;==> false
   (presented? :b {:b nil}) ;;==> false
   (presented? :b {:b \"b\"}) ;;==> true
   ```"
  {:vesion "0.9.0"}
  [k m]
  (when (map? m) (and (contains? m k) (k m))))

(defonce uuid-pattern #"^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$")

(defn str->uuid
  "Cast from string to UUID or returns the value, polimorphic by 
   unary and binary arities in form of [id] [id-key map]
   
   **example:**
   ```clojure
   (str->uuid \"8afc3893-d7a4-42d3-a758-87baec198ac9\")
   ;;==> #uuid \"8afc3893-d7a4-42d3-a758-87baec198ac9\"
   (str->uuid  #uuid \"8afc3893-d7a4-42d3-a758-87baec198ac9\")
   ;;==> #uuid \"8afc3893-d7a4-42d3-a758-87baec198ac9\"
   (str->uuid 123)
   ;;==> 123
   ```
   **example:**
   
   ```clojure
   (let [m {:id \"8afc3893-d7a4-42d3-a758-87baec198ac9\"}]
   (str->uuid :id m))
   ;;==> {:id #uuid \"8afc3893-d7a4-42d3-a758-87baec198ac9\"}
   ```"
  {:version "0.9.0"}
  ([id]
   (try
     (if (and (string? id) (re-matches uuid-pattern id))
       (UUID/fromString id) id)
     (catch Exception e (do (log/warning "Casting uuid exception:" e) id))))
  ([k m]
   (try
     (if (and (string? (k m)) (re-matches uuid-pattern (k m)))
       (assoc m k (UUID/fromString (k m))) m)
     (catch Exception e (do (log/warning "Casting uuid exception:" e) m)))))

(defn keys->uuid
  "Converts keys ending with '_id' or '_by' to UUID values in a map.
   It takes a map 'm' as input and converts the keys ending with '_id' or '_by' to UUID values.
   The converted map is returned as the result.

   Example:
   (keys->uuid {:user_id \"123\", :created_by \"456\"})
   ;; => {:user_id #uuid \"123\", :created_by #uuid \"456\"}"
  {:version "0.9.0"}
  [m]
  (let [tuples (seq m)
        ->uuid
        (fn [t] (let [k (first t)
                      v (second t)]
                  (if (or (str/ends-with? (str k) "_id")
                          (str/ends-with? (str k) "_by"))
                    [k (str->uuid v)] t)))]
    (into {} (map ->uuid tuples))))

(defn str->date
  "Cast from string to java LocalDateTime in UTC or returns nil
   
   **example:**
   ```clojure
   (str->date \"2022-10-08T15:00:32.179853\")
   ;;==> #object[java.time.LocalDateTime 0x677fce88 \"2022-10-08T15:00:32.179853\"]
   ```
   **example:**

   ```clojure
   (let [m {:ts \"2022-10-08T15:00:32.179853\"}]
   (str->date :ts m))
   ;;==> {:ts #object[java.time.LocalDateTime 0x677fce88 \"2022-10-08T15:00:32.179853\"]}
   ```"
  {:version "0.9.0"}
  ([date]
   (try
     (when (string? date)
       (java.time.LocalDateTime/ofInstant (.toInstant (read-instant-date date))
                                          java.time.ZoneOffset/UTC))
     (catch Exception e (do (log/warning "Casting date exception:" e) nil))))
  ([k m]
   (try
     (if (string? (k m))
       (assoc m k (java.time.LocalDateTime/ofInstant
                   (.toInstant (read-instant-date (k m)))
                   java.time.ZoneOffset/UTC))
       (assoc m k nil))
     (catch Exception e (do (log/warning "casting date exception:" e)
                            (assoc m k nil))))))

(defn selected-keys->uuid
  "Cast provided keys of a map to uuid or nil returns
   
   **example:**
   ```clojure
   (let [m {:name \"John Doe\"
            :country \"8afc3893-d7a4-42d3-a758-87baec198ac9\"
            :state \"1afefeef-fda7-45a1-81ff-de21a9b5345a\"
            :user_id \"9d96c05f-0265-4686-b493-cd0a00dfeb89\"}]
   
   (keys->uuid m
              :country
              :state
              :user_id))
   
   ;;==> {:country #uuid \"8afc3893-d7a4-42d3-a758-87baec198ac9\"
   ;;     :state #uuid \"1afefeef-fda7-45a1-81ff-de21a9b5345a\"
   ;;     :user_id #uuid \"9d96c05f-0265-4686-b493-cd0a00dfeb89\"
   ;;     :name \"John Doe\"}
   ```"
  {:version "0.9.0"}
  [m & ks]
  (let [->uuid
        (fn [k]
          (fn [i]
            (update i k str->uuid)))]
    ((apply comp (map ->uuid ks)) m)))

(defn selected-keys->date
  "Cast provided keys of a map to java LocalDateTime in UTC or nil returns
     
   **example:**
   ```clojure
   (let [m {:name \"John Doe\"
            :birthdate \"2000-10-08T15:00:32.179853\"
            :marriage_date \"2022-10-08T15:00:32.179853\"}]
     
   (keys->date m
               :birthdate
               :marriage_date
               :user_id))
   ;;==> 
   ;;  {:birthdate #object[java.time.LocalDateTime 0x677fce88 \"2000-10-08T15:00:32.179853\"]
   ;;   :marriage_date #object[java.time.LocalDateTime 0x677fce88 \"2022-10-08T15:00:32.179853\"]
   ;;   :name \"John Doe\"}
   ```"
  {:version "0.9.0"}
  [m & ks]
  (let [->date
        (fn [k]
          (fn [i]
            (update i k #(str->date %))))]
    ((apply comp (map ->date ks)) m)))

(defn str->localDateTime
  "Cast from string to java LocalDateTime"
  {:version "0.9.0"}
  [date]
  (java.time.LocalDateTime/parse date))

(defn str->localDateTimeWithFormatter
  "Cast from string to java LocalDateTime with any standard format like
   \"yyyy-MM-dd'T'HH:mm:ss.SSSXXX\""
  {:version "0.9.0"}
  [date pattern]
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern pattern)]
    (java.time.LocalDateTime/parse date formatter)))

(defn inst->localDateTime
  "Cast from #inst to java LocalDateTime"
  {:version "0.9.0"}
  [inst_date]
  (java.time.LocalDateTime/ofInstant (.toInstant inst_date)
                                     java.time.ZoneOffset/UTC))

(defn keys->date
  "Converts keys ending with '_date' or '_at' to date values in the given map `m`.

   Parameters:
     - m: The map containing key-value pairs.

   Returns:
     A new map with the keys ending with '_date' or '_at' converted to date values.

   Example:
   (def m {:start_date \"2022-01-01\"
           :end_date \"2022-01-05\"
           :created_at \"2022-01-01T10:00:00Z\"})
   (keys->date m)

   The resulting map will have the keys ending with '_date' or '_at' converted to date values."

  {:version "0.9.0"}
  [m]
  (let [tuples (seq m)
        ->uuid
        (fn [t]
          (let [k (first t)
                v (second t)]
            (if (or (str/ends-with? (str k) "_date")
                    (str/ends-with? (str k) "_at"))
              [k (str->date v)]
              t)))]
    (into {} (map ->uuid tuples))))

(defn xor
  "Performs the \"Exclusive OR\" operation on two or more values.
   It yields true if exactly one (but not both) of the conditions is true.
   
   A xor B = (A nand !B) nand (!A nand B)
   
   Examples:
   (xor true true)   ; false
   (xor true false)  ; true
   (xor false true)  ; true
   (xor false false) ; false
   
   Usage:
   (xor 1 2)        ; false
   (xor 5 0)        ; false
   (xor 0 2 0)      ; true"
  {:version "0.9.0"}
  ([] nil)
  ([_] nil)
  ([a b] (and (or a b) (or (not a) (not b))))
  ([a b & more]
   (if (empty? more)
     (xor a b)
     (recur (xor a b) (first more) (rest more)))))


