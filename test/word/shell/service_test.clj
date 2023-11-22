(ns word.shell.service-test
  (:require [clojure.test :refer [deftest is run-tests testing
                                  ;; run-test
                                  ]]
            [word.shell.service :as service]))

(deftest version-validity-test
  (let [versions-sample {:versions [{:version "2"
                                     :is_published false
                                     :created_at (java.time.LocalDateTime/now)}
                                    {:version "1"
                                     :is_published true
                                     :created_at (.minus (java.time.LocalDateTime/now) (java.time.Period/ofWeeks 2))}
                                    {:version "0"
                                     :is_published true
                                     :created_at (.minus (java.time.LocalDateTime/now) (java.time.Period/ofWeeks 3))}]}]
    (testing "Resulted map should contain 'draft_version' key"
      (is (contains? (service/rename-version-keys versions-sample) :draft_version)))

    (testing "Resulted map should contain 'latest_published_version' key"
      (is (contains? (service/rename-version-keys versions-sample) :latest_published_version)))

    (testing "Draft version should be 'published version' plus 1"
      (let [result (service/rename-version-keys versions-sample)
            latest-published-version (Integer/parseInt (:latest_published_version result))
            draft-version (Integer/parseInt (:draft_version result))]
        (is (= (+ latest-published-version 1) draft-version))))))

(deftest versions-condisions-test
  (testing "Renaming versions should work properly in a condition with draft version and published version"
    (let [versions-sample {:versions [{:version "1"
                                       :is_published false
                                       :created_at (java.time.LocalDateTime/now)}
                                      {:version "0"
                                       :is_published true
                                       :created_at (.minus (java.time.LocalDateTime/now) (java.time.Period/ofWeeks 2))}]}
          result (service/rename-version-keys versions-sample)
          latest-published-version (Integer/parseInt (:latest_published_version result))
          draft-version (Integer/parseInt (:draft_version result))]
      (is (= (+ latest-published-version 1) draft-version))))

  (testing "Renaming versions should work properly in a condition with draft version and without published version"
    (let [versions-sample {:versions [{:version "0"
                                       :is_published false
                                       :created_at (java.time.LocalDateTime/now)}]}
          result (service/rename-version-keys versions-sample)
          draft-version (Integer/parseInt (:draft_version result))]
      (is (= draft-version 0))
      (is (= (:latest_published_version versions-sample) nil))))

  (testing "Renaming versions should work properly in a condition without draft version and with published version"
    (let [versions-sample {:versions [{:version "0"
                                       :is_published true
                                       :created_at (java.time.LocalDateTime/now)}]}
          result (service/rename-version-keys versions-sample)
          latest-published-version (Integer/parseInt (:latest_published_version result))]
      (is (= latest-published-version 0))
      (is (= (:draft_version versions-sample) nil)))))


(run-tests)