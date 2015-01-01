(ns iap-viewer.core
  (:require [clojure.pprint])
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (org.bouncycastle.cert.jcajce JcaX509CertificateConverter)
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




;; the input is a raw entry, in the form of a DLSequence #<DLSequence [a, b, #<content>]>
;; in case of purchase --> a = 17 and b = 1
(defn- is-purchase
  "Return true if the input is a purchase"
  [^org.bouncycastle.asn1.DLSequence x]
  (let [a (.getObjectAt x 0)
        b (.getObjectAt x 1)]
    (and (= 17 (.. a getValue intValue)) (= 1 (.. b getValue intValue)))))


;; see https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html
;;
(defn- parse-purchase-field [^org.bouncycastle.asn1.DLSequence x]
  (let [^org.bouncycastle.asn1.ASN1Integer field-type-obj (.getObjectAt x 0)
        field-type (.. field-type-obj getValue intValue)
        field-value-obj (.getObjectAt x 2)
        field-value (ASN1Primitive/fromByteArray (.getOctets field-value-obj))]
    (cond
     (= field-type 1701) {:quantity (.. field-value getValue intValue)}
     (= field-type 1702) {:product-id (.getString field-value)}
     (= field-type 1703) {:transaction-id (.getString field-value)}
     (= field-type 1705) {:org-transaction-id (.getString field-value)}
     (= field-type 1704) {:purchase-date (.getString field-value)}
     (= field-type 1706) {:org-purchase-date (.getString field-value)}
     (= field-type 1708) {:subscription-exp-date (.getString field-value)}
     (= field-type 1712) {:cancellation-date (.getString field-value)})))


;; it gets a purchase as #<DLSequence [17, 1, #<DEROctetString>]>, i.e. a sequence
;; of three objects. The third one is a byte sequence to be interpreted as a DLSet,
;; the set of fields of the purchase record
(defn- parse-purchase [^org.bouncycastle.asn1.DLSequence raw-purchase]
  (let [purchase-as-dlset (ASN1Primitive/fromByteArray (.getOctets (.getObjectAt raw-purchase 2)))
        field-sequence (enumeration-seq (.getObjects purchase-as-dlset))]
    (reduce conj {} (map parse-purchase-field field-sequence))))


;; return a list of org.bouncycastle.asn1.DLSequence objects
;; representing the purchases
(defn- get-purchases [records]
  (filter is-purchase records))

(defn- get-content-info
  "Get the content info object representing the Apple receipt"
  [^java.io.BufferedInputStream input]
  (-> (ASN1InputStream. input)
      .readObject
      ContentInfo/getInstance))

;; this function extract the signed data from the Apple receipt
;; it does while catching every exception because we care only
;; about the nominal case. Tempted to use the maybe-m monad ;-)
;; but we have a chain on interop calls, so not sure how beneficial
;; would have been using the monad
(defn- get-signed-data
  "Get the all the info inside the Apple receipt, returning as enumeration-seq"
  [^java.io.BufferedInputStream input]
  (try  (->(get-content-info input)
           CMSSignedData.
           .getSignedContent
           .getContent
           ASN1Primitive/fromByteArray
           .getObjects
           enumeration-seq)
        (catch Exception e)))

;; this function extract the certificates from the receipt
;; TODO support selection based on signer id
(defn get-certificates [^java.io.BufferedInputStream stream]
  (->(get-content-info stream)
     CMSSignedData.
     .getCertificates
     (.getMatches nil)))


;; =======================
;;      entry point
;; =======================
(defn get-purchases-from-url
  "Return a list of maps, one for every purchase. As input the url to the apple receipt"
  [receipt-url]
  (with-open [stream (.openStream receipt-url)]
    (map parse-purchase (get-purchases (get-signed-data stream)))))

;; to be continued
(defn main
  ([] (println "WARN: please specify the file to parse (expected der encoded filename)"))
  ([input-file-name]
   (let [url (clojure.java.io/as-url (java.io.File. input-file-name))]
     (clojure.pprint/print-table (get-purchases-from-url url)))))
