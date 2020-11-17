(ns workable-leankit.core
  (:require [clj-http.client :as http]
            [clojure.stacktrace :refer :all]
            [clojure.data.json :as json]))

(def leankit-url (System/getenv "LEANKIT_URL"))
(def leankit-token (System/getenv "LEANKIT_TOKEN"))
(def workable-url (System/getenv "WORKABLE_URL"))
(def workable-token (System/getenv "WORKABLE_TOKEN"))

(defn status-try [f message]
  (try (f) 
       (catch clojure.lang.ExceptionInfo e
         {:status :error :message message})))

(defn migrate-lanes-to-leankit [board-id lanes-body]
  (http/put (str leankit-url "/io/board/" board-id "/layout")
            {:bearer-auth leankit-token
             :body lanes-body})
  {:status :error :message "Failed to get candidates"})

(defn get-stages [board-id]
  (let [stages-response
        (http/get (str workable-url "/stages")
                  {:bearer-auth workable-token })

        stages-map (json/read-str (:body stages-response)
                                  :key-fn keyword)
        stages (:stages stages-map)
        lanes (map (fn [item] {:title (:name item)}) stages)
        lanes-body (json/write-str {:lanes lanes})]

    (status-try #(migrate-lanes-to-leankit board-id lanes-body)
                 "Failed to add stages to board")))

(defn migrate-workable-to-leankit []
  (let [board-id-response 
        (http/post (str leankit-url "/io/board")
                   {:bearer-auth leankit-token})
        board-id-map (json/read-str (:body board-id-response)
                                    :key-fn keyword)
        board-id (:id board-id-map)]

    (status-try #(get-stages board-id)
                "Failed to get stages")))

(defn main 
  "Synchronises Workable with a Leankit board"
  [& args]
  (status-try migrate-workable-to-leankit "Failed to create board"))

