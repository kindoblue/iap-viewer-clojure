(ns iap-viewer.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn common [& body]
  (html5
    [:head
     [:title "Apple In-App Purchases dumper/verifier"]
     (include-css "css/bootstrap-sortable.css" "css/bootstrap.min.css")
     (include-js "js/bootstrap-sortable.js" "js/bootstrap.min.js" "js/jquery.min.js" "js/moment.min.js")]
    [:body body]))
