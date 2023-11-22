(ns application.routes
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [wow.utils.shared.http :as utils-http]
            [application.state.syncer :as initiator]
            [word.handlers :as word-handlers]))


(def routes
  [[:document-management
    ["/"  ^:interceptors [(body-params/body-params)
                          http/html-body
                          utils-http/validate-path-params
                          utils-http/wrap-exception-handling
                          utils-http/cast-req-uuids
                          utils-http/cast-req-dates]
     ["/document-management"
      ["/word/documents"
       {:post `word-handlers/create
        :get `word-handlers/get-meta-list}
       ["/:document_id"
        {:delete `word-handlers/delete}
        ["/archive"
         {:put `word-handlers/archive}]
        ["/revert"
         {:put `word-handlers/revert}]
        ["/discard"
         {:put `word-handlers/discard}]
        ["/publish"
         {:put `word-handlers/publish}]
        ["/meta"
         {:get `word-handlers/get-meta}
         {:put `word-handlers/edit-meta}]
        ["/versions"
         ["/:version"
          {:get `word-handlers/get-version}
          ["/new"
           {:put `word-handlers/create-new-version}]
          ["/content"
           {:put `word-handlers/auto-save-content}]]]
        ["/version-form"
         {:get `word-handlers/version-form}]]]]]

    ["/initiate-document-management"
     {:put `initiator/handler}]]])

