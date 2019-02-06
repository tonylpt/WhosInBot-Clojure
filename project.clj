(defproject whosin "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.1"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.trace "0.7.10"]
                 [clj-time "0.15.0"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [digest "1.4.8"]
                 [camel-snake-kebab "0.4.0"]
                 [morse "0.4.0" :exclusions [org.clojure/core.async]]
                 [aero "1.1.3"]
                 [mount "0.1.16"]
                 [ragtime "0.8.0"]
                 [com.zaxxer/HikariCP "3.3.0"]
                 [org.postgresql/postgresql "42.2.5"]
                 [hikari-cp "2.6.0" :exclusions [org.postgresql/postgresql com.zaxxer/HikariCP]]
                 [org.clojure/java.jdbc "0.7.8"]
                 [honeysql "0.9.4"]
                 [nilenso/honeysql-postgres "0.2.5"]
                 [pjstadig/humane-test-output "0.8.2"]]

  :injections [(require 'pjstadig.humane-test-output)
               (pjstadig.humane-test-output/activate!)]

  :aliases {"migrate"  ["run" "--" "--migrate"]
            "rollback" ["run" "--" "--rollback"]}

  :main ^:skip-aot whosin.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
