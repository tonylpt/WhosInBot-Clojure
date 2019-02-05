(ns whosin.cli-test
  (:require [clojure.test :refer :all])
  (:require [whosin.cli :refer [parse-args]]
            [clojure.string :as string]))

(deftest parse-args-test
  (letfn [(parse-str-args [str] (parse-args (->> (string/split str #"\s+")
                                                 (filter #(not (string/blank? %))))))]
    (testing "returns action when there is no input error"
      (are [o i] (= o (parse-str-args i))
                 {:action :migrate} "--db:migrate"
                 {:action :rollback} "--db:rollback"
                 {:action :start} ""))
    (testing "returns error when both migrate and rollback flags are specified"
      (is (not= nil (:exit (parse-str-args "--db:migrate --db:rollback")))))
    (testing "returns error when an argument is passed"
      (is (not= nil (:exit (parse-str-args "argument")))))
    (testing "returns error when there is error parsing input"
      (is (not= nil (:exit (parse-str-args "--unknown")))))))
