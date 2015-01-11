(ns iap-viewer.test.data
  (:import (java.security Security KeyPair KeyPairGenerator SecureRandom)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (org.bouncycastle.asn1.x500 X500Name)
           (org.bouncycastle.asn1.x509 Extension BasicConstraints KeyUsage)
           (org.bouncycastle.cert.jcajce JcaX509v3CertificateBuilder)
           (org.bouncycastle.operator.jcajce JcaContentSignerBuilder)
           (org.bouncycastle.cert.jcajce JcaX509CertificateConverter JcaX509ExtensionUtils)
           (java.math BigInteger)))

;; see
;; http://www.bouncycastle.org/docs/pkixdocs1.5on/index.html
;; https://github.com/joschi/cryptoworkshop-bouncycastle/blob/master/src/main/java/cwguide/JcaUtils.java
;; http://www.cryptoworkshop.com/guide/cwguide-070313.pdf


;; TEMP init the bouncy castle provider
(defn init-bc-provider
  "Add Bouncy Castle as security provider"
  []
  (Security/addProvider (BouncyCastleProvider.)))

;; generate rsa key pair
(defn generate-rsa-keys
  "Generate a key pair, RSA, 2048bit key len"
  []
  (let [generator (KeyPairGenerator/getInstance "RSA" "BC")]
    (.initialize generator 2048 (SecureRandom.))
    (.generateKeyPair generator)))


(defn get-certificate-builder
  [issuer not-after subject public-key]
  (let [x509-issuer  (X500Name. issuer)
        serial       (BigInteger/valueOf 123)
        now          (java.util.Date. )
        valid        (java.util.Date.)
        x509-subject (X500Name. subject)]

    ;; create the builder, correctly configured
    (JcaX509v3CertificateBuilder. x509-issuer
                                  serial
                                  now
                                  valid
                                  x509-subject
                                  public-key)))

(defn build-content-signer
  [private-key]
  (let [signer-builder (.setProvider (JcaContentSignerBuilder. "SHA1withRSA") "BC")]
    (.build signer-builder private-key)))


;;
;; build generic certificate
;;
;;
;;
(defn build-certificate
  [subject-public-key   ;; the public key of the subject being certified
   subject-x500-name    ;; the x500 name of the subject
   signing-private-key  ;; the private key of the issuer (the intermediate)
   signing-certificate
   key-usage]
  (let [content-signer (build-content-signer signing-private-key)
        signer-x500-name (.getSubjectX500Principal signing-certificate)
        builder (get-certificate-builder subject-x500-name "a" signer-x500-name subject-public-key)
        ext-util  (JcaX509ExtensionUtils.)]
    (doto builder
      (.addExtension Extension/authorityKeyIdentifier false (.createAuthorityKeyIdentifier ext-util signing-certificate))
      (.addExtension Extension/subjectKeyIdentifier false (.createSubjectKeyIdentifier ext-util subject-public-key))
      (.addExtension Extension/basicConstraints true (BasicConstraints. false))
      (.addExtension Extension/keyUsage true key-usage))
    (.build builder content-signer)))

;;
;;
;;
;;
(defn build-root-certificate
  [key-pair]
  (let [content-signer (build-content-signer (.getPrivate key-pair))
        builder (get-certificate-builder "CN=Stefano" "a" "CN=Stefano" (.getPublic key-pair))]
    (.build builder content-signer)))

;;
;;
;;
;;
(defn build-intermediate-certificate
  [subject-public-key   ;; the public key of the subject being certified
   subject-x500-name
   signing-private-key  ;; the private key of the issuer (the CA in this case)
   ca-certificate       ;; the CA certificate
   ]
  (let [key-usage (KeyUsage. (bit-or KeyUsage/digitalSignature KeyUsage/keyCertSign KeyUsage/cRLSign))]
    (build-certificate subject-public-key subject-x500-name signing-private-key ca-certificate key-usage)))

;;
;;
;;
;;
(defn build-end-certificate
  [subject-public-key   ;; the public key of the subject being certified
   signing-private-key  ;; the private key of the issuer (the intermediate)
   signing-certificate       ;; the CA certificate
   ]
  (let [content-signer (build-content-signer signing-private-key)
        signer-x500-name (.getSubjectX500Principal signing-certificate)
        builder (get-certificate-builder "CN=End" "a" signer-x500-name subject-public-key)
        ext-util  (JcaX509ExtensionUtils.)
        key-usage (KeyUsage. (bit-or KeyUsage/digitalSignature KeyUsage/keyEncipherment))]
    (doto builder
      (.addExtension Extension/authorityKeyIdentifier false (.createAuthorityKeyIdentifier ext-util signing-certificate))
      (.addExtension Extension/subjectKeyIdentifier false (.createSubjectKeyIdentifier ext-util subject-public-key))
      (.addExtension Extension/basicConstraints true (BasicConstraints. false))
      (.addExtension Extension/keyUsage true key-usage))
    (.build builder content-signer)))

;;
;;
;;
;;
(defn- convert-to-x509
  "Convert the X509CertificateHolder to a X509Certificate"
  [holder]
  (-> (JcaX509CertificateConverter.)
      (.setProvider "BC")
      (.getCertificate holder)))
