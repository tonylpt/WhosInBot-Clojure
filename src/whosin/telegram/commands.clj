(ns whosin.telegram.commands
  (:require [whosin.domain.roll-calls :as domain]
            [clojure.string :as string])
  (:import (clojure.lang Keyword)))

(defrecord Command [chat-id
                    user-id
                    user-name
                    command-arg
                    text])

(def ^:const no-roll-call-available "No roll call in progress.")

(defn start-roll-call
  [^Command {chat-id :chat-id title :command-arg}]
  (domain/open-new-roll-call! chat-id title)
  "Roll call started.")

(defn end-roll-call
  [^Command {:keys [chat-id]}]
  (if (domain/close-roll-call! chat-id)
    "Roll call ended."
    no-roll-call-available))

(defn set-title
  [^Command {chat-id :chat-id title :command-arg}]
  (cond
    (string/blank? title) "Please provide a title."
    (domain/set-roll-call-title! chat-id title) "Roll call title set."
    :else no-roll-call-available))

(defn set-quiet
  [^Command {:keys [chat-id]}]
  (if (domain/set-roll-call-quiet! chat-id true)
    "Ok fine, I'll be quiet. \uD83E\uDD10"
    no-roll-call-available))

(declare list-responses)

(defn set-loud
  [^Command {:keys [chat-id] :as command}]
  (if (domain/set-roll-call-quiet! chat-id false)
    (str "Sure. \uD83D\uDE03" \newline \newline
         (list-responses command))
    no-roll-call-available))

(defn- get-response-str-full
  [responses]
  (let [resp-by-status (group-by :status responses)
        resp-by-sorted-status (->> [:in :out :maybe]
                                   (mapv #(vector % (% resp-by-status)))
                                   (filter (fn [[_ group]]
                                             (not (empty? group)))))

        response->str (fn [{:keys [user-name reason]}]
                        (str " - " user-name (and (not-empty reason)
                                                  (str " (" reason ")"))))

        status->str (fn [status]
                      (status {:in "In", :out "Out", :maybe "Maybe"}))

        resp-str-by-status (for [[status resp] resp-by-sorted-status
                                 :let [resp-str (->> resp
                                                     (sort-by :updated-at)
                                                     (map #(response->str %))
                                                     (string/join \newline))]]
                             [status resp-str (count resp)])

        resp-group-strings (for [[status resp count] resp-str-by-status
                                 :let [status-str (status->str status)]]
                             (str status-str " (" count ")" \newline resp))]

    (string/join "\n\n" resp-group-strings)))

(defn- get-response-str-short
  [responses]
  (let [resp-by-status (group-by :status responses)
        count-by-status (into {} (for [[status responses] resp-by-status]
                                   [status (count responses)]))]
    (format "Total: %d in, %d out, %d might come."
            (:in count-by-status 0)
            (:out count-by-status 0)
            (:maybe count-by-status 0))))

(defn- get-responses-str
  [{:keys [title quiet]} responses]
  (letfn [(with-title [body] (->> [title body]
                                  (filter #(not (string/blank? %)))
                                  (string/join \newline)))]
    (with-title
      (cond
        (empty? responses) "No responses yet. \uD83D\uDE22"
        quiet (get-response-str-short responses)
        :else (get-response-str-full responses)))))

(defn list-responses
  [^Command {:keys [chat-id]}]
  (if-let [{:keys [roll-call responses]} (domain/roll-call-with-responses chat-id)]
    (get-responses-str roll-call responses)
    no-roll-call-available))

(defn- set-attendance-reply [user-name attendance]
  (str user-name " " (attendance {:in    "is in!"
                                  :out   "is out!"
                                  :maybe "might come!"})))

(defn set-attendance-fn
  [^Keyword attendance]
  (fn [^Command {chat-id   :chat-id
                 user-id   :user-id
                 user-name :user-name
                 reason    :command-arg :as command}]
    (if-not (domain/set-roll-call-attendance! chat-id attendance user-id user-name reason)
      no-roll-call-available
      (str (set-attendance-reply user-name attendance) \newline \newline
           (list-responses command)))))

(defn- ->name-and-reason [text]
  (let [[first-word remaining] (string/split text #"\s+" 2)]
    {:name   first-word
     :reason remaining}))

(defn set-attendance-for-fn
  [^Keyword attendance]
  (fn [^Command {:keys [chat-id command-arg] :as command}]
    (let [{:keys [name reason]} (->name-and-reason command-arg)]
      (cond
        (string/blank? name)
        "Please provide the persons name."

        (nil? (domain/set-roll-call-attendance-for! chat-id attendance name reason))
        no-roll-call-available

        :else
        (str (set-attendance-reply name attendance) \newline \newline
             (list-responses command))))))

(defn available-commands
  [_]
  (->> ["start_roll_call"
        "end_roll_call"
        "set_title"
        "shh"
        "louder"
        "in"
        "out"
        "maybe"
        "set_in_for"
        "set_out_for"
        "set_maybe_for"
        "whos_in"
        "available_commands"]
       (map #(str " \uD83C\uDF7A /" %))
       (into ["Available commands:"])
       (string/join \newline)))
