(ns word.shell.db.document-management.word-versions.commands
  (:require
   [wow.utils.shared.ds :refer [command!]]
   [word.shell.db.document-management.version-members.commands :as members-cmd]
   [word.shell.db.document-management.word-sections.commands :as sections-cmd]
   [word.shell.db.document-management.word-subsections.commands :as subsec-cmd]
))

(defn- build-insert-value
  [document]
  (select-keys document
               [:version
                :document_id
                :is_published
                :publish_date
                :publish_note
                :created_by
                :created_at]))

(defn- build-publish-value
  [document]
  (select-keys document
               [:publish_date
                :publish_note
                :is_published]))

(defn save
  [document]
  (let [values [(build-insert-value document)]]
    {:insert-into [:document_management.word_versions]
     :values values}))

(defn delete
  [document_id]
  {:delete-from [:document_management.word_versions]
   :where [:= :document_id document_id]})

(defn discard
  [cdb document]
  (let [{:keys [document_id +old]} document
        {:keys [draft_version]} +old
        discard-version-cmd {:delete-from [:document_management.word_versions]
                             :where [:and
                                     [:= :document_id document_id]
                                     [:= :version draft_version]]}

        discard-members-cmd (members-cmd/discard document_id draft_version)
        discard-sections-cmd (sections-cmd/discard document_id draft_version)
        discard-subsections-cmd (subsec-cmd/discard document_id draft_version)


        result (command! cdb [discard-version-cmd
                              discard-members-cmd
                              discard-sections-cmd
                              discard-subsections-cmd])]

    (if-not result
      (assoc {} :document_id document_id :error :internal-error)
      (assoc {} :document_id document_id))))


(defn publish
  [document]
  (let [{:keys [document_id +old]} document
        {:keys [draft_version]} +old
        values (build-publish-value document)]
    {:update [:document_management.word_versions]
     :set values
     :where [:and [:= :document_id document_id]
             [:= :version draft_version]]}))