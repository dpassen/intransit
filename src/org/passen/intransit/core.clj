(ns org.passen.intransit.core
  (:require
   [cljc.java-time.local-date-time :as local-date-time]
   [cljc.java-time.zone-id :as zone-id]
   [cljc.java-time.zoned-date-time :as zoned-date-time]
   [clojure.data.json :as json]
   [clojure.set :as set]
   [clojure.string :as str]
   [java-http-clj.core :as http]
   [lambdaisland.uri :as uri]))

(def ^:const ^:private output-type "JSON")

(defn- parse-cta-timestamp
  [ts]
  (zoned-date-time/of
   (local-date-time/parse ts)
   (zone-id/of "America/Chicago")))

(defn- parse-common
  [{:keys [body]}]
  (-> body
      (json/read-str :key-fn keyword)
      :ctatt
      (dissoc :TimeStamp)
      (set/rename-keys {:errCd :error-code
                        :errNm :error-message
                        :tmst  :timestamp})
      (update :error-code #(Long/parseLong %))
      (update :timestamp parse-cta-timestamp)))

(defn- handle-arrival
  [arrival]
  (-> arrival
      (select-keys [:staNm :arrT :rt :destNm :rn])
      (set/rename-keys {:staNm  :station
                        :arrT   :arrival-time
                        :rt     :route
                        :destNm :destination
                        :rn     :run-number})
      (update :arrival-time parse-cta-timestamp)
      (update :run-number #(Long/parseLong %))
      (update :route keyword)))

(defn arrivals
  "Returns an object containing a list of arrival predictions
  for all platforms at a given train station"
  [api-key & {:keys [station-id stop-id route max-results]}]
  (let [base-url     (uri/uri "http://lapi.transitchicago.com/api/1.0/ttarrivals.aspx")
        query-params (uri/map->query-string
                      {:key        api-key
                       :mapid      station-id
                       :stpid      stop-id
                       :rt         (cond-> route (some? route) name)
                       :max        max-results
                       :outputType output-type})
        url          (assoc base-url :query query-params)]
    (-> (http/get (str url))
        parse-common
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
  (let [base-url     (uri/uri "http://lapi.transitchicago.com/api/1.0/ttfollow.aspx")
        query-params (uri/map->query-string
                      {:key        api-key
                       :runnumber  run-number
                       :outputType output-type})
        url          (assoc base-url :query query-params)]
    (-> (http/get (str url))
        parse-common
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
      (update :run-number #(Long/parseLong %))))

(defn- handle-route
  [{:keys [train] :as route}]
  (let [color     (-> "@name" keyword route str/capitalize keyword)
        positions (cond-> train (map? train) list)]
    [color (map handle-position positions)]))

(defn positions
  "Returns an object containing a list of in-service trains
  and basic info and their locations for one or more specified routes"
  [api-key routes]
  (let [base-url     (uri/uri "http://lapi.transitchicago.com/api/1.0/ttpositions.aspx")
        query-params (uri/map->query-string
                      {:key        api-key
                       :rt         (map name routes)
                       :outputType output-type})
        url          (assoc base-url :query query-params)]
    (-> (http/get (str url))
        parse-common
        (set/rename-keys {:route :routes})
        (update :routes (partial into {} (map handle-route))))))
