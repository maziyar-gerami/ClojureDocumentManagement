(ns word.shell.db.document-management.word-versions.queries 
  (:require [wow.utils.shared.ds :refer [query!]]))

(defn get-versions
  [qdb document]
  (let [{:keys [document_id]} document
        query {:select [:version
                        :document_id
                        :is_published
                        :publish_note
                        :publish_date
                        :created_at
                        :created_by]
               :from [:document_management.word_versions]
               :where [:= :document_id document_id]
               :order-by [[:created_at :desc]]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))

(defn get-one-version
  [qdb document]
  (let [{:keys [document_id version]} document
        query {:select [:version 
                        :is_published
                        :publish_note
                        :publish_date
                        :created_at
                        :created_by]
               :from [:document_management.word_versions]
               :where [:and
                       [:= :version version]
                       [:= :document_id document_id]]}
        result (query! qdb query)]

    (when-not (contains? result :error)
      (first result))))

(defn retrieve-version-document [qdb document]
  (let [{:keys [document_id version]} document
        query {:select [:version
                        :document_id]
               :from [:document_management.word_versions]
               :where [:and
                       [:= :document_id document_id]
                       [:= :version version]]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      (first result))))