(ns whosin.domain.roll-calls-test
  (:require [clojure.test :refer :all]
            [whosin.db.spec :refer [db-spec]]
            [whosin.fixtures :as fixtures :refer [truncate-tables]]
            [whosin.domain.roll-calls :as d :refer :all])
  (:import (whosin.domain.roll_calls RollCall RollCallResponse)))

(use-fixtures :each fixtures/with-db)

(def db-res->RollCall #'d/db-res->RollCall)
(def db-res->RollCallResponse #'d/db-res->RollCallResponse)

(deftest db-res->RollCall-test
  (testing "returns RollCall instance"
    (let [input {:title  "new call"
                 :status "open"
                 :quiet  true}
          expected (map->RollCall {:title  "new call"
                                   :status :open
                                   :quiet  true})
          actual (db-res->RollCall input)]
      (is (instance? RollCall actual))
      (is (= actual expected))))

  (testing "returns nil when input is nil"
    (is (nil? (db-res->RollCall nil)))))

(deftest db-res->RollCallResponse-test
  (testing "returns RollCallResponse instance"
    (let [input {:user-name "User 1"
                 :status    "in"
                 :reason    "I'm in"}
          expected (map->RollCallResponse {:user-name "User 1"
                                           :status    :in
                                           :reason    "I'm in"})
          actual (db-res->RollCallResponse input)]
      (is (instance? RollCallResponse actual))
      (is (= actual expected))))

  (testing "returns nil when input is nil"
    (is (nil? (db-res->RollCallResponse nil)))))

(deftest close-roll-call!-test
  (testing "returns false when no open roll call is found"
    (truncate-tables)
    (is (false? (close-roll-call! 1234))))

  (testing "returns true when successfully close a roll call"
    (truncate-tables)
    (open-new-roll-call! 1234 "title")
    (is (true? (close-roll-call! 1234)))
    (is (nil? (roll-call-with-responses 1234)))))

(deftest integration-test
  (truncate-tables)
  (open-new-roll-call! 1234 "call title")
  (set-roll-call-title! 1234 "new call")
  (set-roll-call-quiet! 1234 true)
  (set-roll-call-attendance! 1234 :in 1 "User 1" "I'm in")
  (set-roll-call-attendance-for! 1234 :out "User 2" "I'm out")
  (set-roll-call-attendance-for! 1234 :maybe "User 3" "I might come")

  (let [{:keys [roll-call responses]} (roll-call-with-responses 1234)
        roll-call (select-keys roll-call [:title :status :quiet])
        responses (->> responses
                       (sort-by :id)
                       (map #(select-keys % [:user-name :status :reason])))]
    (is (= {:title  "new call"
            :status :open
            :quiet  true} roll-call))
    (is (= [{:user-name "User 1" :status :in :reason "I'm in"}
            {:user-name "User 2" :status :out :reason "I'm out"}
            {:user-name "User 3" :status :maybe :reason "I might come"}] responses))))
