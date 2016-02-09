(ns clojider.examples)

(def ping-simulation
  [{:name "Test"
   :requests [{:name "ping" :http "http://clj-gatling-demo-server.herokuapp.com/ping"}]}])
