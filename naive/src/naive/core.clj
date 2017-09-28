(ns naive.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.core.matrix :as m])
  (:gen-class))

(defn load-data [filename]
  (with-open [reader (io/reader filename)]
    (doall (csv/read-csv reader))))

(defn sinc [x] (if x (inc x) 1))

(defn split-row [row]
  [(subvec row 0 (dec (count row))) (last row)])

(defn compute-qty [data]
  (reduce (fn [bacc row]
            (let [[params cl] (split-row row)]
              (update-in (reduce (fn [sacc [i p]] (update-in sacc [cl i p] sinc)) bacc
                                 (map-indexed (fn [i p] [i p]) params))
                         [cl :sum] sinc))) {} data))

(defn additive-smoothing [class-count a] (+ a (/ 1 class-count))) 

(defn compute-class-prob [quants]
  (reduce (fn [acc [k v]] (assoc acc k (/ (:sum v) 70))) {} quants))

(def test-animal-cropped [0,0,1,0,0,1,1,1,1,0,0,1,0,1,0,0])
(def test-animal-full [0,0,1,0,0,1,1,1,1,0,0,1,0,1,0,0, 4])

(defn compute-param-vals-prob [quants]
  (reduce
   (fn [bacc [cls params]]
     (let [s (:sum params)]
       (reduce (fn [sacc [param values]]
                 (reduce
                  (fn [macc [p q]] (assoc-in macc [cls param p] (/ q s))) sacc values))
               bacc (dissoc params :sum)))){} quants))

(defn predict [params-prob class-prob quants obj cls]
  (let [vals-probs-cls (params-prob cls)
        add-smth (partial additive-smoothing (get-in quants [cls :sum]))
        computed-val-prob
        (reduce (fn [acc [param values]]
                  (* acc (add-smth (or (values (nth obj param)) 0)))) 1 vals-probs-cls)]
    (* (class-prob cls) computed-val-prob)))

(defn get-class [predict-fn classes obj]
  (second
   (apply max-key first (map (fn [x] [(predict-fn obj x) x]) classes))))

(defn check [predict-fn row]
  (let [[params cls] (split-row row)]
    (= (predict-fn params) cls)))

(defn -main [& args]
  (let [train-data (as-> (load-data "data_train.csv") $
                       (m/submatrix $ 0 70 2 17)
                       (map #(map read-string %) $)
                       (m/array $))
        test-data (as-> (load-data "data_test.csv") $
                        (m/submatrix $ 0 31 2 17)
                        (map #(map read-string %) $)
                        (m/array $))
        test-count (count test-data)
        quants (compute-qty train-data)
        class-prob (compute-class-prob quants)
        param-vals-prob (compute-param-vals-prob quants)
        local-predict (partial predict param-vals-prob class-prob quants)
        local-get-class (partial get-class local-predict (keys class-prob))
        local-check (partial check local-get-class)]
    (float (* 100 (/ (count (filter local-check test-data)) test-count)))))
