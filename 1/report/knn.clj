(ns knn.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.core.matrix :as m]
            [clojure.core.matrix.dataset :as ds])
  (:gen-class))

(def threads 8)

(defn load-data [filename]
  (let [data (with-open [reader (io/reader filename)]
               (doall (csv/read-csv reader)))]
    (ds/row-maps (ds/dataset (map keyword (first data)) (rest data)))))

(defn sinc
  [x] (if x (inc x) 1))

(def manifest {:elevation :num :aspect :num :slope :num :hor_dist_hyd :num
               :ver_dist_hyd :num :hor_dist_road :num :hillshade_9am :num
               :hillshade_noon :num :hillshade_3pm :num :hor_dist_fire :num
               :wild_1 :enum :wild_2 :enum :wild_3 :enum :wild_4 :enum
               :soil_1 :enum :soil_2 :enum :soil_3 :enum :soil_4 :enum
               :soil_5 :enum :soil_6 :enum :soil_7 :enum :soil_8 :enum
               :soil_9 :enum :soil_10 :enum :soil_11 :enum :soil_12 :enum
               :soil_13 :enum :soil_14 :enum :soil_15 :enum :soil_16 :enum
               :soil_17 :enum :soil_18 :enum :soil_19 :enum :soil_20 :enum
               :soil_21 :enum :soil_22 :enum :soil_23 :enum :soil_24 :enum
               :soil_25 :enum :soil_26 :enum :soil_27 :enum :soil_28 :enum
               :soil_29 :enum :soil_30 :enum :soil_31 :enum :soil_32 :enum
               :soil_33 :enum :soil_34 :enum :soil_35 :enum :soil_36 :enum
               :soil_37 :enum :soil_38 :enum :soil_39 :enum :soil_40 :enum
               :cov_type :class}) 

(defn get-by-val [hm val]
  (first (filter (comp #{val} hm) (keys hm))))

(defn distance [x y]
  (reduce + (map (fn [a b] (Math/pow (- (Integer/parseInt a) (Integer/parseInt b)) 2)) (vals x) (vals y))))

(defn class-qty [lst]
  (reduce (fn [acc {:keys [class]}]
            (update acc class sinc)) {} lst))

(defn max-class [qties]
  (apply max-key (fn [[k v]] v) qties))

(defn predict [manifest k training-ds y]
  (let [cls-field (get-by-val manifest :class)
        dis (fn [x]
              (let [to-remove
                    (clojure.set/difference (set (keys x)) (set (keys (dissoc manifest cls-field))))]
                (reduce (fn [acc p] (dissoc acc p)) x to-remove)))]
    (as-> y $
      (map (fn [row]
             (let [cls (cls-field row)]
               {:class cls
                :dist (distance (dis row) (dis $))})) training-ds)
      (->> $
           (sort-by :dist)
           (take k)
           class-qty max-class first))))

(defn test-accuracy [manifest test-ds predict-fn]
  (let [qty (count test-ds)
        class-field (get-by-val manifest :class)
        chunks (partition (int (/ qty threads)) test-ds)
        right-qty-split (pmap #(reduce (fn [acc row]
                                        (if (= (class-field row) (predict-fn row))
                                          (inc acc) acc)) 1 %) chunks)
        right-qty (reduce + right-qty-split)]
    (float (/ (* 100 right-qty) qty))))

(defn -main [& args]
  (let [train-data (load-data "cov_train_r.csv")
        test-data (load-data "cov_test_r.csv")
        predict-fn (partial predict manifest 15 train-data)]
    (println (test-accuracy manifest test-data predict-fn))))

;; > 69.72324
