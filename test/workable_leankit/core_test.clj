(ns workable-leankit.core-test
  (:require [clojure.test :refer :all]
            [workable-leankit.core :refer :all]
            [clj-http.client :as http]
            [clj-http-mock.core :as http-mock]))

(deftest workable-leankit-test
  (testing "Errors creating the board"
    (http-mock/with-mock-routes
      [(http-mock/route 
         :post 
         (str (System/getenv "LEANKIT_URL") "/io/board"))
       (constantly {:status 403 
                    :body "{\"statusCode\": 403, \"message\": \"Forbidden\"}"})]
      (is (= {:status :error
              :message "Failed to create board"} 
             (main)))))
  (testing "Errors gettting the stages"
    (http-mock/with-mock-routes
      [(http-mock/route 
         :post
         (str (System/getenv "LEANKIT_URL") "/io/board"))
       (constantly {:status 200 
                    :body "{\"id\": 123}"})
       (http-mock/route 
         :get
         (str (System/getenv "WORKABLE_URL") "/stages"))
       (constantly {:status 401
                    :body "{\"error\": \"Not authorized\"}"})]
      (is (= {:status :error
              :message "Failed to get stages"}
             (main)))))

  (testing "Errors add a stage on the board"
    (http-mock/with-mock-routes
      [(http-mock/route 
         :post
         (str (System/getenv "LEANKIT_URL") "/io/board"))
       (constantly {:status 200 
                    :body "{\"id\": 123}"})
       (http-mock/route 
         :get
         (str (System/getenv "WORKABLE_URL") "/stages"))
       (constantly {:status 200
                    :body "{\"stages\": [{\"name\": \"phone interview\"}]}"})
       (http-mock/route 
         :put
         (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
       (constantly {:status 401
                    :body "{\"statusCode\": 403, \"message\": \"Forbidden\"}"})]
      (is (= {:status :error
              :message "Failed to add stages to board"}
             (main)))))
  (testing "Error fetching candidates"
    (http-mock/with-mock-routes
      [(http-mock/route 
         :post
         (str (System/getenv "LEANKIT_URL") "/io/board"))
       (constantly {:status 200 
                    :body "{\"id\": 123}"})
       (http-mock/route 
         :get
         (str (System/getenv "WORKABLE_URL") "/stages"))
       (constantly {:status 200
                    :body "{\"stages\": [{\"name\": \"phone interview\"}]}"})
       (http-mock/route 
         :put
         (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
       (constantly {:status 200
                    :body "{ \"lanes\": [ { \"title\": \"phone interview\", \"wipLimit\": 0, \"columns\": 1, \"orientation\": \"vertical\", \"index\": 0, \"type\": \"ready\", \"classType\": \"backlog\", \"cardStatus\": \"notStarted\", \"description\": null, \"isConnectionDoneLane\": false, \"isDefaultDropLane\": false, \"children\": [] } ], \"layoutChecksum\": \"b41c4d1deb7e46b2180a636020b2e5cf\" }"})
       (http-mock/route
         :get
         (str (System/getenv "WORKABLE_URL") "/candidates"))
       (constantly {:status 401
                    :body "{\"error\": \"not authorized\"}"})]
      (is (= (main)
             {:status :error :message "Failed to get candidates"}))))
  )




