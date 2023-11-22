(ns wow.utils.shared.ds
  (:require [clojure.string :as str]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [next.jdbc.result-set :as q-result]
            [wow.utils.shared.common :refer [--> str->uuid]]
            [wow.utils.shared.log :as log]
            [clojure.data.json :as json]
            [clojure.walk :as walk])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn- find-order
  "Returns the order of data sort
    1 or any = asc
   -1 desc"
  {:version "0.9.0"}
  [arrange]
  (if (= arrange -1)
    :desc
    :asc))

(defn- with-sort
  "If `order-by` sent by the user as a query-param,
   adds `ORDER BY` to the query. This key defines the field
   which data should be sorted by. (Required)
   If `arrange` sent by the user as a query-param,
   defines the data sort order. (Optional)
   Allowed values:
   -1 : Desc
   1 or any: Asc
   
   Example:
   http://localhost:8080/some-url?sort={\"usage_count\": 1}"
  {:version "0.9.0"}
  [conds q]
  (let [{:keys [sort]} conds]
    (if (not-empty sort)
      (assoc q :order-by [[(keyword (key (first (json/read-str sort)))) (find-order (val (first (json/read-str sort))))]])
      q)))

(defn- with-pagination
  "If `order-by` and `page` sent by the user as  conds,
   adds pagination to the query. 
   `order-by` defines the field which data should be sorted by.
   `page` defines the page number (Both Required).
   If `arrange` sent by the user as a query-param,
   defines the data sort order. (Optional)
   Allowed values:
   -1 : Desc
   1 or any: Asc
   If `rows` sent,
   defines the number of rows in the page. (Optional)
   Deafult value: 10
   
   Example:
   http://localhost:8080/some-url?sort={\"usage_count\": 1}&pagination={\"page\": 1, \"rows\": 5}"
  {:version "0.9.0"}
  [conds q]
  (let [{:keys [pagination sort]} conds]
    (if (and (not-empty pagination)
             (not-empty sort))
      (let [pagination (walk/keywordize-keys (json/read-str pagination))
            {:keys [page rows]} pagination]
        (assoc q
               :limit rows
               :offset (* rows (dec page))))
      q)))

(defn- transform [d]
  (if (str/ends-with? (first d) "_id")
    (into [] (concat [(first d)] [(mapv str->uuid (second d))]))
    (if (or (= (str/lower-case (first (second d))) "true")
            (= (str/lower-case (first (second d))) "false"))
      (into [] (concat [(first d)] [(Boolean/valueOf (str/lower-case (first (second d))))]))
      (if (< 1 (count (second d)))
        d
        (into [] (concat [(first d)] [(first (second d))]))))))

(defn- add-equal-sign
  "Adds := to the vactor"
  {:version "0.9.0"}
  [vec]
  (replace (conj vec :=) [2 0 1]) ;; == (into [](concat [:=] vec)
  )


(defn- make-associted-vector
  "Makes the vector should be added to the query
   to implement the filter.
   If q has where, adds the new conditions to it.
   else adds a where to q."
  {:version "0.9.0"}
  [where-q additional-vector-to-q]
  (if (not-empty where-q)
    (if (= (first where-q) :and)
      (into [] (concat where-q additional-vector-to-q))
      (into [] (concat (into [] (concat [:and] [where-q])) additional-vector-to-q)))
    (if (< 1 (count additional-vector-to-q))
      (into [] (concat [:and] additional-vector-to-q))
      additional-vector-to-q)))


(defn- make-or [key vals]
  (mapv #(into [] (concat [key %])) vals))

(defn- prepared-query [d]
  (if (vector? (second d))
    (into []
          (concat [:or] (mapv add-equal-sign (make-or (first d) (second d)))))
    (add-equal-sign d)))

(defn- with-filter
  "If `filter-on` and `case` sent by the user as conds,
   runs a filter on data. 
   `filter-on` defines the field which data should be filtered by.
   `case` defines the desired value. (Both Required).
   
   Example:
   http://localhost:8080/some-url?filter-on=name&case=hamid"
  {:version "0.9.0"}
  [conds q]
  (if (not-empty (:filter conds))
    (let [keywordized-map           (walk/keywordize-keys (json/read-str (:filter conds)))
          transformed               (mapv transform keywordized-map)
          prepared-additional-query (mapv prepared-query transformed)]
      (assoc q :where (make-associted-vector (:where q) prepared-additional-query)))
    q))


(defn add-conds
  "Adds an additional part, consist of
   filters, pagination and sort, to the query.
   Allowed conds:
   
   //for sort
   order-by 
   arrange

   //for pagination
   order-by 
   arrange
   page
   rows

   //for filter
   filter-on
   case"
  {:version "0.9.0"}
  [q conds] 
  (if (not-empty conds)
    (let [with-sort-partial       (partial with-sort conds)
          with-pagination-partial (partial with-pagination conds)
          with-filter-partial     (partial with-filter conds)]

      (--> q
           with-sort-partial
           with-pagination-partial
           with-filter-partial))
    q))

(defn query!
  "Executes a query on the DB and returns the result as a 
   result-set and unqualified keys.
   in case of error it will be returned by 
   :internal-error and :error-msg and query itself."
  {:version "0.9.0"}
  [db q]
  (try
    (jdbc/execute! db
                   (sql/format q)
                   {:multi-rs   false
                    :builder-fn q-result/as-unqualified-maps})
    (catch Exception e
      (do (log/error (str "Error in excuting query: " e))
          (log/debug (str "Exception while executing query: " q) db)
          [{:error :internal-error
            :error-msg e
            :q (sql/format q)}]))))

(defn- transact!
  "JDBC transaction tx and a sequence of commands cmds to be executed within the transaction."
  {:version "0.9.0"}
  [tx cmds]
  (if (empty? cmds)
    true
    (recur tx (do
                (jdbc/execute! tx (sql/format (first cmds)))
                (rest cmds)))))

(defn command!
  "executing SQL series of commands on a DB transaction.
   The result will be throwed as an exception if any of the commands
   failed to commit; otherwise, it returns true.

   **example:**
   ```clojure
   (command! cmd-db [insert-to-parent
                     insert-to-child
                     insert-to-grandchild])
   ```"
  {:version "0.9.0"}
  [cdb cmds]
  (try
    (jdbc/with-transaction [tx cdb]
      (let [exec (partial transact! tx)]
        (trampoline exec (remove nil? cmds)))) true
    (catch Exception e
      (do (log/error "Exception caught in executing DB command: " e)
          (log/debug "Executing failed for command vector: " cmds)
          (throw e)))))

(defn create-connection-pool
  "Creates a connection pool for a database using HikariCP.
   It takes a db parameter and attempts to create a HikariDataSource connection pool.
   If an exception occurs during the creation, it logs the error and returns nil.

   Usage:
   (create-connection-pool db)"
  {:version "0.9.0"}
  [db]
  (try
    ^HikariDataSource (connection/->pool HikariDataSource db)
    (catch Exception e
      (log/error "Caught Exception while creating connection pool: " e)
      (log/debug "Exception while creating DB connection: " db))))

(defn close-ds-connection
  "Closes the connection of a HikariDataSource.
   It takes a db parameter and attempts to close the connection.
   If an exception occurs during the closing, it logs the error.

   Usage:
   (close-ds-connection db)"
  {:version "0.9.0"}
  [db]
  (try
    (.close (jdbc/get-connection db))
    (catch Exception e
      (log/error "Caught Exception while closing DB connection: " e)
      (log/debug "Exception while closing DB connection: " db))))
