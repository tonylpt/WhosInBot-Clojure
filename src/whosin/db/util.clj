(ns whosin.db.util
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [clj-time.local :as time]
            [clj-time.coerce :as time-coerce]))

(defn normalize-column-name [col-name]
  (->kebab-case col-name))

(def jdbc-query-opts
  "Options for clojure.java.jdbc to change underscores
  in column names into dashes when querying."
  {:identifiers normalize-column-name})

(def jdbc-update-return-opts
  (merge jdbc-query-opts
         {:return-keys true}))

(defn current-timestamp []
  (time-coerce/to-timestamp (time/local-now)))
