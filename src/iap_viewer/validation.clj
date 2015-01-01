(ns iap-viewer.validation
  (:require [iap-viewer.core])
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (java.security.cert CertificateFactory TrustAnchor PKIXParameters CertPathValidator)
           (java.security Security)
           (java.io.File)))


;; init the bouncy castle provider
(defn init-bc-provider
  "Add Bouncy Castle as security provider"
  []
  (Security/addProvider (BouncyCastleProvider.)))

;; The Bouncy Castle APIs generate X509CertificateHolder objects
;; which can then be converted into Java X509Certificate objects
;; if required. The reason this is done is to allow the provision
;; and manipulation of certificates in a JVM independent manner
(defn convert-to-x509
  "Convert the X509CertificateHolder to a X509Certificate"
  [holder]
  (-> (JcaX509CertificateConverter.)
      (.setProvider "BC")
      (.getCertificate holder)))

;; get the local CA certificate
(defn apple-ca-cert []
  (let [cert (clojure.java.io/resource "AppleIncRootCertificate.cer")]
    (with-open [stream (.openStream cert)]
      (.generateCertificate (CertificateFactory/getInstance "X.509" "BC") stream ))))

;; get the trust anchor, wrapped in a set.
;; the trust anchor is based on our LOCAL apple CA certificate
(defn trust-anchor-set
  []
  #{(TrustAnchor. (apple-ca-cert) nil)})

(defn pkix-params
  "Returns the pkix parameters based of trust anchor object and correcty configured"
  []
  (doto (PKIXParameters. (trust-anchor-set))
    (.setDate (java.util.Date.))
    (.setRevocationEnabled false)))

;; TODO to complete
(defn get-x509-certificates []
  (let [receipt-url  (clojure.java.io/resource "1000000101882225.cer")]
    (with-open [stream (.openStream receipt-url)]
      (map convert-to-x509 (get-certificates stream)))))

;; create the cert path
(defn cert-path
  "Create the cert path with the input list of certificates"
  [cert-list]
  (.generateCertPath (CertificateFactory/getInstance "X.509" "BC") cert-list))

(defn validate-cert-path
  [cert-path params]
  (let [validator (CertPathValidator/getInstance "PKIX" "BC")]
    (.validate validator cert-path params)))
