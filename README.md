[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9c1decbe5aad4ce3989eab10eab08e5d)](https://www.codacy.com/gh/codacy/codacy-sonar-visual-basic?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-sonar-visual-basic&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/codacy/codacy-sonar-visual-basic.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-sonar-visual-basic)

# Codacy Sonar Visual Basic

This is the docker engine we use at Codacy to run [SonarC#](https://github.com/SonarSource/sonar-dotnet) developed by SonarSource.

You can also create a docker to integrate the tool and language of your choice!
Check the **Docs** section for more information.

## Usage

### Publish the docker

```bash
make publish
docker build -t codacy-sonar-visual-basic .
```

### Run the docker

```bash
docker run --user=docker --rm=true -v <PATH-TO-CODE>:/src -v <PATH-TO>/.codacyrc:/.codacyrc codacy-sonar-visual-basic
```

> Make sure all the volumes mounted have the right permissions for user `docker`

### Generate Docs

**Requirements:**

-   `xmllint` utility installed on your system:

    -   On ubuntu:

            apt-get install libxml2-utils

    -   On alpine

            apk add libxml2-utils

**Generate:**

```sh
make documentation
```

## Docs

[Tool Developer Guide](https://support.codacy.com/hc/en-us/articles/207994725-Tool-Developer-Guide)

[Tool Developer Guide - Using Scala](https://support.codacy.com/hc/en-us/articles/207280379-Tool-Developer-Guide-Using-Scala)

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## What is Codacy?

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features:

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
test
