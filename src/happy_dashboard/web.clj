(ns happy-dashboard.core
  (:use [compojure.core :only (defroutes GET)]
        ring.util.response
        org.httpkit.server)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [cheshire.core :refer :all]))

(def clients (atom {}))

(defn ws
  [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))))

(future (loop []
          (doseq [client @clients]
            (send! (key client) (generate-string
                                 {:happiness (rand 10)})
                   false))
          (Thread/sleep 5000)
          (recur)))

(defroutes routes
  (GET "/happiness" [] ws))

(def application (-> (handler/site routes)
                     reload/wrap-reload))

(defn -main [& args]
  (let [port (Integer/parseInt 
               (or (System/getenv "PORT") "8080"))]
    (run-server application {:port port :join? false})))