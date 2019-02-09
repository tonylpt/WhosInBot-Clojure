(ns whosin.domain.roll-calls-test
  (:require [clojure.test :refer :all]
            [whosin.db.spec :refer [db-spec]]
            [whosin.fixtures :as fixtures :refer [truncate-tables]]
            [whosin.domain.roll-calls :refer :all]))

(use-fixtures :each fixtures/with-db)

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
  (set-roll-call-quiet! 1234 true)
  (set-roll-call-attendance! 1234 :in 1 "User 1" "I'm in")
  (set-roll-call-attendance-for! 1234 :out "User 2" "I'm out")
  (set-roll-call-attendance-for! 1234 :maybe "User 3" "I might come")

  (let [{:keys [roll-call responses]} (roll-call-with-responses 1234)
        roll-call (select-keys roll-call [:title :status :quiet])
        responses (->> responses
                       (sort-by :id)
                       (map #(select-keys % [:user-name :status :reason])))]
    (is (= {:title  "call title"
            :status :open
            :quiet  true} roll-call))
    (is (= [{:user-name "User 1" :status :in :reason "I'm in"}
            {:user-name "User 2" :status :out :reason "I'm out"}
            {:user-name "User 3" :status :maybe :reason "I might come"}] responses))))
