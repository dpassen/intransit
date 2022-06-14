(ns org.passen.intransit.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [java-http-clj.core :as http]
   [matcher-combinators.test]
   [org.passen.intransit.core :as intransit]
   [vcr-clj.core :refer [with-cassette]])
  (:import
   (java.time ZonedDateTime)))

(def ^:private fake-api-key "abc123")

(defn- zoned-date-time?
  [zdt]
  (instance? ZonedDateTime zdt))

(def ^:private common-matcher
  {:error-code    int?
   :error-message nil?
   :timestamp     zoned-date-time?})

(deftest arrivals
  (with-cassette ::arrivals [{:var #'http/get}]
    (let [{:keys [arrivals] :as response} (intransit/arrivals fake-api-key :station-id 40800)]
      (is (match? common-matcher response))
      (is (match? {:arrivals seq?} response))
      (doseq [arrival arrivals]
        (is (match?
             {:station      string?
              :arrival-time zoned-date-time?
              :route        keyword?
              :destination  string?
              :run-number   int?}
             arrival))))))

(deftest follow
  (with-cassette ::follow [{:var #'http/get}]
    (let [{:keys [follows] :as response} (intransit/follow fake-api-key :run-number 423)]
      (is (match? common-matcher response))
      (is (match? {:follows seq?} response))
      (doseq [follow follows]
        (is (match?
             {:station      string?
              :arrival-time zoned-date-time?
              :destination  string?}
             follow))))))

(deftest positions
  (with-cassette ::positions [{:var #'http/get}]
    (let [{:keys [routes] :as response} (intransit/positions fake-api-key [:brn])]
      (is (match? common-matcher response))
      (is (match? {:routes map?} response))
      (doseq [route (:Brn routes)]
        (is (match?
             {:next-station string?
              :arrival-time zoned-date-time?
              :destination  string?
              :run-number   int?}
             route))))))
