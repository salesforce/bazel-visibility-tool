package com.salesforce.tools.visibility.query;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Attribute;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Rule;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import com.google.idea.blaze.base.model.primitives.Label;

/**
 * A structure for working with {@link com.google.devtools.build.lib.query2.proto.proto2api.Build.Target#getRule()} and
 * its attributes.
 */
public class BazelRuleWithAttributes {

    /**
     * Workaround for https://github.com/bazelbuild/bazel/issues/20918
     */
    private static BinaryOperator<Attribute> firstOneWinsBazelDuplicateWorkaround = (first, second) -> first;

    public static BazelRuleWithAttributes forTarget(Target target) {
        return new BazelRuleWithAttributes(requireNonNull(target.getRule(), () -> "Target is not a rule: " + target));
    }

    private final Rule rule;

    private final Map<String, Attribute> attributesByAttributeName;

    private final Label label;

    BazelRuleWithAttributes(Rule rule) {
        this.rule = rule;
        // multiple attributes with the same name are not expected but can happen (https://github.com/bazelbuild/bazel/issues/20918)
        // we therefore use a merge function which discards all duplicates
        try {
            attributesByAttributeName = rule.getAttributeList()
                    .stream()
                    .collect(toMap(Attribute::getName, Function.identity(), firstOneWinsBazelDuplicateWorkaround));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    format(
                        "Error loading attributes of rule '%s(%s)'. There were duplicate attributes. Is this allowed?",
                        rule.getRuleClass(),
                        rule.getName()),
                    e);
        }

        label = Label.create(rule.getName());
    }

    public Boolean getBoolean(String name) {
        var attribute = attributesByAttributeName.get(name);
        if (attribute == null) {
            return null;
        }

        switch (attribute.getType()) {
            case BOOLEAN:
                return attribute.getBooleanValue();
            default:
                throw new IllegalArgumentException("Unexpected value type: " + attribute.getType());
        }
    }

    public boolean getBooleanOrDefault(String name, boolean defaultVaule) {
        var value = getBoolean(name);
        return value != null ? value : defaultVaule;
    }

    /**
     * @return the label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * @return value of the attribute name if present, otherwise {@link Label#targetName()} from the rule's label.
     */
    public String getName() {
        var name = getString("name");
        if (name != null) {
            return name;
        }

        return getLabel().targetName().toString();
    }

    Rule getRule() {
        return rule;
    }

    public String getRuleClass() {
        return rule.getRuleClass();
    }

    public String getString(String name) {
        var attribute = attributesByAttributeName.get(name);
        if (attribute == null) {
            return null;
        }

        switch (attribute.getType()) {
            case LABEL:
            case STRING:
                return attribute.getStringValue();
            default:
                throw new IllegalArgumentException("Unexpected value type: " + attribute.getType());
        }
    }

    public List<String> getStringList(String name) {
        var attribute = attributesByAttributeName.get(name);
        if (attribute == null) {
            return null;
        }

        switch (attribute.getType()) {
            case LABEL_LIST:
            case STRING_LIST:
                return attribute.getStringListValueList();
            default:
                throw new IllegalArgumentException("Unexpected value type: " + attribute.getType());
        }
    }

    /**
     * Returns <code>true</code> if there is an attribute named <code>tags</code> containing the specified tag.
     *
     * @param tag
     *            the tag to check
     * @return <code>true</code> if the attribute <code>tags</code> is present and has the specified tag,
     *         <code>false</code> otherwise
     */
    public boolean hasTag(String tag) {
        var tags = getStringList("tags");
        return (tags != null) && tags.contains(tag);
    }

    @Override
    public String toString() {
        return "BazelRuleWithAttributes [rule=" + rule + "]";
    }
}
