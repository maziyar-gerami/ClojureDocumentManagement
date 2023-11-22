(ns word.shell.db.document-management.documents.queries 
  (:require [wow.utils.shared.ds :refer [query! add-conds]]))

(defn get-meta-list
  [qdb document]
  (let [query {:select [:document_id
                        :title
                        :type
                        :origin
                        :origin_id
                        :company_id
                        :is_personal
                        :is_from_company
                        :is_from_wowtower
                        :is_archived
                        :archive_date
                        :archive_note
                        :created_by
                        :created_at
                        :updated_at]
               :from [:document-management.documents]
               :order-by [[:created_at :desc]]}
        result (query! qdb (add-conds query document))]
    (when-not (contains? result :error)
      result)))


(defn get-meta
  [qdb document]
  (let [{:keys [document_id]} document
        query {:select [:document_id
                        :title
                        :type
                        :origin
                        :origin_id
                        :company_id
                        :is_personal
                        :is_from_company
                        :is_from_wowtower
                        :is_archived
                        :archive_date
                        :archive_note
                        :created_by
                        :created_at
                        :updated_at]
               :from [:document-management.documents]
               :where [:= :document_id document_id]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      (first result))))