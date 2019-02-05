(ns whosin.domain.roll-calls
  (:require [clojure.tools.trace :as t]
            [whosin.db.roll-calls :as db-roll-calls]
            [whosin.db.roll-call-responses :as db-roll-call-responses])
  (:import (clojure.lang Keyword)
           (org.joda.time LocalDate)))

(defrecord RollCall
  [^Integer id
   ^String chat-id
   ^Keyword status
   ^String title
   ^Boolean quiet
   ^LocalDate created-at
   ^LocalDate updated-at])

(defrecord RollCallResponse
  [^Integer id
   ^Integer user-id
   ^Integer user-name
   ^Keyword status
   ^String reason
   ^Integer roll-call-id
   ^String unique-token
   ^LocalDate created-at
   ^LocalDate updated-at])

(defn- db-res->RollCall
  ^RollCall [db-res]
  (some-> db-res
          (update :status keyword)
          (map->RollCall)))

(defn- db-res->RollCallResponse
  ^RollCallResponse [db-res]
  (some-> db-res
          (update :status keyword)
          (map->RollCallResponse)))

(defn open-new-roll-call!
  ^RollCall
  [chat-id title]
  (-> (db-roll-calls/insert-new! chat-id title)
      (db-res->RollCall)))

(defn close-roll-call!
  ^Boolean
  [chat-id]
  (db-roll-calls/close-current! chat-id))

(defn set-roll-call-title!
  ^RollCall
  [chat-id new-title]
  (some-> (db-roll-calls/set-current-title! chat-id new-title)
          (map->RollCall)))

(defn set-roll-call-quiet!
  ^RollCall
  [chat-id quiet?]
  (some-> (db-roll-calls/set-current-quiet! chat-id quiet?)
          (map->RollCall)))

(defn- roll-call-responses
  [roll-call-id]
  (as-> (db-roll-call-responses/get-responses roll-call-id) rs
        (or rs [])
        (map db-res->RollCallResponse rs)))

(defn roll-call-with-responses
  [chat-id]
  (when-some [roll-call (db-roll-calls/get-current chat-id)]
    {:roll-call (db-res->RollCall roll-call)
     :responses (roll-call-responses (:id roll-call))}))

(defn set-roll-call-attendance!
  [chat-id ^Keyword status user-id user-name reason]
  (some-> (db-roll-calls/get-current chat-id)
          (:id)
          (db-roll-call-responses/set-attendance! (name status) user-id user-name reason)
          (db-res->RollCallResponse)))

(defn set-roll-call-attendance-for!
  [chat-id ^Keyword status user-name reason]
  (some-> (db-roll-calls/get-current chat-id)
          (:id)
          (db-roll-call-responses/set-attendance-for! (name status) user-name reason)
          (db-res->RollCallResponse)))
