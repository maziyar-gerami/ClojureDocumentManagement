(ns word.shell.db.document-management.version-members.commands 
  (:require 
            [wow.utils.shared.common :refer [-->]]))

(defn transform-data [document]
  (let [{:keys [document_id members version]} document]
   (map #(assoc % :document_id document_id :version version) members)))

(defn- build-insert-value
  [document]
  (--> (select-keys document
                    [:members
                     :version
                     :document_id])
       transform-data))

(defn save
  [document]
  (let [values (build-insert-value document)]
    {:insert-into [:document_management.version_members]
     :values values}))

(defn- build-insert-value-copy-members
  [document]
  (--> (select-keys document
                    [:document_id
                     :version
                     :worker_id])))

(defn save-copy-members
  [document]
  (let [values (build-insert-value-copy-members document)]
    {:insert-into [:document_management.version_members]
     :values [values]}))

(defn delete
  [document_id]
  {:delete-from [:document_management.version_members]
   :where [:= :document_id document_id]})

(defn discard
  [document_id draft_version]
  {:delete-from [:document_management.version_members]
   :where [:and
           [:= :document_id document_id]
            [:= :version draft_version]]})