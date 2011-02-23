(ns clj-freckle.core
  (:use
    [clojure.xml :only [parse]]
    [clojure.contrib.str-utils :only [str-join]]
    [clj-freckle.util])
  (:require
    [clj-http.client :as client]
    [clojure.contrib.json :as json]))

(defstruct credentials :token :client)

(def resources 
     {:users     {:tag :user}
      :projects  {:tag :project}
      :entries   {:tag :entry}
      :tags   {:tag :tag}})

(defstruct search-options :people :projects :tags :from :to)

(defn- create-search-param [[map-key map-value]]
  (str "search["(name map-key) "]=" map-value))

(defn- create-search-params [options]
  (str-join "&" (map create-search-param options)))

(defn- resource-url [{:keys [token client]} resource type & [options]]
  (str "http://" client ".letsfreckle.com/api/" (name resource) "." type "?token=" token
       (if options
	 (str "&" (create-search-params options)))))

(defn xml-resource-url [creds resource & options]
  (apply resource-url creds resource "xml" options))

(defn json-resource-url [creds resource & options]
  (apply resource-url creds resource "json" options))

(defn- get-xml-resource [credentials resource & [options]]
  (flatten-xml 
   (parse (xml-resource-url credentials resource options))
   (-> resources resource :tag)))

(defn get-json-resource [credentials resource & [options]]
  (let [response (client/get (json-resource-url credentials resource options))]
    (json/read-json (:body response))))

(defn get-users [credentials]
    (get-xml-resource credentials :users))

(defn get-projects [credentials]
    (get-xml-resource credentials :projects))

(defn get-tags [credentials]
    (get-xml-resource credentials :tags))

(defn get-entries [credentials & [options]] 
  (get-xml-resource credentials :entries options))

(defn sum-hours [entries]
  (float (/ (reduce #(+ %1 (new Integer (%2 :minutes))) 0 entries) 60 )))