(ns clojider.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojider.rc :as rc]
            [clojider.utils :refer [symbol-namespace]]
            [clj-gatling.core :as gatling]
            [clj-time.core :as t]
            [clojider.aws :as aws])
  (:gen-class))

(def cli-options
  [["-r" "--region REGION" "Which region to use"]
   ["-b" "--bucket BUCKET" "Amazon S3 bucket name for Clojider result files"]
   ["-f" "--file FILE" "Path of uberjar"]
   ["-s" "--simulation SIMULATION" "Fully qualified name of simulation"]
   ["-c" "--concurrency CONCURRENCY" "Concurrency"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-n" "--nodes NODES" "How many Lambda nodes to use"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--duration DURATION" "Duration in seconds"
    :default (t/seconds 1)
    :parse-fn #(t/seconds (Integer/parseInt %))]])

(defn run-with-lambda [{:keys [simulation region bucket concurrency nodes duration]}]
  (rc/run-simulation (symbol simulation) {:region region
                                          :concurrency concurrency
                                          :node-count nodes
                                          :bucket-name bucket
                                          :duration duration}))


(defn run-using-local-machine [{:keys [simulation concurrency duration]}]
  (load (symbol-namespace simulation))
  (gatling/run-simulation (eval (read-string simulation))
                          concurrency
                          {:root "tmp"
                           :duration duration}))

(def cmds
  {"install" aws/install-lambda
   "uninstall" aws/uninstall-lambda
   "update" aws/update-lambda
   "load-lambda" run-with-lambda
   "load-local" run-using-local-machine})

(defn -main [& args]
  (let [options (parse-opts args cli-options)
        cmd-str (first (:arguments options))]
    (if-let [cmd (get cmds cmd-str)]
      (cmd (:options options))
      (println "Unknown command:" cmd-str))))

