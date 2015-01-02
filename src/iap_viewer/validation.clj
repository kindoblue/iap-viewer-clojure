(ns iap-viewer.validation
  (:require [iap-viewer.common :as common])
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (java.security.cert CertificateFactory TrustAnchor PKIXParameters CertPathValidator)
           (org.bouncycastle.cert.jcajce JcaX509CertificateConverter)
           (org.bouncycastle.cert.selector X509CertificateHolderSelector)
           (org.bouncycastle.cms.jcajce JcaSimpleSignerInfoVerifierBuilder)
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
  (let [cert (clojure.java.io/resource "AppleIncRootCertificate.crt")]
    (with-open [stream (.openStream cert)]
      (.generateCertificate (CertificateFactory/getInstance "X.509" "BC") stream ))))

;; get the trust anchor, wrapped in a set.
;; the trust anchor is based on our LOCAL apple CA certificate
(defn trust-anchor-set
  []
  #{(TrustAnchor. (apple-ca-cert) nil)})

(defn create-pkix-params
  "Returns the pkix parameters based of trust anchor object and correcty configured"
  []
  (doto (PKIXParameters. (trust-anchor-set))
    (.setDate (java.util.Date.))
    (.setRevocationEnabled false)))


;; this function extract the certificates from the signed data
;; optionally you can pass a not null signer id to get only the
;; corresponding certificate
(defn get-certificates
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (->(.getCertificates signed-data)
     (.getMatches nil)))


;;
(defn get-x509-certificates
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (map convert-to-x509 (get-certificates signed-data)))

(defn get-signer-info
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (-> signed-data
      .getSignerInfos
      .getSigners
      first))

(defn get-signer-id
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (.getSID (get-signer-info signed-data)))

(defn get-signer-certificate
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (let [signer-id (get-signer-id signed-data)]
    (-> signed-data
        .getCertificates
        (.getMatches signer-id)
        first
        convert-to-x509)))

;; Create the cert path
(defn cert-path-from-list
  "Create the cert path with the input list of certificates"
  [cert-list]
  (.generateCertPath (CertificateFactory/getInstance "X.509" "BC") cert-list))



(defn validate-cert-path-helper
  [cert-path params]
  (let [validator (CertPathValidator/getInstance "PKIX" "BC")]
    (.validate validator cert-path params)))

;; it validates the certificate chain contained into the
;; given signed data.
;; 1) get the list of the certificates in x509 format
;; 2) creates the certificate path from that list
;; 3) creates a pkix parameters to drive the validation
;; 4) validate the cert path (using the helper function)
(defn- validate-cert-path
  "Validates the certificate chain within the gived signed data object"
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (let [certs (get-x509-certificates signed-data)
        cert-path (cert-path-from-list certs)
        pkix-params (create-pkix-params)]
    (validate-cert-path-helper cert-path pkix-params)))


;; given a certificate, creates a verifier object that
;; can be used to verify signatures in the cms signed
;; content
(defn- create-verifier
  "It creates a verifier object using the given certificate"
  [certificate]
  (-> (JcaSimpleSignerInfoVerifierBuilder.)
      (.setProvider "BC")
      (.build certificate)))



;; =======================
;;      entry point
;; =======================
(defn verify-signature
  [^org.bouncycastle.cms.CMSSignedData signed-data]
  (let [signer-info (get-signer-info signed-data)
        signer-cert (get-signer-certificate signed-data)
        with-verifier (create-verifier signer-cert)
        dummy (validate-cert-path signed-data)]
    (.verify signer-info with-verifier)))
