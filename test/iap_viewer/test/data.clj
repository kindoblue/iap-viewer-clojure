;;;
;;;  This namespace contains functions to create data that will be used to test
;;;  validation.
;;;
(ns iap-viewer.test.data
  (:import (java.security Security KeyPair KeyPairGenerator SecureRandom)
           (javax.security.auth.x500 X500Principal)
           (org.bouncycastle.util Store)
           (org.bouncycastle.cert.jcajce JcaCertStore)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (org.bouncycastle.asn1.x500 X500Name)
           (org.bouncycastle.asn1.x509 Extension BasicConstraints KeyUsage)
           (org.bouncycastle.cert.jcajce JcaX509v3CertificateBuilder)
           (org.bouncycastle.operator.jcajce JcaContentSignerBuilder)
           (org.bouncycastle.cert.jcajce JcaX509CertificateConverter JcaX509ExtensionUtils)
           (org.bouncycastle.cms CMSSignedDataGenerator)
           (org.bouncycastle.operator.jcajce JcaContentSignerBuilder)
           (org.bouncycastle.cms.jcajce JcaSignerInfoGeneratorBuilder)
           (org.bouncycastle.cms CMSProcessableByteArray)
           (org.bouncycastle.operator.jcajce JcaDigestCalculatorProviderBuilder)
           (java.math BigInteger)))

;; see
;; http://www.bouncycastle.org/docs/pkixdocs1.5on/index.html
;; http://www.bouncycastle.org/docs/docs1.5on/index.html
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
  [^String issuer
   ^java.util.Date expiration
   ^String subject
   ^java.security.PublicKey public-key]
  (let [x509-issuer  (X500Principal. issuer)
        serial       (BigInteger/valueOf 123)
        now          (java.util.Date. )
        x509-subject (X500Principal. subject)]

    ;; create the builder, correctly configured
    (JcaX509v3CertificateBuilder. x509-issuer
                                  serial
                                  now
                                  expiration
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
   signing-private-key  ;; the private key of the issuer
   signing-certificate
   expiration
   key-usage]
  (let [content-signer (build-content-signer signing-private-key)
        signer-x500-name (.getName (.getSubjectX500Principal signing-certificate))
        builder (get-certificate-builder signer-x500-name expiration subject-x500-name subject-public-key)
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
(defn- convert-to-x509
  "Convert the X509CertificateHolder to a X509Certificate"
  [holder]
  (-> (JcaX509CertificateConverter.)
      (.setProvider "BC")
      (.getCertificate holder)))


;;
;;
;;
;;
(defn build-root-certificate
  [^java.security.KeyPair key-pair
   ^java.util.Date expiration
   ^String name]
  (let [content-signer (build-content-signer (.getPrivate key-pair))
        builder (get-certificate-builder name expiration name (.getPublic key-pair))]
    (-> (.build builder content-signer)
        (convert-to-x509))))

;;
;;
;;
;;
(defn build-intermediate-certificate
  [subject-public-key   ;; the public key of the subject being certified
   subject-x500-name
   signing-private-key  ;; the private key of the issuer (the CA in this case)
   ca-certificate       ;; the CA certificate
   expiration           ;; expiration date
   ]
  (let [key-usage (KeyUsage. (bit-or KeyUsage/digitalSignature KeyUsage/keyCertSign KeyUsage/cRLSign))]
    (-> (build-certificate subject-public-key subject-x500-name signing-private-key ca-certificate expiration key-usage)
        (convert-to-x509))))

;;
;;
;;
;;
(defn build-end-certificate
  [subject-public-key   ;; the public key of the subject being certified
   subject-x500-name    ;; the subject name
   signing-private-key  ;; the private key of the issuer (the intermediate)
   signing-certificate  ;; the CA certificate
   expiration           ;; expiration date
   ]
  (let [key-usage (KeyUsage. (bit-or KeyUsage/digitalSignature KeyUsage/keyEncipherment))]
    (-> (build-certificate subject-public-key subject-x500-name signing-private-key signing-certificate expiration key-usage)
        (convert-to-x509))))


;;;
;;;
;;;
(defn- one-day-after
  [now]
  (let [cal (java.util.Calendar/getInstance)]
    (doto cal
      (.setTime now)
      (.add java.util.Calendar/DATE 1))
    (.getTime cal)))



;;;
;;; 1) generate the key pair for the CA entity
;;; 2) build the root CA certificate
;;; 3) generate the key pair for intermediate entity
;;; 4) build the intermediate certificate
;;; 5) generate the key pair for end entity
;;; 6) build the end certificate
;;; 7) return the three certificate
(defn generate-test-certificates
  [^String root-subject-name
   ^String intermediate-subject-name
   ^String end-subject-name]
  (let [now (java.util.Date.)
        tomorrow (one-day-after now)
        ca-key-pair (generate-rsa-keys)
        intermediate-key-pair (generate-rsa-keys)
        end-key-pair (generate-rsa-keys)
        root-certificate (build-root-certificate ca-key-pair tomorrow root-subject-name)
        intermediate-certificate (build-intermediate-certificate (.getPublic intermediate-key-pair) intermediate-subject-name (.getPrivate ca-key-pair) root-certificate tomorrow)
        end-certificate (build-intermediate-certificate (.getPublic intermediate-key-pair) end-subject-name (.getPrivate intermediate-key-pair) intermediate-certificate tomorrow)
        ]
    {:root root-certificate
     :intermediate intermediate-certificate
     :end end-certificate
     :private (.getPrivate end-key-pair)}))

;;
;;  CREATE CMSSignedData
;;  see https://www.bouncycastle.org/docs/pkixdocs1.5on/org/bouncycastle/cms/CMSSignedDataGenerator.html
;;

(defn create-cert-store
  [{root-cert :root intermediate-cert :intermediate end-cert :end}]
  (let [cert-list [root-cert intermediate-cert end-cert]
        cert-store (JcaCertStore. cert-list)]
    cert-store))

(defn create-content-signer
  [private-key]
  (let [content-signer (JcaContentSignerBuilder. "SHA1withRSA")]
    (-> content-signer
        (.setProvider "BC")
        (.build private-key))))

(defn create-signer-info-generator
  [content-signer
   certificate]
  (let [digest-calculator-builder
        (-> (JcaDigestCalculatorProviderBuilder.)
            (.setProvider "BC")
            (.build))
        generator-builder (JcaSignerInfoGeneratorBuilder. digest-calculator-builder)]
    (.build generator-builder content-signer certificate)))

(defn create-cms-generator
  [{certificate :end private-key :private :as cert-map}]
  (let [cert-store (create-cert-store cert-map)
        generator (CMSSignedDataGenerator.)
        content-signer (create-content-signer private-key)
        signer-info-gen (create-signer-info-generator content-signer certificate)]
    (doto generator
      (.addSignerInfoGenerator signer-info-gen)
      (.addCertificates cert-store))))

(defn create-processable-data
  [^String message]
  (let [bytes (.getBytes message)]
    (CMSProcessableByteArray. bytes)))


;; entry point

(defn create-signed-data
  [^String message
   cert-map]
  (let [processable-data (create-processable-data message)
        cms-generator (create-cms-generator cert-map)]
    (.generate cms-generator processable-data true)))
