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

(defn- smallest-vector [vector-of-vectors]
  (reduce (fn [m k]
            (if (< (count k) (count m))
              k
              m))
          (first vector-of-vectors)
          (rest vector-of-vectors)))

(defn- idx-of-smallest-vector [vector-of-vectors]
  (.indexOf vector-of-vectors (smallest-vector vector-of-vectors)))

(defn split-to-number-of-buckets [xs bucket-count]
  (reduce (fn [m v]
            (update m (idx-of-smallest-vector m) conj v))
          (vec (repeat bucket-count []))
          xs))

