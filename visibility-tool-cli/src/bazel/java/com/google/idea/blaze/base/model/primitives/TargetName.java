/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.google.idea.blaze.base.model.primitives;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

/** The target name part of a label */
public final class TargetName {

    // This is a subset of the allowable target names in Blaze
    private static final String ALNUM_REGEX_STR = "[a-zA-Z0-9]*";
    private static final Pattern ALNUM_REGEX = Pattern.compile(ALNUM_REGEX_STR);

    // Rule names must be alpha-numeric or consist of the following allowed chars:
    // (note, rule names can also contain '/'; we handle that case separately)
    private static final ImmutableSet<Character> ALLOWED_META =
            ImmutableSet.of('+', '_', ',', '=', '-', '.', '@', '~', '#', ' ', '(', ')', '$', '!');

    public static TargetName create(String targetName) {
        var error = validate(targetName);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return new TargetName(targetName);
    }

    /** Silently returns null if the string is not a valid target name. */
    @Nullable
    public static TargetName createIfValid(String targetName) {
        return validate(targetName) == null ? new TargetName(targetName) : null;
    }

    /**
     * Validates a rule name using the same logic as Blaze. Returns null on success or an error message otherwise.
     */
    @Nullable
    public static String validate(String targetName) {
        if (targetName.isEmpty()) {
            return "target names cannot be empty";
        }
        // Forbidden start chars:
        if (targetName.charAt(0) == '/') {
            return "Invalid target name: " + targetName + "\n" + "target names may not start with \"/\"";
        }
        if (targetName.charAt(0) == '.') {
            if (targetName.equals(".")) {
                return null;
            }
            if (targetName.startsWith("../") || targetName.equals("..")) {
                return "Invalid target name: " + targetName + "\n"
                        + "target names may not contain up-level references \"..\"";
            }
            if (targetName.startsWith("./")) {
                return "Invalid target name: " + targetName + "\n"
                        + "target names may not contain \".\" as a path segment";
            }
        }

        for (var i = 0; i < targetName.length(); ++i) {
            var c = targetName.charAt(i);
            if (ALLOWED_META.contains(c)) {
                continue;
            }
            if (c == '/') {
                // Forbidden substrings: "/../", "/./", "//"
                if (targetName.contains("/../")) {
                    return "Invalid target name: " + targetName + "\n"
                            + "target names may not contain up-level references \"..\"";
                }
                if (targetName.contains("/./")) {
                    return "Invalid target name: " + targetName + "\n"
                            + "target names may not contain \".\" as a path segment";
                }
                if (targetName.contains("//")) {
                    return "Invalid target name: " + targetName + "\n"
                            + "target names may not contain \"//\" path separators";
                }
                continue;
            }
            var isAlnum = ALNUM_REGEX.matcher(String.valueOf(c)).matches();
            if (!isAlnum) {
                return "Invalid target name: " + targetName + "\n" + "target names may not contain " + c;
            }
        }

        // Forbidden end chars:
        if (targetName.endsWith("/..")) {
            return "Invalid target name: " + targetName + "\n"
                    + "target names may not contain up-level references \"..\"";
        }
        if (targetName.endsWith("/.")) {
            return null;
        }
        if (targetName.endsWith("/")) {
            return "Invalid target name: " + targetName + "\n" + "target names may not end with \"/\"";
        }
        return null;
    }

    private final String name;

    private TargetName(String ruleName) {
        this.name = ruleName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TargetName) {
            var that = (TargetName) obj;
            return Objects.equal(name, that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
