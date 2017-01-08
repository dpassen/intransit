(defproject intransit "0.2.1"
  :description "Clojure library for retrieving data from the CTA API"
  :url "https://github.com/dpassen1/intransit"
  :license {:name "MIT"
            :url  "http://opensource.org/licenses/MIT"}
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :dependencies [[org.clojure/clojure   "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http              "3.4.1"]
                 [clojure.java-time     "0.2.2"]])
