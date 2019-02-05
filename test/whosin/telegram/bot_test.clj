(ns whosin.telegram.bot-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [<!!]]
            [morse.api :as t]
            [whosin.telegram.bot :as bot]
            [whosin.telegram.commands :as commands]
            [whosin.test-util :refer :all]))

(def extract-command-arg #'bot/extract-command-arg)
(def morse-msg->Command #'bot/morse-msg->Command)
(def wrap-handler-fn #'bot/wrap-handler-fn)

(def sample-message {:message_id 0,
                     :from       {:id            11111111,
                                  :is_bot        false,
                                  :first_name    "First Name",
                                  :username      "Username",
                                  :language_code "en"},
                     :chat       {:id         22222222,
                                  :first_name "Chat Name",
                                  :username   "Chat Username",
                                  :type       "private"},
                     :date       1549267294,
                     :text       "/start_roll_call roll call title",
                     :entities   [{:offset 0, :length 16, :type "bot_command"}]})


(deftest extract-command-arg-test
  (testing "when text contains both command and param"
    (is (= "param1 param2" (extract-command-arg "/start_roll_call param1 param2"))))
  (testing "when text contains command only"
    (is (= "" (extract-command-arg "/start_roll_call"))))
  (testing "when text contains command with space"
    (is (= "" (extract-command-arg "/start_roll_call  ")))))

(deftest morse-msg->Command-test
  (let [expected (commands/map->Command {:chat-id     22222222
                                         :user-id     11111111
                                         :user-name   "First Name"
                                         :command-arg "roll call title"
                                         :text        "/start_roll_call roll call title"})]
    (testing "converts Morse message to Command correctly"
      (is (= expected (morse-msg->Command sample-message))))))

(deftest wrap-handler-fn-test
  (testing "returns an async Morse handler that sends reply from wrapped Command handler"
    (expect-called
      [t/send-text ["tg-token" 22222222 "command reply"] nil]
      (let [expected-command (morse-msg->Command sample-message)
            command-handler-fn (fn [command]
                                 (is (= expected-command command))
                                 "command reply")
            morse-handler-fn (wrap-handler-fn "tg-token" command-handler-fn)
            result-chan (morse-handler-fn sample-message)]
        (<!! result-chan))))

  (testing "returns an async Morse handler that sends error reply when wrapped Command handler throws Exception"
    (with-no-log
      (expect-called
        [t/send-text ["tg-token" 22222222 "An error has occurred."] nil]
        (let [command-handler-fn (fn [_] (throw (Exception. "test error")))
              morse-handler-fn (wrap-handler-fn "tg-token" command-handler-fn)
              result-chan (morse-handler-fn sample-message)]
          (<!! result-chan))))))