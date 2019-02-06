(ns whosin.db.roll-call-responses
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.trace :as t]
            [whosin.db.spec :refer [db-spec]]
            [whosin.db.util :as util]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all]
            [honeysql-postgres.format :refer :all]
            [honeysql-postgres.helpers :as postgres-h]
            [whosin.db.spec :refer [db-spec]]
            [clojure.string :as string]
            [digest :refer [sha-256]])
  (:refer-clojure :exclude [update]))

(def statuses #{"in" "out" "maybe"})

(defn get-responses
  [roll-call-id]
  (->> (jdbc/query db-spec
                   (-> (select :*)
                       (from :w-roll-call-responses)
                       (where [:= :roll-call-id roll-call-id])
                       (order-by [:updated-at :asc])
                       (sql/format))
                   util/jdbc-query-opts)
       (sort-by :updated-at)))

(defn- set-attendance*
  [{:keys [roll-call-id
           unique-token
           status
           user-id
           user-name
           reason]}]

  {:pre [(not (string/blank? unique-token))
         (contains? statuses status)
         (or (nil? user-id)
             (integer? user-id))]}

  (let [now-ts (util/current-timestamp)]
    (jdbc/execute! db-spec
                   (-> (insert-into :w-roll-call-responses)
                       (values [{:roll-call-id roll-call-id
                                 :unique-token unique-token
                                 :status       status
                                 :user-id      user-id
                                 :user-name    user-name
                                 :reason       (or reason "")
                                 :created-at   now-ts
                                 :updated-at   now-ts}])

                       (postgres-h/upsert
                         (-> (postgres-h/on-conflict :roll-call-id
                                                     :unique-token)
                             (postgres-h/do-update-set :status
                                                       :user-id
                                                       :user-name
                                                       :reason
                                                       :updated-at)))
                       (postgres-h/returning :*)
                       (sql/format))
                   util/jdbc-update-return-opts)))

(defn set-attendance!
  [roll-call-id ^String status user-id user-name reason]
  (set-attendance* {:roll-call-id roll-call-id
                    :status       status
                    :user-id      user-id
                    :user-name    user-name
                    :reason       reason
                    :unique-token (->> user-id
                                       (str)
                                       (sha-256)
                                       (format "self:%s"))}))

(defn set-attendance-for!
  [roll-call-id ^String status user-name reason]
  (set-attendance* {:roll-call-id roll-call-id
                    :status       status
                    :user-name    user-name
                    :reason       reason
                    :unique-token (->> user-name
                                       (string/lower-case)
                                       (sha-256)
                                       (format "for:%s"))}))
