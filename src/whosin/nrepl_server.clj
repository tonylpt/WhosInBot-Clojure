(ns whosin.nrepl-server
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [whosin.config :refer [config]]))

(declare server)
(mount/defstate server
  :start (let [{{:keys [host port] :or {host "localhost"}} :nrepl-server} config]
           (log/infof "Starting nREPL server on port %d" port)
           (start-server :port port :bind host))

  :stop (do (stop-server server)
            (log/infof "Stopped nREPL server.")))
