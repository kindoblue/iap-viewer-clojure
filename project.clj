(defproject iap-viewer "0.1.0-SNAPSHOT"
  :description "A simple webapp to show the in app purchases within a receipt file from Apple"
  :url "http://iap-viewer.kindoblue.nl"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [ring-server "0.3.1"]
                 [org.bouncycastle/bcprov-jdk15on "1.50"]
                 [org.bouncycastle/bcmail-jdk15on "1.50"]
                 [org.bouncycastle/bcpkix-jdk15on "1.50"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler iap-viewer.handler/app
         :init iap-viewer.handler/init
         :destroy iap-viewer.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.3.1"]]}})
