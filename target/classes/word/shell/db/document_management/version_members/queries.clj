(ns word.shell.db.document-management.version-members.queries 
  (:require [wow.utils.shared.ds :refer [query!]]))

(defn get-members-version [qdb document]
  (let [{:keys [document_id version ]} document
        query {:select [:worker_id]
               :from [:document_management.version_members]
               :where [:and [:= :document_id document_id]
                       [:= :version version]]}]
    (query! qdb query)))


(defn retrieve-version-document [qdb document]
  (let [{:keys [document_id version]} document
        query {:select [:version
                        :document_id
                        :worker_id]
               :from [:document_management.version_members]
               :where [:and
                       [:= :document_id document_id]
                       [:= :version version]]}
        result (query! qdb query)]
    (when-not (contains? result :error)
      result)))