(ns word.shell.db.document-management.companies.queries
  (:require [wow.utils.shared.ds :refer [query!]]))

(defn get-company
  [qdb company-id]
  (let [query {:select [:company_id
                        :title
                        :avatar
                        :selected_package]
               :from [:document-management.companies]
               :where [:= :company_id company-id]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      (first result))))