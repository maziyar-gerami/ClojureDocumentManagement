(ns word.core.model-test
  (:require [word.core.model :as model]
            [clojure.test :refer [deftest is testing run-tests
                                  ;; run-test
                                  ]]))


(deftest transformation-test
  (let [word-sample-create
        {:title "  hr template    "
         :type "  word  "
         :origin "  company_hr.onboarding  "
         :members [{:worker_id "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6"}
                   {:worker_id "f6cb0faa-8e37-44f1-b670-58f8e90fde25"}]}

        result (model/transform word-sample-create)]

    (testing "The \"title\" field should be trimmed"
      (is (= (:title result) "hr template"))
      (is (not= (:title result) (:title word-sample-create))))

    (testing "The \"type\" field should be trimmed"
      (is (= (:type result) "word"))
      (is (not= (:type result) (:type word-sample-create))))

    (testing "The \"origin\" field should be trimmed"
      (is (= (:origin result) "company_hr.onboarding")))

    (testing "The worker_id in the \"members\" filed should be uuid"
      (is (every? uuid? (map :worker_id (get-in (model/transform word-sample-create) [:members])))))))

(deftest validation-test
  (let [word-sample
        {:document_id #uuid "e3251455-19c4-4477-917e-21dc4e72ff4c"
         :title "hr template"
         :type "word"
         :origin "company_hr.onboarding"
         :origin_id #uuid "b2e6a1c3-1a5e-44ae-a8fd-81f76fd715cf"
         :company_id #uuid "b2e6a1c3-1a5e-44ae-a8fd-81f76fd715cf"
         :company {:company_id #uuid "b2e6a1c3-1a5e-44ae-a8fd-81f76fd715cf"
                   :title "EPFC"}
         :is_personal false
         :is_from_wowtower false
         :is_from_company true
         :is_archived false
         :archive_date (java.time.LocalDateTime/now)
         :archive_note "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean m1"
         :created_by #uuid "2c41423f-9997-c07e-3ae7-83a382b96cc0"
         :created_at (java.time.LocalDateTime/now)
         :updated_at (java.time.LocalDateTime/now)
         :members [{:worker_id #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6"}
                   {:worker_id #uuid "f6cb0faa-8e37-44f1-b670-58f8e90fde25"}]}

        empty-title (assoc word-sample :title "")
        long-title (assoc word-sample :title "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean ma")

        empty-origin (assoc word-sample :type "")
        long-origin (assoc word-sample :type "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean ma")
        empty-type (assoc word-sample :type "")
        long-type (assoc word-sample :type "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean ma")

        empty-company (assoc word-sample :company nil)

        empty-is-personal (assoc word-sample :is_personal nil)
        string-is-personal (assoc word-sample :is_personal "false")
        empty-is-from-wowtower (assoc word-sample :is_from_wowtower nil)
        string-is-from-wowtower (assoc word-sample :is_personal "false")
        empty-is-from-company (assoc word-sample :is_from_company nil)
        string-is-from-company (assoc word-sample :is_personal "false")

        string-created-by (assoc word-sample :created_by "7c22941f-8a37-4333-bbdd-4b386741eb5d")
        empty-created-by (assoc word-sample :created_by nil)

        string-archive_date (assoc word-sample :archive_date "2023-09-25T09:16:05.306172")
        empty-archive_date (assoc word-sample :archive_date nil)]

    (testing "Validation happy path"
      (is (not (contains? (model/validate-keys-create word-sample) :error))))

    (testing "The \"title\" field should be short string and not empty"
      (is (contains? (model/validate-keys-create empty-title) :error))
      (is (contains? (model/validate-keys-create long-title) :error)))

    (testing "The \"type\" field should be short string and not empty"
      (is (contains? (model/validate-keys-create empty-type) :error))
      (is (contains? (model/validate-keys-create long-type) :error)))

    (testing "The \"origin\" field should be short string and not empty"
      (is (contains? (model/validate-keys-create empty-origin) :error))
      (is (contains? (model/validate-keys-create long-origin) :error)))

    (testing "The \"company\" field could be empty"
      (is (not (contains? (model/validate-keys-create empty-company) :error))))

    (testing "The \"is_personal\" and \"is_from_wowtower\" and \"is_from_company\" should be boolean and not empty"
      (is (contains? (model/validate-keys-create empty-is-personal) :error))
      (is (contains? (model/validate-keys-create string-is-personal) :error))
      (is (contains? (model/validate-keys-create empty-is-from-wowtower) :error))
      (is (contains? (model/validate-keys-create string-is-from-wowtower) :error))
      (is (contains? (model/validate-keys-create empty-is-from-company) :error))
      (is (contains? (model/validate-keys-create string-is-from-company) :error)))

    (testing "The \"archive_date\" firld should be date or empty"
      (is (contains? (model/validate-keys-create string-archive_date) :error))
      (is (not (contains? (model/validate-keys-create empty-archive_date) :error))))

    (testing "The \"created_by\" should be uuid and not empty"
      (is (contains? (model/validate-keys-create string-created-by) :error))
      (is (contains? (model/validate-keys-create empty-created-by) :error)))))


(deftest check-booleans-test
  (let [booleans-sample {:is_personal false
                         :is_from_wowtower false
                         :is_from_company true}

        wrong-booleans-sample {:is_personal true
                               :is_from_wowtower true
                               :is_from_company true}]

    (testing "Onle one of the \"is_personal\", \"is_from_wowtower\" and the \"is_from_company\" from should be true"
      (is (not (contains? (model/check-booleans booleans-sample) :error)))
      (is (contains? (model/check-booleans wrong-booleans-sample) :error)))))

(deftest check-archive-restricrion-test
  (let [word-draft-sample {:type "word",
                           :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                           :archive_note "string",
                           :+old {:archive_date nil,
                                  :draft_version "1",
                                  :is_archived false,
                                  :latest_published_version "0",
                                  :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                                  :archive_note nil,
                                  :type "word"
                                  :is_published false,
                                  :publish_note nil,
                                  :publish_date nil}}]

    (testing "Only the published document can be archive"
      (is (contains? (model/check-draft-is-empty word-draft-sample) :error)))))

(deftest check-if-revertable-test
  (let [word-sample-not-archived {:document_id #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                                  :+old {:is_from_wowtower false,
                                         :archive_date nil,
                                         :draft_version nil,
                                         :is_archived false,
                                         :latest_published_version "1",
                                         :document_id #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                                         :archive_note nil, :type "word", :title "string",
                                         :updated_at #inst "2023-09-27T13:33:25.500000000-00:00",
                                         :is_published true, :publish_note "ps", :is_from_company true,
                                         :origin "company_hr.onboarding",
                                         :publish_date #inst "2023-09-27T13:32:46.300000000-00:00",
                                         :created_by #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6", :is_personal false,
                                         :company {:company_id #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6", :title "EPFC",
                                                   :avatar "/path/to/avatar", :selected_package "Service Provider"},
                                         :created_at #inst "2023-09-27T13:33:25.500000000-00:00"}}


        word-sample-archived {:document_id #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                              :+old {:is_from_wowtower false,
                                     :archive_date #inst "2023-09-27T13:33:25.500000000-00:00",
                                     :draft_version nil,
                                     :is_archived true,
                                     :latest_published_version "1",
                                     :document_id #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                                     :archive_note "120", :type "word", :title "string",
                                     :updated_at #inst "2023-09-27T13:33:25.500000000-00:00",
                                     :is_published true, :publish_note "ps", :is_from_company true,
                                     :origin "company_hr.onboarding",
                                     :publish_date #inst "2023-09-27T13:32:46.300000000-00:00",
                                     :created_by #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6",
                                     :is_personal false,
                                     :company {:company_id #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6", :title "EPFC",
                                               :avatar "/path/to/avatar", :selected_package "Service Provider"},
                                     :created_at #inst "2023-09-27T13:33:25.500000000-00:00"}}]

    (testing "Only the archived documents can be revert"
      (is (not (contains? (model/check-is-archived word-sample-archived) :error)))
      (is (contains? (model/check-is-archived word-sample-not-archived) :error)))))



(deftest check-ownership-test
  (let [word-sample {:type "word",
                     :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                     :archive_note "string",
                     :created_by #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                     :+old {:archive_date nil,
                            :draft_version "1",
                            :is_archived false,
                            :latest_published_version "0",
                            :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                            :archive_note nil,
                            :type "word"
                            :is_published false,
                            :publish_note nil,
                            :publish_date nil
                            :created_by #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6"}}]

    (testing "Only the owner of the document can publish, delete, archive or revert the documents."
      (is (contains? (model/check-ownership word-sample) :error)))))


(deftest check-draft-version-is-zero-test
  (let [word-sample {:type "word",
                     :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                     :archive_note "string",
                     :created_by #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                     :+old {:archive_date nil,
                            :draft_version "1",
                            :is_archived false,
                            :latest_published_version "0",
                            :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                            :archive_note nil,
                            :type "word"
                            :is_published false,
                            :publish_note nil,
                            :publish_date nil
                            :created_by #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6"}}]

    (testing "Only the documents with draft_version=0 can be deleted."
      (is (contains? (model/check-draft-version-is-zero word-sample) :error)))))

(deftest check-edit-validation
  (let [word-sample {:document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0"
                     :title "new title"}]
    (testing "The input map for editing document should be valid."
      (is (not (contains? (model/validate-edit word-sample) :error)))
      (is (contains? (model/validate-edit (assoc word-sample :title nil)) :error)))))

(deftest check-check-is-draft-test
  (let [word-published-sample {:type "word",
                               :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                               :archive_note "string",
                               :+old {:archive_date nil,
                                      :draft_version nil,
                                      :is_archived false,
                                      :latest_published_version "0",
                                      :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                                      :archive_note nil,
                                      :type "word"
                                      :is_published false,
                                      :publish_note nil,
                                      :publish_date nil}}]

    (testing "Only the draft document can be discard"
      (is (contains? (model/check-draft-is-not-empty word-published-sample) :error)))))


(deftest check-draft-version-not-zero-test
  (let [word-sample {:type "word",
                     :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                     :archive_note "string",
                     :created_by #uuid "60e2cbab-ef77-4aed-9746-9e2eb41deaf4",
                     :+old {:archive_date nil,
                            :draft_version "0",
                            :is_archived false,
                            :latest_published_version "0",
                            :document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                            :archive_note nil,
                            :type "word"
                            :is_published false,
                            :publish_note nil,
                            :publish_date nil
                            :created_by #uuid "b792e8ae-2cb4-4209-85b9-32be4c2fcdd6"}}]

    (testing "A document with draft_version=0 can not be discarded"
      (is (contains? (model/check-draft-version-not-zero word-sample) :error)))))

(deftest check-is-from-company-test
  (let [word-sample {:document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                     :is_from_company true
                     :company_id nil}]

    (testing "If a document craetion is from company, \"company_id\" should not be null"
      (is (contains? (model/check-is-from-company word-sample) :error)))))


(deftest check-publish-note-test
  (let [word-sample {:document_id #uuid "73cc752d-3086-4198-8637-8bd21fc842d0",
                     :publish_note nil}]

    (testing "For plishing a document, publish note is mandatory"
      (is (contains? (model/check-publish-note word-sample) :error)))))

(run-tests)


;; (run-test transformation-test)
;; (run-test validation-test)
;; (run-test check-booleans-test)
;; (run-test check-archive-restricrion-test)
;; (run-test check-if-revertable)
;; (run-test check-ownership-test)
;; (run-test check-draft-version-is-zero-test)
;; (run-test check-draft-version-not-zero-test)