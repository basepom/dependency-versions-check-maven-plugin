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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QualifiedNameTest
{
    @Test
    public void testSimple()
    {
        QualifiedName name = new QualifiedName("groupId", "artifactId", "demo", "none");
        assertEquals("groupId", name.getGroupId());
        assertEquals("artifactId", name.getArtifactId());

        assertTrue(name.getType().isPresent());
        assertEquals("demo", name.getType().get());

        assertTrue(name.getClassifier().isPresent());
        assertEquals("none", name.getClassifier().get());

        assertEquals("groupId:artifactId:demo:none", name.getFullName());
    }

    @Test
    public void testNoType()
    {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            new QualifiedName("groupId", "artifactId", null, "none");
        });

        assertEquals("Classifier must be null if type is null", e.getMessage());

        QualifiedName noType = new QualifiedName("groupId", "artifactId", null, null);

        assertEquals("groupId", noType.getGroupId());
        assertEquals("artifactId", noType.getArtifactId());

        assertFalse(noType.getType().isPresent());
        assertFalse(noType.getClassifier().isPresent());
        assertEquals("groupId:artifactId", noType.getFullName());
    }

    @Test
    public void testNoClassifier()
    {
        QualifiedName noClassifier = new QualifiedName("groupId", "artifactId", "demo", null);
        assertEquals("groupId", noClassifier.getGroupId());
        assertEquals("artifactId", noClassifier.getArtifactId());

        assertTrue(noClassifier.getType().isPresent());
        assertEquals("demo", noClassifier.getType().get());

        assertFalse(noClassifier.getClassifier().isPresent());

        assertEquals("groupId:artifactId:demo", noClassifier.getFullName());
    }

    @Test
    public void testNoTypeAndClassifier()
    {
        QualifiedName noClassifier = new QualifiedName("groupId", "artifactId", null, null);
        assertEquals("groupId", noClassifier.getGroupId());
        assertEquals("artifactId", noClassifier.getArtifactId());

        assertFalse(noClassifier.getType().isPresent());
        assertFalse(noClassifier.getClassifier().isPresent());

        assertEquals("groupId:artifactId", noClassifier.getFullName());
    }

    @Test
    public void testEquality()
    {
        QualifiedName n1 = new QualifiedName("groupId", "artifactId", "demo", "none");
        QualifiedName n2 = new QualifiedName("groupId", "artifactId", "demo", "none");

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
        assertEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    public void testTestJarEquality()
    {
        // test jar equality
        QualifiedName t1 = new QualifiedName("groupId", "artifactId", "test-jar", null);
        QualifiedName t2 = new QualifiedName("groupId", "artifactId", "jar", "tests");
        assertTrue(t1.equals(t2));
        assertTrue(t2.equals(t1));
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testNullTypeEquality()
    {
        // type null and jar equality
        QualifiedName j1 = new QualifiedName("groupId", "artifactId", "jar", null);
        QualifiedName j2 = new QualifiedName("groupId", "artifactId", null, null);
        assertTrue(j1.equals(j2));
        assertTrue(j2.equals(j1));
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testNullClassifierEquality()
    {
        // classifier null and empty
        QualifiedName c1 = new QualifiedName("groupId", "artifactId", "jar", null);
        QualifiedName c2 = new QualifiedName("groupId", "artifactId", "jar", "");
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testTestJars()
    {
        QualifiedName c1 = new QualifiedName("groupId", "artifactId", "test-jar", null);
        QualifiedName c2 = new QualifiedName("groupId", "artifactId", "jar", "tests");

        assertTrue(c1.equals(c2));

        assertEquals("groupId:artifactId:test-jar", c1.getFullName());
        assertEquals("groupId:artifactId:jar:tests", c2.getFullName());

        assertEquals(0, c1.compareTo(c2));
        assertEquals(0, c2.compareTo(c1));
    }
}
