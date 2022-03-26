(ns build
  (:require
   [clojure.tools.build.api :as b]
   [org.corfield.build :as bb]))

(def lib 'org.passen/intransit)
(def version (format "0.3.%s" (b/git-count-revs nil)))

(defn jar
  "Build the JAR file."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/clean)
      (bb/jar)))

(defn install
  "Install the JAR locally."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy
  "Deploy the JAR to Clojars."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
