(ns intransit.core
  (:require [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zxml]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn- parse-cta-timestamp [ts]
  (f/parse
    (f/with-zone
      (f/formatter "yyyyMMdd HH:mm:ss")
      (t/time-zone-for-id "America/Chicago"))
    ts))

(defn- parse-common-info [response]
  (let [error-code (zxml/xml1-> response :errCd zxml/text)
        error-message (zxml/xml1-> response :errNm zxml/text)
        timestamp (zxml/xml1-> response :tmst zxml/text)]
    {:error-code (Long/parseLong error-code)
     :error-message error-message
     :timestamp (parse-cta-timestamp timestamp)}))

(defn- handle-arrival [arrival]
  (let [station (zxml/xml1-> arrival :staNm zxml/text)
        arrival-time (zxml/xml1-> arrival :arrT zxml/text)
        route (zxml/xml1-> arrival :rt zxml/text)
        destination (zxml/xml1-> arrival :destNm zxml/text)]
    {:station station
     :arrival-time (parse-cta-timestamp arrival-time)
     :route route
     :destination destination}))

(defn- handle-arrivals [response]
  (let [common (parse-common-info response)
        arrivals (zxml/xml-> response :eta)]
    (merge
      {:arrivals (into [] (map handle-arrival arrivals))}
      common)))

(defn arrivals
  [api-key & {:keys [station-id stop-id route]
              :or {station-id "" stop-id "" route ""}}]
  (let [base-url "http://lapi.transitchicago.com/api/1.0/ttarrivals.aspx?key=%s&mapid=%s&stpid=%s&rt=%s"
        url (format base-url api-key station-id stop-id (name route))
        response (zip/xml-zip (xml/parse url))]
    (handle-arrivals response)))

(defn- handle-follow [follow]
  (let [station (zxml/xml1-> follow :staNm zxml/text)
        arrival-time (zxml/xml1-> follow :arrT zxml/text)
        destination (zxml/xml1-> follow :destNm zxml/text)]
    {:station station
     :arrival-time (parse-cta-timestamp arrival-time)
     :destination destination}))

(defn- handle-follows [response]
  (let [common (parse-common-info response)
        follows (zxml/xml-> response :eta)]
    (merge
      {:follows (into [] (map handle-follow follows))}
      common)))

(defn follow
  [api-key run-number]
  (let [base-url "http://lapi.transitchicago.com/api/1.0/ttfollow.aspx?key=%s&runnumber=%s"
        url (format base-url api-key run-number)
        response (zip/xml-zip (xml/parse url))]
    (handle-follows response)))

(defn- handle-position [position]
  (let [next-station (zxml/xml1-> position :nextStaNm zxml/text)
        arrival-time (zxml/xml1-> position :arrT zxml/text)]
    {:next-station next-station
     :arrival-time (parse-cta-timestamp arrival-time)}))

(defn- handle-route [route]
  (let [positions (zxml/xml-> route :train)]
    {(keyword (str/capitalize (zxml/attr route :name)))
     (into [] (map handle-position positions))}))

(defn- handle-positions [response]
  (let [common (parse-common-info response)
        routes (zxml/xml-> response :route)]
    (merge
      {:routes (into {} (map handle-route routes))}
      common)))

(defn positions
  [api-key & routes]
  (let [base-url "http://lapi.transitchicago.com/api/1.0/ttpositions.aspx?key=%s&rt="
        authorized-url (format base-url api-key)
        params (apply str (interpose "&rt=" (map name routes)))
        url (str authorized-url params)
        response (zip/xml-zip (xml/parse url))]
    (handle-positions response)))
