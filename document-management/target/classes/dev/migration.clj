(ns dev.migration
  (:require [application.system-map :as system-map]
            [migratus.core :as migratus]
            [next.jdbc :as jdbc]
            [wow.utils.shared.log :as log]))

(def mig-dir "dev/migration/")
(def seed-dir "dev/seed/")
(def all-dirs [mig-dir seed-dir])

(def config {:store :database
             :migration-dir "dev/migration/"
             :init-in-transaction? false
             :migration-table-name "migration_details"
             :db (get-in (system-map/load-conf :dev) [:ds :cdb])})

(defn drop-schema [schema-name]
  (let [my-datasource (jdbc/get-datasource (get-in (system-map/load-conf :dev) [:ds :cdb]))
        query-drop-schema (str "drop schema if exists " schema-name " cascade;")
        query-drop-table (str "drop table if exists " (:migration-table-name config) ";")]
    (with-open [connection         (jdbc/get-connection my-datasource)]
      (jdbc/execute! connection [query-drop-schema])
      (jdbc/execute! connection [query-drop-table]))))

(defn help [_]
  (println "Use this command to migrate migration files according the below instruction\n"
           "\"migrate\"    : migrates all not-migrated data\n"
           "\"migrate -r\" : migrates 'migration' folder with ressseting\n"
           "\"migrate -s\" : migrates seed data\n"
           "\"migrate -a\" : migrates all data, including 'seed' and 'migration' folderse\n"))

(defn -r [_]
  (let [ds                       (:ds (system-map/load-conf :dev))]
    (drop-schema (get-in ds [:qdb :schema])))
  (try (migratus/migrate config)
       (catch Exception e
         (log/error "Resseting data encountered a problem" e)
         (log/debug "Exception occurd during resseting: " e)))
  (log/info "Migration completed with resetting!"))

(defn -s [_] 
  (try (migratus/migrate (assoc config :migration-dir seed-dir))
       (catch Exception e
         (log/error "Migrating seed data encountered a problem")
         (log/debug "Exception occurd during migrating seeds: " e)))
  (log/info "Migration completed for 'seed' folder"))

(defn -a [_]
  (try (migratus/migrate (assoc config :migration-dir all-dirs))
       (catch Exception e
         (log/error "Migrating all data encountered a problem")
         (log/debug "Exception occurd during migrating all data: " e)))

  (log/info "Migration completed for 'seed' and 'migration' folders"))

(defn migrate [_]
  (try (migratus/migrate (assoc config :migration-dir mig-dir))
       (catch Exception e
         (log/error "Migrating encountered a problem")
         (log/debug "Exception occurd during migrating: " e))))