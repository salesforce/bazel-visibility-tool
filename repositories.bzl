"""
 Load the pinned Maven artifacts.

 This file defines the loading of dependencies required by bazel_maven_deps itself.
"""

#load("//third_party/dependencies/pinned_catalog:index.bzl", "setup_maven_dependencies")

_DEFAULT_REPOSITORIES = [
    "https://repo.maven.apache.org/maven2/",
]

def bazel_visibility_tool_repositories(repositories = _DEFAULT_REPOSITORIES):
    """Loads dependencies needed to build and run the tools.

    Note, users must load rules_jvm_external repo first in their WORKSPACE:

    Args:
      repositories: list of Maven repos
    """

    # Maven Dependencies
    #setup_maven_dependencies(repositories)
    pass
