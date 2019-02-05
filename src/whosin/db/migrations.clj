(ns whosin.db.migrations
  (:require [mount.core :as mount]
            [whosin.db.spec :as db-spec]
            [whosin.config :as config]
            [clojure.tools.logging :as log]
            [ragtime.repl :as ragtime]
            [ragtime.jdbc :as rag-jdbc]))

(defn- load-config []
  (-> (mount/only #{#'config/config
                    #'db-spec/db-spec})
      (mount/start))
  {:datastore  (rag-jdbc/sql-database db-spec/db-spec)
   :migrations (rag-jdbc/load-resources "migrations")})

(defn migrate-db []
  (let [config (load-config)]
    (log/info "Applying database migration...")
    (ragtime/migrate config)
    (log/info "Migration was applied successfully.")))

(defn rollback-db []
  (let [config (load-config)]
    (log/info "Rolling back database migration...")
    (ragtime/rollback config)
    (log/info "Migration has been rolled back.")))
