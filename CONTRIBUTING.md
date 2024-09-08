*This is a suggested `CONTRIBUTING.md` file template for use by open sourced Salesforce projects. The main goal of this file is to make clear the intents and expectations that end-users may have regarding this project and how/if to engage with it. Adjust as needed (especially look for `{project_slug}` which refers to the org and repo name of your project) and remove this paragraph before committing to your repo.*

# Contributing Guide For bazel-visibility-tool

This page lists the operational governance model of this project, as well as the recommendations and requirements for how to best contribute to bazel-visibility-tool. We strive to obey these as best as possible. As always, thanks for contributing – we hope these guidelines make it easier and shed some light on our approach and processes.

# Governance Model

## Published but not supported

The intent and goal of open sourcing this project is because it may contain useful or interesting code/concepts that we wish to share with the larger open source community. Although occasional work may be done on it, we will not be looking for or soliciting contributions.

# Getting started

Clone the repo.

## Setup Eclipse

Import into Eclipse using *File > Import... > Bazel > Import Bazel Workspace*.
Use [bazel_visibility_tool.bazelproject](bazel_visibility_tool.bazelproject) to conveniently import everything.

Setup launch configurations as need for the [VisibilityToolCli](tool/src/main/java/com/salesforce/tools/visibility/cli/VisibilityToolCli.java).

## Build & Run from Command Line

Ensure you have Bazelisk installed (`brew install bazelisk`).

Then you can build & run the visibiity tool:

```shell
bazel run :visibility-tool -- --help
```

# Issues, requests & ideas

Use GitHub Issues page to submit issues, enhancement requests and discuss ideas.

# Contribution Checklist

- [x] Clean, simple, well styled code
- [x] Commits should be atomic and messages must be descriptive. Related issues should be mentioned by Issue number.
- [x] Tests
  - The test suite, if provided, must be complete and pass
  - Increase code coverage, not versa.
- [x] Dependencies
  - Minimize number of dependencies.
  - Prefer Apache 2.0, BSD3, MIT, ISC and MPL licenses.
- [x] Reviews
  - Changes must be approved via peer code review

# Creating a Pull Request

1. **Ensure the bug/feature was not already reported** by searching on GitHub under Issues.  If none exists, create a new issue so that other contributors can keep track of what you are trying to add/fix and offer suggestions (or let you know if there is already an effort in progress).
3. **Clone** the forked repo to your machine.
4. **Create** a new branch to contain your work (e.g. `git br fix-issue-11`)
4. **Commit** changes to your own branch.
5. **Push** your work back up to your fork. (e.g. `git push fix-issue-11`)
6. **Submit** a Pull Request against the `main` branch and refer to the issue(s) you are fixing. Try not to pollute your pull request with unintended changes. Keep it simple and small.
7. **Sign** the Salesforce CLA (you will be prompted to do so when submitting the Pull Request)

> **NOTE**: Be sure to [sync your fork](https://help.github.com/articles/syncing-a-fork/) before making a pull request.


# Code of Conduct
Please follow our [Code of Conduct](CODE_OF_CONDUCT.md).

# License
By contributing your code, you agree to license your contribution under the terms of our project [LICENSE](LICENSE.txt) and to sign the [Salesforce CLA](https://cla.salesforce.com/sign-cla)