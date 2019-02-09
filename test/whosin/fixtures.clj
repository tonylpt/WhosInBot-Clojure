(ns whosin.fixtures
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [whosin.db.spec :refer [db-spec]]
            [whosin.config :as config]
            [whosin.db.spec :as db-spec]
            [mount.core :refer [only swap start]]
            [clojure.tools.logging :as log]))

(defn with-db [f]
  (-> (only #{#'config/config
              #'db-spec/db-spec})
      (swap {#'config/config (config/load-config "config.test.edn")})
      (start))
  (f))

(defn truncate-tables []
  (log/info "Cleaning up database tables...")
  (jdbc/execute! db-spec "TRUNCATE w_roll_calls, w_roll_call_responses"))
