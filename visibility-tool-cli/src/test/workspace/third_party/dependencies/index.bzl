load("//third_party/dependencies:org_junit.bzl", _org_junit_boms = "MAVEN_BOM_IMPORTS", _org_junit_deps = "MAVEN_DEPENDENCIES", _org_junit_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_projectlombok.bzl", _org_projectlombok_boms = "MAVEN_BOM_IMPORTS", _org_projectlombok_deps = "MAVEN_DEPENDENCIES", _org_projectlombok_exclusions = "MAVEN_EXCLUSIONS")

#
# Index of Maven dependencies used in the CRM Core App build
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Create a TODO file or GUS work item to capture technical debt.
#
# https://sfdc.co/graph-tool
#

MAVEN_BOM_IMPORTS = []
MAVEN_BOM_IMPORTS += org_junit_boms
MAVEN_BOM_IMPORTS += org_projectlombok_boms

MAVEN_DEPENDENCIES = []
MAVEN_DEPENDENCIES += org_junit_deps
MAVEN_DEPENDENCIES += org_projectlombok_deps

MAVEN_EXCLUSIONS = []
MAVEN_EXCLUSIONS += org_junit_exclusions
MAVEN_EXCLUSIONS += org_projectlombok_exclusions
