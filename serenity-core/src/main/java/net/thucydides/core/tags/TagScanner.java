package net.thucydides.core.tags;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.annotations.TestAnnotations;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.util.EnvironmentVariables;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TagScanner {

    private final List<TestTag> providedTags;

    public TagScanner(EnvironmentVariables environmentVariables) {
        this.providedTags = tagsProvidedBy(environmentVariables);
    }

    public boolean shouldRunForTags(List<String> tags) {
        if (providedTags.isEmpty()) { return true; }

        return tagsMatchAPositiveTag(tags, providedTags) && !tagsMatchANegativeTag(tags, providedTags);
    }

    public boolean shouldRunClass(Class<?> testClass) {
        if (providedTags.isEmpty()) { return true; }

        return testClassMatchesAPositiveTag(testClass, providedTags)
               && testClassDoesNotMatchANegativeTag(testClass, providedTags);
    }

    public boolean shouldRunMethod(Class<?> testClass, String methodName) {
        if (!isATaggable(testClass)) { return true; }
        if (providedTags.isEmpty()) { return true; }

        return testMethodMatchesAPositiveTag(testClass, methodName, providedTags)
                && testMethodDoesNotMatchANegativeTag(testClass, methodName, providedTags);
    }

    // Cucumber and JBehave have their own filtering mechanisms.
    // If the default tag scanner is applied to these test runners, it will interfere
    // with the real filtering.
    private boolean isATaggable(Class<?> testClass) {
        RunWith runWith = testClass.getAnnotation(RunWith.class);
        return (runWith != null && Taggable.class.isAssignableFrom(runWith.value()));
    }

    private boolean testClassMatchesAPositiveTag(Class<?> testClass, List<TestTag> expectedTags) {
        List<TestTag> tags = TestAnnotations.forClass(testClass).getTags();
        return containsAPositiveMatch(expectedTags, tags);
    }

    private boolean tagsMatchAPositiveTag(List<String> tagValues, List<TestTag> expectedTags) {
        List<TestTag> tags = definedIn(tagValues);
        return containsAPositiveMatch(expectedTags, tags);
    }

    private boolean tagsMatchANegativeTag(List<String> tagValues, List<TestTag> expectedTags) {
        List<TestTag> tags = definedIn(tagValues);
        return containsANegativeMatch(expectedTags, tags);
    }

    private List<TestTag> definedIn(List<String> tagValues) {

        return tagValues.stream()
                .map(TestTag::withValue)
                .collect(Collectors.toList());
    }

    private boolean containsAPositiveMatch(List<TestTag> expectedTags, List<TestTag> tags) {
        return positive(expectedTags).isEmpty() || tagsMatch(positive(expectedTags), tags);
    }

    private boolean testClassDoesNotMatchANegativeTag(Class<?> testClass, List<TestTag> negatedTags) {
        List<TestTag> tags = TestAnnotations.forClass(testClass).getTags();
        return !containsANegativeMatch(negatedTags, tags);
    }

    private List<TestTag> positive(List<TestTag> tags) {
        List<TestTag> positiveTags = new ArrayList<>();
        for (TestTag tag : tags) {
            if (!isANegative(tag)) {
                positiveTags.add(tag);
            }
        }
        return positiveTags;
    }

    private boolean isANegative(TestTag tag) {
        return tag.getType().startsWith("~");
    }

    private List<TestTag> negative(List<TestTag> tags) {
        List<TestTag> negativeTags = new ArrayList<>();
        for (TestTag tag : tags) {
            if (isANegative(tag)) {
                negativeTags.add(TestTag.withName(tag.getName()).andType(tag.getType().substring(1)));
            }
        }
        return negativeTags;
    }


    private boolean testMethodMatchesAPositiveTag(Class<?> testClass, String methodName, List<TestTag> expectedTags) {
        List<TestTag> tags = TestAnnotations.forClass(testClass).getTagsForMethod(methodName);
        return containsAPositiveMatch(expectedTags, tags);
    }

    private boolean testMethodDoesNotMatchANegativeTag(Class<?> testClass, String methodName, List<TestTag> expectedTags) {
        List<TestTag> tags = TestAnnotations.forClass(testClass).getTagsForMethod(methodName);
        return !containsANegativeMatch(expectedTags, tags);
    }

    private boolean containsANegativeMatch(List<TestTag> expectedTags, List<TestTag> tags) {
        if (negative(expectedTags).isEmpty()) {return false;}

        return tagsMatch(negative(expectedTags), tags);
    }


    private boolean tagsMatch(List<TestTag> expectedTags, List<TestTag> tags) {
        for (TestTag expectedTag : expectedTags) {
            if (tags.contains(expectedTag)) {
                return true;
            }
        }
        return false;
    }

    private List<TestTag> tagsProvidedBy(EnvironmentVariables environmentVariables) {
        String tagListValue = environmentVariables.getProperty(ThucydidesSystemProperty.TAGS);
        if (StringUtils.isNotEmpty(tagListValue)) {
            List<String> tagList = Lists.newArrayList(Splitter.on(",").trimResults().split(tagListValue));
            return tagList.stream()
                    .map(TestTag::withValue)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
}
