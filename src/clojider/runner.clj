(ns clojider.runner
  (:require [clj-gatling.simulation :as simulation]
            [clojure-csv.core :refer [write-csv]]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-gatling.report :as report]
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

(defn- gatling-csv-writer [timestamp start-time bucket folder node-id simulation idx results]
  (println "Got results" results)
  (let [result-lines (report/gatling-csv-lines start-time simulation idx results)
        csv (write-csv result-lines :delimiter "\t" :end-of-line "\n")]
    (store-to-s3 bucket (str folder "/simulation-" timestamp "-" node-id "-" idx ".log") csv)))

(defn run-simulation [simulation {:keys [node-id users context duration] :as options}]
  (println "Running simulation in node" node-id)
  (reset! stored-objects [])
  (let [custom-formatter (f/formatter "yyyyMMddHHmmss")
        start-time (LocalDateTime.)
        timestamp (f/unparse custom-formatter (t/now))
        step-timeout (or (:timeout-in-ms options) 5000)
        result (simulation/run simulation {:users users
                                           :duration (t/millis duration)
                                           :context context
                                           :timeout-in-ms step-timeout})]
    (report/create-result-lines simulation
                                buffer-size
                                result
                                (partial gatling-csv-writer
                                         timestamp
                                         start-time
                                         (:bucket-name options)
                                         (:folder-name options)
                                         node-id))
    {:results @stored-objects}))
