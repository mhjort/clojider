(ns clojider.lambda
  (:require [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.java.io :as io]
            [clj-time.core :refer [millis]]
            [clj-gatling.pipeline :as pipeline]
            [cheshire.core :refer [generate-stream parse-stream]]))

(deflambdafn clojider.LambdaFn
  [is os ctx]
  (let [input (parse-stream (io/reader is) true)
        output (io/writer os)]
    (println "Running simulation with config" input)
    (let [result (pipeline/simulation-runner (read-string (:simulation input))
                                             (-> (:options input)
                                                 (update :duration millis)
                                                 (assoc :results-dir (System/getProperty "java.io.tmpdir"))
                                                 (update :reporters #(map read-string %))))]
      (println "Returning result" result)
      (generate-stream result output)
      (.flush output))))
