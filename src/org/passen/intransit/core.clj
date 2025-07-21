(ns org.passen.intransit.core
  "Intransit is a library for retrieving data from the CTA API."
  (:require
   [babashka.http-client :as http]
   [babashka.json :as json]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.time LocalDateTime ZoneId ZonedDateTime)))

(def ^:const ^:private output-type "JSON")

(defn- parse-cta-timestamp
  [ts]
  (ZonedDateTime/of
   (LocalDateTime/parse ts)
   (ZoneId/of "America/Chicago")))

(defn- parse-common
  [{:keys [body]}]
  (-> body
      (json/read-str)
      (get :ctatt)
      (dissoc :TimeStamp)
      (set/rename-keys {:errCd :error-code
                        :errNm :error-message
                        :tmst  :timestamp})
      (update :error-code parse-long)
      (update :timestamp parse-cta-timestamp)))

(defn- parse-flag
  [flag]
  (not (zero? (parse-long flag))))

(defn- handle-arrival
  [arrival]
  (-> arrival
      (select-keys [:staNm :arrT :rt :destNm :rn :isDly :isFlt :isApp :isSch])
      (set/rename-keys {:staNm  :station
                        :arrT   :arrival-time
                        :rt     :route
                        :destNm :destination
                        :rn     :run-number
                        :isDly  :delayed?
                        :isFlt  :fault?
                        :isApp  :approaching?
                        :isSch  :scheduled?})
      (update :arrival-time parse-cta-timestamp)
      (update :run-number parse-long)
      (update :route keyword)
      (update :delayed? parse-flag)
      (update :fault? parse-flag)
      (update :approaching? parse-flag)
      (update :scheduled? parse-flag)))

(defn arrivals
  "Returns an object containing a list of arrival predictions
  for all platforms at a given train station"
  [api-key & {:keys [station-id stop-id route max-results]}]
  (let [url "http://lapi.transitchicago.com/api/1.0/ttarrivals.aspx"]
    (-> (http/get url {:query-params
                       {:key        api-key
                        :mapid      station-id
                        :stpid      stop-id
                        :rt         (cond-> route (some? route) name)
                        :max        max-results
                        :outputType output-type}})
        (parse-common)
        (set/rename-keys {:eta :arrivals})
        (update :arrivals (partial map handle-arrival)))))

(defn- handle-follow
  [follow]
  (-> follow
      (select-keys [:staNm :arrT :destNm])
      (set/rename-keys {:staNm  :station
                        :arrT   :arrival-time
                        :destNm :destination})
      (update :arrival-time parse-cta-timestamp)))

(defn follow
  "Returns an object containing a list of arrival predictions for a given train
  at all subsequent stations for which that train is estimated to arrive"
  [api-key & {:keys [run-number]}]
  (let [url "http://lapi.transitchicago.com/api/1.0/ttfollow.aspx"]
    (-> (http/get url {:query-params
                       {:key        api-key
                        :runnumber  run-number
                        :outputType output-type}})
        (parse-common)
        (dissoc :position)
        (set/rename-keys {:eta :follows})
        (update :follows (partial map handle-follow)))))

(defn- handle-position
  [position]
  (-> position
      (select-keys [:nextStaNm :arrT :destNm :rn])
      (set/rename-keys {:nextStaNm :next-station
                        :arrT      :arrival-time
                        :destNm    :destination
                        :rn        :run-number})
      (update :arrival-time parse-cta-timestamp)
      (update :run-number parse-long)))

(defn- handle-route
  [{:keys [train] :as route}]
  (let [route-name (keyword "@name")
        color      (-> route (get route-name) (str/capitalize) (keyword))
        positions  (cond-> train (map? train) list)]
    [color (map handle-position positions)]))

(defn positions
  "Returns an object containing a list of in-service trains
  and basic info and their locations for one or more specified routes"
  [api-key routes]
  (let [url "http://lapi.transitchicago.com/api/1.0/ttpositions.aspx"]
    (-> (http/get url {:query-params
                       {:key        api-key
                        :rt         (map name routes)
                        :outputType output-type}})
        (parse-common)
        (set/rename-keys {:route :routes})
        (update :routes (partial into {} (map handle-route))))))
