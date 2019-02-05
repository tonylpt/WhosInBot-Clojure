(ns whosin.telegram.commands-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clojure.string :as string]
            [whosin.telegram.commands :as commands]
            [whosin.test-util :refer [expect-called]]
            [whosin.domain.roll-calls :as domain]))

(def get-response-str-short #'commands/get-response-str-short)
(def get-response-str-full #'commands/get-response-str-full)

(def sample-response-list
  [{:id 1, :status :in, :user-name "User 1", :reason "", :updated-at (time/local-date 2019 01 01)}
   {:id 2, :status :out, :user-name "User 2", :reason "busy", :updated-at (time/local-date 2019 01 01)}
   {:id 5, :status :maybe, :user-name "User 5", :reason "not sure", :updated-at (time/local-date 2019 01 01)}
   {:id 3, :status :in, :user-name "User 3", :reason "yay", :updated-at (time/local-date 2018 01 01)}
   {:id 4, :status :out, :user-name "User 4", :reason "", :updated-at (time/local-date 2019 01 02)}])

(deftest start-roll-call-test
  (expect-called
    [domain/open-new-roll-call! [1234 "chat title"] {:title "chat title"}]
    (let [result (commands/start-roll-call {:chat-id     1234
                                            :command-arg "chat title"})]
      (is (= "Roll call started." result)))))

(deftest end-roll-call-test
  (testing "when there is an open roll call"
    (expect-called
      [domain/close-roll-call! [1234] true]
      (let [result (commands/end-roll-call {:chat-id 1234})]
        (is (= "Roll call ended." result)))))

  (testing "when there is no open roll call"
    (expect-called
      [domain/close-roll-call! [1234] false]
      (let [result (commands/end-roll-call {:chat-id 1234})]
        (is (= "No roll call in progress." result))))))

(deftest set-title-test
  (testing "when there is an open roll call"
    (expect-called
      [domain/set-roll-call-title! [1234 "new title"] true]
      (let [result (commands/set-title {:chat-id     1234
                                        :command-arg "new title"})]
        (is (= "Roll call title set." result)))))

  (testing "when no title is provided"
    (let [result (commands/set-title {:chat-id     1234
                                      :command-arg ""})]
      (is (= "Please provide a title." result))))

  (testing "when there is no open roll call"
    (expect-called
      [domain/set-roll-call-title! [1234 "new title"] nil]
      (let [result (commands/set-title {:chat-id     1234
                                        :command-arg "new title"})]
        (is (= "No roll call in progress." result))))))

(deftest get-response-str-short-test
  (testing "returns correct result when there are responses of all status types"
    (let [responses [{:id 1 :status :in}
                     {:id 2 :status :in}
                     {:id 3 :status :out}
                     {:id 4 :status :maybe}
                     {:id 5 :status :maybe}]
          result (get-response-str-short responses)]
      (is (= "Total: 2 in, 1 out, 2 might come." result))))

  (testing "returns correct result when there are status types without responses"
    (let [responses [{:id 1 :status :in}
                     {:id 2 :status :in}]
          result (get-response-str-short responses)]
      (is (= "Total: 2 in, 0 out, 0 might come." result)))))

(deftest get-response-str-full-test
  (testing "returns result in correct order when there are responses of all status types"
    (let [result (get-response-str-full sample-response-list)
          expected (string/join \newline ["In (2)"
                                          " - User 3 (yay)"
                                          " - User 1"
                                          ""
                                          "Out (2)"
                                          " - User 2 (busy)"
                                          " - User 4"
                                          ""
                                          "Maybe (1)"
                                          " - User 5 (not sure)"])]
      (is (= expected result))))

  (testing "returns result in correct order when there are status types without responses"
    (let [responses [{:id 1, :status :in, :user-name "User 1", :reason "", :updated-at (time/local-date 2019 01 01)}
                     {:id 3, :status :in, :user-name "User 3", :reason "yay", :updated-at (time/local-date 2018 01 01)}
                     {:id 5, :status :maybe, :user-name "User 5", :reason "not sure", :updated-at (time/local-date 2019 01 01)}]
          result (get-response-str-full responses)
          expected (string/join \newline ["In (2)"
                                          " - User 3 (yay)"
                                          " - User 1"
                                          ""
                                          "Maybe (1)"
                                          " - User 5 (not sure)"])]
      (is (= expected result)))))

(deftest list-responses-test
  (testing "when there is no open roll call"
    (expect-called
      [domain/roll-call-with-responses [1234] nil]
      (let [result (commands/list-responses {:chat-id 1234})]
        (is (= "No roll call in progress." result)))))

  (let [roll-call {:title "Call Title"}
        responses sample-response-list
        quiet-resp-str (get-response-str-short responses)
        full-resp-str (get-response-str-full responses)]

    (testing "returns title and short response listing for quiet roll call"
      (expect-called
        [domain/roll-call-with-responses [1234] {:roll-call (assoc roll-call :quiet true)
                                                 :responses responses}]
        (let [result (commands/list-responses {:chat-id 1234})]
          (is (= (str "Call Title" \newline quiet-resp-str) result)))))

    (testing "returns title and full response listing for non-quiet roll call"
      (expect-called
        [domain/roll-call-with-responses [1234] {:roll-call (assoc roll-call :quiet false)
                                                 :responses responses}]
        (let [result (commands/list-responses {:chat-id 1234})]
          (is (= (str "Call Title" \newline full-resp-str) result)))))))

(deftest set-quiet-test
  (testing "when there is no open roll call"
    (expect-called
      [domain/set-roll-call-quiet! [1234 true] nil]
      (let [result (commands/set-quiet {:chat-id 1234})]
        (is (= "No roll call in progress." result)))))

  (testing "sets roll call to be quiet"
    (expect-called
      [domain/set-roll-call-quiet! [1234 true] {:title "Call Title" :quiet true}]
      (let [result (commands/set-quiet {:chat-id 1234})]
        (is (string/includes? result "Ok fine, I'll be quiet."))))))

(deftest set-loud-test
  (testing "when there is no open roll call"
    (expect-called
      [domain/set-roll-call-quiet! [1234 false] nil]
      (let [result (commands/set-loud {:chat-id 1234})]
        (is (= "No roll call in progress." result)))))

  (testing "sets roll call to be loud and returns full response list"
    (let [roll-call {:title "Call Title" :quiet false}
          responses sample-response-list
          full-resp-str (get-response-str-full responses)]
      (expect-called
        [domain/set-roll-call-quiet! [1234 false] roll-call
         domain/roll-call-with-responses [1234] {:roll-call roll-call
                                                 :responses responses}]
        (let [result (commands/set-loud {:chat-id 1234})]
          (is (string/includes? result "Sure."))
          (is (string/includes? result full-resp-str)))))))

(deftest set-attendance-fn-test
  (testing "when there is no open roll call"
    (expect-called
      [domain/set-roll-call-attendance! [1234 :in 5678 "user" "I'm in"] nil]
      (let [result ((commands/set-attendance-fn :in) {:chat-id     1234
                                                      :user-id     5678
                                                      :user-name   "user"
                                                      :command-arg "I'm in"})]
        (is (= "No roll call in progress." result)))))

  (testing "sets attendance and returns response list"
    (let [responses sample-response-list
          response-list-str (get-response-str-full responses)]
      (expect-called
        [domain/set-roll-call-attendance! [1234 :out 5678 "user" "I'm in"] {:status :out}
         domain/roll-call-with-responses [1234] {:roll-call {:quiet false}
                                                 :responses responses}]
        (let [result ((commands/set-attendance-fn :out) {:chat-id     1234
                                                         :user-id     5678
                                                         :user-name   "user"
                                                         :command-arg "I'm in"})]
          (is (string/includes? result "user is out!"))
          (is (string/includes? result response-list-str)))))))

(deftest set-attendance-for-fn-test
  (testing "when there is no command argument"
    (let [result ((commands/set-attendance-for-fn :in) {:chat-id     1234
                                                        :user-id     5678
                                                        :user-name   "user"
                                                        :command-arg ""})]
      (is (= "Please provide the persons name." result))))

  (testing "when there is no open roll call"
    (expect-called
      [domain/set-roll-call-attendance-for! [1234 :in "Peter" "He's in"] nil]
      (let [result ((commands/set-attendance-for-fn :in) {:chat-id     1234
                                                          :user-id     5678
                                                          :user-name   "user"
                                                          :command-arg "Peter He's in"})]
        (is (= "No roll call in progress." result)))))

  (testing "sets attendance and returns response list"
    (let [responses (conj sample-response-list
                          {:id 2, :status :out, :user-name "Peter", :reason "busy", :updated-at (time/local-date 2019 01 01)})
          response-list-str (get-response-str-full responses)]
      (expect-called
        [domain/set-roll-call-attendance-for! [1234 :out "Peter" "busy"] {:status :out}
         domain/roll-call-with-responses [1234] {:roll-call {:quiet false}
                                                 :responses responses}]
        (let [result ((commands/set-attendance-for-fn :out) {:chat-id     1234
                                                             :user-id     5678
                                                             :user-name   "user"
                                                             :command-arg "Peter busy"})]
          (is (string/includes? result "Peter is out!"))
          (is (string/includes? result response-list-str)))))))
