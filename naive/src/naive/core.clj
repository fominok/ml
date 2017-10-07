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

(defn sinc [x] (if x (inc x) 1))

(def manifest
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

(defn get-by-val [hm val]
  (first (filter (comp #{val} hm) (keys hm))))

(defn dispatch [& args] (first args))

(defmulti summarize-fn #'dispatch)

(defmethod summarize-fn :enum
  [attr-type cls acc attr value]
  (update-in acc [cls attr value] sinc))

(defmulti prob-fn #'dispatch)

(defmethod prob-fn :enum
  [attr-type cls acc attr values-summary]
  (let [sum (reduce + (vals values-summary))
        probs (into (hash-map) (map (fn [[k v]] [k (/ v sum)]) values-summary))]
    (assoc-in acc [cls attr] (fn [x] (get probs x)))))

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

(defn test-accuracy [test-ds predict-fn]
  (let [qty (count test-data)
        right-qty (reduce (fn [acc row]
                        (if (= (:type row) (predict-fn row))
                          (inc acc) acc)) 1 test-ds)]
    (float (/ (* 100 right-qty) qty))))

(defn -main [& args]
  (let [train-data (load-data "data_train.csv")
        test-data (load-data "data_test.csv")
        qty (count train-data)
        smooth-fn (partial additive-smoothing qty)
        summarized (summarize manifest train-data)
        attr-probs (calc-probs manifest summarized)
        class-probs (calc-probs-class qty summarized)
        predict-fn (partial predict smooth-fn class-probs attr-probs)]
    (println (test-accuracy test-data predict-fn))))

(-main)
