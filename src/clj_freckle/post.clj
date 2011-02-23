(ns clj-freckle.post
  (:require
    [clj-freckle.core :as freckle]
    [clj-freckle.util :as util]
    [clojure.contrib.prxml :as prxml]
    [clj-http.client :as client])
  (:import [java.util Date]))


(defn create-import-entry
  ([minutes user project-name description]
    (create-import-entry minutes user project-name description (util/format-iso-date (Date.))))
  ([minutes user project-name description date]
    {:minutes minutes
     :user user
     :project-name project-name
     :description description
     :date date}))

(defn prep-import-xml-entry
  [entry]
  "creates the data structure necessary for outputting XML entries"
  (vec (conj (seq entry) :entry)))

(defn create-entry
  ([minutes user project-id description]
    (create-entry minutes user project-id description (util/format-iso-date (Date.))))
  ([minutes user project-id description date]
    {:minutes minutes
     :user user
     :project-id project-id
     :description description
     :date date}))

(defn prep-xml-entry
  [entry]
  "creates the data structure necessary for outputting XML entries"
  (let [entry-base (seq (dissoc entry :project-id))
        details (conj entry-base
                  [:project-id {:type "integer"} (:project-id entry)]
                  :entry)]
    (vec details)))

;(defn create-entry-xml
;  "Creates the xml necessary to post a single time entry.
;Defaulting date to today (formated in YYYY-MM-DD)"
;  ([minutes user project-id description]
;    (create-entry-xml minutes user project-id description (util/format-iso-date (Date.))))
;  ([minutes user project-id description date]
;    (prxml/prxml
;      [:decl! {:version "1.0"}] 
;      [:entry
;       [:minutes minutes] 
;       [:user user]
;       [:project-id {:type "integer"} project-id]
;       [:description description]
;       [:date date]])))
;
;(defn create-import-entry-xml
;  "Creates the xml necessary when posting multiple time entries.
;Defaulting date to today (formated in YYYY-MM-DD)"
;  ([minutes user project-name description]
;    (create-entry-xml minutes user project-name description (util/format-iso-date (Date.))))
;  ([minutes user project-name description date]
;    (prxml/prxml
;      [:decl! {:version "1.0"}] 
;      [:entry
;       [:minutes minutes] 
;       [:user user]
;       [:project-name project-name]
;       [:description description]
;       [:date date]])))


(defn import-entries
  "POST to /api/entries/import.xml"
  [{:keys [token client] :as creds} entries]
  (let [entries-xml (vec (conj (map prep-import-xml-entry entries)
                           :entries))
        xml (with-out-str (prxml/prxml [:decl! {:version "1.0"}] entries-xml))
        url (freckle/xml-resource-url creds "entries/import")]
    (client/post url
      {:body xml
       :headers {"X-FreckleToken:" token}
       :content-type "text/xml"})))

(defn import-entry
  "POST to /api/entries.xml"
  [creds & entry-details]
  (import-entries creds [(apply create-import-entry entry-details)]))


(defn post-entry
  "POST to /api/entries.xml"
  [{:keys [token client] :as creds} & entry-details]
  (let [entry (apply create-entry entry-details)
        xml (with-out-str (prxml/prxml [:decl! {:version "1.0"}]
                            (prep-xml-entry entry)))
        url (freckle/xml-resource-url creds "entries")]
    (client/post url
      {:body xml
       :headers {"X-FreckleToken:" token}
       :content-type "text/xml"})))



(defn post-json-entry
  "POST to /api/entries.xml"
  [{:keys [token client] :as creds} & entry-details]
  (let [entry (apply create-entry entry-details)
        url (freckle/json-resource-url creds "entries")]
    (client/post url
      {:body 
;       (client/generate-query-string
;         (reduce (fn [m [k v]]
;                   (assoc m (str "entry[" (name k) "]") v))
;           {} entry))
       (apply str (interpose "&"
                    (map (fn [[k v]] (str "entry[" (name k) "]" v)) entry)))
       :headers {"X-FreckleToken:" token}
       :content-type :json})))