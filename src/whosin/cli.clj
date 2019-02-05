(ns whosin.cli
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as string]
            [clojure.core.match :refer [match]]))

(def ^:private cli-options
  [["-m" "--db:migrate" "Apply database migrations"]
   ["-r" "--db:rollback" "Rollback database migrations"]
   ["-h" "--help"]])

(defn- usage-str ^String [summary]
  (->> [""
        "Usage: bot [options]"
        ""
        "Options:"
        summary
        ""]
       (string/join \newline)))

(defn- errors-str ^String [errors summary]
  (->> (usage-str summary)
       (conj errors)
       (string/join \newline)))

(defn parse-args [args]
  (let [{:keys [summary] :as parsed-opts} (cli/parse-opts args cli-options)
        migrate-flag :db:migrate
        rollback-flag :db:rollback]

    (match [parsed-opts]
           [{:errors ([_ & _] :as errors)}]
           {:exit (errors-str errors summary)}

           [{:arguments ([_ & _] :as arguments)}]
           {:exit (errors-str (mapv #(str "Unrecognized argument: " %) arguments)
                              summary)}

           [{:options {:help _}}]
           {:exit (usage-str summary) :ok? true}

           [{:options {migrate-flag true, rollback-flag true}}]
           {:exit (errors-str ["Cannot apply and rollback database migrations at the same time."]
                              summary)}

           [{:options {migrate-flag true}}]
           {:action :migrate}

           [{:options {rollback-flag true}}]
           {:action :rollback}

           :else
           {:action :start})))
