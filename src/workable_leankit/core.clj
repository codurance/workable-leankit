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

(defn main 
  "Synchronises Workable with a Leankit board"
  [& args]
  (try 
    (let [board-id-response 
          (http/post (str leankit-url "/io/board")
                     {:bearer-auth leankit-token})
          board-id-map (json/read-str (:body board-id-response)
                                      :key-fn keyword)
          board-id (:id board-id-map)]

      (status-try (fn []
                    (http/get (str workable-url "/stages")
                              {:bearer-auth workable-token})
                    {:status :error
                     :message "Failed to add stages to board"})
                  "Failed to get stages"))
    (catch clojure.lang.ExceptionInfo e
      {:status :error :message "Failed to create board"})))

