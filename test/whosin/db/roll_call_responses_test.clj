(ns whosin.db.roll-call-responses-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [whosin.db.spec :refer [db-spec]]
            [whosin.fixtures :as fixtures :refer [truncate-tables]]
            [whosin.db.roll-calls :as roll-calls-db]
            [whosin.db.roll-call-responses :refer :all]
            [clojure.string :as string])
  (:import (java.sql Timestamp)))

(use-fixtures :each fixtures/with-db)

(deftest set-attendance!-test
  (testing "should insert and return a new roll call response record"
    (truncate-tables)
    (let [user-id 1, status "in"
          roll-call (roll-calls-db/insert-new! 1234 "call title")
          result (set-attendance! (:id roll-call) status user-id "user-1" "reason 1")
          db-records (jdbc/query db-spec
                                 (format "select * from w_roll_call_responses where user_id=%d and status='%s'"
                                         user-id status))
          db-record (first db-records)]
      (is (= 1 (count db-records)))
      (is (= user-id (:user_id db-record)))
      (is (= "user-1" (:user_name db-record)))
      (is (= status (:status db-record)))
      (is (= "reason 1" (:reason db-record)))

      (is (not= nil (:created_at db-record)))
      (is (not= nil (:updated_at db-record)))

      (is (= result (->> db-record
                         (map (fn dashify-key [[k v]] [(->kebab-case k) v]))
                         (into {}))))
      ))

  (testing "should insert maximum 1 response record for each user-id"
    (truncate-tables)
    (let [user-id 1
          roll-call (roll-calls-db/insert-new! 1234 "call title")
          _ (set-attendance! (:id roll-call) "in" user-id "user-1" "reason 1")
          _ (Thread/sleep 100)
          _ (set-attendance! (:id roll-call) "out" user-id "user-1" "reason 2")
          rs (jdbc/query db-spec "select * from w_roll_call_responses")
          first-rs (first rs)]

      (is (= 1 (count rs)))

      ;; the most recent update
      (is (= "out" (:status first-rs)))
      (is (= "reason 2" (:reason first-rs)))

      (is (.before ^Timestamp (:created_at first-rs)
                   ^Timestamp (:updated_at first-rs))))))

(deftest set-attendance-for!-test
  (testing "should insert and return a new roll call response record"
    (truncate-tables)
    (let [user-name "User 1", status "in"
          roll-call (roll-calls-db/insert-new! 1234 "call title")
          result (set-attendance-for! (:id roll-call) status user-name "reason 1")
          db-records (jdbc/query db-spec
                                 (format "select * from w_roll_call_responses where user_name='%s' and status='%s'"
                                         user-name status))
          db-record (first db-records)]
      (is (= 1 (count db-records)))
      (is (= user-name (:user_name db-record)))
      (is (= status (:status db-record)))
      (is (= "reason 1" (:reason db-record)))

      (is (not= nil (:created_at db-record)))
      (is (not= nil (:updated_at db-record)))

      (is (= result (->> db-record
                         (map (fn dashify-key [[k v]] [(->kebab-case k) v]))
                         (into {}))))))

  (testing "should insert maximum 1 response record for each user-name, case-insensitive"
    (truncate-tables)
    (let [user-name "User 1"
          roll-call (roll-calls-db/insert-new! 1234 "call title")
          _ (set-attendance-for! (:id roll-call) "in" (string/upper-case user-name) "reason 1")
          _ (Thread/sleep 100)
          _ (set-attendance-for! (:id roll-call) "out" user-name "reason 2")
          rs (jdbc/query db-spec "select * from w_roll_call_responses")
          first-rs (first rs)]

      (is (= 1 (count rs)))

      ;; the most recent update
      (is (= user-name (:user_name first-rs)))
      (is (= "out" (:status first-rs)))
      (is (= "reason 2" (:reason first-rs)))

      (is (.before ^Timestamp (:created_at first-rs)
                   ^Timestamp (:updated_at first-rs))))))

(deftest get-responses-test
  (testing "should return all responses"
    (truncate-tables)
    (let [roll-call (roll-calls-db/insert-new! 1234 "call title")
          _ (set-attendance! (:id roll-call) "out" 1 "User 1" "reason 1")
          _ (set-attendance-for! (:id roll-call) "in" "User 2" "reason 2")
          result (get-responses (:id roll-call))
          result-by-status (group-by :status result)]

      (is (= 2 (count result)))
      (is (= {:user-id   1
              :user-name "User 1"
              :reason    "reason 1"} (-> (get result-by-status "out")
                                         (first)
                                         (select-keys [:user-id :user-name :reason]))))
      (is (= {:user-id   nil
              :user-name "User 2"
              :reason    "reason 2"} (-> (get result-by-status "in")
                                         (first)
                                         (select-keys [:user-id :user-name :reason])))))))
