(ns clojider.examples
  (:require [org.httpkit.client :as http]))

(def base-url "http://clj-gatling-demo-server.herokuapp.com")

(defn- http-get [url callback _]
  (let [check-status (fn [{:keys [status]}] (callback (= 200 status)))]
    (http/get (str base-url url) {} check-status)))

(defn- http-get-with-ids [url ids callback {:keys [user-id]}]
  (let [check-status (fn [{:keys [status]}] (callback (= 200 status)))
        id (nth ids user-id)]
    (http/get (str base-url url id) {} check-status)))

(def ping
  (partial http-get "/ping"))

(def ping-simulation
  [{:name "Ping scenario"
    :requests [{:name "Ping Endpoint" :fn ping}]}])

(def article-read
  (partial http-get-with-ids "/metrics/article/read/" (cycle (range 100 200))))

(def program-start
  (partial http-get-with-ids "/metrics/program/start/" (cycle (range 200 400))))

(def metrics-simulation
  [{:name "Article read scenario"
    :weight 2
    :requests [{:name "Article read request"
                :fn article-read}]}
   {:name "Program start scenario"
    :weight 1
    :requests [{:name "Program start request"
                :fn program-start}]}])
