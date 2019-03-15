(defproject clojider-example "0.1.0"
  :description "Clojider example"
  :url "https://github.com/mhjort/clojider"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojider "0.6.0"]]
  :plugins [[lein-clj-lambda "0.12.1"]]
  :lambda {"clojider" [{:handler "clojider.LambdaFn"
                        :memory-size 1536
                        :timeout 300
                        :function-name "clojider-load-testing-lambda"
                        :region "eu-west-1"
                        :policy-statements [{:Effect "Allow"
                                             :Action ["s3:PutObject"]
                                             :Resource "arn:aws:s3:::clojider-*/*"}
                                            {:Effect "Allow"
                                             :Action ["logs:CreateLogGroup"
                                                      "logs:CreateLogStream"
                                                      "logs:PutLogEvents"]
                                             :Resource ["arn:aws:logs:*:*:*"]}]}]}
  :uberjar-exclusions [#"scala.*"]
  :aot [clojider.core clojider.lambda]
  :main clojider.core)
