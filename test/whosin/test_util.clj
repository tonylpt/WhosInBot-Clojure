(ns whosin.test-util
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]))

(defn- expect-called*
  [[fun expected-arg-list return-val & rest] body]
  (let [body (if (empty? rest) body [(expect-called* rest body)])]
    `(let [called?# (atom false)
           captured-args# (atom nil)]
       (with-redefs [~fun (fn [& args#]
                            (reset! called?# true)
                            (reset! captured-args# args#)
                            ~return-val)]
         ~@body
         (is (true? @called?#)
             (str "Function " '~fun " was not called as expected."))
         (is (= ~expected-arg-list @captured-args#)
             (str "Function " '~fun " was called with different arguments than expected."))))))

(defmacro expect-called
  [bindings & body]
  (assert (not-empty bindings)
          "Bindings must be supplied in the form of [[f-1][arg-list-1][ret-1] [f-2][arg-list-2][ret-2]...].")
  (assert (zero? (mod (count bindings) 3))
          "Each binding must be in the form [func][arg-list][return-value].")
  (expect-called* bindings body))

(defmacro with-no-log
  [& body]
  `(with-redefs [log/log* (fn [& _#] (println "Logs are disabled for test."))]
     ~@body))
