(ns whosin.rate-limiter
  (:require [mount.core :refer [defstate]]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.connections :as conns]
            [whosin.config :as config]
            [clojure.tools.logging :as log])
  (:import (java.io Closeable)))

(declare redis-conn-pool)
(defstate redis-conn-pool
  :start (let [{:keys [minimum-idle maximum-pool-size]} (get-in config/config [:rate-limiter :redis])]
           (log/info "Starting RateLimiter Redis connection pool")
           (conns/conn-pool :mem/fresh
                            {:min-idle-per-key  minimum-idle
                             :max-total-per-key maximum-pool-size}))
  :stop (do (.close ^Closeable redis-conn-pool)
            (log/info "Closed RateLimiter Redis connection pool")))

(defmacro wcar* [& body]
  `(let [spec# {:uri (get-in config/config [:rate-limiter :redis :uri])}]
     (car/wcar {:pool redis-conn-pool :spec spec#} ~@body)))

(defn- rate-limit-lua
  [key rate-limit rate-limit-window-seconds]
  (let [lua-script "local current = tonumber(redis.call(\"get\", _:rl-key))
                    if (current ~= nil and current >= tonumber(_:rl-count)) then
                      return 1
                    end
                    local result = redis.call(\"incr\", _:rl-key)
                    if (result == 1) then
                      redis.call(\"expire\", _:rl-key, tonumber(_:rl-window))
                    end
                    return 0"]
    (pos? (wcar* (car/lua
                   lua-script
                   {:rl-key key}
                   {:rl-count  rate-limit
                    :rl-window rate-limit-window-seconds})))))

(defn throttle! [key]
  (let [{:keys [rate-limit rate-limit-window]} (:rate-limiter config/config)]
    (let [key (str "rl." key)]
      (rate-limit-lua key
                      rate-limit
                      rate-limit-window))))
