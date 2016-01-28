(ns clojider.runner
  (:require [clj-gatling.simulation :as simulation]
            [clojure-csv.core :as csv]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-gatling.report :as report]
            [clj-gatling.simulation-util :refer [choose-runner
                                                 weighted-scenarios]]
            [clojure.core.async :refer [<!!]])
  (:import [org.joda.time LocalDateTime]
           [java.io File]
           [com.amazonaws.services.s3 AmazonS3Client]))

(defonce client (AmazonS3Client.))

(def buffer-size 20000)
(def stored-objects (atom []))

(defn- write-to-temp-file [csv]
  (let [temp-file (File/createTempFile "simulation" nil nil)]
    (.deleteOnExit temp-file)
    (with-open [wrtr (io/writer temp-file)]
      (.write wrtr csv))
    temp-file))

(defn- store-to-s3 [bucket s3-object-key csv]
  (let [temp-file (write-to-temp-file csv)]
    (swap! stored-objects conj s3-object-key)
    (.putObject client bucket s3-object-key temp-file)))

(defn- gatling-csv-writer [bucket folder node-id idx result-lines]
  (let [csv (csv/write-csv result-lines :delimiter "\t" :end-of-line "\n")]
    (store-to-s3 bucket (str folder "/simulation-" node-id "-" idx ".log") csv)))

(defn run-simulation [node-id simulation users & [options]]
  (println "Running simulation in node" node-id)
  (reset! stored-objects [])
  (let [start-time (LocalDateTime.)
        step-timeout (or (:timeout-in-ms options) 5000)
        result (simulation/run-scenarios {:runner (choose-runner simulation (count users)
                                                                 (update options :duration t/millis))
                                          :timeout step-timeout}
                                         (weighted-scenarios users simulation))]
    (report/create-result-lines start-time
                                buffer-size
                                result
                                (partial gatling-csv-writer (:bucket-name options) (:folder-name options) node-id))
    {:results @stored-objects}))
