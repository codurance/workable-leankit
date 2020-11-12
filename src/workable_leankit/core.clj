(ns workable-leankit.core
  (:require [clj-http.client :as http]
            [clojure.stacktrace :refer :all]))

(def leankit-url (System/getenv "LEANKIT_URL"))
(def leankit-token (System/getenv "LEANKIT_TOKEN"))

(defn main 
  "Synchronises Workable with a Leankit board"
  [& args]
  (try 
    (http/post (str leankit-url "/io/board")
               {:bearer-auth leankit-token})
    {:status :error :message "Failed to get stages"}
    (catch clojure.lang.ExceptionInfo e
      {:status :error :message "Failed to create board"})))
