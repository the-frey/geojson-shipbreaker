# GeoJSON Shipbreaker

Example data is in the `data` folder, just unzip it. Results by default are placed in the `data/geojson` folder for UK OAs. Bear in mind the licence is the same as the UK Geoportal licence for this.

I might extend this with some more utility stuff, or I might not.

## Usage

Run via repl:

    lein repl

### Elasticsearch

Install Elasticsearch via brew:

    brew search elasticsearch

Then install the 2.x version listed; for me that's:

    brew install elasticsearch@2.4

If your ES version is not compatible with Elastisch, use this cURL command:

```
curl -XPUT 'localhost:9200/output_areas?pretty' -H 'Content-Type: application/json' -d'
{
    "mappings": {
        "doc": {
            "properties": {
                "location": {
                    "type": "geo_shape",
                    "tree": "quadtree",
                    "precision": "1m"
                }
            }
        }
    }
}
'
```

If some of the polygons are rejected by Elasticsearch with the error:

    MapperParsingException[failed to parse [location]]; nested: IllegalArgumentException[Points of LinearRing do not form a closed linestring]

Then this probably means that your polygons are not `ISO 19107:2003` compliant. You can find out more info on [this GitHub issue](https://github.com/elastic/elasticsearch/issues/12325), and you might need to download and run [prepair](https://github.com/tudelft3d/prepair) on the polygons you know to be broken, before importing the fixed GeoJSON files individually.

For output areas, the culprits are likely to be areas like `E00018302`.

## License

See appropriate licences for UK Geodata, the code is MIT. 

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
