(ns whosin.db.spec
  (:require [mount.core :as mount]
            [whosin.config :as config]
            [hikari-cp.core :as hikari]
            [clojure.string :as string]))

(defn- validate-jdbc-url [jdbc-url]
  ;; only PostgreSQL is supported.
  (when-not (string/starts-with? (or jdbc-url "") "jdbc:postgresql://")
    (throw (IllegalArgumentException. "JDBC Url must start with jdbc:postgresql://")))
  jdbc-url)

(defn- ds-config [{conf :database}]
  {:jdbc-url          (validate-jdbc-url (:jdbc-url conf))
   :pool-name         (:pool-name conf)
   :minimum-idle      (:pool-size conf "whosin")
   :maximum-pool-size (:pool-size conf 10)
   :register-mbeans   true})

(declare db-spec)
(mount/defstate db-spec
  :start {:datasource (hikari/make-datasource
                        (ds-config config/config))}
  :stop (hikari/close-datasource
          (:datasource db-spec)))
