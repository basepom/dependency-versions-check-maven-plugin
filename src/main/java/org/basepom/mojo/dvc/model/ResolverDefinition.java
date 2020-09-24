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
package org.basepom.mojo.dvc.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.basepom.mojo.dvc.QualifiedNameMatcher;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a "resolver" element in the configuration section.
 */
public final class ResolverDefinition
{
    private String strategyName = "";
    private ImmutableList<QualifiedNameMatcher> includes = ImmutableList.of();

    public void setStrategyName(final String strategyName)
    {
        this.strategyName = checkNotNull(strategyName, "strategyName is null");
    }

    public void setIncludes(final String[] includes)
    {
        checkNotNull(includes, "includes is null");

        ImmutableList.Builder<QualifiedNameMatcher> builder = ImmutableList.builder();
        for (final String include : includes) {
            builder.add(new QualifiedNameMatcher(include));
        }
        this.includes = builder.build();
    }

    public String getStrategyName()
    {
        return strategyName;
    }

    public ImmutableList<QualifiedNameMatcher> getIncludes()
    {
        return includes;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResolverDefinition that = (ResolverDefinition) o;
        return strategyName.equals(that.strategyName) &&
                includes.equals(that.includes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(strategyName, includes);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("strategyName", strategyName)
                .add("includes", includes)
                .toString();
    }
}


