(ns whosin.db.roll-calls
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.trace :as t]
            [whosin.db.spec :refer [db-spec]]
            [whosin.db.util :as util]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all]
            [honeysql-postgres.helpers :as postgres-h]
            [whosin.db.spec :refer [db-spec]])
  (:refer-clojure :exclude [update]))

(def ^:const status-open "open")
(def ^:const status-closed "closed")
(def ^:const records-to-keep 10)

(defn- close-all-existing*
  [db-conn chat-id timestamp]
  (jdbc/execute! db-conn
                 (-> (update :w-roll-calls)
                     (sset {:status     status-closed
                            :updated-at timestamp})
                     (where [:= :chat-id chat-id]
                            [:<> :status status-closed])
                     (sql/format))))

(defn- delete-old-records*
  [db-conn chat-id num-latest-to-keep]
  (let [latest-keep-rows (-> (select :created-at)
                             (from :w-roll-calls)
                             (where [:= :chat-id chat-id])
                             (order-by [:created-at :desc])
                             (limit num-latest-to-keep))
        earliest-created-at (-> (select :%min.latest.created-at)
                                (from [latest-keep-rows :latest]))]

    (jdbc/execute! db-conn
                   (-> (delete-from :w-roll-calls)
                       (where [:= :chat-id chat-id]
                              [:< :created-at earliest-created-at])
                       (sql/format)))))

(defn- insert-new*
  [db-conn chat-id title timestamp]
  (jdbc/execute! db-conn
                 (-> (insert-into :w-roll-calls)
                     (values [{:chat-id    chat-id
                               :status     status-open
                               :title      title
                               :created-at timestamp
                               :updated-at timestamp}])
                     (postgres-h/returning :*)
                     (sql/format))
                 util/jdbc-update-return-opts))

(defn insert-new!
  "Inserts a new roll call into the database, after cleaning up very old roll calls and
   closing all existing roll calls for the chat-id.
   Returns the new inserted record."
  [chat-id title]
  (let [now-ts (util/current-timestamp)]
    (jdbc/with-db-transaction [txn db-spec]
                              (delete-old-records* txn chat-id (dec records-to-keep))
                              (close-all-existing* txn chat-id now-ts)
                              (insert-new* txn chat-id title now-ts))))

(defn close-current!
  "Closes the current roll call if there is any.
   Returns true if there was an open call that was closed."
  ^Boolean
  [chat-id]
  (let [now-ts (util/current-timestamp)]
    (->> (close-all-existing* db-spec chat-id now-ts)
         (first)
         (pos?)
         (true?))))

(defn- current-roll-call-query
  [chat-id & cols]
  (let [cols-or-all (or (not-empty cols)
                        [:*])]
    (-> (apply select cols-or-all)
        (from :w-roll-calls)
        (where [:= :status status-open]
               [:= :chat-id chat-id])
        (order-by [:created-at :desc])
        (limit 1))))

(defn get-current
  [chat-id]
  (->> (jdbc/query db-spec
                   (sql/format (current-roll-call-query chat-id))
                   util/jdbc-query-opts)
       first))

(defn- update-current-attrs!
  [chat-id attrs]
  (let [now-ts (util/current-timestamp)]
    (jdbc/execute! db-spec
                   (-> (update :w-roll-calls)
                       (sset (merge {:updated-at now-ts}
                                    attrs))
                       (where [:= :chat-id chat-id]
                              [:in :id (current-roll-call-query chat-id :id)])
                       (postgres-h/returning :*)
                       (sql/format))
                   util/jdbc-update-return-opts)))

(defn set-current-title!
  "Sets title for the currently open roll call.
   Returns the updated record, or nil if there is no open roll call."
  [chat-id ^String new-title]
  (update-current-attrs! chat-id {:title new-title}))

(defn set-current-quiet!
  "Changes the quiet setting for the currently open roll call.
   Returns the updated record, or nil if there is no open roll call."
  [chat-id ^Boolean quiet?]
  (update-current-attrs! chat-id {:quiet quiet?}))
