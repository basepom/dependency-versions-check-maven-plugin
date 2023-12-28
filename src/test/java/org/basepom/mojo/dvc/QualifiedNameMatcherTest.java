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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class QualifiedNameMatcherTest {

    @Test
    public void testEmpty() {
        Pattern matchEmpty = QualifiedNameMatcher.compileWildcard("");

        assertTrue(matchEmpty.matcher("").matches());
        assertTrue(matchEmpty.matcher("abc").matches());
        assertTrue(matchEmpty.matcher("Hello, World").matches());
    }

    @Test
    public void testWildcard() {
        Pattern matchWildcard = QualifiedNameMatcher.compileWildcard("*");

        assertTrue(matchWildcard.matcher("").matches());
        assertTrue(matchWildcard.matcher("abc").matches());
        assertTrue(matchWildcard.matcher("Hello, World").matches());
    }

    @Test
    public void testTailWildcard() {
        Pattern matchTail = QualifiedNameMatcher.compileWildcard("a*");

        assertFalse(matchTail.matcher("").matches());
        assertTrue(matchTail.matcher("abc").matches());
        assertFalse(matchTail.matcher("Hello, World").matches());
        assertFalse(matchTail.matcher("Stuff before the a and after").matches());
    }

    @Test
    public void testHeadWildcard() {
        Pattern matchHead = QualifiedNameMatcher.compileWildcard("*d");

        assertFalse(matchHead.matcher("").matches());
        assertFalse(matchHead.matcher("abc").matches());
        assertTrue(matchHead.matcher("Hello, World").matches());

        assertFalse(matchHead.matcher("this is text before d and after it").matches());
    }

    @Test
    public void testMiddleWildcard() {
        Pattern matchMiddle = QualifiedNameMatcher.compileWildcard("H*d");

        assertFalse(matchMiddle.matcher("").matches());
        assertFalse(matchMiddle.matcher("abc").matches());
        assertTrue(matchMiddle.matcher("Hello, World").matches());
    }

    @Test
    public void testDoubleWildcard() {
        Pattern matchDouble = QualifiedNameMatcher.compileWildcard("H*o, W*d");

        assertFalse(matchDouble.matcher("").matches());
        assertFalse(matchDouble.matcher("abc").matches());
        assertTrue(matchDouble.matcher("Hello, World").matches());
        assertTrue(matchDouble.matcher("Ho, Wd").matches());
        assertFalse(matchDouble.matcher("Yoh, World").matches());
    }

    @Test
    public void testDots() {
        Pattern matchDots = QualifiedNameMatcher.compileWildcard("org.apache");

        assertFalse(matchDots.matcher("org_apache").matches());
        assertTrue(matchDots.matcher("org.apache").matches());
        assertFalse(matchDots.matcher("somewhere in org.apache").matches());
    }

    @Test
    public void testQualifiedNameMatchers() {
        QualifiedName name = new QualifiedName("the.test.group", "just-an-artifact", null, null);

        QualifiedNameMatcher exact = QualifiedNameMatcher.fromQualifiedName(name);
        assertTrue(exact.matches(name));

        QualifiedNameMatcher groupOnly = new QualifiedNameMatcher(name.getGroupId());
        assertTrue(groupOnly.matches(name));

        QualifiedNameMatcher groupAndWildcard = new QualifiedNameMatcher(name.getGroupId() + ":*");
        assertTrue(groupAndWildcard.matches(name));

        QualifiedNameMatcher groupWildcard = new QualifiedNameMatcher(name.getGroupId() + "*:*");
        assertTrue(groupWildcard.matches(name));

        QualifiedNameMatcher allWildcard = new QualifiedNameMatcher("*");
        assertTrue(allWildcard.matches(name));
    }
}
