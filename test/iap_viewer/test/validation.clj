(ns iap-viewer.test.validation
  (:use clojure.test
        iap-viewer.validation
        iap-viewer.test.data))

;; macro to run tests on private functions
;; copied from http://nakkaya.com/2009/11/18/unit-testing-in-clojure/
(defmacro with-private-fns [[ns fns] & tests]
  "Refers private fns from ns and runs tests in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
     ~@tests))


;; a fixture to setup things before tests
(defn my-fixture [f]
  ;; register the bouncy castle provider
  (init-bc-provider)
  ;; run the test
  (f))

;; register the fixture
(use-fixtures :once my-fixture)

;; test validation
(with-private-fns [iap-viewer.validation [apple-ca-cert]]
  (deftest test-root-certificate
    (testing "Verify we can load the local root CA certificate"
      (let [apple-ca  (apple-ca-cert)]

        ;; is the certificate a x509 object?
        (is (= (.toString(type apple-ca))
               "class org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateObject"))

        ;; is the subject the one from Apple?
        (is (= (.toString (.getSubjectDN apple-ca))
               "C=US,O=Apple Inc.,OU=Apple Certification Authority,CN=Apple Root CA"))))))

(with-private-fns [iap-viewer.validation [validate-cert-path get-x509-certificates]]
  (deftest test-cert-path
    (testing "Verify we can validate the certification path"
      (let [certificate-map (generate-test-certificates "CN=CA Stefano" "CN=Intermediate" "CN=End")
            trust-anchor (:root certificate-map)
            signed-data (create-signed-data "Lallero" certificate-map)]

        ;(println (:intermediate certificate-map))
        ;(println (:end certificate-map))
        (println (get-x509-certificates  signed-data))
        (validate-cert-path signed-data trust-anchor)

        ))))
