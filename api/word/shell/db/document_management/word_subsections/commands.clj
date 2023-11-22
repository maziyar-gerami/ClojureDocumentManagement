(ns word.shell.db.document-management.word-subsections.commands)

(defn- build-insert-value
  [document]
  (select-keys document
               [:subsection_id
                :order_number
                :section_id
                :version
                :document_id
                :title
                :is_archived
                :is_locked
                :is_visible
                :content
                :created_at
                :created_by]))

(defn- build-update-value
  [subsection]
  (select-keys subsection
               [:content]))
(defn save
  [document]
  (let [values [(build-insert-value document)]]
    {:insert-into [:document_management.word_subsections]
     :values values}))

(defn auto-save
  [subsection]
  (let [{:keys [subsection_id]} subsection
        values (build-update-value subsection)]
    {:update [:document_management.word_subsections]
     :set values
     :where [:= :subsection_id subsection_id]}))

(defn delete
  [document_id]
  {:delete-from [:document_management.word_subsections]
   :where [:= :document_id document_id]})

(defn discard
  [document_id draft_version]
  {:delete-from [:document_management.word_subsections]
   :where [:and
           [:= :document_id document_id]
            [:= :version draft_version]]})