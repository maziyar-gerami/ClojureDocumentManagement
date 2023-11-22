(ns wow.utils.shared.http
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.set :refer [subset?]]
            [io.pedestal.interceptor :as intc]
            [wow.utils.shared.common :refer [keys->date keys->uuid]]
            [wow.utils.shared.log :as log]))

(defn call-api
  "Makes an HTTP GET request to the specified URL and returns the parsed JSON response.
  
  Arguments:
  - url: A string representing the URL to make the request to.

  Returns:
  - If the request is successful and the response is valid JSON, it returns the parsed data.
  - If there is an error during the request or the response cannot be parsed, it returns a map with the `:error` key set to `:internal-error`."

  [url]
  (log/debug "Asking for resource: " url)
  (try
    (:data
     (json/read-str
      (:body (http/get url))
      :key-fn keyword))
    (catch Exception e (do (log/error "failed to get url: " e)
                           (log/debug "failed to get url: " url)
                           (assoc url :error :internal-error)))))


(defn json-response
  "Creates a JSON response with a given status code and body.
   It takes a status and body parameter and returns a map representing the JSON response.

   Usage:
   (json-response status body)"
  {:version "0.9.0"}
  [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:data body})})

(def successful (partial json-response 200))
(def created (partial json-response 201))
(def bad-request (partial json-response 400))
(def not-found (partial json-response 404))
(def conflict (partial json-response 409))
(def internal-error (partial json-response 500))

(defn response-builder
  "Handle service results by a fn as a response builder.
   in case of error the body and status will be handled
   by type of error, otherwise, 
   the (response-model result) will be evaluated.
   
   **example:**
   ```clojure
   (defn find-by-id
  [request]
  (let [{:keys [path-params db]} request
        response-model #(http/successful %)
        result (notification/find-by-id db path-params)]
   
    (http/response-builder result response-model)))
   ```"
  {:version "0.9.0"}
  [result response-model]
  (let [contains-err? (and (coll? result) (contains? (into {} result) :error))
        error (when  contains-err? (:error (into {} result)))]
    
    (log/trace "Result of processed request: " result)
    (cond
      (and result (nil? error)) (response-model result)
      (and error (= :bad-request error)) (bad-request (str "Bad Request."))
      (and error (= :not-found error)) (not-found (str "Not Found."))
      (and error (= :conflict error)) (conflict "Does Exist.")
      (and error (= :internal-error error))
      (do
        (log/error "Internal server error:" error)
        (log/debug "Request failed due to an internal error for: " result)
        (internal-error "Temporarily out of service."))
      :else
      (do
        (log/error "Internal server error:" error)
        (log/debug "Request failed due to an internal error for: " result)
        (internal-error "Unexpected error! Temporarily out of service")))))


(def cast-req-uuids
  ;; Interceptor to cast request parameter keys ending with '_id' or '_by' to UUID values.
  ;;  It takes a context parameter and updates the 'json-params', 'path-params', and 'query-params'
  ;;  of the request with keys converted to UUID values using the 'keys->uuid' function.
  (intc/interceptor
   {:name ::cast-req-uuids
    :enter
    (fn [context]
      (-> context (update-in  [:request :json-params] keys->uuid)
          (update-in  [:request :path-params] keys->uuid)
          (update-in  [:request :query-params] keys->uuid)))}))

(defn- exception-handler 
  "Builds the response in case of an exception occurring.

   Parameters:
     - context: The context object representing the current request context.
     - exception: The exception object that occurred.

   Returns:
     The updated context object with a response indicating the exception.

   Example:
   (def context {:request {:path \"/api/endpoint\"}})
   (def exception (Exception. \"An unexpected error occurred\"))
   (exception-handler context exception)

   The resulting context will have a response with a status code of 500,
   an error message indicating the unexpected error, and appropriate logging of the error and context."
  {:version "0.9.0"}
  [context exception]
  (let [status 500
        error-message "Unexpected error! Temporarily out of service"
        response {:status status
                  :headers {"Content-Type" "application/json"}
                  :body (json/write-str {:message error-message})}]
    (log/error "Unhandled internal server error:" exception)
    (log/debug "Context failed due to an exception: " context)
    (assoc context :response response)))

(def wrap-exception-handling
  ;; Interceptor to add exception handling for each exception that is not handled in the system
  (intc/interceptor
   {:name ::exception-handling
    :enter (fn [context] context)
    :error (fn [context exception] (exception-handler context exception))}))


(def cast-req-dates
  ;; Interceptor to cast all dates from string to date
  (intc/interceptor
   {:name ::cast-req-dates
    :enter
    (fn [context]
      (update-in context [:request :json-params] keys->date))}))

(def validate-path-params
  ;; This interceptor is designed to validate and handle cases where path parameters
  ;; and JSON parameters conflict or are not properly set in certain requests.
  (intc/interceptor
   {:name ::validate-path-params
    :enter
    (fn [context]
      (let [pp (get-in context [:request :path-params])
            jp (get-in context [:request :json-params])
            ->set #(into #{} %)
            method (get-in context [:request :request-method])
            body-includes-params? (subset? (->set pp) (->set jp))]

        (if (and (not-empty pp)
                 (some #{:put :post} [method])
                 (not body-includes-params?))
          (update-in context [:request :json-params]
                     #(assoc % :error :bad-request))
          context)))}))

(defn with-ctx
  "Interceptor that adds contextual information to the request.
   It associates a timestamp and a UUID with the request context."
  {:version "0.9.0"}
  [ctx]
  (intc/interceptor
   {:name ::with-ctx
    :enter
    (fn [context]
      (let [ctx (-> ctx
                    (assoc-in [:opts :ts] (java.time.LocalDateTime/now))
                    (assoc-in [:opts :uuid] (java.util.UUID/randomUUID)))]
        (update context :request #(assoc % :ctx ctx))))}))


(defn response-body-parser
  "Parses the JSON string in the `:body` key of the `msg` parameter into a Clojure data structure.
   The resulting data structure is converted to keywords.
   Handles exceptions that may occur during the parsing process.
   
   - msg: A map containing the message data.
   
   Example usage:
   (response-body-parser {:body \"{\"name\": \"John\", \"age\": 30}\"})
   
   Returns:
   The updated `msg` map with the parsed data in the `:body` key, or an error message if an exception occurs."
  {:version "0.9.0"}
  [msg]
  (let [->json #(json/read-str % :key-fn keyword)]
    (try (update msg :body ->json)
         (catch Exception e
           (log/error "Exception occurred while parsing JSON: " e)
           (log/debug "Failed parsing the json bodey: " msg)
           (assoc msg :error "Invalid JSON")))))