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

(def manifest {:cov_type :class})

(defn get-by-val [hm val]
  (first (filter (comp #{val} hm) (keys hm))))

(defn -main [& args]
  (let [train-data (load-data "cov_train.csv")
        test-data (load-data "cov_test.csv")
        qty (count train-data)
        smooth-fn (partial additive-smoothing qty)
        summarized (summarize manifest train-data)
        attr-probs (calc-probs manifest summarized)
        class-probs (calc-probs-class qty summarized)
        predict-fn (partial predict smooth-fn class-probs attr-probs)]
    (println (test-accuracy manifest test-data predict-fn))))

;; > 59.624565
