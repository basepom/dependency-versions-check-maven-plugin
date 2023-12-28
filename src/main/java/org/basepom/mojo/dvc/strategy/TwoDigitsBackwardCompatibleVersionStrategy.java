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
package org.basepom.mojo.dvc.strategy;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Relaxed variant of APR, very suitable for Java code. It is assumed that for every non-backwards compatible change, the artifactId
 * is changed (e.g. by attaching a number to the artifactId) and the code is repackaged into a different package. So it is possible to
 * have multiple, non-backwards compatible major versions on the classpath (foo vs. foo2 vs.foo3). So all versions with the same artifactId
 * are backwards compatible; only forwards compatibility must be ensured.
 * <p>
 * By using the APR parser, the major version flags forwards compatibility, the minor and patch are not used. If a qualifier is present,
 * it must match.
 */
@Named("two-digits-backward-compatible")
@Singleton
public class TwoDigitsBackwardCompatibleVersionStrategy
        extends AprVersionStrategy
{
    @Override
    public String getName()
    {
        return "two-digits-backward-compatible";
    }

    @Override
    protected int checkMajorCompatible(int expectedMajor, int resolvedMajor)
    {
        // treat majors like minors in apache.
        return super.checkMinorCompatible(expectedMajor, resolvedMajor);
    }

    @Override
    protected int checkMinorCompatible(int expectedMinor, int resolvedMinor)
    {
        // treat minors like patch in apache.
        return super.checkPatchCompatible(expectedMinor, resolvedMinor);
    }

    @Override
    protected int checkPatchCompatible(int expectedPatch, int resolvedPatch)
    {
        if (expectedPatch != 0 || resolvedPatch != 0) {
            return -1; // ensure that this is really a two digit version.
        }

        return 0;
    }
}

