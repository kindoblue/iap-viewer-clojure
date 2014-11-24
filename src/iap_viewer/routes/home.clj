(ns iap-viewer.routes.home
  (:require [compojure.core :refer :all]
            [iap-viewer.views.layout :as layout])
  (:import (org.bouncycastle.asn1 ASN1InputStream ASN1Primitive)
           (org.bouncycastle.asn1.cms ContentInfo SignedData)
           (org.bouncycastle.cms CMSSignedData)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (java.security.cert CertificateFactory)
           (java.security Security)))

(defn home []
  (layout/common [:h1 "Hello World!"]))

(defroutes home-routes
  (GET "/" [] (home)))
