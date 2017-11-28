(ns mlthree.core
  (:require [reagent.core :as r]
            [ajax.core :as j]))

(enable-console-print!)

(def host "http://localhost:5000")

(defonce app-state (r/atom {}))

(defn result-page []
  (fn []
    [:div
     [:h1 "Results"]
     [:pre (:results @app-state)]]))

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
  (swap! app-state assoc :fragment (if (:loaded response)
                                     result-page
                                     uploader-form)))

(j/GET (str host "/loaded") {:handler status-checked
                             :keywords? true
                             :response-format :json})

(r/render-component [container] (. js/document (getElementById "app")))
