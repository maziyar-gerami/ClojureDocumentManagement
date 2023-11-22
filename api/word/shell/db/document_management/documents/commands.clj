(ns word.shell.db.document-management.documents.commands
  (:require
   [wow.utils.shared.ds :refer [command!]]
   [word.shell.db.document-management.version-members.commands :as members-command]
   [word.shell.db.document-management.word-versions.commands :as versions-command]
   [word.shell.db.document-management.word-sections.commands :as sections-command]
   [word.shell.db.document-management.word-subsections.commands :as subsections-command]))

(defn- build-insert-value
  [document]
  (select-keys document
               [:document_id
                :title
                :type
                :origin
                :origin_id
                :company_id
                :is_personal
                :is_from_wowtower
                :is_from_company
                :is_archived
                :archive_note
                :archive_date
                :created_by
                :created_at
                :updated_at]))

(defn save
  [cdb document]
  (let [values [(build-insert-value document)]
        insert-document-cmd {:insert-into [:document_management.documents]
                             :values values}
        insert-members-cmd (members-command/save document)
        insert-versions-cmd (versions-command/save document)
        insert-sections-cmd (sections-command/save document)
        insert-subsections-cmd (subsections-command/save document)
        result (command! cdb [insert-document-cmd
                              insert-members-cmd
                              insert-versions-cmd
                              insert-sections-cmd
                              insert-subsections-cmd])]
    (if-not result
      (assoc document :error :internal-error)
      document)))


(defn archive
  [cdb document]
  (let [{:keys [document_id archive_note archive_date]} document
        set-as-archive-cmd {:update [:document_management.documents]
                            :set {:is_archived true
                                  :archive_note archive_note
                                  :archive_date archive_date}
                            :where [:= :document_id document_id]}
        result (command! cdb [set-as-archive-cmd])]

    (if-not result
      (assoc document :error :internal-error)
      document)))

(defn revert
  [document]
  (let [{:keys [document_id]} document
        update-document-cmd {:update [:document_management.documents]
                             :set {:is_archived false
                                   :archive_date nil
                                   :archive_note nil}
                             :where [:= :document_id document_id]}]
    [update-document-cmd]))

(defn delete
  [cdb document]
  (let [{:keys [document_id]} document
        delete-document-cmd {:delete-from [:document_management.documents]
                             :where [:= :document_id document_id]}
        delete-members-cmd (members-command/delete document_id)
        delete-versions-cmd (versions-command/delete document_id)
        delete-sections-cmd (sections-command/delete document_id)
        delete-subsections-cmd (subsections-command/delete document_id)

        result (command! cdb [delete-document-cmd
                              delete-members-cmd
                              delete-versions-cmd
                              delete-sections-cmd
                              delete-subsections-cmd])]

    (if-not result
      (assoc document :error :internal-error)
      document)))

(defn- build-update-values [document]
  (select-keys document
               [:document_id
                :title
                :updated_at]))

(defn edit
  [cdb document]
  (let [{:keys [document_id]} document
        values (build-update-values document)
        cmd {:update [:document_management.documents]
             :set values
             :where [:= :document_id document_id]}
        result (command! cdb [cmd])]
    (if result
      document
      (assoc document :error :internal-error))))
