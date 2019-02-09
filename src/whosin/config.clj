(ns whosin.config
  (:require [clojure.tools.logging :as log]
            [aero.core :refer [read-config]]
            [mount.core :as mount]
            [clojure.string :as string]))

(defn load-config [path]
  (let [url (clojure.java.io/resource path)]
    (log/infof "Reading configuration from %s ..." url)
    (read-config url)))

(defn- config-path []
  (cond
    (System/getenv "ENVIRONMENT") (format "config.%s.edn" (string/lower-case (System/getenv "ENVIRONMENT")))
    :else "config.edn"))

(declare config)
(mount/defstate config
  :start (load-config (config-path)))
