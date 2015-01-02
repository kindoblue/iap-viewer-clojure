(ns iap-viewer.test.validation
  (:use clojure.test
        iap-viewer.core))

;; test validation
(deftest test-validation
  (testing "validation"
    (let [receipt-url  (clojure.java.io/resource "1000000101882225.cer")]

      )))

(defn geppo []
  (let [receipt-url  (clojure.java.io/resource "1000000101882225.cer")]
    (with-open [stream (.openStream receipt-url)]
      (get-certificates stream))))
