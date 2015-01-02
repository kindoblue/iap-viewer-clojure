(ns iap-viewer.common
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (org.bouncycastle.asn1.cms ContentInfo)))


(defn get-cms-signed-data
  "Get the content info object representing the Apple receipt"
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


(defn geppo []
  (let [receipt-url  (clojure.java.io/resource "1000000101882225.cer")]
    (with-open [stream (.openStream receipt-url)]
      (get-cms-signed-data stream))))
