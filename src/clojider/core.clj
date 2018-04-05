(ns clojider.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojider.rc :as rc]
            [clojure.string :refer [split]]
            [clojider-gatling-highcharts-sthree-reporter.core :as s3]
            [clj-gatling.core :as gatling]
            [clj-time.core :as t]
            [clojider.aws :as aws])
  (:gen-class))

(def cli-options
  [["-r" "--region REGION" "Which region to use"]
   ["-b" "--bucket BUCKET" "Amazon S3 bucket name for Clojider result files"]
   ["-f" "--file FILE" "Path of uberjar"]
   ["-s" "--simulation SIMULATION" "Fully qualified name of simulation"]
   ["-e" "--custom-reporters CUSTOM-REPORTERS" "Comma separated list of fully qualified names of custom reporters to use"]
   ["-c" "--concurrency CONCURRENCY" "Concurrency"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-n" "--nodes NODES" "How many Lambda nodes to use"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-t" "--timeout TIMEOUT" "Rquest timeout in milliseconds"
    :default 5000
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--duration DURATION" "Duration in seconds"
    :default (t/seconds 1)
    :parse-fn #(t/seconds (Integer/parseInt %))]])

(defn- choose-reporters [reporters-str]
  (if reporters-str
    (map #(eval (read-string %)) (split reporters-str #","))
    [s3/reporter]))

(defn run-with-lambda [{:keys [simulation region bucket concurrency nodes duration timeout custom-reporters] :as options}]
  (println "Running simulation" simulation "with options" options)
  (rc/run-simulation (read-string simulation) {:region region
                                               :concurrency concurrency
                                               :node-count nodes
                                               :bucket-name bucket
                                               :reporters (choose-reporters custom-reporters)
                                               :timeout-in-ms timeout
                                               :duration duration}))

(defn run-using-local-machine [{:keys [simulation concurrency duration timeout custom-reporters] :as options}]
  (println "Running simulation" simulation "with options" options)
  (gatling/run simulation
               {:concurrency concurrency
                :root "tmp"
                :reporters (choose-reporters custom-reporters)
                :timeout-in-ms timeout
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

