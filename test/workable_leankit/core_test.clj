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
                    :body "{\"stages\": [{\"name\": \"phone interview\",\"position\": 0}]}"})
       (http-mock/route 
         :put
         (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
       (constantly {:status 401
                    :body "{\"statusCode\": 403, \"message\": \"Forbidden\"}"})]
      (is (= {:status :error
              :message "Failed to add stages to board"}
             (main)))))
  (testing "Error fetching jobs"
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
                    :body "{\"stages\": [{\"name\": \"phone interview\",\"position\": 0}]}"})
       (http-mock/route 
         :put
         (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
       (constantly {:status 200
                    :body "{ \"lanes\": [ {\"id\": \"1 \",  \"title\": \"phone interview\", \"wipLimit\": 0, \"columns\": 1, \"orientation\": \"vertical\", \"index\": 0, \"type\": \"ready\", \"classType\": \"backlog\", \"cardStatus\": \"notStarted\", \"description\": null, \"isConnectionDoneLane\": false, \"isDefaultDropLane\": false, \"children\": [] } ], \"layoutChecksum\": \"b41c4d1deb7e46b2180a636020b2e5cf\" }"})
       (http-mock/route 
         :get
         (str (System/getenv "WORKABLE_URL") "/jobs?status=published"))
       (constantly {:status 401
                    :body "Failed to get jobs"})]
      (is (= (main)
             {:status :error :message "Failed to get jobs"}))))

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
                    :body "{\"stages\": [{\"name\": \"phone interview\",\"position\": 0}]}"})
       (http-mock/route 
         :put
         (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
       (constantly {:status 200
                    :body "{ \"lanes\": [ {\"id\": \"1 \",  \"title\": \"phone interview\", \"wipLimit\": 0, \"columns\": 1, \"orientation\": \"vertical\", \"index\": 0, \"type\": \"ready\", \"classType\": \"backlog\", \"cardStatus\": \"notStarted\", \"description\": null, \"isConnectionDoneLane\": false, \"isDefaultDropLane\": false, \"children\": [] } ], \"layoutChecksum\": \"b41c4d1deb7e46b2180a636020b2e5cf\" }"})
       (http-mock/route 
         :get
         (str (System/getenv "WORKABLE_URL") "/jobs?status=published"))
       (constantly {:status 200
                    :body "{\"jobs\": [{\"id\": \"61884e2\",\"title\": \"Sales Intern\",\"full_title\": \"Sales Intern - US/3/SI\",\"shortcode\": \"GROOV003\",\"code\": \"US/3/SI\", \"state\": \"published\", \"department\": \"Sales\", \"url\": \"https://groove-tech.workable.com/jobs/102268944\", \"application_url\": \"https://groove-tech.workable.com/jobs/102268944/candidates/new\", \"shortlink\": \"https://groove-tech.workable.com/j/GROOV003\", \"location\": { \"location_str\": \"Portland, Oregon, United States\", \"country\": \"United States\", \"country_code\": \"US\", \"region\": \"Oregon\", \"region_code\": \"OR\", \"city\": \"Portland\", \"zip_code\": \"97201\", \"telecommuting\": false }, \"created_at\": \"2015-07-01T00:00:00Z\" } ] }"})
       (http-mock/route
         :get
         (str (System/getenv "WORKABLE_URL") "/candidates?shortcode=GROOV003"))
       (constantly {:status 401
                    :body "{\"error\": \"not authorized\"}"})]
      (is (= (main)
             {:status :error :message "Failed to get candidates"}))))
(testing "Error fetching second page of candidates"
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
                  :body "{\"stages\": [{\"name\": \"phone interview\",\"position\": 0}]}"})
     (http-mock/route 
       :put
       (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
     (constantly {:status 200
                  :body "{ \"lanes\": [ {\"id\": \"1 \",  \"title\": \"phone interview\", \"wipLimit\": 0, \"columns\": 1, \"orientation\": \"vertical\", \"index\": 0, \"type\": \"ready\", \"classType\": \"backlog\", \"cardStatus\": \"notStarted\", \"description\": null, \"isConnectionDoneLane\": false, \"isDefaultDropLane\": false, \"children\": [] } ], \"layoutChecksum\": \"b41c4d1deb7e46b2180a636020b2e5cf\" }"})
     (http-mock/route 
       :get
       (str (System/getenv "WORKABLE_URL") "/jobs?status=published"))
     (constantly {:status 200
                  :body "{\"jobs\": [{\"id\": \"61884e2\",\"title\": \"Sales Intern\",\"full_title\": \"Sales Intern - US/3/SI\",\"shortcode\": \"GROOV003\",\"code\": \"US/3/SI\", \"state\": \"published\", \"department\": \"Sales\", \"url\": \"https://groove-tech.workable.com/jobs/102268944\", \"application_url\": \"https://groove-tech.workable.com/jobs/102268944/candidates/new\", \"shortlink\": \"https://groove-tech.workable.com/j/GROOV003\", \"location\": { \"location_str\": \"Portland, Oregon, United States\", \"country\": \"United States\", \"country_code\": \"US\", \"region\": \"Oregon\", \"region_code\": \"OR\", \"city\": \"Portland\", \"zip_code\": \"97201\", \"telecommuting\": false }, \"created_at\": \"2015-07-01T00:00:00Z\" } ] }"})
     (http-mock/route
       :get
       (str (System/getenv "WORKABLE_URL") "/candidates?shortcode=GROOV003"))
     (constantly {:status 200
                  :body " { \"candidates\": [ { \"id\": \"ce4da98\", \"name\": \"Lakita Marrero\", \"firstname\": \"Lakita\", \"lastname\": \"Marrero\", \"headline\": \"Operations Manager\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV005\", \"title\": \"Office Manager\" }, \"stage\": \"Interview\", \"disqualified\": true, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/216323526\", \"email\": \"lakita_marrero@gmail.com\", \"domain\": \"twitter.com\", \"created_at\": \"2015-06-26T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" }, { \"id\": \"108d1748\", \"name\": \"Cindy Sawyers\", \"firstname\": \"Cindy\", \"lastname\": \"Sawyers\", \"headline\": \"Talented Operations Executive\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV005\", \"title\": \"Office Manager\" }, \"stage\": \"Applied\", \"disqualified\": false, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/277680758\", \"email\": \"cindy_sawyers@gmail.com\", \"domain\": \"indeed.com\", \"created_at\": \"2015-07-08T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" } ], \"paging\": { \"next\": \"https://www.workable.com/spi/v3/accounts/groove-tech/candidates?limit=3&since_id=2789d6dg\" } }"})
     (http-mock/route
       :get
       "https://www.workable.com/spi/v3/accounts/groove-tech/candidates?limit=3&since_id=2789d6dg")
     (constantly {:status 401
                  :body "{\"error\": \"not authorized\"}"})]
    (is (= (main)
           {:status :error :message "Failed to get candidates"}))))
(testing "Error registering first candidate"
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
                  :body "{\"stages\": [{\"name\": \"phone interview\",\"position\": 0}]}"})
     (http-mock/route 
       :put
       (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
     (constantly {:status 200
                  :body "{ \"lanes\": [ { \"id\": \"1 \", \"title\": \"phone interview\", \"wipLimit\": 0, \"columns\": 1, \"orientation\": \"vertical\", \"index\": 0, \"type\": \"ready\", \"classType\": \"backlog\", \"cardStatus\": \"notStarted\", \"description\": null, \"isConnectionDoneLane\": false, \"isDefaultDropLane\": false, \"children\": [] } ], \"layoutChecksum\": \"b41c4d1deb7e46b2180a636020b2e5cf\" }"})
     (http-mock/route 
       :get
       (str (System/getenv "WORKABLE_URL") "/jobs?status=published"))
     (constantly {:status 200
                  :body "{\"jobs\": [{\"id\": \"61884e2\",\"title\": \"Sales Intern\",\"full_title\": \"Sales Intern - US/3/SI\",\"shortcode\": \"GROOV003\",\"code\": \"US/3/SI\", \"state\": \"published\", \"department\": \"Sales\", \"url\": \"https://groove-tech.workable.com/jobs/102268944\", \"application_url\": \"https://groove-tech.workable.com/jobs/102268944/candidates/new\", \"shortlink\": \"https://groove-tech.workable.com/j/GROOV003\", \"location\": { \"location_str\": \"Portland, Oregon, United States\", \"country\": \"United States\", \"country_code\": \"US\", \"region\": \"Oregon\", \"region_code\": \"OR\", \"city\": \"Portland\", \"zip_code\": \"97201\", \"telecommuting\": false }, \"created_at\": \"2015-07-01T00:00:00Z\" } ] }"})
     (http-mock/route
       :get
       (str (System/getenv "WORKABLE_URL") "/candidates?shortcode=GROOV003"))
     (constantly {:status 200
                  :body " { \"candidates\": [ { \"id\": \"ce4da98\", \"name\": \"Lakita Marrero\", \"firstname\": \"Lakita\", \"lastname\": \"Marrero\", \"headline\": \"Operations Manager\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV005\", \"title\": \"Office Manager\" }, \"stage\": \"phone interview\", \"disqualified\": true, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/216323526\", \"email\": \"lakita_marrero@gmail.com\", \"domain\": \"twitter.com\", \"created_at\": \"2015-06-26T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" }, { \"id\": \"108d1748\", \"name\": \"Cindy Sawyers\", \"firstname\": \"Cindy\", \"lastname\": \"Sawyers\", \"headline\": \"Talented Operations Executive\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV005\", \"title\": \"Office Manager\" }, \"stage\": \"phone interview\", \"disqualified\": false, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/277680758\", \"email\": \"cindy_sawyers@gmail.com\", \"domain\": \"indeed.com\", \"created_at\": \"2015-07-08T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" } ], \"paging\": { \"next\": \"https://www.workable.com/spi/v3/accounts/groove-tech/candidates?limit=3&since_id=2789d6dg\" } }"})
     (http-mock/route
       :get
       "https://www.workable.com/spi/v3/accounts/groove-tech/candidates?limit=3&since_id=2789d6dg")
     (constantly {:status 200
                  :body " { \"candidates\": [ { \"id\": \"ce4da99\", \"name\": \"Angus Young\", \"firstname\": \"Angus\", \"lastname\": \"Young\", \"headline\": \"Software Craftman\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV007\", \"title\": \"Software Craftman\" }, \"stage\": \"phone interview\", \"disqualified\": true, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/216323526\", \"email\": \"angus_young@gmail.com\", \"domain\": \"twitter.com\", \"created_at\": \"2015-06-26T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" }], \"paging\": { } }"})
     (http-mock/route
       :post
       (str (System/getenv "LEANKIT_URL") "/io/card"))
     (constantly {:status 500
                  :body "{\"error\": \"Internal Server Error\""})]
    (is (= (main)
           {:status :error :message "Failed to migrate candidates"}))))
(testing "Success registering all candidates"
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
                  :body "{\"stages\": [{\"name\": \"phone interview\",\"position\": 0}]}"})
     (http-mock/route 
       :put
       (str (System/getenv "LEANKIT_URL") "/io/board/123/layout"))
     (constantly {:status 200
                  :body "{ \"lanes\": [ { \"id\": \"1 \", \"title\": \"phone interview\", \"wipLimit\": 0, \"columns\": 1, \"orientation\": \"vertical\", \"index\": 0, \"type\": \"ready\", \"classType\": \"backlog\", \"cardStatus\": \"notStarted\", \"description\": null, \"isConnectionDoneLane\": false, \"isDefaultDropLane\": false, \"children\": [] } ], \"layoutChecksum\": \"b41c4d1deb7e46b2180a636020b2e5cf\" }"})
     (http-mock/route 
       :get
       (str (System/getenv "WORKABLE_URL") "/jobs?status=published"))
     (constantly {:status 200
                  :body "{\"jobs\": [{\"id\": \"61884e2\",\"title\": \"Sales Intern\",\"full_title\": \"Sales Intern - US/3/SI\",\"shortcode\": \"GROOV003\",\"code\": \"US/3/SI\", \"state\": \"published\", \"department\": \"Sales\", \"url\": \"https://groove-tech.workable.com/jobs/102268944\", \"application_url\": \"https://groove-tech.workable.com/jobs/102268944/candidates/new\", \"shortlink\": \"https://groove-tech.workable.com/j/GROOV003\", \"location\": { \"location_str\": \"Portland, Oregon, United States\", \"country\": \"United States\", \"country_code\": \"US\", \"region\": \"Oregon\", \"region_code\": \"OR\", \"city\": \"Portland\", \"zip_code\": \"97201\", \"telecommuting\": false }, \"created_at\": \"2015-07-01T00:00:00Z\" } ] }"})
     (http-mock/route
       :get
       (str (System/getenv "WORKABLE_URL") "/candidates?shortcode=GROOV003"))
     (constantly {:status 200
                  :body " { \"candidates\": [ { \"id\": \"ce4da98\", \"name\": \"Lakita Marrero\", \"firstname\": \"Lakita\", \"lastname\": \"Marrero\", \"headline\": \"Operations Manager\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV005\", \"title\": \"Office Manager\" }, \"stage\": \"phone interview\", \"disqualified\": true, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/216323526\", \"email\": \"lakita_marrero@gmail.com\", \"domain\": \"twitter.com\", \"created_at\": \"2015-06-26T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" }, { \"id\": \"108d1748\", \"name\": \"Cindy Sawyers\", \"firstname\": \"Cindy\", \"lastname\": \"Sawyers\", \"headline\": \"Talented Operations Executive\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV005\", \"title\": \"Office Manager\" }, \"stage\": \"phone interview\", \"disqualified\": false, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/277680758\", \"email\": \"cindy_sawyers@gmail.com\", \"domain\": \"indeed.com\", \"created_at\": \"2015-07-08T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" } ], \"paging\": { \"next\": \"https://www.workable.com/spi/v3/accounts/groove-tech/candidates?limit=3&since_id=2789d6dg\" } }"})
     (http-mock/route
       :get
       "https://www.workable.com/spi/v3/accounts/groove-tech/candidates?limit=3&since_id=2789d6dg")
     (constantly {:status 200
                  :body " { \"candidates\": [ { \"id\": \"ce4da99\", \"name\": \"Angus Young\", \"firstname\": \"Angus\", \"lastname\": \"Young\", \"headline\": \"Software Craftman\", \"account\": { \"subdomain\": \"groove-tech\", \"name\": \"Groove Tech\" }, \"job\": { \"shortcode\": \"GROOV007\", \"title\": \"Software Craftman\" }, \"stage\": \"phone interview\", \"disqualified\": true, \"disqualification_reason\": null, \"sourced\": false, \"profile_url\": \"https://groove-tech.workable.com/backend/jobs/376844767/candidates/216323526\", \"email\": \"angus_young@gmail.com\", \"domain\": \"twitter.com\", \"created_at\": \"2015-06-26T00:00:00Z\", \"updated_at\": \"2015-07-08T14:46:48Z\" }], \"paging\": { } }"})
     (http-mock/route
       :post
       (str (System/getenv "LEANKIT_URL") "/io/card"))
     (constantly {:status 201
                  :body "{\"id\": \"Hello World\""})]

    (is (= (main)
           {:status :success :message "Migration completed successfully"})))
  )
)
