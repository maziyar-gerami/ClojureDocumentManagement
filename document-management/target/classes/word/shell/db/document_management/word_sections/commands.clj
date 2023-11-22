(ns word.shell.db.document-management.word-sections.commands)

(defn- build-insert-value
  [document]
  (select-keys document
               [:section_id
                :order_number
                :version
                :document_id
                :title
                :is_private
                :is_archived
                :is_locked
                :is_visible
                :created_at
                :created_by]))

(defn save
  [document]
  (let [values [(build-insert-value document)]]
    {:insert-into [:document_management.word_sections]
     :values values}))


(defn delete
  [document_id]
  {:delete-from [:document_management.word_sections]
   :where [:= :document_id document_id]})

(defn discard
  [document_id draft_version]
  {:delete-from [:document_management.word_sections]
   :where [:and
           [:= :document_id document_id]
            [:= :version draft_version]]})