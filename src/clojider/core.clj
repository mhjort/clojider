(ns clojider.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojider.aws :as aws])
  (:gen-class))

(def cli-options
  [["-r" "--region REGION" "Which region to use"]
   ["-f" "--file FILE" "Path of uberjar"]])

(def cmds
  {"install" aws/install-lambda
   "uninstall" aws/uninstall-lambda
   "update" aws/update-lambda})

(defn -main [& args]
  (let [options (parse-opts args cli-options)
        cmd-str (first (:arguments options))]
    (if-let [cmd (get cmds cmd-str)]
      (cmd (:options options))
      (println "Unknown command:" cmd-str))))

