(ns org.passen.intransit.core
  (:require
   [clj-http.client :as http]
   [clojure.set :as set]
   [clojure.string :as str]
   [java-time]
   [jsonista.core :as jsonista]))

(def ^:const ^:private output-type "JSON")

(def json-mapper
  (jsonista/object-mapper
   {:decode-key-fn keyword}))

(defn- parse-cta-timestamp
  [ts]
  (java-time/zoned-date-time
   (java-time/local-date-time "yyyy-MM-dd'T'HH:mm:ss" ts)
   "America/Chicago"))

(defn- parse-common
  [{:keys [body]}]
  (-> body
      (jsonista/read-value json-mapper)
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
  [api-key {:keys [station-id stop-id route max-results]}]
  (-> "http://lapi.transitchicago.com/api/1.0/ttarrivals.aspx"
      (http/get {:query-params {:key        api-key
                                :mapid      station-id
                                :stpid      stop-id
                                :rt         (cond-> route (some? route) name)
                                :max        max-results
                                :outputType output-type}})
      parse-common
      (set/rename-keys {:eta :arrivals})
      (update :arrivals (partial map handle-arrival))))

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
  [api-key {:keys [run-number]}]
  (-> "http://lapi.transitchicago.com/api/1.0/ttfollow.aspx"
      (http/get {:query-params {:key        api-key
                                :runnumber  run-number
                                :outputType output-type}})
      parse-common
      (dissoc :position)
      (set/rename-keys {:eta :follows})
      (update :follows (partial map handle-follow))))

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
  (-> "http://lapi.transitchicago.com/api/1.0/ttpositions.aspx"
      (http/get {:query-params {:key        api-key
                                :rt         (map name routes)
                                :outputType output-type}})
      parse-common
      (set/rename-keys {:route :routes})
      (update :routes (partial into {} (map handle-route)))))
