(ns iap-viewer.routes.home
  (:require [compojure.core :refer :all]
            [iap-viewer.views.layout :as layout]
            [iap-viewer.core :as core]
            [clojure.java.io :as io]
            (ring.middleware [multipart-params :as mp])))



;; home page definition
;; in input the list of purchases
(defn home [purchases]
  (layout/common
   (layout/upload-display-page purchases)))

;;
;; parse the receipt file and render the
;; home page
;;
;; in input you have a map containing the
;; tempfile key, i.e. the uploaded receipt
;;
;; {:size 62063,
;;  :tempfile #<File /var/folders/9_/0vs5vx611s74xpg0kymc0q3r0000gn/T/ring-multipart-2430006034048162240.tmp>,
;;  :content-type application/pdf,
;;  :filename blablala.der}
;;
(defn- parse-file [file-map]
  (let [receipt-url (io/as-url (file-map :tempfile))]
    (home (core/get-purchases-from-url receipt-url))))

;;
;; routes definitions
;;
(defroutes home-routes
  (GET "/" [] (home '()))
  (mp/wrap-multipart-params
   (POST "/verify" {params :params}
         (parse-file (get params :upfile)))))
