(ns prometheus.core-test
  (:require [prometheus.core :as sut]
            [clojure.test :as t]))

(t/deftest test-box-metrics

  (def reg-c (sut/new-registry))

  (sut/reg-object reg-c :obj 1)

  (t/is 
   (= (sut/get-object reg-c :obj) 1))

  (sut/counter reg-c :my_counter {:box "aidbox"})

  (Thread/sleep 50)
  (t/is (= (sut/get-metric reg-c :my_counter {:box "aidbox"}) 1))


  (sut/counter reg-c :my_counter {:box "aidbox"})

  (Thread/sleep 50)
  (t/is (= (sut/get-metric reg-c :my_counter {:box "aidbox"}) 2))

  (t/is (= (sut/serialize reg-c)
           "# TYPE my_counter counter\nmy_counter{box=\"aidbox\",} 2\n"))

  (sut/counter-add reg-c :my_counter {:box "aidbox"} 5)

  (Thread/sleep 50)
  (t/is (= (sut/get-metric reg-c :my_counter {:box "aidbox"}) 7))

  (sut/counter reg-c :minutes :slow_counter {:box "aidbox"})
  (Thread/sleep 50)
  (t/is (= (sut/serialize reg-c :minutes)
           "# TYPE slow_counter counter\nslow_counter{box=\"aidbox\",} 1\n"))

  (def reg-g (sut/new-registry))
  (sut/gauge reg-g :my_gauge {:box "aidbox"} 10.34)

  (Thread/sleep 50)
  (t/is (= (sut/get-metric reg-g :my_gauge {:box "aidbox"}) 10.34))

  (sut/gauge reg-g :my_gauge {:box "aidbox"} 7.85)

  (Thread/sleep 50)
  (t/is (= (sut/get-metric reg-g :my_gauge {:box "aidbox"}) 7.85))

  (Thread/sleep 50)
  (t/is (= (sut/serialize reg-g)
           "# TYPE my_gauge gauge\nmy_gauge{box=\"aidbox\",} 7.85\n"))

  (def reg-h (sut/new-registry))
  (sut/register-metric-meta reg-h :my_histogram :buckets [0 5 30 50 100 200 500 1000 2000 3000 10000 20000])


  (Thread/sleep 100)
  (sut/histogram reg-h :my_histogram {:label "aidbox"} 35)
  (sut/histogram reg-h :my_histogram {:label "aidbox"} 234)
  (sut/histogram reg-h :my_histogram {:label "aidbox"} 230)
  (sut/histogram reg-h :my_histogram {:label "aidbox"} 290)
  (sut/histogram reg-h :my_histogram {:label "aidbox"} 12000)
  (sut/histogram reg-h :my_histogram {:label "aidbox"} 100000)
  (sut/histogram reg-h :my_histogram {:label "other"} 100000)
  (sut/histogram reg-h :my_histogram {:label "other"} 12000)

  (Thread/sleep 100)

  (t/is (= (sut/serialize reg-h)
           "# TYPE my_histogram histogram
my_histogram_bucket{label=\"aidbox\",le=\"0\",} 0
my_histogram_bucket{label=\"aidbox\",le=\"5\",} 0
my_histogram_bucket{label=\"aidbox\",le=\"30\",} 0
my_histogram_bucket{label=\"aidbox\",le=\"50\",} 1
my_histogram_bucket{label=\"aidbox\",le=\"100\",} 1
my_histogram_bucket{label=\"aidbox\",le=\"200\",} 1
my_histogram_bucket{label=\"aidbox\",le=\"500\",} 4
my_histogram_bucket{label=\"aidbox\",le=\"1000\",} 4
my_histogram_bucket{label=\"aidbox\",le=\"2000\",} 4
my_histogram_bucket{label=\"aidbox\",le=\"3000\",} 4
my_histogram_bucket{label=\"aidbox\",le=\"10000\",} 4
my_histogram_bucket{label=\"aidbox\",le=\"20000\",} 5
my_histogram_bucket{label=\"aidbox\",le=\"+Inf\",} 6
my_histogram_count{label=\"aidbox\",} 6
my_histogram_sum{label=\"aidbox\",} 112789
my_histogram_bucket{label=\"other\",le=\"0\",} 0
my_histogram_bucket{label=\"other\",le=\"5\",} 0
my_histogram_bucket{label=\"other\",le=\"30\",} 0
my_histogram_bucket{label=\"other\",le=\"50\",} 0
my_histogram_bucket{label=\"other\",le=\"100\",} 0
my_histogram_bucket{label=\"other\",le=\"200\",} 0
my_histogram_bucket{label=\"other\",le=\"500\",} 0
my_histogram_bucket{label=\"other\",le=\"1000\",} 0
my_histogram_bucket{label=\"other\",le=\"2000\",} 0
my_histogram_bucket{label=\"other\",le=\"3000\",} 0
my_histogram_bucket{label=\"other\",le=\"10000\",} 0
my_histogram_bucket{label=\"other\",le=\"20000\",} 1
my_histogram_bucket{label=\"other\",le=\"+Inf\",} 2
my_histogram_count{label=\"other\",} 2
my_histogram_sum{label=\"other\",} 112000
"))

  )
