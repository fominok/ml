(ns naive.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.core.matrix :as m]
            [clojure.core.matrix.dataset :as ds])
  (:gen-class))

(defn load-data [filename]
  (let [data (with-open [reader (io/reader filename)]
               (doall (csv/read-csv reader)))]
    (ds/row-maps (ds/dataset (map keyword (first data)) (rest data)))))

(defn sinc
  [x] (if x (inc x) 1))

#_(def manifest
  {:hair :enum
   :feathers :enum
   :eggs :enum
   :milk :enum
   :airborne :enum
   :aquatic :enum
   :predator :enum
   :toothed :enum
   :backbone :enum
   :breathes :enum
   :venomous :enum
   :fins :enum
   :legs :enum
   :tail :enum
   :domestic :enum
   :catsize :enum
   :type :class})

(def manifest
  {:elevation      :num
   :aspect         :num
   :slope          :num
   :hor_dist_hyd   :num
   :ver_dist_hyd   :num
   :hor_dist_road  :num
   :hillshade_9am  :num
   :hillshade_noon :num
   :hillshade_3pm  :num
   :hor_dist_fire  :num
   :wild_1         :enum
   :wild_2         :enum
   :wild_3         :enum
   :wild_4         :enum
   :soil_1         :enum
   :soil_2         :enum
   :soil_3         :enum
   :soil_4         :enum
   :soil_5         :enum
   :soil_6         :enum
   :soil_7         :enum
   :soil_8         :enum
   :soil_9         :enum
   :soil_10        :enum
   :soil_11        :enum
   :soil_12        :enum
   :soil_13        :enum
   :soil_14        :enum
   :soil_15        :enum
   :soil_16        :enum
   :soil_17        :enum
   :soil_18        :enum
   :soil_19        :enum
   :soil_20        :enum
   :soil_21        :enum
   :soil_22        :enum
   :soil_23        :enum
   :soil_24        :enum
   :soil_25        :enum
   :soil_26        :enum
   :soil_27        :enum
   :soil_28        :enum
   :soil_29        :enum
   :soil_30        :enum
   :soil_31        :enum
   :soil_32        :enum
   :soil_33        :enum
   :soil_34        :enum
   :soil_35        :enum
   :soil_36        :enum
   :soil_37        :enum
   :soil_38        :enum
   :soil_39        :enum
   :soil_40        :enum
   :cov_type       :class
   })

(defn get-by-val [hm val]
  (first (filter (comp #{val} hm) (keys hm))))

(defn dispatch [& args] (first args))

(defmulti summarize-fn #'dispatch)

(defmethod summarize-fn :enum
  [_ cls acc attr value]
  (update-in acc [cls attr value] sinc))

(defmethod summarize-fn :num
  [_ cls acc attr value]
  (update-in acc [cls attr :vec] conj value))

(defmulti prob-fn #'dispatch)

(defmethod prob-fn :enum
  [_ cls acc attr values-summary]
  (let [sum (reduce + (vals values-summary))
        probs (into (hash-map) (map (fn [[k v]] [k (/ v sum)]) values-summary))]
    (assoc-in acc [cls attr] (fn [x] (get probs x)))))

(defmethod prob-fn :num
  [_ cls acc attr values-summary]
  (let [values (map #(Integer/parseInt %) (:vec values-summary))
        n (count values)
        sum (reduce + values)
        mean (/ sum n)
        stdev-raw (/ (reduce + (map #(Math/pow (- % mean) 2) values)) n)
        stdev (if (= 0 stdev-raw) (+ stdev-raw 0.000001) stdev-raw)]
    (assoc-in acc [cls attr]
              (fn [x]
                (* (Math/exp (- (/ (Math/pow (- (Integer/parseInt x) mean) 2)
                                   (* 2 (Math/pow stdev 2)))))
                   (/ 1 (* stdev (Math/sqrt (* 2 Math/PI)))))))))

(defn summarize [manifest dataset]
  (let [class-field (get-by-val manifest :class)]
    (reduce (fn [outer-acc row]
              (reduce (fn [inner-acc [attr value]]
                        (if-let [attr-type (attr manifest)]
                          (summarize-fn attr-type (class-field row) inner-acc attr value)
                          inner-acc)) outer-acc (dissoc row class-field))) {} dataset)))

(defn calc-probs [manifest summarized]
  (reduce (fn [acc [cls attr-summary]]
            (reduce (fn [inner-acc [attr values-summary]]
                      (prob-fn (attr manifest) cls inner-acc attr values-summary)) acc attr-summary))
          {} summarized))

(defn calc-probs-class [count summarized]
  (reduce (fn [acc [k v]]
            (assoc acc k (/ (->> v first second vals (reduce +)) count))) {} summarized))

(defn additive-smoothing [class-count a] (+ (or a 0) (/ 1 class-count)))

(defn predict* [smooth cls probs-attrs row]
  (reduce (fn [acc [k v]]
            (if-let [f (get-in probs-attrs [cls k])]
              (* acc (smooth (f v)))
              (* acc (smooth 0)))) 1 row))

(defn predict [smooth probs-class probs-attrs row]
  (first
   (reduce (fn [acc [cls prob]]
             (let [next-prob (* (get probs-class cls) (predict* smooth cls probs-attrs row))]
               (max-key second acc [cls next-prob]))) [nil 0] probs-class)))

(defn test-accuracy [manifest test-ds predict-fn]
  (let [qty (count test-ds)
        class-field (get-by-val manifest :class)
        right-qty (reduce (fn [acc row]
                        (if (= (class-field row) (predict-fn row))
                          (inc acc) acc)) 1 test-ds)]
    (float (/ (* 100 right-qty) qty))))

(defn -main [& args]
  (let [train-data (load-data "cov_train_r.csv")
        test-data (load-data "cov_test_r.csv")
        qty (count train-data)
        smooth-fn (partial additive-smoothing qty)
        summarized (summarize manifest train-data)
        attr-probs (calc-probs manifest summarized)
        class-probs (calc-probs-class qty summarized)
        predict-fn (partial predict smooth-fn class-probs attr-probs)]
    (println (test-accuracy manifest test-data predict-fn))))

;; > 61.287094
