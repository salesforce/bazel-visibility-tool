# Visibility Tool for Bazel

Bazel has a built-in way to manage the dependency graph.
You can read more about [Bazel Visibility here](https://bazel.build/concepts/visibility).

However, in a large workspace, things can get overwhelming very quickly.
This is where the Visibility Tool comes to the rescue.

The Visibility Tool provides the following features:

1. Centralized definition of visibility layers and dependency rules using Starlark.
2. Centralized allow and block list management.
3. Reporting of violation in [SARIF](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html) format.
4. Tools for querying and extracting group and membership information.

It's very helpful when you are dealing with a large legacy code base where it's not always easy or desirable to move code around for easier visibility management with Bazel.

## Usage

### Setup in WORKSPACE

Please see the GitHub releases for snippet how to embed into `WORKSPACE` or `MODULE.bazel` file.

Note, there are a few important conventions that should be followed:

1. Use `@bazel_visibility_tool` as the external repository name

2. Create your visibility tools in package `//tools/build/visibility` in the `BUILD.bazel` file.

If you don't follow these the world will not end but you will run into a few issues because some defaults don't work out of the box.

### Invoke CLI

```shell
> bazel run @bazel_visibility_tools//:cli -- --help
...

tbd.
```

## Concepts

In order to use the tool you need to familiarize yourself with a few concepts.

The Visibility Tools exists to assist with Bazel's [Visibility concept](https://bazel.build/concepts/visibility).
It focuses on target visibility assistance.
In order to provide assistance the tool relies on a few assumptions and conventions.

- Bazel targets/packages are organized in groups (Visibility Groups).

- Each target of a group must use programmatic way to compute its `visibility` attribute value.
  For third party dependencies integration to `bazel_maven_deps` is available.

- Visibility must be explicitly granted.
  The default is subpackages, i.e. targets are only visible within the package and its subpackages.

- The Visibility Tool can query up-to-date group and membership information.

### Visibility Groups (aka. Layers & Components)

Visibility groups are used to define applications, layers and components in a Bazel workspace.
You can define any logical grouping you want.
They organize code into logical components.

Visibility groups define metadata for generating package groups as well as looking up group membership for targets at analysis time.

Each Bazel package (and target) must be listed in at most one visibility group.
This allows to reduce cognitive load and avoid dealing with complicated AND, OR, XOR or XAND situations.
It also makes it easier to read the rules.
They can rely on visibility being explicit.

In addition to regular package groups, visibility groups also allow to group external repositories.
However, their use is limited as they won't be represented in Bazel as a package group.
They can be used for membership lookup at analysis time, which allows to compute their `visibility` attributes.

Visibility is defined in the context of a group, i.e. each group defines which other groups it is visible to.
This enforces the architecture to be defined at once, i.e. it is not possible to extend visibility dynamically.

Visibility groups and their visibility are defined in Starlark like this:

```starlark

visibility_group(
    name = "app-frontend-api",
    visible_to = [
        # to any other api module in the frontend
        "app-frontend-api",
    ],
)

visibility_group(
    name = "app-frontend-internal"
)

visibility_group(
    name = "app-frontend-tests"
)

visibility_group(
    name = "app-backend-api"
    visible_to = [
        # to any other api module in the backend
        "app-backend-api",
    ],
)

visibility_group(
    name = "app-backend-internal"
)

visibility_group(
    name = "app-backend-tests"
)

visibility_group(
    name = "app-shared-libraries"
    visible_to = [                  
        # visibile to any target in the entire app layer (it is required to be explicit here, no wildcards)
        ":app-frontend-api",
        ":app-frontend-internal",
        ":app-frontend-test",
        ":app-backend-api",
        ":app-backend-internal",
        ":app-backend-test",
    ],
)

visibility_group(
    name = "app-shared-libraries-tests"
)

visibility_group(
    name = "test-utilities"
    visible_to = [
        # visible to any test group and the within test-utilities group
        ":test-utilities",
        ":app-frontend-tests",
        ":app-backend-tests",
        ":app-shared-libraries-tests",
    ],
)

# defining a group for external libraries we don't want to allow anyone to depend on
visibility_group(
    name = "forbidden-external-libraries"
    visible_to = [], # not visibile to any other group (can be omitted, empty list is the default)
    visibility_allow_list = "//tools/build/visibility/allowlists:forbidden_libraries",  # a managed allowlist with package names
)

# define additional information for bazel_maven_deps integration to apply visibility to selected targets
maven_deps_visibility_info(
    group = ":forbidden-external-libraries",
    include_patterns = [
        "@*lombok*",
        "@org_reflections*",
    ],
)
```

The `name` will be used to identify the visibility group when computing the `visibility` for a specific target.

Visibility groups will be mapped to package groups (`package_group`) by the Visibility Tool.
The Visibility Tool need to be executed whenever the build graph changes to refresh the package groups.

The `visible_to` list contains a list of group labels to which the group will be made visible to.

The `visibility_allow_list` is label reference to a `package_group` providing additional visibility.
This is useful in case you want to prohibit access in general but have some legacy code.
The visibility tool (together with buildozer) can be used to manage those lists.
It has build in support for querying the build graph and re-computing the allow lists.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

