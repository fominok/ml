(ns naive.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.core.matrix :as m])
  (:gen-class))

(defn deep-merge [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(defn load-data [filename]
  (with-open [reader (io/reader filename)]
    (doall (csv/read-csv reader))))

(defn -main [& args]
  (let [train-data (as-> (load-data "data_train.csv") $
                       (m/submatrix $ 0 70 2 17)
                       (map #(map read-string %) $)
                       (m/array $))])
  (println "Hello, World!"))

(def data (as-> (load-data "data_train.csv") $
              (m/submatrix $ 0 70 2 17)
              (map #(map read-string %) $)
              (m/array $)))

(m/submatrix (load-data "data_train.csv") 0 70 2 17)

(defn sinc [x]
  "Safe increment"
  (if x (inc x) 1))

(def quants
  (reduce (fn [bacc row]
            (let [cl (last row)
                  params (subvec row 0 (dec (count row)))]
              (update-in (reduce (fn [sacc [i p]] (update-in sacc [cl i p] sinc)) bacc
                                 (map-indexed (fn [i p] [i p]) params))
                         [cl :sum] sinc))) {} data))

(def class-prob (reduce (fn [acc [k v]] (assoc acc k (/ (:sum v) 70))) {} quants))

(def params-prob
  (reduce
   (fn [bacc [cls params]]
     (let [s (:sum params)]
       (reduce (fn [sacc [param values]]
                 (reduce
                  (fn [macc [p q]] (assoc-in macc [cls param p] (/ q s))) sacc values))
               bacc (dissoc params :sum)))){} quants))

(clojure.pprint/pprint (into (sorted-map) quants) (io/writer "pretty"))
(clojure.pprint/pprint (into (sorted-map) class-prob) (io/writer "pretty-prob-class"))
(clojure.pprint/pprint (into (sorted-map) params-prob) (io/writer "pretty-prob-params"))
