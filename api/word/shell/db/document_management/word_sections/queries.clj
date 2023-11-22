(ns word.shell.db.document-management.word-sections.queries
  (:require [wow.utils.shared.ds :refer [query!]]))


(defn get-sections
  [qdb document]
  (let [{:keys [document_id version]} document
        query {:select [:section_id
                        :order_number
                        :title
                        :is_private
                        :is_archived
                        :is_locked
                        :is_visible
                        :created_at
                        :created_by]
               :from [:document_management.word_sections]
               :where [:and
                       [:= :document_id document_id]
                       [:= :version version]]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))


(defn retrieve-version-document [qdb document]
  (let [{:keys [document_id version]} document
        query {:select [:section_id
                        :order_number
                        :version
                        :document_id
                        :title
                        :is_private
                        :is_archived
                        :is_locked
                        :is_visible
                        :created_at
                        :created_by]
               :from [:document_management.word_sections]
               :where [:and
                       [:= :document_id document_id]
                       [:= :version version]]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))