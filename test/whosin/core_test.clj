(ns whosin.core-test
  (:require [clojure.test :refer :all])
  (:require [whosin.core :refer [-main]]))

(deftest -main-test
  (testing "a failing test to verify Travis integration"
    (is (not= 1 1))))
