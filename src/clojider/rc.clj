(ns clojider.rc
 (:require [clojure.edn :as edn]
           [clojure.java.io :as io]
           [clojure.string :refer [split]]
           [clj-time.core :as t]
           [clj-time.format :as f]
           [clj-gatling.chart :as chart]
           [cheshire.core :refer [generate-string parse-stream]]
           [clj-gatling.simulation-util :refer [split-to-number-of-buckets]]
           [clojure.core.async :refer [thread <!!]])
  (:import [com.amazonaws.auth BasicAWSCredentials]
           [com.amazonaws ClientConfiguration]
           [com.amazonaws.regions Regions]
           [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.services.lambda.model InvokeRequest]
           [com.amazonaws.services.lambda AWSLambdaClient]))

(def aws-credentials
  (BasicAWSCredentials. (System/getenv "AWS_ACCESS_KEY_ID")
                        (System/getenv "AWS_SECRET_ACCESS_KEY")))

(defn parse-result [result]
  (-> result
      (.getPayload)
      (.array)
      (java.io.ByteArrayInputStream.)
      (io/reader)
      (parse-stream true)))

(defn create-dir [dir]
    (.mkdirs (java.io.File. dir)))

(defn download-file [results-dir bucket object-key]
  (let [client (AmazonS3Client. aws-credentials)]
  (io/copy (.getObjectContent (.getObject client bucket object-key))
           (io/file (str results-dir "/" (last (split object-key #"/")))))))

(defn- generate-folder-name []
  (let [custom-formatter (f/formatter "yyyyMMddHHmmssSSS")]
    (f/unparse custom-formatter (t/now))))

(defn create-chart [results bucket-name folder-name]
  (let [input-dir (str "tmp/" folder-name "/input")]
    (create-dir input-dir)
    (println "Downloading" results "from" bucket-name)
    (doseq [result results]
      (download-file input-dir bucket-name result))
    (chart/create-chart (str "tmp/" folder-name))))

(defn invoke-lambda [simulation lambda-function-name options]
  (println "Invoking Lambda for" (:node-id options))
  (let [client-config (-> (ClientConfiguration.)
                          (.withSocketTimeout (* 6 60 1000)))
        client (-> (AWSLambdaClient. aws-credentials client-config)
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

(defn symbol-namespace [^clojure.lang.Symbol simulation]
  (str "/"
       (clojure.string/join "/"
                            (-> simulation
                                (resolve)
                                (str)
                                (subs 2)
                                (clojure.string/replace #"\." "/")
                                (clojure.string/replace #"-" "_")
                                (clojure.string/split #"/")
                                (drop-last)))))

(defn fully-qualified-name [^clojure.lang.Symbol simulation]
  (subs (str (resolve simulation)) 2))

(defn invoke-lambda-sequentially-in-thread [simulation simu-name lambda-function-name folder-name options node-id users]
  (let [durations (split-to-durations (t/in-millis (:duration options)))]
    (thread
      (apply merge-with concat
             (mapv #(invoke-lambda simu-name
                                  lambda-function-name
                                  (assoc options :folder-name folder-name
                                         :node-id node-id
                                         :users users
                                         :duration %
                                         :simulation-namespaces [(symbol-namespace simulation)]))
                  durations)))))

(defn run-simulation [^clojure.lang.Symbol simulation {:keys [concurrency lambda-function-name node-count bucket-name duration] :as options
                                                       :or {lambda-function-name "clojider-load-testing-lambda"
                                                            node-count 1}}]
  (let [splitted-users (split-to-number-of-buckets (range concurrency) node-count)
        folder-name (generate-folder-name)
        simu-name (fully-qualified-name simulation)
        result-channels (mapv (partial invoke-lambda-sequentially-in-thread simulation simu-name lambda-function-name folder-name options)
                              (range)
                              splitted-users)
        all-results (mapcat :results (map <!! result-channels))]
    (println "Got results" all-results)
    (create-chart all-results bucket-name folder-name)
    (println "Open" (str "tmp/" folder-name "/index.html to see full report"))))
