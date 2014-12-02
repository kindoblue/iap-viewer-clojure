(ns iap-viewer.test.handler
  (:use clojure.test
        ring.mock.request
        iap-viewer.handler
        iap-viewer.core))

(def expected-home-page "<!DOCTYPE html>\n<html><head><title>Apple In-App Purchases dumper/verifier</title><link href=\"css/bootstrap-sortable.css\" rel=\"stylesheet\" type=\"text/css\"><link href=\"css/bootstrap.min.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"js/bootstrap-sortable.js\" type=\"text/javascript\"></script><script src=\"js/bootstrap.min.js\" type=\"text/javascript\"></script><script src=\"js/jquery.min.js\" type=\"text/javascript\"></script><script src=\"js/moment.min.js\" type=\"text/javascript\"></script></head><body><div class=\"container\"><div class=\"page-header\"><h1>Apple In-App Purchases dumper</h1></div><p class=\"lead\">The receipt you receive from Apple is DER encoded PKCS#7 signed message. You can upload a receipt and see the content here. For the moment the receipt is not validated.</p><div class=\"row\"><div class=\"col-md-6\"><form action=\"/verify\" enctype=\"multipart/form-data\" method=\"post\"><div class=\"form-group\"><input id=\"der-input-file\" name=\"upfile\" size=\"20000\" type=\"file\" /><div class=\"help-block\">Select an Apple receipt (DER format)</div></div><input class=\"btn btn-default\" name=\"submit\" type=\"submit\" value=\"Upload\" /></form></div></div></div></body></html>")

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) expected-home-page))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))


;; test receipt
(deftest test-parse-receipt
  (testing "parsing the receipt"
    (let [receipt-url  (clojure.java.io/resource "1000000101882225.cer")
          purchases (get-purchases-from-url receipt-url)]
      (let [{:keys [org-purchase-date purchase-date product-id org-transaction-id transaction-id quantity cancellation-date subscription-exp-date]} (first purchases)]
        (is (= org-purchase-date "2014-02-18T16:18:09Z"))
        (is (= purchase-date "2014-02-19T13:26:54Z"))
        (is (= product-id "1_month_subscription"))
        (is (= org-transaction-id "1000000101874971"))
        (is (= transaction-id "1000000101874971"))
        (is (= quantity 1))
        (is (= cancellation-date ""))
        (is (= subscription-exp-date ""))))))
