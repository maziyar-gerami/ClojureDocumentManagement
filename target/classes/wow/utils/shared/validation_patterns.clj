(ns wow.utils.shared.validation-patterns 
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]))

(defn optional 
  [s]
  {:version "0.9.0"}
  (spec/or :empty empty? :valid s))

(def short-str
  ;; Validation Pattern for required short string field.
  ;; version 0.9
  (spec/and string?
            (complement str/blank?)
            #(<= (count %) 100)))

(def long-str
  ;; Validation Pattern for required long string field.
  ;; version 0.9
  (spec/and string?
            (complement str/blank?)
            #(<= (count %) 500)))

(def n-america-num
  ;; Validation Pattern for required North of America phone number field.
  ;; examples:
  ;; 1234567890, 123-456-7890,
  ;; 123.456.7890, 123 456 7890,
  ;; (123) 456 7890, and all related combinations.
  ;; version 0.1
  (spec/and string?
            (complement str/blank?)
            (partial re-matches
                     #"^\(?([0-9]{3})\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})$")))

(def postal-code
  ;; Validation Pattern for required USA & Canada postal code field.
  ;; version 0.9
  (spec/and string?
            (complement str/blank?) 
            (spec/or :canada
                     #(re-matches
                       #"^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$" %)
                     :usa
                     #(re-matches #"^\d{5}(?:[-\s]\d{4})?$" %))))

(def email
  ;; Validation Pattern for required email field.
  ;; version 0.9
  (spec/and string?
            (complement str/blank?)
            (partial re-matches #"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")))


(def date
  ;; Validation Pattern for required date field according to RFC3339 standards.
  ;; examples:
  ;; 2022-11-28T08:16:24-00:00
  ;; version 0.9
  (partial instance? java.time.LocalDateTime))

(defn- is-past-date?
  ;; Check input date field is before current date.
  ;; return True if it is, otherwise return False.
  ;; version 0.9
  {:version "0.9.0"}
  [date]
  (let [now (java.time.LocalDateTime/now)]
    (.isAfter now date)))

(def past-date
  ;; Validation Pattern for required past date field.
  ;; version 0.9
  (spec/and (complement nil?)
            (partial instance? java.time.LocalDateTime)
            is-past-date?))

(def file-url
  ;; Validation Pattern for file URL pattern
  ;; version 0.9
  (spec/and string?
            (complement str/blank?)
            (fn [url]
              (let [pattern #"^(http|https|arn)://.+/([^/]+)$"
                    re (re-pattern pattern)]
                (re-matches re url)))))
