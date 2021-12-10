(ns prometheus.core
  (:require [clojure.string :as str]))

(defn new-registry []
  (agent {}))


(defn send-ztx [ztx & params]
  (apply send (:box/metrics @ztx) params))

(defn set-counter
  ([ztx metric labels v]
   (send-ztx ztx assoc-in [:counter metric labels] v))
  ([ztx ns metric labels v]
   (send-ztx ztx assoc-in [ns :counter metric labels] v)))

(defn counter
  ([ztx metric labels]
   (send-ztx ztx update-in [:counter metric labels] (fn [x] (inc (or x 0)))))
  ([ztx ns metric labels]
   (send-ztx ztx update-in [ns :counter metric labels] (fn [x] (inc (or x 0))))))

(defn counter-add
  ([ztx metric labels inc]
   (send-ztx ztx update-in [:counter metric labels] (fn [x] (+ (or x 0) inc))))
  ([ztx ns metric labels inc]
   (send-ztx ztx update-in [ns :counter metric labels] (fn [x] (+ (or x 0) inc)))))

(defn gauge
  ([ztx metric labels v]
   (send-ztx ztx assoc-in [:gauge metric labels] v))
  ([ztx ns metric labels v]
   (send-ztx ztx assoc-in [ns :gauge metric labels] v)))

(defn get-metric
  ([ztx metric]
   (let [reg (:box/metrics @ztx)]
     (or
      (get-in @reg [:counter metric])
      (get-in @reg [:gauge metric])
      (get-in @reg [:histogram metric]))))
  ([ztx metric labels]
   (let [reg (:box/metrics @ztx)]
     (or
      (get-in @reg [:counter metric labels])
      (get-in @reg [:gauge metric labels])
      (get-in @reg [:histogram metric labels])))))

(defn register-metric-meta [ztx metric meta-name value]
  (let [reg (:box/metrics @ztx)]
    (when-not (get-in @reg [:meta metric meta-name])
      (send reg assoc-in [:meta metric meta-name] value))))

(defn register-metric-meta! [ztx metric meta-name value]
  (let [reg (:box/metrics @ztx)]
    (send reg assoc-in [:meta metric meta-name] value)))

(def default-buckets [0.005 0.01 0.025 0.05 0.1 0.25 0.5 1.0 2.5 5.0 10])


(defn find-bound [buckets val]
  (loop [[x & rst] buckets]
    (if (nil? x)
      nil
      (if (<= val x)
        x
        (recur rst)))))

(defn histogram
  ([ztx metric labels val]
   (when-let  [reg (some-> ztx deref :box/metrics)]
     (let [buckets (or (get-in @reg [:meta metric :buckets]) default-buckets)
           bnd (find-bound buckets val)]
       (send reg (fn [reg]
                   (-> reg
                       (cond-> bnd (update-in [:histogram metric labels :le bnd] (fn [x] (inc (or x 0)))))
                       (update-in [:histogram metric labels :count] (fn [x] (inc (or x 0))))
                       (update-in [:histogram metric labels :sum] (fn [x] (+ (or x 0) val)))))))))
  ([ztx ns metric labels val]
   (when-let  [reg (:box/metrics @ztx)]
     (let [buckets (or (get-in @reg [:meta metric :buckets]) default-buckets)
           bnd (find-bound buckets val)]
       (send reg (fn [reg]
                   (-> reg
                       (cond-> bnd (update-in [ns :histogram metric labels :le bnd] (fn [x] (inc (or x 0)))))
                       (update-in [ns :histogram metric labels :count] (fn [x] (inc (or x 0))))
                       (update-in [ns :histogram metric labels :sum] (fn [x] (+ (or x 0) val))))))))))

(defn summary [ztx metric labels val]
  ;;TODO: when needed
  )

(defn escape-help-comment [s]
  (-> s
      (str/replace "\n" "\\n")
      (str/replace "\\" "\\\\")))

(defn escape-label-value [s]
  (if s
    (-> s
        (str/replace "\\" "\\\\")
        (str/replace "\n" "\\n")
        (str/replace "\"" "\\\""))
    "\"\""))

(defn print-labels [^StringBuilder out lbls]
  (when-not (empty? lbls)
    (.append out "{")
    (doseq [[k v] lbls]
      (when v
        (.append out (name k))
        (.append out "=\"")
        (.append out (escape-label-value v))
        (.append out "\",")))
    (.append out "}")))


(defn print-line [^StringBuilder out nm lbls v]
  (.append out nm)
  (print-labels out lbls)
  (.append out " ")
  (.append out (str v))
  (.append out "\n"))

(defn help-line [^StringBuilder out registry m]
  (when-let [help-message (get-in registry [:meta m :help])]
    (.append out "# HELP ")
    (.append out (escape-help-comment help-message))
    (.append out "\n")))

(defn serialize [ztx & [ns]]
  (let [reg (:box/metrics @ztx)
        ^StringBuilder out (StringBuilder.)
        registry (if ns (get @reg ns) @reg)]
    (doseq [[m ms] (:counter registry)]
      (help-line out registry m)
      (print-line out (str "# TYPE " (name m)) {} "counter")

      (doseq [[lbls v] ms]
        (print-line out (name m) lbls v)))

    (doseq [[m ms] (:gauge registry)]
      (help-line out registry m)
      (print-line out (str "# TYPE " (name m)) {} "gauge")
      (doseq [[lbls v] ms]
        (print-line out (name m) lbls v)))

    (doseq [[m ms] (:histogram registry)]
      (help-line out registry m)
      (print-line out (str "# TYPE " (name m)) {} "histogram")
      (doseq [[lbls {sum :sum cnt :count les :le}] ms]
        (->> (or (get-in @reg [:meta m :buckets]) default-buckets)
             (reduce (fn [acc le]
                       (let [v (get les le 0)
                             v+ (+ v acc)]
                         (print-line out (str (name m) "_bucket") (assoc lbls :le (str le)) v+)
                         v+))
                     0))
        (print-line out (str (name m) "_bucket") (assoc lbls :le "+Inf") cnt)
        (print-line out (str (name m) "_count") lbls cnt)
        (print-line out (str (name m) "_sum") lbls sum)))
    (.toString out)))

;; exporters api
(defmulti eval-exporter (fn [ztx sym metrics & [reg-name]] sym))
