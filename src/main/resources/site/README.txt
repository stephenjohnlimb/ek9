repoindex.html needs to be copied to S3 bucket repo.ek9lang.org
All the other content needs to be copied to S3 bucket www.ek9lang.org

The idea is to have:
1. www.ek9lang.org as the site for manuals, the language and how to use the EK9 tooling.
2. repo.ek9lang.org as the read only repository open to the public to just use https to get zips and sha256 checksums.
3. search.ek9lang.org as an internet wide open site for developers to enter key words or parts of artefacts to find versions.
4. deploy.ek9lang.org as a limited access service that enables developers to signup and then deploy their packaged modules to the site.

The zips and sha256 files are just available via the web to all and it just takes a https get to get them. Uses cloud front for caching.

The search capability uses a combination of S3 meta-data and dynamoDB to hold an easily searchable db.

The deploy is a web site the developer can sign up to and also publish via POST zip/encrypted sha256 and public key for decrypting sha256.

Explore Amazon API gateway for API access.

AWS Cognito for user details user ids signing and signin via facebook etc.

AWS Lambdas for processing.

Fargate for the main web app where the user logs in to manage their organisation and the module names they own/manage and also
manage their own users/committers.