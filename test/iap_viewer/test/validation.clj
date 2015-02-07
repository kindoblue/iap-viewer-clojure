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

;; ---------------------
;; first set of tests
;; ---------------------
(with-private-fns [iap-viewer.validation [apple-ca-cert
                                          trust-anchor-set
                                          create-pkix-params]]
  (deftest test-root-certificate
    (testing "Local root CA certificate"
      (let [apple-ca  (apple-ca-cert)]

        ;; is the certificate a x509 object?
        (is (= (.toString(type apple-ca))
               "class org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateObject"))

        ;; is the subject the one from Apple?
        (is (= (.toString (.getSubjectDN apple-ca))
               "C=US,O=Apple Inc.,OU=Apple Certification Authority,CN=Apple Root CA"))))

    (testing "Trust anchor"
      (let [certificate  (generate-single-certificate "CN=Sample")
            trust-set (trust-anchor-set certificate)
            trust-anchor (first trust-set)
            pkix-params (create-pkix-params certificate)]

        (is (= (count trust-set) 1))

        (is (= (.toString (type trust-anchor))
               "class java.security.cert.TrustAnchor"))

        (is (= (.getTrustedCert trust-anchor) certificate))

        (is (= (.toString (type pkix-params))
               "class java.security.cert.PKIXParameters"))

        (is (not (.isRevocationEnabled pkix-params)))))))



;; ---------------------
;; second set of tests
;; ---------------------
(with-private-fns [iap-viewer.validation [cert-path-from-list
                                          validate-cert-path
                                          get-x509-certificates
                                          get-signer-certificate
                                          get-signer-id]]
  (deftest test-validation

    (testing "Creation of certificate path from a list of certificate"
      (let [cert-map (generate-test-certificates "CN=CA Stefano" "CN=Intermediate" "CN=End")
            certificate-list (list  (:root cert-map) (:intermediate cert-map) (:end cert-map))
            cert-path (cert-path-from-list certificate-list)]

        (is (= (.toString (type cert-path))
               "class org.bouncycastle.jcajce.provider.asymmetric.x509.PKIXCertPath"))

        (is (= (.getType cert-path) "X.509"))

        (is (= (count (.getCertificates cert-path)) 3))))


    (testing "Validation of the certification path"
      (let [certificate-map (generate-test-certificates "CN=CA Stefano" "CN=Intermediate" "CN=End")
            trust-anchor (:root certificate-map)
            signed-data (create-signed-data "Lallero" certificate-map)
            wrong-trust-anchor (generate-single-certificate "CN=WRONG!!")]


        ;; check the nominal case
        (is (validate-cert-path signed-data trust-anchor))

        ;; check the error case
        (is (thrown? java.security.cert.CertPathValidatorException
                     (validate-cert-path signed-data wrong-trust-anchor)))))

    (testing "Retrival of signer certificate and infos from signed data"
      (let [certificate-map (generate-test-certificates "CN=CA Stefano" "CN=Intermediate" "CN=End")
            trust-anchor (:root certificate-map)
            signed-data (create-signed-data "Lallero" certificate-map)
            signer-cert (get-signer-certificate signed-data)
            signer-id   (get-signer-id signed-data)
            ]

        (is (= (.toString (type signer-cert))
               "class org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateObject"))

        (is (= (.. signer-cert getSubjectDN getName) "CN=End"))

        (is (= (.. signer-cert getIssuerDN getName) "CN=Intermediate"))

        (is (= (.toString (type signer-id))
               "class org.bouncycastle.cms.SignerId"))

        (is (= 123 (.getSerialNumber signer-id)))

        ))))
