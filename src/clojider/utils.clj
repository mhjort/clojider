(ns clojider.utils)

(defn symbol-namespace [^clojure.lang.Symbol simulation]
  (str "/"
       (clojure.string/join "/"
                            (-> simulation
                                (str)
                                (clojure.string/replace #"\." "/")
                                (clojure.string/replace #"-" "_")
                                (clojure.string/split #"/")
                                (drop-last)))))

(defn fully-qualified-name [^clojure.lang.Symbol simulation]
  (str simulation))
