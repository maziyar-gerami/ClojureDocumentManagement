(ns word.shell.events.document-management
  (:require [wow.utils.sns.producer :refer [pub!]]))

(defn published
  [topic document]
  (let [{:keys [document_id
                +old]} document
        
        {:keys [origin]} +old

        event {:_id (java.util.UUID/randomUUID)
               :group_id (str "document_management.word." origin)
               :ref (str "http://document-management.wow.local:8080/word/documents/" document_id "/versions/latest")
               :sub document_id
               :ts (str (java.time.LocalDateTime/now))
               :event :published
               :body {:document_id document_id}}]
    
    (when (pub! topic event) document)))

