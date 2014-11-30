(ns iap-viewer.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.core :refer [html]]))

(defn common [& body]
  (html5
    [:head
     [:title "Apple In-App Purchases dumper/verifier"]
     (include-css "css/bootstrap-sortable.css" "css/bootstrap.min.css")
     (include-js "js/bootstrap-sortable.js" "js/bootstrap.min.js" "js/jquery.min.js" "js/moment.min.js")]
    [:body body]))

;; helper function, to add the upload form
(defn- add-upload-form []
  (html
    [:form {:action "/verify" :method "post"}
      [:div.form-group
       [:input#der-input-file {:type "file" :name "name" :size 20}]
       [:div.help-block "Select an Apple receipt (DER format)"]]
     [:input.btn.btn-default {:type "submit" :name "submit" :value "Upload"}]]))

;; helper function to unroll the th's in the purchases table
(defn- map-tag [tag xs]
  (map (fn [x] [tag x]) xs))

(defn upload-display-page [purchases]
  (html
   [:div.container
    [:div.page-header
     [:h1 "Apple In-App Purchases dumper"]]
    [:p.lead "The receipt you receive from Apple is DER encoded PKCS#7 signed message. You can upload a receipt and see the content here. For the moment the receipt is not validated."]
    [:div.row
     [:div.col-md-6
      (add-upload-form)]]
    (if (seq purchases)
      [:div.row
       [:table#results-table.table.table-striped.sortable {:style "margin-top:20pt"}
        [:thead
         [:tr (map-tag :th ["Transaction ID" "Product ID" "Purchase Date" "Original Purchase Date" "Quantity"])]]
        [:tbody
         (for [{:keys [transaction-id product-id purchase-date org-purchase-date quantity]} purchases]
           [:tr
            [:td transaction-id]
            [:td product-id]
            [:td purchase-date]
            [:td org-purchase-date]
            [:td quantity]])]]])]))
