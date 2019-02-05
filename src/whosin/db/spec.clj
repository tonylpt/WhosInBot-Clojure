(ns whosin.db.spec
  (:require [mount.core :as mount]
            [whosin.config :as config]
            [hikari-cp.core :as hikari]))

(defn- ds-config [{conf :database}]
  {:adapter           "postgresql"
   :server-name       (:db-host conf)
   :port-number       (:db-port conf)
   :database-name     (:db-name conf)
   :username          (:username conf)
   :password          (:password conf)
   :pool-name         (:pool-name conf)
   :minimum-idle      (:pool-size conf)
   :maximum-pool-size (:pool-size conf)
   :register-mbeans   true})

(declare db-spec)
(mount/defstate db-spec
  :start {:datasource (hikari/make-datasource
                        (ds-config config/config))}
  :stop (hikari/close-datasource
          (:datasource db-spec)))
