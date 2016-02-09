(ns clojider.aws
  (:require [cheshire.core :refer [generate-string]])
  (:import [com.amazonaws.services.identitymanagement AmazonIdentityManagementClient]
           [com.amazonaws.services.identitymanagement.model AttachRolePolicyRequest
                                                            CreatePolicyRequest
                                                            CreateRoleRequest
                                                            DeleteRoleRequest
                                                            DeletePolicyRequest
                                                            ListRolePoliciesRequest
                                                            DetachRolePolicyRequest]
           [com.amazonaws.services.lambda AWSLambdaClient]
           [com.amazonaws.services.lambda.model CreateFunctionRequest
                                                DeleteFunctionRequest
                                                UpdateFunctionCodeRequest
                                                FunctionCode]
           [com.amazonaws.auth BasicAWSCredentials]
           [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]
           [java.io File]))

(def aws-credentials
  (BasicAWSCredentials. (System/getenv "AWS_ACCESS_KEY_ID")
                        (System/getenv "AWS_SECRET_ACCESS_KEY")))

(defonce s3-client
  (delay (AmazonS3Client. aws-credentials)))

(defn- create-results-bucket [bucket-name region]
  (println "Creating bucket" bucket-name "for the results.")
  (if (= "us-east-1" region)
    (.createBucket @s3-client bucket-name)
    (.createBucket @s3-client bucket-name region)))

(defn- store-jar-to-bucket [bucket-name jar-path]
  (println "Uploading code to S3 from" jar-path)
  (.putObject @s3-client
              bucket-name
              "clojider.jar"
              (File. jar-path)))

(defn- delete-results-bucket [bucket-name]
  (.deleteBucket @s3-client bucket-name))

(defn- delete-all-objects-from-bucket [bucket-name]
  (println "Deleting all objects from bucket" bucket-name)
  (let [object-keys (map #(.getKey %)
                         (.getObjectSummaries (.listObjects @s3-client bucket-name)))]
    (doseq [object-key object-keys]
      (println "Deleting" object-key)
      (.deleteObject @s3-client bucket-name object-key))))

(def role
  {:Version "2012-10-17"
   :Statement {:Effect "Allow"
                :Principal {:Service "lambda.amazonaws.com"}
                :Action "sts:AssumeRole"}})

(defn policy [bucket-name]
  {:Version "2012-10-17"
   :Statement [{:Effect "Allow"
                :Action ["s3:PutObject"]
                :Resource (str "arn:aws:s3:::" bucket-name "/*")}
               {:Effect "Allow"
                :Action ["logs:CreateLogGroup"
                         "logs:CreateLogStream"
                         "logs:PutLogEvents"]
                :Resource ["arn:aws:logs:*:*:*"]}]})

(defn create-role-and-policy [role-name policy-name bucket-name]
  (println "Creating role" role-name "with policy" policy-name)
  (let [client (AmazonIdentityManagementClient. aws-credentials)
        role (.createRole client (-> (CreateRoleRequest.)
                                     (.withRoleName role-name)
                                     (.withAssumeRolePolicyDocument (generate-string role))))]
    (let [policy-result (.createPolicy client (-> (CreatePolicyRequest.)
                                                  (.withPolicyName policy-name)
                                                  (.withPolicyDocument (generate-string (policy bucket-name)))))]
      (.attachRolePolicy client (-> (AttachRolePolicyRequest.)
                                    (.withPolicyArn (-> policy-result .getPolicy .getArn))
                                    (.withRoleName role-name))))
    (-> role .getRole .getArn)))

(defn delete-role-and-policy [role-name policy-name]
  (println "Deleting role" role-name "with policy" policy-name)
  (let [client (AmazonIdentityManagementClient. aws-credentials)
        policy-arn (.getArn (first (filter #(= policy-name (.getPolicyName %))
                                           (.getPolicies (.listPolicies client)))))]
    (.detachRolePolicy client (-> (DetachRolePolicyRequest.)
                                  (.withPolicyArn policy-arn)
                                  (.withRoleName role-name)))
    (.deletePolicy client (-> (DeletePolicyRequest.)
                              (.withPolicyArn policy-arn)))
    (.deleteRole client (-> (DeleteRoleRequest.)
                            (.withRoleName role-name)))))

(defn- create-lambda-client [region]
  (-> (AWSLambdaClient. aws-credentials)
      (.withRegion (Regions/fromName region))))

(defn delete-lambda-fn [lambda-name region]
  (println "Deleting Lambda function" lambda-name "from region")
  (let [client (create-lambda-client region)]
    (.deleteFunction client (-> (DeleteFunctionRequest.)
                                (.withFunctionName lambda-name)))))

(defn- update-lambda-fn [lambda-name bucket-name region]
  (println "Updating Lambda function" lambda-name "in region")
  (let [client (create-lambda-client region)]
    (.updateFunctionCode client (-> (UpdateFunctionCodeRequest.)
                                    (.withFunctionName lambda-name)
                                    (.withS3Bucket bucket-name)
                                    (.withS3Key "clojider.jar")))))

(defn- create-lambda-fn [lambda-name bucket-name region role-arn]
  (println "Creating Lambda function" lambda-name "to region" region)
  (let [client (create-lambda-client region)]
    (.createFunction client (-> (CreateFunctionRequest.)
                                (.withFunctionName lambda-name)
                                (.withMemorySize (int 1536))
                                (.withTimeout (int 300))
                                (.withRuntime "java8")
                                (.withHandler "clojider.LambdaFn")
                                (.withCode (-> (FunctionCode.)
                                               (.withS3Bucket bucket-name)
                                               (.withS3Key "clojider.jar")))
                                (.withRole role-arn)))))

(defn install-lambda [{:keys [region file]}]
  (let [bucket-name (str "clojider-results-" region)
        role (str "clojider-role-" region)
        policy (str "clojider-policy-" region)
        role-arn (create-role-and-policy role policy bucket-name)]
    (create-results-bucket bucket-name region)
    (store-jar-to-bucket bucket-name file)
    (create-lambda-fn "clojider-load-testing-lambda" bucket-name region role-arn)))

(defn update-lambda [{:keys [region file]}]
  (let [bucket-name (str "clojider-results-" region)]
    (store-jar-to-bucket bucket-name file)
    (update-lambda-fn "clojider-load-testing-lambda" bucket-name region)))

(defn uninstall-lambda [{:keys [region]}]
  (let [bucket-name (str "clojider-results-" region)
        role (str "clojider-role-" region)
        policy (str "clojider-policy-" region)]
    (delete-role-and-policy role policy)
    (delete-lambda-fn "clojider-load-testing-lambda" region)
    (delete-all-objects-from-bucket bucket-name)
    (delete-results-bucket bucket-name)))
