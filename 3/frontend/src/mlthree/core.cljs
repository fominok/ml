(ns mlthree.core
  (:require [reagent.core :as r]
            [d3]
            [garden.core :as garden]
            [goog.string :as gstring]
            [goog.string.format]
            [ajax.core :as j]))

(enable-console-print!)

(def host "http://localhost:5000")

(defonce app-state (r/atom {}))

(defn d3-scatter [layers]
  (let [state (r/atom {})]
    (r/create-class
     {
      :component-did-mount
      (fn [this]
        (let [node (:container @state)
              margin {:top 20 :right 15 :bottom 60 :left 60}
              width (- 960 (:left margin) (:right margin))
              height (- 500 (:top margin) (:bottom margin))
              all-x (mapcat :x layers)
              all-y (mapcat :y layers)
              x (.. js/d3 scaleLinear (domain #js [(.min js/d3 (clj->js all-x)), (.max js/d3 (clj->js all-x))]) (range #js [0 width]))
              y (.. js/d3 scaleLinear (domain #js [(.min js/d3 (clj->js all-y)), (.max js/d3 (clj->js all-y))]) (range #js [height 0]))
              chart (.. js/d3 (select node) (append "svg:svg")
                        (attr "width" (+ width (:right margin) (:left margin)))
                        (attr "height" (+ height (:top margin) (:bottom margin)))
                        (attr "class" "chart"))

              main (.. chart (append "g")
                       (attr "transform" (str "translate(" (:left margin) "," (:top margin) ")"))
                       (attr "width" width)
                       (attr "height" height)
                       (attr "class" "main"))

              x-axis (.axisBottom js/d3 x)
              _ (.. main (append "g")
                    (attr "transform" (str "translate(0," height ")"))
                    (attr "class" "main axis date")
                    (call x-axis))
              y-axis (.axisLeft js/d3 y)
              _ (.. main (append "g")
                    (attr "transform" "translate(0,0)")
                    (attr "class" "main axis date")
                    (call y-axis))
              g (.. main (append "svg:g"))
              _ (doall (for [l layers]
                   (.. g (selectAll "scatter-dots")
                       (data (clj->js (:y l)))
                       enter
                       (append "svg:circle")
                       (attr "cy" (fn [d] (y d)))
                       (attr "cx" (fn [d i] (x (nth (:x l) i))))
                       (attr "r" 5)
                       (style "opacity" 0.6)
                       (style "fill" (:color l)))))

              legend-background (.. g (append "rect")
                                    (attr "x" 10)
                                    (attr "y" 0)
                                    (attr "width" 150)
                                    (attr "height" 70)
                                    (style "stroke" "black")
                                    (style "fill" "white")
                                    )
              legend (.. g (selectAll "g.legend")
                         (data (clj->js layers))
                         enter
                         (append "svg:g")
                         (attr "transform" (fn [d i] (str "translate(20," (+ 10 (* i 20)) ")" ))))
              _ (.. legend (append "text")
                    (attr "x" "8px")
                    (attr "y" "3px")
                    (style "font-size" "12px")
                    (text (fn [d i] (str "Class " i (when-let [p (.-p d)] (gstring/format ", p = %.4f" p)))))
                    )
              _ (.. legend (append "svg:circle")
                    (style "fill" (fn [d i] (js/console.log d i (.-color d)) (.-color d)))
                    (attr "r" 3))]
          nil
          ))

      :reagent-render
      (fn [& layers]
        [:div.d3-cont {:ref (fn [this] (swap! state assoc :container this))}])})))

(defn colorize [layers]
  (mapv #(assoc %1 :color %2) layers ["darkolivegreen" "darkorange" "darkorchid" "darkred" "darksalmon" "darkseagreen" "darkslateblue"]))

(defn result-page []
  (fn []
    [:div
     [:h1 "Results"]
     [:pre (get-in @app-state [:results :report])]
     [d3-scatter (colorize (get-in @app-state [:results :corr_scatter]))]
     [d3-scatter (colorize (get-in @app-state [:results :lda_scatter]))]]))

(defn upload-success [response]
  (swap! app-state assoc :fragment result-page
         :results (:results response)
         :errors nil))

(defn upload-failed [response]
  (swap! app-state assoc :errors "Upload failed."))

(defn uploader-form []
  (r/create-class
   {:component-did-mount
    (fn [this]
      (swap! app-state assoc :form (r/dom-node (.getElementById js/document "file-form"))))

    :reagent-render
    (fn []
      [:div#uploader-form
       [:h1 "Dataset upload"]
       [:p "Required format: csv with header; class column is the last one."]
       [:p (:errors @app-state)]
       [:form#file-form
        [:input {:type :text
                 :name "class_field"
                 :placeholder "Name of class column"}]
        [:input.csv {:name :file
                     :type :file}]]
       (when-let [form (:form @app-state)]
         (println (clj->js (js/FormData. form)))
         [:button {:on-click #(j/POST (str host "/upload") {:handler upload-success
                                                            :error-handler upload-failed
                                                            :response-format :json
                                                            :keywords? true
                                                            :body (js/FormData. form)})}
          "Upload"])])}))


(defn container []
  (fn []
    [:div.container
     (when-let [fragment (:fragment @app-state)]
       [fragment])]))

(defn status-checked [response]
  (if-let [results (:results response)]
    (swap! app-state assoc
           :fragment result-page
           :results results)
    (swap! app-state assoc :fragment uploader-form)))

(j/GET (str host "/loaded") {:handler status-checked
                             :keywords? true
                             :response-format :json})

(r/render-component [container] (. js/document (getElementById "app")))
