/*-
 * Copyright (c) {year} Salesforce.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.idea.blaze.base.model.primitives;

class StringUtil {

    public static String trimEnd(String value, String suffixToRemove) {
        if (value.endsWith(suffixToRemove)) {
            return value.substring(0, value.length() - suffixToRemove.length());
        }
        return value;
    }

    public static String trimStart(String value, String prefixToRemove) {
        if (value.startsWith(prefixToRemove)) {
            return value.substring(prefixToRemove.length());
        }

        return value;
    }

}
