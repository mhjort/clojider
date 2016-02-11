# clojider

![alt text](https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Atlas_November_2005.jpg/220px-Atlas_November_2005.jpg "Large Hadron Collider")

Run clj-gatling load tests on your local machine or by utilizing AWS Lambda technology.

## Installation

### Basic setup

  Create new Clojure project & add the following to your `project.clj` `:dependencies`:

  ```clojure
  [clojider "0.2.2"]
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

You can find few simple examples [here](https://github.com/mhjort/clojider/blob/master/src/clojider/examples.clj).

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

  And when you have updated your simulation (the scenario code), you have to update latest code to Lambda via

  ```sh
  lein uberjar
  lein run update -r <lambda-region> -f target/<your-uberjar-path>
  ```

## Uninstall

This will uninstall all created AWS resources (S3 bucket, role, policy and Lambda function).
The cost of keeping these available in your account for the next load testing run is almost zero.
Lambda pricing is totally based on the usage and S3 bucket contains only smallish old result files.
However, I still wanted to have an option to destroy everything when you don't need the tool anymore.

  ```sh
  lein run uninstall -r <lambda-region>
  ```
