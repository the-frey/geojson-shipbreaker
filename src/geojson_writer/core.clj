(ns geojson-writer.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]))

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

(def configured-index-name "output_areas")

(def geo-shape-mapping
  {"doc" {
         :properties {
                      :location {
                                 :type "geo_shape",
                                 :tree "quadtree",
                                 :precision "1m"
                                 }}}})

(defn create-index-with-mapping []
  (let [conn (esr/connect "http://127.0.0.1:9200")
        response (esi/create conn configured-index-name {:mappings geo-shape-mapping})]
    (println response)))

(defn delete-index []
  (let [conn (esr/connect "http://127.0.0.1:9200")
        response (esi/delete conn configured-index-name)]
    (println response)))

(defn polygon-doc->multipolygon-doc [{location :location
                                      :as doc}]
  (let [geometry (-> location
                     :coordinates)
        new-location {:location {:type "multipolygon"
                                 :coordinates [geometry]}}]
    (merge doc
           new-location)))

(defn smash-data-into-elasticsearch []
  (with-open [reader (-> "./data/simplified-wgs84.json"
                         io/file
                         io/reader)]
    (let [conn (esr/connect "http://127.0.0.1:9200")
          json (json/read reader)
          features (get json "features")]
      (map (fn [geojson-feature]
             (let [polygon-type (-> geojson-feature
                                    (get "geometry")
                                    (get "type")
                                    clojure.string/lower-case)
                   properties (-> geojson-feature
                                  (get "properties"))
                   coordinates (-> geojson-feature
                                   (get "geometry")
                                   (get "coordinates"))
                   document {:location {:type polygon-type
                                        :coordinates coordinates}
                             :oa11cd (get properties "oa11cd")
                             :lad11cd (get properties "lad11cd")}
                   response (try
                              (esd/create conn
                                          configured-index-name
                                          "doc"
                                          document)
                              (catch Exception e
                                (do
                                  (println (.toString e))
                                  (println "> Retrying as multipolygon")
                                  (esd/create conn
                                              configured-index-name
                                              "doc"
                                              (polygon-doc->multipolygon-doc document)))
                                ))]
               (do
                 (println "> Inserted: ")
                 (println response))))
           features))))


