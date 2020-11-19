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

(defn get-candidates [url processed-candidates]
  (let [url (if (nil? url) (str workable-url "/candidates") url)
        response (http/get url
                           { :bearer-auth workable-token })
        candidates-body (json/read-str (:body response) :key-fn keyword)
        processed-candidates (conj (:candidates candidates-body) processed-candidates)]
    (if (nil? (:next (:paging candidates-body)))
      processed-candidates
      (get-candidates (:next (:paging candidates-body)) processed-candidates))))

(defn migrate-candidates [board-id lanes-map]
  (defn migrate-candidate [c]
    (if (not (empty? c))
      ((http/post (str leankit-url "/io/card")
                  {:bearer-auth leankit-token
                   :body (json/write-str (first c))})
       (migrate-candidate (rest c)))))

  (let [candidates (get-candidates nil '())
        cards-map (map (fn [i] {:title (str (:name i) " - " (:title (:job i)))
                                :description (:profile_url i)
                                :boardId board-id
                                :laneId (lanes-map (:stage i))})
                       candidates)]
    (status-try
      (fn []
        (migrate-candidate cards-map)
        {:status :success :message "Migration completed successfully"})
      "Failed to migrate candidates")) )

(defn migrate-lanes-to-leankit [board-id lanes-body]
  (let [lanes-response
        (http/put (str leankit-url "/io/board/" board-id "/layout")
                  {:bearer-auth leankit-token
                   :body lanes-body})

        lanes-info (json/read-str (:body lanes-response)
                                  :key-fn keyword)

        lanes-map (into {} (map (fn [i] {(:title i) (:id i)}) (:lanes lanes-info)))]

    (status-try #(migrate-candidates board-id lanes-map) "Failed to get candidates")))

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
