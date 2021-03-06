(ns whosin.cli-test
  (:require [clojure.test :refer :all])
  (:require [whosin.cli :refer [parse-args]]
            [clojure.string :as string]))

(deftest parse-args-test
  (letfn [(parse-str-args [str] (parse-args (->> (string/split str #"\s+")
                                                 (filter #(not (string/blank? %))))))]
    (testing "returns action when there is no input error"
      (are [o i] (= o (parse-str-args i))
                 {:action :migrate} "--migrate"
                 {:action :rollback} "--rollback"
                 {:action :start} ""))

    (testing "returns exit without error when help flag is specified"
      (let [result (parse-str-args "--help")]
        (is (not= nil (:exit result)))
        (is (true? (:ok? result)))))

    (testing "returns error when both migrate and rollback flags are specified"
      (is (not= nil (:exit (parse-str-args "--migrate --rollback")))))

    (testing "returns error when an argument is passed"
      (is (not= nil (:exit (parse-str-args "argument")))))

    (testing "returns error when there is error parsing input"
      (is (not= nil (:exit (parse-str-args "--unknown")))))))
