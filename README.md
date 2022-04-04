#  prometheus.clj

Pure clojure, zero-deps prometheus client library

```clj

(require '[prometheus.core :as sut])

(def registry (sut/new-registry))

(sut/reg-object registry :obj 1)

(sut/counter registry :my_counter {:box "aidbox"})

(sut/serialize registry)
;;   "# TYPE my_counter counter\nmy_counter{box=\"aidbox\",} 2\n"

(sut/counter-add registry :my_counter {:box "aidbox"} 5)
(sut/counter registry :minutes :slow_counter {:box "aidbox"})
(sut/serialize registry :minutes)
;; "# TYPE slow_counter counter\nslow_counter{box=\"aidbox\",} 1\n"

(sut/gauge registry :my_gauge {:box "aidbox"} 10.34)

(sut/get-metric registry :my_gauge {:box "aidbox"})
;; 10.34

(sut/register-metric-meta registry :my_histogram :buckets [0 5 30 50 100 200 500 1000 2000 3000 10000 20000])


(sut/histogram registry :my_histogram {:label "aidbox"} 35)
(sut/histogram registry :my_histogram {:label "aidbox"} 234)
(sut/histogram registry :my_histogram {:label "aidbox"} 230)
(sut/histogram registry :my_histogram {:label "aidbox"} 290)

(sut/serialize registry)

;; "# TYPE my_histogram histogram
;; my_histogram_bucket{label=\"aidbox\",le=\"0\",} 0
;; my_histogram_bucket{label=\"aidbox\",le=\"5\",} 0
;; my_histogram_bucket{label=\"aidbox\",le=\"30\",} 0
;; my_histogram_bucket{label=\"aidbox\",le=\"50\",} 1
;; my_histogram_bucket{label=\"aidbox\",le=\"100\",} 1
;; my_histogram_bucket{label=\"aidbox\",le=\"200\",} 1
```
