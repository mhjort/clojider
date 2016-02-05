(ns clojider.aws
  (:require [cheshire.core :refer [generate-string]])
  (:import [com.amazonaws.services.identitymanagement AmazonIdentityManagementClient]
           [com.amazonaws.services.identitymanagement.model AttachRolePolicyRequest
                                                            CreatePolicyRequest
                                                            CreateRoleRequest]
           [com.amazonaws.services.lambda AWSLambdaClient]
           [com.amazonaws.services.lambda.model CreateFunctionRequest
                                                FunctionCode]
           [com.amazonaws.auth BasicAWSCredentials]
           [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]
           [java.io File]))

(def aws-credentials
  (BasicAWSCredentials. (System/getenv "AWS_ACCESS_KEY_ID")
                        (System/getenv "AWS_SECRET_ACCESS_KEY")))

(defn- create-results-bucket [bucket-name region]
  (let [client (AmazonS3Client. aws-credentials)]
    (.createBucket client bucket-name region)))

(defn- store-jar-to-bucket [bucket-name]
  (let [client (AmazonS3Client. aws-credentials)]
  (.putObject client
              bucket-name
              "clojider-0.1.6-standalone.jar"
              (File. "target/clojider-0.1.6-standalone.jar"))))

(def role
  {:Version "2012-10-17"
   :Statement {:Effect "Allow"
                :Principal {:Service "lambda.amazonaws.com"}
                :Action "sts:AssumeRole"}})

(defn policy [bucket-name]
  {:Version "2012-10-17"
   :Statement [{:Effect "Allow"
                :Action ["s3:PutObject"]
                :Resource [(str "arn:aws:s3:::" bucket-name)]}
               {:Effect "Allow"
                :Action ["logs:CreateLogGroup"
                         "logs:CreateLogStream"
                         "logs:PutLogEvents"]
                :Resource ["arn:aws:logs:*:*:*"]}]})

(defn create-role-and-policy [role-name policy-name bucket-name]
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

(defn create-lambda [lambda-name region role-arn]
  (let [client (-> (AWSLambdaClient. aws-credentials)
                   (.withRegion (Regions/fromName region)))]
    (.createFunction client (-> (CreateFunctionRequest.)
                                (.withFunctionName lambda-name)
                                (.withMemorySize (int 1536))
                                (.withTimeout (int 300))
                                (.withRuntime "java8")
                                (.withHandler "clojider.LambdaFn")
                                (.withCode (-> (FunctionCode.)
                                               (.withS3Bucket "trolo")
                                               (.withS3Key "clojider-0.1.6-standalone.jar")))
                                (.withRole role-arn)))))

(defn init [region]
  (let [bucket-name (str "clojider-results-" region)
        role (str "clojider-role-" region)
        policy (str "clojider-policy-" region)
        role-arn (create-role-and-policy role policy bucket-name)]
    (create-results-bucket bucket-name region)
    (store-jar-to-bucket bucket-name)
    (create-lambda "clojider-load-testing-lambda" region role-arn)))

;(init "eu-west-1")
