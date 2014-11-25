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
  "Get the receipts inside the Apple receipt"
  [receipt-url]
  (->(get-content-info receipt-url)
     CMSSignedData.
     .getSignedContent
     .getContent
     ASN1Primitive/fromByteArray
     .getObjects
     enumeration-seq))

(defn decode [x]
  (-> (.getObjects x)
      enumeration-seq))

(defn parse [x]
  (let [asn1-seq (decode x)]
    (let [a (first asn1-seq)
          b (second asn1-seq)]
      [(.. a getValue intValue) (.. b getValue intValue)])))

(map parse (get-signed-data receipt-url))

;; for example
(get-signed-data receipt-url)

(defn home []
  (layout/common
   (layout/upload-display-page)))

(defroutes home-routes
  (GET "/" [] (home)))
