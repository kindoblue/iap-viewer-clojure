(ns iap-viewer.routes.home
  (:require [compojure.core :refer :all]
            [iap-viewer.views.layout :as layout]
            (ring.middleware [multipart-params :as mp]))
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (java.security.cert CertificateFactory)
           (java.security Security)))


;; test receipt
(def receipt-url (clojure.java.io/resource "1000000101882225.cer"))


(defn get-content-info
  "Get the content info object representing the Apple receipt"
  [receipt-url]
  (-> (.openStream receipt-url)
      ASN1InputStream.
      .readObject
      ContentInfo/getInstance))

(defn get-signed-data
  "Get the all the info inside the Apple receipt, returning as enumeration-seq"
  [receipt-url]
  (->(get-content-info receipt-url)
     CMSSignedData.
     .getSignedContent
     .getContent
     ASN1Primitive/fromByteArray
     .getObjects
     enumeration-seq))

(defn get-receipt-fields [x]
  (-> (.getObjects x)
      enumeration-seq))

;; the input is a raw entry, in the form of a DLSequence #<DLSequence [a, b, #<content>]>
;; in case of purchase --> a = 17 and b = 1
(defn is-purchase
  "Return true if the input is a purchase"
  [^org.bouncycastle.asn1.DLSequence x]
  (let [fields (get-receipt-fields x)
        a (first fields)
        b (second fields)]
    (and (= 17 (.. a getValue intValue)) (= 1 (.. b getValue intValue)))))


;; return a list of org.bouncycastle.asn1.DLSequence objects
;; representing the purchases
(defn get-purchases-raw [records]
  (filter is-purchase records))


(defn parse-quantity [^org.bouncycastle.asn1.DEROctetString input]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input))
        b (.. a getValue intValue)]
    {:quantity b}))

(defn parse-product-id [^org.bouncycastle.asn1.DEROctetString input]
   (let [a (ASN1Primitive/fromByteArray (.getOctets input))
         b (.getString a)]
     {:product-id b}))

(defn parse-transaction-id [^org.bouncycastle.asn1.DEROctetString input]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input))
        b (.getString a)]
    {:transaction-id b}))

(defn parse-original-transaction-id [^org.bouncycastle.asn1.DEROctetString input]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input))
        b (.getString a)]
    {:org-transaction-id b}))

(defn parse-purchase-date [^org.bouncycastle.asn1.DEROctetString input-date]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input-date))
        b (.getString a)]
    {:purchase-date b}))

(defn parse-original-purchase-date [^org.bouncycastle.asn1.DEROctetString input-date]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input-date))
        b (.getString a)]
    {:org-purchase-date b})))

(defn parse-subscription-exp-date [^org.bouncycastle.asn1.DEROctetString input-date]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input-date))
        b (.getString a)]
    {:subscription-exp-date b})))

(defn parse-cancellation-date [^org.bouncycastle.asn1.DEROctetString input-date]
  (let [a (ASN1Primitive/fromByteArray (.getOctets input-date))
        b (.getString a)]
    {:cancellation-date b})))


;; see https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html
;;
(defn parse-purchase-field [^org.bouncycastle.asn1.DLSequence x]
  (let [^org.bouncycastle.asn1.ASN1Integer field-type-obj (.getObjectAt x 0)
        field-type (.. field-type-obj getValue intValue)
        field-value (.getObjectAt x 2)]
    (cond
     (= field-type 1701) (parse-quantity field-value)
     (= field-type 1702) (parse-product-id field-value)
     (= field-type 1703) (parse-transaction-id field-value)
     (= field-type 1705) (parse-original-transaction-id field-value)
     (= field-type 1704) (parse-purchase-date field-value)
     (= field-type 1706) (parse-original-purchase-date field-value)
     (= field-type 1708) (parse-subscription-exp-date field-value)
     (= field-type 1712) (parse-cancellation-date field-value)
     :else nil)))

(defn parse-purchase-fields [^org.bouncycastle.asn1.DLSet fields]
  (let [field-seq (enumeration-seq (.getObjects fields))]
    (map parse-purchase-field field-seq)))

;; it gets a purchase as #<DLSequence [17, 1, #<DEROctetString>]>, i.e. a sequence
;; of three objects. The third one is a byte sequence to be interpreted as a DLSet,
;; the set of fields of the purchase record
(defn parse-purchase [^org.bouncycastle.asn1.DLSequence raw-purchase]
  (let [purchase-as-dlset (ASN1Primitive/fromByteArray (.getOctets (.getObjectAt raw-purchase 2)))]
    (parse-purchase-fields purchase-as-dlset)))

(map parse-purchase (get-purchases-raw (get-signed-data receipt-url)))

;; for example
(get-signed-data receipt-url)

(defn home []
  (layout/common
   (layout/upload-display-page)))

(defroutes home-routes
  (GET "/" [] (home)))
