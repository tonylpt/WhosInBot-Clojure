(ns whosin.rate-limiter-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [mount.core :refer :all]
            [taoensso.carmine :as car]
            [whosin.config :as config]
            [whosin.rate-limiter :as rate-limiter]))

(defn with-redis [f]
  (let [states (-> (only #{#'config/config
                           #'rate-limiter/redis-conn-pool})
                   (swap {#'config/config (config/load-config "config.test.edn")}))]
    (start states)
    (f)
    (stop states)))

(defn clear-redis []
  (log/info "Cleaning up redis...")
  (rate-limiter/wcar* (car/flushall)))

(use-fixtures :each with-redis)

(deftest throttle!-test
  (let [key "key1"]
    (testing "returns true when key is rate-limited"
      (clear-redis)
      (dotimes [_ 3]
        (is (false? (rate-limiter/throttle! key))))
      (is (true? (rate-limiter/throttle! key))))

    (testing "resets when rate limiting window is passed"
      (clear-redis)
      (dotimes [_ 3]
        (is (false? (rate-limiter/throttle! key)))
        (Thread/sleep 200))
      (is (true? (rate-limiter/throttle! key)))
      (Thread/sleep 500)
      (is (false? (rate-limiter/throttle! key))))))
