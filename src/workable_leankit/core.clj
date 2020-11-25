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
                           {:headers {"Content-Type" "application/json"
                                      "Authorization" (str "Bearer " workable-token)}})
        candidates-body (json/read-str (:body response) :key-fn keyword)
        new-candidates (:candidates candidates-body)
        new-valid-candidates (filter (fn [i] (not (:disqualified i)))
                                     new-candidates)
        new-processed-candidates (into [] (concat processed-candidates new-valid-candidates))]
    (println (str "Getting candidates. " (count new-processed-candidates) " found."))
    (Thread/sleep 500)
    (if (nil? (:next (:paging candidates-body)))
      new-processed-candidates
      (get-candidates (:next (:paging candidates-body)) new-processed-candidates))))

(defn get-jobs []
  (http/get (str workable-url "/jobs?status=published")
            {:headers {"Content-Type" "application/json"
                       "Authorization" (str "Bearer " workable-token)}}))

(defn migrate-candidates-for-job [board-id lanes-map job]
  (defn migrate-candidate [c]
    (println (str "Migrating candidates. " (count c) " remaining candidates."))
    (if (not (empty? c))
      ((http/post (str leankit-url "/io/card")
                  {:headers {"Content-Type" "application/json"
                             "Authorization" (str "Bearer " leankit-token)}
                   :body (json/write-str (first c))})
       (migrate-candidate (rest c)))))

  (let [candidates (get-candidates (str workable-url 
                                        "/candidates?shortcode="
                                        (:shortcode job)) 
                                   [])
        cards-map (map (fn [i] {:title (str (:name i) " - " (:title job)) 
                                :customId (:city (:location job)) 
                                :description (:profile_url i)
                                :boardId board-id
                                :laneId (lanes-map (:stage i))})
                       candidates)]
    (status-try
      (fn []
        (migrate-candidate cards-map)
        (Thread/sleep 500)
        {:status :success :message "Migration completed successfully"})
      "Failed to migrate candidates")) )

(defn migrate-candidates [board-id lanes-map]
  (defn process-jobs [j]
    (if (not (empty? j))
      (let [result (migrate-candidates-for-job board-id lanes-map (first j))]
        (if (= (:status result) :error)
          result
          (process-jobs (rest j))))
      {:status :success :message "Migration completed successfully"}))
  (status-try 
    (fn []
      (let [jobs-response (get-jobs)
            jobs-body (json/read-str (:body jobs-response) :key-fn keyword)
            jobs (:jobs jobs-body)]
        (status-try #(process-jobs jobs) "Failed to get candidates")))
    "Failed to get jobs"))
(defn migrate-lanes-to-leankit [board-id lanes-body]
  (let [lanes-response
        (http/put (str leankit-url "/io/board/" board-id "/layout")
                  {:headers {"Content-Type" "application/json"
                             "Authorization" (str "Bearer " leankit-token)}
                   :body lanes-body})

        lanes-info (json/read-str (:body lanes-response)
                                  :key-fn keyword)

        lanes-map (into {} (map (fn [i] {(:title i) (:id i)}) (:lanes lanes-info)))]

    (status-try #(migrate-candidates board-id lanes-map) "Failed to get candidates")))

(defn get-stages [board-id]
  (let [stages-response
        (http/get (str workable-url "/stages")
                  {:headers {"Content-Type" "application/json"
                             "Authorization" (str "Bearer " workable-token)}})

        stages-map (json/read-str (:body stages-response)
                                  :key-fn keyword)
        stages (:stages stages-map)
        lanes (map (fn [item] {
                               :title (:name item) 
                               :classType 
                               (cond 
                                 (<= (:position item) 1) "backlog"
                                 (= (count stages) (:position item)) "archive"
                                 :else "active")
                               :index (:position item)}) stages)
        lanes-body (json/write-str {:lanes lanes})]

    (status-try #(migrate-lanes-to-leankit board-id lanes-body)
                "Failed to add stages to board")))

(defn migrate-workable-to-leankit []
  (let [board-id-response 
        (http/post (str leankit-url "/io/board")
                   {:headers {"Content-Type" "application/json"
                              "Authorization" (str "Bearer " leankit-token)}
                    :body (json/write-str {:title (str "Test " (System/currentTimeMillis))})})
        board-id-map (json/read-str (:body board-id-response)
                                    :key-fn keyword)
        board-id (:id board-id-map)]

    (status-try #(get-stages board-id)
                "Failed to get stages")))

(defn main 
  "Synchronises Workable with a Leankit board"
  [& args]
  (status-try migrate-workable-to-leankit "Failed to create board"))
