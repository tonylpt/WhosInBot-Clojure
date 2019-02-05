(ns whosin.config
  (:require [clojure.tools.logging :as log]
            [aero.core :refer [read-config]]
            [mount.core :as mount]
            [clojure.string :as string]))

(defn- config-path []
  (cond
    (System/getenv "CONFIG") (System/getenv "CONFIG")
    (System/getenv "ENVIRONMENT") (clojure.java.io/resource
                                    (format "config.%s.edn"
                                            (string/lower-case (System/getenv "ENVIRONMENT"))))
    :else (clojure.java.io/resource "config.edn")))

(declare config)
(mount/defstate config
  :start (do (log/info "Reading configuration...")
             (read-config (config-path))))
