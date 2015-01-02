(ns iap-viewer.core
  (:require [clojure.pprint]
            [iap-viewer.common :as common]
            [iap-viewer.validation :as validation])
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (java.security.cert CertificateFactory)
           (java.security Security)
           (java.io.File)))


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
(defn- get-raw-purchases [records]
  (filter is-purchase records))



;; =======================
;;      entry point
;; =======================
(defn get-purchases-from-url
  "Return a list of maps, one for every purchase. As input the url to the apple receipt"
  [receipt-url]
  (with-open [stream (.openStream receipt-url)]
    (let [signed-data (common/get-cms-signed-data stream)]
      (validation/init-bc-provider)
      (validation/validate signed-data)
      (map parse-purchase (get-raw-purchases (common/get-content signed-data))))))

;; to be continued
(defn main
  ([] (println "WARN: please specify the file to parse (expected der encoded filename)"))
  ([input-file-name]
   (let [url (clojure.java.io/as-url (java.io.File. input-file-name))]
     (clojure.pprint/print-table (get-purchases-from-url url)))))
