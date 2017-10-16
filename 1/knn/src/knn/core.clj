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

(def manifest
  {
   :hair :enum
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

(defn get-by-val [hm val]
  (first (filter (comp #{val} hm) (keys hm))))

(defn distance [x y]
  (reduce + (map (fn [a b] (Math/pow (- (Integer/parseInt a) (Integer/parseInt b)) 2)) (vals x) (vals y))))

(defn dis [x]
  (-> x
      (dissoc :type)
      (dissoc :name)))

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
        chunks (partition (/ qty threads) test-ds)
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

;; 93.59786
