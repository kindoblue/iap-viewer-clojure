(ns iap-viewer.common
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.asn1.cms ContentInfo)))


(defn get-cms-signed-data
  "Get the cms signed data object, which represent the Apple receipt"
  [^java.io.BufferedInputStream input]
  (-> (ASN1InputStream. input)
      .readObject
      ContentInfo/getInstance
      CMSSignedData.))

;; this function extract the signed data from the Apple receipt
;; it does while catching every exception because we care only
;; about the nominal case. Tempted to use the maybe-m monad ;-)
;; but we have a chain on interop calls, so not sure how beneficial
;; would have been using the monad
;; TODO rename as get-receipt-content and remove try catch
(defn get-content
  "Get the all the info inside the Apple receipt, returning as enumeration-seq"
  [^org.bouncycastle.cms.CMSSignedData input]
  (try  (->
         (.getSignedContent input)
         .getContent
         ASN1Primitive/fromByteArray
         .getObjects
         enumeration-seq)
        (catch Exception e)))
