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

(def manifest
  {:cov_type :class
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
   :type :enum})

(defn get-by-val [hm val]
  (first (filter (comp #{val} hm) (keys hm))))

(defn distance [x y]
  (reduce + (map (fn [a b] (Math/pow (- (Integer/parseInt a) (Integer/parseInt b)) 2)) (vals x) (vals y))))

(defn dis [x]
  (-> x
      (dissoc :cov_type)
      (dissoc :name)))

(distance (dis (first tr-d)) (dis (second tr-d)))

(def tr-d (load-data "data_train.csv"))
(def te-d (load-data "data_test.csv"))

#_(defn -main [& args]
  (let [train-data (load-data "data_train.csv")
        test-data (load-data "data_test.csv")
        qty (count train-data)
        smooth-fn (partial additive-smoothing qty)
        summarized (summarize manifest train-data)
        attr-probs (calc-probs manifest summarized)
        class-probs (calc-probs-class qty summarized)
        predict-fn (partial predict smooth-fn class-probs attr-probs)]
    (println (test-accuracy manifest test-data predict-fn))))
