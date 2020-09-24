/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.basepom.mojo.dvc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Matches the group-id/artifact-id pair of a qualified name. May contain wildcards (*) in either group-id and artifact-id.
 */
public final class QualifiedNameMatcher
{

    private static final Pattern WILDCARD_REGEXP = Pattern.compile("[^*]+|(\\*)");
    private static final Pattern WILDCARD_MATCH = Pattern.compile(".*");

    private final Pattern groupPattern;
    private final Pattern artifactPattern;

    public static QualifiedNameMatcher fromQualifiedName(final QualifiedName name)
    {
        checkNotNull(name, "name is null");

        return new QualifiedNameMatcher(name.getMinimalName());
    }

    public QualifiedNameMatcher(final String pattern)
    {
        checkNotNull(pattern, "pattern is null");
        final List<String> elements = Splitter.on(':').trimResults().splitToList(pattern);
        checkState(elements.size() > 0 && elements.size() < 3, "Pattern %s is not a valid inclusion pattern!", pattern);

        this.groupPattern = compileWildcard(elements.get(0).trim());
        this.artifactPattern = compileWildcard(elements.size() > 1 ? elements.get(1).trim() : ""); // use wildcard match if no artifact present
    }

    public boolean matches(QualifiedName artifactName)
    {
        checkNotNull(artifactName, "artifactName is null");

        return groupPattern.matcher(artifactName.getGroupId()).matches()
                && artifactPattern.matcher(artifactName.getArtifactId()).matches();
    }

    @VisibleForTesting
    static Pattern compileWildcard(final String wildcard)
    {
        if (wildcard.isEmpty()) {
            return WILDCARD_MATCH;
        }

        final Matcher m = WILDCARD_REGEXP.matcher(wildcard);
        final StringBuffer b = new StringBuffer();
        while (m.find()) {
            if (m.group(1) != null) {
                m.appendReplacement(b, ".*");
            }
            else {
                m.appendReplacement(b, "\\\\Q" + m.group(0) + "\\\\E");
            }
        }
        m.appendTail(b);
        return Pattern.compile(b.toString());
    }
}
