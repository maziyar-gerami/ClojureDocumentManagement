(ns word.shell.service
  (:require
   [clojure.set :refer [rename-keys]]
   [word.core.model :as model]
   [word.shell.events.document-management :as events]
   [word.shell.db.document-management.companies.queries :as coy-qry]
   [word.shell.db.document-management.documents.commands :as doc-cmd]
   [word.shell.db.document-management.documents.queries :as doc-qry]
   [word.shell.db.document-management.version-members.commands :as members-cmd]
   [word.shell.db.document-management.version-members.queries :as members-qry]
   [word.shell.db.document-management.word-sections.commands :as sec-cmd]
   [word.shell.db.document-management.word-sections.queries :as sec-qry]
   [word.shell.db.document-management.word-subsections.commands :as subs-cmd]
   [word.shell.db.document-management.word-subsections.queries :as subs-qry]
   [word.shell.db.document-management.word-versions.commands :as vers-cmd]
   [word.shell.db.document-management.word-versions.queries :as version-qry]
   [wow.utils.shared.common :refer [-->]]
   [wow.utils.shared.ds :refer [command!]]))

(defn prnn
  [x]
  (prn "here >>>>" x)
  x)

(defn- enrich-all-versions
  [qdb document]
  (let [versions (version-qry/get-versions qdb document)]
    (assoc document :versions versions)))

(defn- enrich-version
  [qdb document]
  (if-let [version (version-qry/get-one-version qdb document)]
    (merge document version)
    (assoc document :error :not-found)))

(defn- enrich-company
  [qdb document]
  (let [{:keys [company_id]} document
        company (coy-qry/get-company qdb company_id)]
    (if (and (not (= company_id nil))
             (empty? company))
      (assoc document :error :not-found)
      (assoc document :company company))))

(defn- enrich-members-version [qdb document]
  (let [members (members-qry/get-members-version qdb document)]
    (assoc document :members members)))

(defn- enrich-sections
  [qdb document]
  (let [sections (sec-qry/get-sections qdb document)]
    (assoc document :sections sections)))

(defn- enrich-subsections
  [qdb document]
  (let [subsections (subs-qry/get-subsections qdb document)]
    (assoc document :subsections subsections)))

(defn- enrich-latest-version
  [qdb document]
  (let [all-versions (version-qry/get-versions qdb document)
        last-version (:version (first all-versions))]
    (assoc document :version last-version)))

(defn- get-one-section-subsections [qdb section]
  (let [section-subsections (subs-qry/get-ids-by-section qdb section)
        section-subsections-vec (vec (map :subsection_id section-subsections))]
    (assoc section :subsections section-subsections-vec)))

(defn- enrich-sections-subsections [qdb document]
  (let [{:keys [sections]} document
        sub-of-one-section-fn (partial get-one-section-subsections qdb)
        sub-of-multiple-sections (map sub-of-one-section-fn sections)]
    (assoc document :sections sub-of-multiple-sections)))

(defn rename-version-keys [meta]
  (let [versions (:versions meta)
        [first-item second-item] versions

        published-without-draft
        (-> (rename-keys first-item {:version :latest_published_version})
            (assoc :draft_version nil))

        draft-without-published
        (->  (rename-keys first-item {:version :draft_version})
             (assoc :latest_published_version nil))

        published-with-draft
        (-> (rename-keys first-item {:version :draft_version})
            (assoc :latest_published_version (:version second-item)))

        cond-result (cond (and (not (:is_published first-item))
                               (:is_published second-item)) 
                          published-with-draft

                          (:is_published first-item)
                          published-without-draft
                          
                          :else draft-without-published)]

    (merge cond-result meta)))

(defn create
  [ctx document]
  (let [{:keys [ds opts]} ctx
        {:keys [ts uuid]} opts
        {:keys [qdb cdb]} ds
        with-uuid&ts #(assoc %
                             :document_id uuid
                             :section_id (java.util.UUID/randomUUID)
                             :subsection_id (java.util.UUID/randomUUID)
                             :created_at ts
                             :updated_at ts)
        with-default-vals #(assoc %
                                  :version "0"
                                  :is_archived false
                                  :archive_date nil
                                  :archive_note nil
                                  :is_published false
                                  :publish_date nil
                                  :publish_note nil
                                  :order_number 0
                                  :content "{}"
                                  :is_private false
                                  :is_locked false
                                  :is_visible true)
        with-company (partial enrich-company qdb)
        save! (partial doc-cmd/save cdb)]

    (--> document
         with-uuid&ts
         with-default-vals
         with-company
         model/transform
         model/validate-create
         save!)))

(defn get-meta-list
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [qdb]} ds
        meta-list (partial doc-qry/get-meta-list qdb)
        check-meta-exist #(or % (assoc document :error :not-found))
        add-versions-fn (partial enrich-all-versions qdb)
        with-versions #(map add-versions-fn %)
        with-renamed-versions #(map rename-version-keys %)
        add-company-fn (partial enrich-company qdb)
        with-company #(map add-company-fn %)
        truncate-meta #(dissoc % :company_id :versions)
        truncate-meta-list #(map truncate-meta %)]

    (--> document
         meta-list
         check-meta-exist
         with-versions
         with-renamed-versions
         with-company
         truncate-meta-list)))


(defn get-meta
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [qdb]} ds
        with-raw-meta (fn [doc] (let [meta (doc-qry/get-meta qdb doc)]
                                  (or meta (assoc document :error :not-found))))
        with-versions (partial enrich-all-versions qdb)
        with-company (partial enrich-company qdb)
        truncate-meta #(dissoc % :company_id :versions)]

    (--> document
         with-raw-meta
         with-versions
         rename-version-keys
         with-company
         truncate-meta)))

(defn- enrich-old [ctx document]
  (let [meta (get-meta ctx document)]
    (if-not (contains? meta :error)
      (assoc document :+old meta)
      (assoc document :error :not-found))))

(defn get-version
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [qdb]} ds

        with-meta (fn [doc]
                    (let [meta (get-meta ctx document)]
                      (if-not (contains? meta :error)
                        (assoc doc :meta meta)
                        (assoc doc :error :not-found))))

        truncate-meta #(update-in % [:meta]
                                  dissoc
                                  :is_published
                                  :publish_note
                                  :publish_date)
        with-version (partial enrich-version qdb)
        with-members (partial enrich-members-version qdb)
        with-sections (partial enrich-sections qdb)
        with-sections-subs (partial enrich-sections-subsections qdb)
        with-subsections (partial enrich-subsections qdb)
        remove-document-id #(dissoc % :document_id)]

    (--> document
         with-meta
         truncate-meta
         with-version
         with-members
         with-sections
         with-sections-subs
         with-subsections
         remove-document-id)))

(defn get-latest-version
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [qdb]} ds
        with-latest-version (partial enrich-latest-version qdb)
        get-latest (partial get-version ctx)]
    (--> document
         with-latest-version
         get-latest)))


(defn archive
  [ctx document]
  (let [{:keys [ds opts]} ctx
        {:keys [ts]} opts
        {:keys [cdb]} ds
        with-archive-date #(assoc % :archive_date ts)
        with-old (partial enrich-old ctx)
        set-as-archive! (partial doc-cmd/archive cdb)]

    (--> document
         with-archive-date
         with-old
         model/check-ownership
         model/check-draft-is-empty
         set-as-archive!)))

(defn- dublicate-sec-subs
  [qdb section]
  (let [{:keys [version]} section
        subs (subs-qry/get-by-section qdb section)

        section-id (java.util.UUID/randomUUID)
        duplicated-sec (assoc section :section_id section-id)
        dublicate-subs #(assoc % :section_id section-id
                               :subsection_id (java.util.UUID/randomUUID)
                               :version version
                               :created_at (:created_at section))
        dublicated-subs (map dublicate-subs subs)
        section-cmds (sec-cmd/save duplicated-sec)
        subsection-cmds (first (map subs-cmd/save dublicated-subs))]
    [section-cmds subsection-cmds]))

(defn- create-new-version-commands-builder
  [ctx document]
  (let [{:keys [ds opts]} ctx
        {:keys [qdb]} ds

        new-version(->> document 
                        (get-meta ctx)
                        :latest_published_version
                        Integer/parseInt
                        inc
                        str)
        
        dublicate-secs-and-subs (partial dublicate-sec-subs qdb)
        assoc-new-version #(assoc % :version new-version)
        with-defaults #(assoc %
                              :created_at (:ts opts)
                              :created_by (:created_by document)
                              :is_published false
                              :is_archived false)

        word-ver-insert-cmd (->> document
                                 (version-qry/retrieve-version-document qdb)
                                 with-defaults
                                 assoc-new-version
                                 vers-cmd/save
                                 vector)

        sub-subs-insert-cmd (->> document
                                 (sec-qry/retrieve-version-document qdb)
                                 (map assoc-new-version)
                                 (map #(assoc % :created_at (:ts opts)))
                                 (map dublicate-secs-and-subs)
                                 (apply concat))

        members-insert-cmd (->> document
                                (members-qry/retrieve-version-document qdb)
                                (map assoc-new-version)
                                (map #(members-cmd/save-copy-members %)))]

    (concat word-ver-insert-cmd
            sub-subs-insert-cmd
            members-insert-cmd)))

(defn revert
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [cdb qdb]} ds
        with-latest-version (partial enrich-latest-version qdb)
        with-old (partial enrich-old ctx)
        create-revert-cmds (fn [doc]
                             (let [doc-cmd (doc-cmd/revert doc)
                                   ver-cmds (create-new-version-commands-builder
                                             ctx doc)]
                               (concat doc-cmd ver-cmds)))
        revert-cmd! (fn [cmds]
                      (let [result (command! cdb cmds)]
                        (if result document
                            (assoc document :error :internal-error))))]

    (--> document
         with-old
         with-latest-version
         model/check-ownership
         model/check-is-archived
         create-revert-cmds
         revert-cmd!)))

(defn delete
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [cdb]} ds
        with-old (partial enrich-old ctx)
        delete-cmd! (partial doc-cmd/delete cdb)]

    (--> document
         with-old
         model/check-ownership
         model/check-draft-version-is-zero
         delete-cmd!)))


(defn edit-meta
  [ctx document]
  (let [{:keys [ds opts]} ctx
        {:keys [cdb]} ds
        {:keys [ts]} opts
        with-old (partial enrich-old ctx)
        with-ts #(assoc % :updated_at ts)
        edit-cmd! (partial doc-cmd/edit cdb)]

    (--> document
         with-ts
         with-old
         model/transform
         model/validate-edit
         edit-cmd!)))

(defn discard
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [cdb]} ds
        with-old (partial enrich-old ctx)
        discard-cmd! (partial vers-cmd/discard cdb)]

    (--> document
         with-old
         model/check-ownership
         model/check-draft-is-not-empty
         model/check-draft-version-not-zero
         discard-cmd!)))

(defn publish [ctx document]
  (let [{:keys [opts ds topic]} ctx
        {:keys [ts]} opts
        {:keys [cdb]} ds
        with-old (partial enrich-old ctx)
        with-publish-data #(assoc % :publish_date ts
                                  :is_published true)
        event-publisher! (partial events/published topic)
        publish-cmd! (fn [doc]
                       (let [cmd (vers-cmd/publish doc)
                             result (command! cdb [cmd])]
                         (if result doc (assoc doc :error :internal-error))))]
    (--> document
         with-publish-data
         with-old
         model/check-draft-is-not-empty
         model/check-ownership
         model/check-publish-note
         publish-cmd!
         event-publisher!)))

(defn create-new-version
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [qdb cdb]} ds

        create-new-cmds (partial create-new-version-commands-builder ctx)
        with-old (partial enrich-old ctx)
        with-version (partial enrich-version qdb)
        create-new! (fn [cmds]
                      (let [result (command! cdb cmds)]
                        (if result document
                            (assoc document :error :internal-error))))]
    (--> document
         with-old
         with-version
         model/check-draft-is-empty
         create-new-cmds
         create-new!)))

(defn version-form
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [qdb]} ds

        with-versions (partial enrich-all-versions qdb)
        build-version-form (fn [doc]
                             (let [{:keys [versions]} doc
                                   ver-vec (map :version versions)]
                               (assoc doc :versions ver-vec)))]
    (--> document
         with-versions
         build-version-form)))

(defn auto-save
  [ctx document]
  (let [{:keys [ds]} ctx
        {:keys [cdb qdb]} ds
        with-old (partial enrich-old ctx)
        with-version (partial enrich-version qdb)
        extract-subsections #(:subsections %)
        auto-save-cmds #(map subs-cmd/auto-save %)
        transform #(map model/auto-save-transform %)
        save! (fn [subs] (let [result (command! cdb subs)]
                           (if result document
                               (assoc document :error :internal-error))))]

    (--> document
         with-old
         with-version
         extract-subsections
         transform
         auto-save-cmds
         save!)))

