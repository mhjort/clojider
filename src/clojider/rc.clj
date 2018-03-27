(ns clojider.rc
 (:require [clojider.aws :refer [aws-credentials]]
           [clojure.java.io :as io]
           [clj-time.core :as t]
           [clj-gatling.core :as gatling]
           [clojider-gatling-highcharts-sthree-reporter.core :as s3]
           [cheshire.core :refer [generate-string parse-stream]])
  (:import [com.amazonaws ClientConfiguration]
           [com.amazonaws.regions Regions]
           [com.amazonaws.services.lambda.model InvokeRequest]
           [com.amazonaws.services.lambda AWSLambdaClient]))

(defn parse-result [result]
  (-> result
      (.getPayload)
      (.array)
      (java.io.ByteArrayInputStream.)
      (io/reader)
      (parse-stream true)))

(defn invoke-lambda [simulation lambda-function-name options]
  (println "Invoking Lambda for node:" (:node-id options))
  (let [client-config (-> (ClientConfiguration.)
                          (.withSocketTimeout (* 6 60 1000)))
        client (-> (AWSLambdaClient. @aws-credentials client-config)
                   (.withRegion (Regions/fromName (:region options))))
        request (-> (InvokeRequest.)
                    (.withFunctionName lambda-function-name)
                    (.withPayload (generate-string {:simulation simulation
                                                    :options options})))]

    (parse-result (.invoke client request))))

(def max-runtime-in-millis (* 4 60 1000))

(defn split-to-durations [millis]
  (loop [millis-left millis
         buckets []]
    (if (> millis-left max-runtime-in-millis)
      (recur (- millis-left max-runtime-in-millis) (conj buckets max-runtime-in-millis))
      (conj buckets millis-left))))

(defn invoke-lambda-sequentially [simulation lambda-function-name options node-id]
  (let [durations (split-to-durations (t/in-millis (:duration options)))]
    (apply merge-with concat
           (mapv #(invoke-lambda simulation
                                 lambda-function-name
                                 (assoc options
                                        :node-id node-id
                                        :duration %
                                        :timeout-in-ms (:timeout-in-ms options)))
                 durations))))

(defn lambda-executor [lambda-function-name node-id simulation options]
  (println "Starting AWS Lambda executor with id:" node-id)
  (let [only-collector-as-str (fn [reporter]
                                (-> reporter
                                    (dissoc :generator)
                                    (update :collector str)))]
    (invoke-lambda-sequentially (str simulation)
                                lambda-function-name
                                (update options :reporters #(map only-collector-as-str %))
                                node-id)))

(defn run-simulation [^clojure.lang.Symbol simulation
                      {:keys [concurrency lambda-function-name node-count bucket-name duration region] :as options
                       :or {lambda-function-name "clojider-load-testing-lambda"
                            node-count 1}}]
  (let [reporter s3/reporter]
    (gatling/run simulation (-> options
                                (update :context #(assoc % :region region :bucket-name bucket-name))
                                (assoc :nodes node-count
                                       :region region
                                       :executor (partial lambda-executor lambda-function-name)
                                       :reporters [reporter])))))
