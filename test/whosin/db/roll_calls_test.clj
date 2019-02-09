(ns whosin.db.roll-calls-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [whosin.db.spec :refer [db-spec]]
            [whosin.fixtures :as fixtures :refer [truncate-tables]]
            [whosin.db.roll-calls :refer :all])
  (:import (java.sql Timestamp)))

(use-fixtures :each fixtures/with-db)

(deftest insert-new!-test
  (testing "should insert a new roll call record"
    (truncate-tables)
    (let [chat-id 1234
          title "test roll call"
          _ (insert-new! chat-id title)
          rs (jdbc/query db-spec (format "select * from w_roll_calls where chat_id=%d" chat-id))
          first-rs (first rs)]
      (is (= 1 (count rs)))
      (is (= title (:title first-rs)))
      (is (= "open" (:status first-rs)))
      (is (false? (:quiet first-rs)))

      ;; jdbc/query does not convert `_` to `-` in column names by default
      (is (not= nil (:created_at first-rs)))
      (is (not= nil (:updated_at first-rs)))))

  (testing "should close all previous roll calls"
    (truncate-tables)
    (let [chat-id 1234
          _ (insert-new! chat-id "old call")
          _ (insert-new! chat-id "old call")
          _ (insert-new! chat-id "old call")
          _ (insert-new! chat-id "old call")
          _ (insert-new! chat-id "new call")
          rs (jdbc/query db-spec
                         (format "select * from w_roll_calls where chat_id=%d" chat-id))
          closed-calls (filter #(= "closed" (:status %)) rs)
          open-calls (filter #(= "open" (:status %)) rs)]

      (is (= 5 (count rs)))
      (is (= 4 (count closed-calls)))
      (is (= 1 (count open-calls)))
      (doseq [closed-call closed-calls]
        (is (= "old call" (:title closed-call))))
      (doseq [open-call open-calls]
        (is (= "new call" (:title open-call))))))

  (testing "should cap the number of roll calls to 10 for each chat-id"
    (truncate-tables)
    (let [chat-id 1234
          _ (dotimes [_ 12] (insert-new! chat-id "old call"))
          rs (jdbc/query db-spec
                         (format "select * from w_roll_calls where chat_id=%d" chat-id))]

      (is (= 10 (count rs))))))

(deftest get-current-test
  (testing "should return nil if there is no open roll call"
    (truncate-tables)
    (is (nil? (get-current 1234))))

  (testing "should return the open roll call for a chat-id with '-' instead of '_' in column names"
    (truncate-tables)
    (let [chat-id 1234
          _ (insert-new! chat-id "old call")
          _ (insert-new! chat-id "old call")
          _ (insert-new! chat-id "new call")
          rc (get-current chat-id)]
      (is (= "open" (:status rc)))
      (is (= "new call" (:title rc)))
      (is (not= nil (:created-at rc)))
      (is (not= nil (:updated-at rc))))))

(deftest set-current-title!-test
  (testing "should return false if there is no open roll call"
    (truncate-tables)
    (is (nil? (set-current-title! 1234 "new title"))))

  (testing "should set title and return the updated record"
    (truncate-tables)
    (let [chat-id 1234
          _ (insert-new! chat-id "old title")
          _ (Thread/sleep 100)
          updated (set-current-title! chat-id "new title")
          rc (get-current chat-id)]
      (is (= rc updated))
      (is (= "new title" (:title updated)))
      (is (.before ^Timestamp (:created-at updated)
                   ^Timestamp (:updated-at updated))))))

(deftest set-current-quiet!-test
  (testing "should return nil if there is no open roll call"
    (truncate-tables)
    (is (nil? (set-current-quiet! 1234 true))))

  (testing "should set quiet and return the updated record"
    (truncate-tables)
    (let [chat-id 1234
          _ (insert-new! chat-id "old title")
          _ (Thread/sleep 100)
          updated-1 (set-current-quiet! chat-id true)
          rc-1 (get-current chat-id)
          updated-2 (set-current-quiet! chat-id false)
          rc-2 (get-current chat-id)]
      (is (= rc-1 updated-1))
      (is (true? (:quiet updated-1)))
      (is (= rc-2 updated-2))
      (is (false? (:quiet updated-2)))
      (is (.before ^Timestamp (:created-at updated-1)
                   ^Timestamp (:updated-at updated-1))))))

(deftest close-current!-test
  (testing "should return false if there is no open roll call"
    (truncate-tables)
    (is (false? (close-current! 1234))))

  (testing "should close roll call and return true if there is an open one"
    (truncate-tables)
    (let [chat-id 1234]
      (insert-new! chat-id "title")
      (is (true? (close-current! chat-id)))
      (is (nil? (get-current chat-id))))))
