(ns geojson-writer.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn out-file-name [slug]
  (str "./data/geojson/" slug ".json"))

(defn write-output-areas-json []
  (with-open [reader (-> "./data/simplified-wgs84.json"
                         io/file
                         io/reader)]
    (let [json (json/read reader)
          features (get json "features")]
      (map (fn [geojson-feature]
             (let [gss-code (-> geojson-feature
                                (get "properties")
                                (get "oa11cd"))
                   file-name (out-file-name gss-code)]
               (with-open [out-file (-> file-name
                                        io/file
                                        io/writer)]
                 (do
                   (println "> Writing " gss-code " to " file-name "...")
                   (json/write geojson-feature out-file)))))
           features))))
