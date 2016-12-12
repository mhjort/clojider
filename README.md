# clojider

![alt text](https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Atlas_November_2005.jpg/220px-Atlas_November_2005.jpg "Large Hadron Collider")

Run clj-gatling load tests on your local machine or by utilizing AWS Lambda technology.

Note! Clojider 0.4.x supports latest [clj-gatling](https://github.com/mhjort/clj-gatling) 0.8 format tests.

## Features

* Test your system using realistic scenarios. Not just one url.
* Write test scenarios in Clojure. No special DSL.
* Run your tests either from local machine or using multiple nodes using AWS Lambda technology.

## Installation

### Basic setup

  Create new Clojure project & add the following to your `project.clj` `:dependencies`:

  ```clojure
  [clojider "0.4.3"]
  ```

  Add this setting to your `project.clj`

  ```clojure
  :main clojider.core
  ```

### AWS Lambda setup

  Note! Clojider has to setup one S3 bucket, IAM role & policy and Lambda function using your AWS credentials. S3 bucket name you have to configure, other resources will be auto-named.
  The credentials are read from standard environment variables or configuration file. See details from  [here](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/set-up-creds.html).

  Add this setting to your `project.clj`. This is required because report generation uses Gatling report generation [module](https://github.com/gatling/gatling-highcharts) which is Scala code.
  Report generation happens in local machine and this setting prevents Scala code to be included in to Jar file that is deployed to AWS Lambda environment.

  ```clojure
  :uberjar-exclusions [#"scala.*"]
  ```

  Deploy your project to AWS Lambda.
  Note! Lambda is available in these regions: eu-west-1, us-east-1, us-west-2 and ap-northeast-1.

  ```sh
  lein uberjar
  lein run install -r <lambda-region> -b <s3-bucket-name> -f target/<your-uberjar-path>
  ```

## Writing tests

You can find few simple examples [here](https://github.com/mhjort/clojider/blob/master/src/clojider/examples.clj)
which you run locally in a following way.

```sh
  lein run load-local -c 5 -d 10 -s clojider.examples/ping-simulation
```
or

```sh
  lein run load-local -c 5 -d 10 -s clojider.examples/metrics-simulation
```

See [clj-gatling](https://github.com/mhjort/clj-gatling) on how to define test scenarios.

## Running tests

### Locally

  ```sh
  lein run load-local -c <concurrency> -d <duration-in-seconds> -s <simulation-symbol>
  ```

### Using AWS Lambda

  ```sh
  lein run load-lambda -r <lambda-region> -b <s3-bucket-name> -c <concurrency> -d <duration-in-seconds> -s <simulation-symbol>
  ```

  And when you have updated your simulation (the scenario code), you have to update latest code to Lambda via

  ```sh
  lein uberjar
  lein run update -r <lambda-region> -b <s3-bucket-name> -f target/<your-uberjar-path>
  ```

### Optional parameters

  * `-t` or `--timeout`` specifies request timeout in milliseconds. By default it is 5000 ms.

## Uninstall

This will uninstall all created AWS resources (S3 bucket, role, policy and Lambda function).
The cost of keeping these available in your account for the next load testing run is almost zero.
Lambda pricing is totally based on the usage and S3 bucket contains only smallish old result files.
However, I still wanted to have an option to destroy everything when you don't need the tool anymore.

  ```sh
  lein run uninstall -r <lambda-region> -b <s3-bucket-name>
  ```



## Contribute

Use [GitHub issues](https://github.com/mhjort/clojider/issues) and [Pull Requests](https://github.com/mhjort/clojider/pulls).
