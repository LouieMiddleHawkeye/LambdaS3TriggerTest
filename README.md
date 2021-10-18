# LambdaS3TriggerTest

## Note

All the README after the 2nd LambdaS3TriggerTest that amazon provided. Some of it I haven't looked at so I'm going to leave it in.

## Buckets

Make sure you have an input bucket _and_ an output bucket as otherwise your output file will trigger the lambda function again and you'll end up with a never ending loop of lambda invocations. 

## IAM
The policies and roles I made can be seen on AWS  (pretty basic, just s3 read write). I also had to give my IAM user lambda permissions to create functions and test them.
I currently have CloudWatch enabled, so I can see logs, but will want to change to datadog I assume if used in prod. 

## Building 

In the `build.gradle` there is a task that'll generate a zip that can be used to upload the code to AWS lambda. Run `aws lambda create-function  --function-name HelloWorld --zip-file fileb://HelloWorldFunction\build\distributions\HelloWorldFunction.zip --handler example.handler --runtime j
ava11 --timeout 10 --memory-size 1024  --role arn:aws:iam::859718945433:role/lambda-s3-role --region eu-west-1 --cli-connect-timeout 6000` to create the lambda function. 

## Testing

To test with the events files run `aws lambda invoke --function-name HelloWorld --invocation-type Event --payload file://events/inputFile.txt outputFile.txt --region eu-west-1 --cli-binary-format raw-in-base6
4-out`. This will use an _existing_ object from s3 bucket to be used in the lambda function. 

To update the code of the lambda function run `aws lambda update-function-code --function-name HelloWorld --zip-file fileb://HelloWorldFunction/build/distributions/HelloWorldFunction.zip`.

You can update config like so `aws lambda update-function-configuration --function-name HelloWorld --handler helloworld.App` (the handler is the package.ClassName)

You can add permissions to your function like so `aws lambda add-permission --function-name HelloWorld --principal s3.amazonaws.com --statement-id s3invoke --action "lambda:InvokeFunction"  --source-arn arn:aws:s3:::louiem-
test --source-account 859718945433 --region eu-west-1`.

You can get current policies like so `aws lambda get-policy --function-name HelloWorld`.

Of course, you can also just add files to the s3 bucket which _should_ work.

## S3 path problem

Can't use regex or wildcards to try and say which prefix/suffix to trigger the function on. This is annoying. Probably easiest to change the file suffix or the files to include whether it is a summary minute or summary segment. This is quite highly coupled though. Not sure what else we could do.
Also, anything with that suffix in the bucket would trigger the function, so would need to keep the spec up to date.

The other option is to have it trigger on _every_ delete in that bucket, and then decide whether to do anything in the code. This will lead to a lot more triggers, but might be easier. Also, the amount of deletions in reality should hopefully not be that many.

# LambdaS3TriggerTest

This project contains source code and supporting files for a serverless application that you can deploy with the SAM CLI. It includes the following files and folders.

- HelloWorldFunction/src/main - Code for the application's Lambda function.
- events - Invocation events that you can use to invoke the function.
- HelloWorldFunction/src/test - Unit tests for the application code (TODO)
- template.yaml - A template that defines the application's AWS resources. (I haven't looked at this all too much)

The application uses several AWS resources, including Lambda functions and an API Gateway API. These resources are defined in the `template.yaml` file in this project. You can update the template to add AWS resources through the same deployment process that updates your application code.

If you prefer to use an integrated development environment (IDE) to build and test your application, you can use the AWS Toolkit.  
The AWS Toolkit is an open source plug-in for popular IDEs that uses the SAM CLI to build and deploy serverless applications on AWS. The AWS Toolkit also adds a simplified step-through debugging experience for Lambda function code. See the following links to get started.

* [CLion](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [GoLand](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [IntelliJ](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [WebStorm](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [Rider](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [PhpStorm](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [PyCharm](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [RubyMine](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [DataGrip](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [VS Code](https://docs.aws.amazon.com/toolkit-for-vscode/latest/userguide/welcome.html)
* [Visual Studio](https://docs.aws.amazon.com/toolkit-for-visual-studio/latest/user-guide/welcome.html)

## Deploy the sample application

The Serverless Application Model Command Line Interface (SAM CLI) is an extension of the AWS CLI that adds functionality for building and testing Lambda applications. It uses Docker to run your functions in an Amazon Linux environment that matches Lambda. It can also emulate your application's build environment and API.

To use the SAM CLI, you need the following tools.

* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Java11 - [Install the Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

To build and deploy your application for the first time, run the following in your shell:

```bash
sam build
sam deploy --guided // TODO Haven't used this yet
```

The first command will build the source of your application. The second command will package and deploy your application to AWS, with a series of prompts:

* **Stack Name**: The name of the stack to deploy to CloudFormation. This should be unique to your account and region, and a good starting point would be something matching your project name.
* **AWS Region**: The AWS region you want to deploy your app to.
* **Confirm changes before deploy**: If set to yes, any change sets will be shown to you before execution for manual review. If set to no, the AWS SAM CLI will automatically deploy application changes.
* **Allow SAM CLI IAM role creation**: Many AWS SAM templates, including this example, create AWS IAM roles required for the AWS Lambda function(s) included to access AWS services. By default, these are scoped down to minimum required permissions. To deploy an AWS CloudFormation stack which creates or modifies IAM roles, the `CAPABILITY_IAM` value for `capabilities` must be provided. If permission isn't provided through this prompt, to deploy this example you must explicitly pass `--capabilities CAPABILITY_IAM` to the `sam deploy` command.
* **Save arguments to samconfig.toml**: If set to yes, your choices will be saved to a configuration file inside the project, so that in the future you can just re-run `sam deploy` without parameters to deploy changes to your application.

You can find your API Gateway Endpoint URL in the output values displayed after deployment.

## Use the SAM CLI to build and test locally

Build your application with the `sam build` command.

```bash
LambdaS3TriggerTest$ sam build
```

The SAM CLI installs dependencies defined in `HelloWorldFunction/build.gradle`, creates a deployment package, and saves it in the `.aws-sam/build` folder.

Test a single function by invoking it directly with a test event. An event is a JSON document that represents the input that the function receives from the event source. Test events are included in the `events` folder in this project.

Run functions locally and invoke them with the `sam local invoke` command.

```bash
LambdaS3TriggerTest$ sam local invoke HelloWorldFunction --event events/event.json
```

NOTE: You might need to add region e.g `--region eu-west-1`

The SAM CLI can also emulate your application's API. Use the `sam local start-api` to run the API locally on port 3000.

NOTE: Not tried this

```bash
LambdaS3TriggerTest$ sam local start-api
LambdaS3TriggerTest$ curl http://localhost:3000/
```

The SAM CLI reads the application template to determine the API's routes and the functions that they invoke. The `Events` property on each function's definition includes the route and method for each path.

```yaml
      Events:
        HelloWorld:
          Type: Api
          Properties:
            Path: /hello
            Method: get
```

## Add a resource to your application
The application template uses AWS Serverless Application Model (AWS SAM) to define application resources. AWS SAM is an extension of AWS CloudFormation with a simpler syntax for configuring common serverless application resources such as functions, triggers, and APIs. For resources not included in [the SAM specification](https://github.com/awslabs/serverless-application-model/blob/master/versions/2016-10-31.md), you can use standard [AWS CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) resource types.

## Fetch, tail, and filter Lambda function logs

To simplify troubleshooting, SAM CLI has a command called `sam logs`. `sam logs` lets you fetch logs generated by your deployed Lambda function from the command line. In addition to printing the logs on the terminal, this command has several nifty features to help you quickly find the bug.

`NOTE`: This command works for all AWS Lambda functions; not just the ones you deploy using SAM.

```bash
LambdaS3TriggerTest$ sam logs -n HelloWorldFunction --stack-name LambdaS3TriggerTest --tail
```

You can find more information and examples about filtering Lambda function logs in the [SAM CLI Documentation](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-logging.html).

## Unit tests

TODO: How to test lambda functions

Tests are defined in the `HelloWorldFunction/src/test` folder in this project.

```bash
LambdaS3TriggerTest$ cd HelloWorldFunction
HelloWorldFunction$ gradle test
```

## Cleanup

To delete the sample application that you created, use the AWS CLI. Assuming you used your project name for the stack name, you can run the following:

```bash
aws cloudformation delete-stack --stack-name LambdaS3TriggerTest
```

## Resources

See the [AWS SAM developer guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html) for an introduction to SAM specification, the SAM CLI, and serverless application concepts.

Next, you can use AWS Serverless Application Repository to deploy ready to use Apps that go beyond hello world samples and learn how authors developed their applications: [AWS Serverless Application Repository main page](https://aws.amazon.com/serverless/serverlessrepo/)
