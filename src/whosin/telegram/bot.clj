(ns whosin.telegram.bot
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [go]]
            [morse.handlers :as h]
            [morse.api :as t]
            [morse.polling :as p]
            [whosin.config :as config]
            [whosin.telegram.commands :as commands]
            [mount.core :as mount]
            [clojure.string :as string])
  (:import (whosin.telegram.commands Command)))

(defn- extract-command-arg
  "Extracts part of the message text following the /command."
  [message-text]
  (-> message-text
      (string/replace-first #"^/\S+(?=\s*)" "")
      (string/trim)))

(defn- morse-msg->Command
  "Converts a Telegram message received via Morse into a Command."
  ^Command
  [morse-msg]
  (let [{{chat-id :id}           :chat,
         {user-id   :id
          user-name :first_name} :from,
         message-text            :text} morse-msg
        command-arg (extract-command-arg message-text)]

    (commands/map->Command {:chat-id     chat-id
                            :user-id     user-id
                            :user-name   user-name
                            :command-arg command-arg
                            :text        message-text})))

(defn- wrap-handler-fn
  [^String token command-handler-fn]
  (letfn [(handle [^Command command]
            (try
              (command-handler-fn command)
              (catch Throwable e
                (log/errorf e "Error handling command %s" command)
                "An error has occurred.")))

          (reply [chat-id ^String reply-msg]
            (try (t/send-text token chat-id reply-msg)
                 (catch Throwable e
                   (log/error e "Error sending reply."))))

          (handler-fn-async [morse-msg]
            (go
              (let [{:keys [chat-id] :as command} (morse-msg->Command morse-msg)]
                (->> command
                     (handle)
                     (reply chat-id)))))]

    handler-fn-async))

(defn- command-fn
  [^String token ^String command handler-fn]
  (h/command-fn command
                (wrap-handler-fn token handler-fn)))

(defn generic-handler-fn
  [token]
  (fn [{{chat-id :id} :chat}]
    (t/send-text token chat-id "I don't understand that.")))

(defn- bot-api [token]
  (h/handlers (command-fn token "start_roll_call" commands/start-roll-call)
              (command-fn token "end_roll_call" commands/end-roll-call)

              (command-fn token "set_title" commands/set-title)
              (command-fn token "shh" commands/set-quiet)
              (command-fn token "louder" commands/set-loud)

              (command-fn token "in" (commands/set-attendance-fn :in))
              (command-fn token "out" (commands/set-attendance-fn :out))
              (command-fn token "maybe" (commands/set-attendance-fn :maybe))

              (command-fn token "set_in_for" (commands/set-attendance-for-fn :in))
              (command-fn token "set_out_for" (commands/set-attendance-for-fn :out))
              (command-fn token "set_maybe_for" (commands/set-attendance-for-fn :maybe))

              (command-fn token "whos_in" commands/list-responses)
              (command-fn token "available_commands" commands/available-commands)
              (h/message-fn (generic-handler-fn token))))

(declare bot)
(mount/defstate bot
  :start (let [token (get-in config/config [:telegram :token])]
           (log/info "Starting Telegram Polling Bot...")
           (p/start token (bot-api token)))

  :stop (do (p/stop bot)
            (log/info "Telegram Polling Bot has been stopped.")))
