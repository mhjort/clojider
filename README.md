# clojider

![alt text](https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Atlas_November_2005.jpg/220px-Atlas_November_2005.jpg "Large Hadron Collider")

Run clj-gatling load tests on your local machine or by utilizing AWS Lambda technology.

## Installation

### Basic setup

  Create new Clojure project & add the following to your `project.clj` `:dependencies`:

  ```clojure
  [clojider "0.2.1"]
  ```

  Add this setting to your `project.clj`

  ```clojure
  :main clojider.core
  ```

### AWS Lambda setup

  Note! Clojider has to setup one S3 bucket, IAM role & policy and Lambda function using your AWS credentials.
  The credentials are read from standard environment variables or configuration file. See details from  [here](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/set-up-creds.html).

  Add this setting to your `project.clj`

  ```clojure
  :uberjar-exclusions [#"scala.*"]
  ```

  Deploy your project to AWS Lambda

  ```sh
  lein uberjar
  lein run install -r <lambda-region> -f target/<your-uberjar-path>
  ```

### Writing tests

See [clj-gatling](https://github.com/mhjort/clj-gatling) on how to define test scenarios.

### Running tests

### Locally

  ```sh
  lein run load-local -c <concurrency> -d <duration-in-seconds> -s <simulation-symbol>
  ```

### Using AWS Lambda

  ```sh
  lein run load-lambda -r <lambda-region> -c <concurrency> -d <duration-in-seconds> -s <simulation-symbol>
  ```
