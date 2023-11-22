(ns word.core.model
  (:require
   [clojure.string :as str]
   [clojure.data.json :as json]
   [clojure.spec.alpha :as spec]
   [wow.utils.shared.validation-patterns :as p]
   [wow.utils.shared.common :refer [str->uuid xor -->]]
   [wow.utils.shared.log :as log]))

(defn- transform-members
  [document]
  (let [->uuid (fn [w] (map #(str->uuid :worker_id %) w))]
    (update document :members ->uuid)))

(defn transform
  [document]
  (-> document
      (update :title #(when (string? %) (str/trim %)))
      (update :type #(when (string? %) (str/trim %)))
      (update :origin #(when (string? %) (str/trim %)))
      transform-members))

(defn auto-save-transform
  [subsection]
  (-> subsection
      (update :content
              #(if (map? %) (json/write-str %) %))
      (update :subsection_id #(str->uuid %))))


(spec/def ::document_id uuid?)

(spec/def ::title p/short-str)

(spec/def ::type (spec/and
                  p/short-str
                  #(or (= "word" %)
                       (= "pdf" %)
                       (= "excel" %))))

(spec/def ::origin p/short-str)

(spec/def ::origin_id uuid?)

(spec/def ::is_personal boolean?)

(spec/def ::is_from_wowtower boolean?)

(spec/def ::is_from_company boolean?)

(spec/def ::company (p/optional map?))

(spec/def ::is_archived boolean?)

(spec/def ::archive_date (spec/or :date p/date
                                  :nil nil?))

(spec/def ::archive_note (p/optional p/long-str))

(spec/def ::created_by uuid?)

(spec/def ::created_at p/date)

(spec/def ::updated_at p/date)

(spec/def ::worker_id uuid?)
(spec/def ::workers (spec/keys :req-un [::worker_id]))
(spec/def ::members (spec/and
                     #(not-empty %)
                     (spec/coll-of ::workers)))


(spec/def ::document (spec/keys :req-un [::document_id
                                         ::title
                                         ::type
                                         ::origin
                                         ::origin_id
                                         ::is_personal
                                         ::is_from_wowtower
                                         ::is_from_company
                                         ::company
                                         ::is_archived
                                         ::archive_date
                                         ::archive_note
                                         ::created_by
                                         ::created_at
                                         ::updated_at
                                         ::members]))

(spec/def ::document-edit (spec/keys :req-un [::document_id
                                              ::title]))

(defn check-booleans
  [document]
  (let [booleans (vals (select-keys document [:is_personal
                                              :is_from_wowtower
                                              :is_from_company]))
        xor-result (apply xor booleans)
        and-result (every? identity booleans)]
    (if (and xor-result (not and-result))
      document
      (assoc document :error :bad-request))))

(defn check-is-from-company [document]
  (let [{:keys [is_from_company company_id]} document]
    (if (and (true? is_from_company) (nil? company_id))
      (assoc document :error :bad-request)
      document)))

(defn validate-keys-create
  [document]
  (if-not (spec/valid? ::document document)
    (do
      (log/trace (spec/explain ::document document))
      (assoc document :error :bad-request))
    document))

(defn validate-keys-edit
  [document]
  (if-not (spec/valid? ::document-edit document)
    (do
      (log/trace (spec/explain ::document-edit document))
      (assoc document :error :bad-request))
    document))


(defn validate-create
  [document]
  (--> document
       validate-keys-create
       check-booleans
       check-is-from-company))


(defn validate-edit
  [document]
  (validate-keys-edit document))


(defn check-draft-is-empty
  [document]
  (let [{:keys [+old]} document
        {:keys [draft_version]} +old]
    (if (nil? draft_version)
      document
      (assoc document :error :bad-request))))


(defn check-is-archived
  [document]
  (let [{:keys [+old]} document
        {:keys [is_archived]} +old]
    (if is_archived
      document
      (assoc document :error :bad-request))))

(defn check-ownership
  [document]
  (let [{:keys [+old]} document]
    (if-not (= (:created_by document) (:created_by +old))
      (assoc document :error :bad-request)
      document)))


(defn check-draft-version-is-zero
  [document]
  (let [{:keys [+old]} document
        {:keys [draft_version]} +old]
    (if (= draft_version "0") document
        (assoc document :error :bad-request))))


(defn check-draft-is-not-empty
  [document]
  (let [{:keys [+old]} document
        {:keys [draft_version]} +old]
    (if (nil? draft_version)
      (assoc document :error :bad-request)
      document)))


(defn check-draft-version-not-zero
  [document]
  (let [{:keys [+old]} document
        {:keys [draft_version]} +old]
    (if (= draft_version "0")
      (assoc document :error :bad-request) 
      document)))

(defn check-publish-note [document]
  (let [{:keys [publish_note]} document]
    (if (nil? publish_note)
      (assoc document :error :bad-request)
      document)))
