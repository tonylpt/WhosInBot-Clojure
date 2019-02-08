(ns whosin.core
  (:require [clojure.core.match :refer [match]]
            [whosin.cli :as cli]
            [whosin.db.migrations :as migrations]
            [whosin.nrepl-server :as nrepl]
            [whosin.config :as config]
            [whosin.telegram.bot :as telegram]
            [whosin.db.spec :as db-spec]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:gen-class))

(defn- exit [status message]
  (println message)
  (System/exit status))

(defn- add-shutdown-hook [hook-fn]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable #(hook-fn))))

(defn stop []
  (mount/stop))

(defn start []
  (log/info "Starting app ...")
  (mount/start #'config/config
               #'nrepl/server)

  (mount/start #'db-spec/db-spec
               #'telegram/bot)

  (add-shutdown-hook stop))

(defn -main
  [& args]
  (match [(cli/parse-args args)]
         [{:exit msg, :ok? true}] (exit 0 msg)
         [{:exit msg}] (exit 1 msg)
         [{:action :migrate}] (migrations/migrate-db)
         [{:action :rollback}] (migrations/rollback-db)
         [{:action :start}] (start)
         :else (throw (IllegalStateException.))))
