(ns word.shell.db.document-management.word-subsections.queries
  (:require [wow.utils.shared.ds :refer [query!]]))


(defn get-ids-by-section
  [qdb section]
  (let [{:keys [section_id]} section
        query {:select [:subsection_id]
               :from [:document_management.word_subsections]
               :where
               [:= :section_id section_id]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))


(defn get-by-section
  [qdb section]
  (let [{:keys [section_id]} section
        query {:select [:subsection_id
                        :section_id
                        :order_number
                        :document_id
                        :title
                        :is_archived
                        :is_locked
                        :is_visible
                        :content
                        :created_at
                        :created_by]
               :from [:document_management.word_subsections]
               :where
               [:= :section_id section_id]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))

(defn get-subsections
  [qdb document]
  (let [{:keys [document_id version]} document
        query {:select [:subsection_id
                        :section_id
                        :order_number
                        :title
                        :is_archived
                        :is_locked
                        :is_visible
                        :content
                        :created_at
                        :created_by]
               :from [:document_management.word_subsections]
               :where [:and
                       [:= :document_id document_id]
                       [:= :version version]]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))