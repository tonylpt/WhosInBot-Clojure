(ns whosin.core-test
  (:require [clojure.test :refer :all]
            [whosin.core :refer [-main start]]
            [whosin.db.migrations :as migrations]
            [whosin.test-util :refer [expect-called]]))

(deftest -main-test
  (testing "run migrations when passed migrate option"
    (expect-called
      [migrations/migrate-db :any nil]
      (-main "--migrate")))

  (testing "rollback migrations when passed rollback option"
    (expect-called
      [migrations/rollback-db :any nil]
      (-main "--rollback")))

  (testing "run app when passed no options"
    (expect-called
      [start :any nil]
      (-main))))
