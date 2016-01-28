(ns clojider.lambda
  (:require [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.java.io :as io]
            [clojider.runner :refer [run-simulation]]
            [cheshire.core :refer [generate-stream parse-stream]]))

(deflambdafn clojider.LambdaFn
  [is os ctx]
  (let [input (parse-stream (io/reader is) true)
        output (io/writer os)]
    (println "Running simulation with config" input)
    (doseq [simulation-ns (:simulation-namespaces (:options input))]
      (load simulation-ns))
    (let [result (run-simulation
                   (eval (read-string (:simulation input)))
                   (:options input))]
      (println "Returning result" result)
      (generate-stream result output)
      (.flush output))))
