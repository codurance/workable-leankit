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
                    :body "{\"id\": 123}"})]
      [(http-mock/route 
         :get
         (str (System/getenv "WORKABLE_URL") "/stages"))
       (constantly {:status 401
                    :body {:error "Not authorized"}})]
      (is (= {:status :error
              :message "Failed to get stages"}
             (main))))))

