(ns word.handlers
  (:require [wow.utils.shared.http :as http]
            [word.shell.service :as service]))

(defn create
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(http/created (:document_id %))
        result (service/create ctx json-params)]

    (http/response-builder result response-model)))

(defn get-meta-list
  [request]
  (let [{:keys [ctx query-params]} request
        response-model #(if (not-empty %)
                          (http/successful %)
                          (http/not-found %))
        result (service/get-meta-list ctx query-params)]

    (http/response-builder result response-model)))


(defn get-meta
  [request]
  (let [{:keys [ctx path-params]} request
        response-model #(if (not-empty %)
                          (http/successful %)
                          (http/not-found %))
        result (service/get-meta ctx path-params)]
    (http/response-builder result response-model)))


(defn edit-meta
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(if (not-empty %)
                          (http/successful (:document_id %))
                          (http/not-found %))
        result (service/edit-meta ctx json-params)]
    (http/response-builder result response-model)))


(defn get-version
  [request]
  (let [{:keys [ctx path-params]} request
        response-model #(if (not-empty %)
                          (http/successful %)
                          (http/not-found %))
        result (if (= (:version path-params) "latest")
                 (service/get-latest-version ctx path-params)
                 (service/get-version ctx path-params))]
    (http/response-builder result response-model)))


(defn archive
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(if (not-empty %)
                          (http/successful (:document_id %))
                          (http/not-found (:document_id %)))
        result (service/archive ctx json-params)]
    (http/response-builder result response-model)))

(defn revert
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(if (not-empty %)
                          (http/successful (:document_id %))
                          (http/not-found (:document_id %)))
        result (service/revert ctx json-params)]
    (http/response-builder result response-model)))


(defn delete
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(if (not-empty %)
                          (http/successful (:document_id %))
                          (http/not-found :document_id %))
        result (service/delete ctx json-params)]

    (http/response-builder result response-model)))

(defn discard
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(if (not-empty %)
                          (http/successful (:document_id %))
                          (http/not-found :document_id %))
        result (service/discard ctx json-params)]

    (http/response-builder result response-model)))

(defn publish
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(http/successful (:document_id %))
        result (service/publish ctx json-params)]

    (http/response-builder result response-model)))

(defn create-new-version
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(http/successful (:document_id %))
        result (service/create-new-version ctx json-params)]

    (http/response-builder result response-model)))


(defn version-form
  [request]
  (let [{:keys [ctx path-params]} request
        response-model #(if (not-empty %)
                          (http/successful %)
                          (http/not-found %))
        result (service/version-form ctx path-params)]
    (http/response-builder result response-model)))

(defn auto-save-content
  [request]
  (let [{:keys [ctx json-params]} request
        response-model #(if (not-empty %)
                          (http/successful (:document_id %))
                          (http/not-found (:document_id %)))
        result (service/auto-save ctx json-params)]
    (http/response-builder result response-model)))