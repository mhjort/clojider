(defproject clojider "0.5.0-beta1"
  :description "AWS Lambda powered, distributed load testing tool for Clojure"
  :url "https://github.com/mhjort/clojider"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.5.0"]
                 [clj-gatling "0.12.0-beta2"]
                 [clojider-gatling-highcharts-s3-reporter "0.1.1"]
                 [uswitch/lambada "0.1.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [com.amazonaws/aws-java-sdk-iam "1.10.50"]
                 [com.amazonaws/aws-java-sdk-lambda "1.10.50"]
                 [com.amazonaws/aws-java-sdk-core "1.10.50"]
                 [com.amazonaws/aws-java-sdk-s3 "1.10.50"]]
  :uberjar-exclusions [#"scala.*"]
  :aot [clojider.core clojider.lambda]
  :main clojider.core)
