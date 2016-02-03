(defproject clojider "0.1.5"
  :description "FIXME: write description"
  :url "https://github.com/mhjort/clojider"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [clj-gatling "0.7.8"]
                 [uswitch/lambada "0.1.0"]
                 [com.amazonaws/aws-java-sdk-lambda "1.10.49"]
                 [com.amazonaws/aws-java-sdk-core "1.10.49"]
                 [com.amazonaws/aws-java-sdk-s3 "1.10.49"]]
  :aot [clojider.lambda])

