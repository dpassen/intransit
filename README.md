# intransit

A Clojure library designed to aid in retrieving data from the CTA API

## Latest version
intransit is deployed to [Clojars](https://clojars.org)  
[![Clojars Project](http://clojars.org/intransit/latest-version.svg)](http://clojars.org/intransit)

## Usage
Note: I recommend using [Environ](https://github.com/weavejester/environ) so as not to store your api-key in your code

```clojure
(require '[intransit.core :as intransit])

(intransit/arrivals api-key)
(intransit/arrivals api-key :max-results 50)
(intransit/arrivals api-key :station-id station-id)
(intransit/arrivals api-key :station-id station-id :route route)
(intransit/arrivals api-key :stop-id stop-id)
(intransit/arrivals api-key :stop-id stop-id :route route)
(intransit/arrivals api-key :station-id station-id :stop-id stop-id)
;; or any other combination of these parameters

(intransit/follow api-key {:run-number run-number})

(intransit/positions api-key "g")
(intransit/positions api-key :G)
(intransit/positions api-key "g" :p "OrG" :RED)
;; or any other exotic capitalizations
```

Obviously, repeating api-key all the time can get annoying ...

Consider using the following:

```clojure
(require '[intransit.core :as intransit])

(def my-arrivals (partial intransit/arrivals api-key)
(my-arrivals :station-id station-id)
;; etc.
```

Or, using [Fidjet](https://github.com/aredington/fidjet):
```clojure
(ns intransit.configured
  (:require [intransit.core]
            [fidjet.core :as f]))

(f/remap-ns-with-arg intransit.core api-key)
```

```clojure
(require '[intransit.configured :as intransit])

(intransit/with-api-key api-key
  (intransit/arrivals :station-id station-id)
  (intransit/follow run-number)
  (intransit/positions "g" :p "OrG" :RED))
```

## License

Copyright © 2016 Derek Passen

Released under an MIT license.
