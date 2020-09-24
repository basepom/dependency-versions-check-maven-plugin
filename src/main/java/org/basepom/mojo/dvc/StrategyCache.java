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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import org.basepom.mojo.dvc.model.ResolverDefinition;
import org.basepom.mojo.dvc.strategy.Strategy;
import org.basepom.mojo.dvc.strategy.StrategyProvider;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class StrategyCache
{
    /**
     * name of an artifact to  version resolution strategy.
     */
    private final Cache<QualifiedName, Strategy> resolverCache = CacheBuilder.newBuilder().concurrencyLevel(10).build();

    private final ImmutableMap<QualifiedNameMatcher, Strategy> resolverPatterns;
    private final Strategy defaultStrategy;

    StrategyCache(final StrategyProvider strategyProvider, final ResolverDefinition[] resolvers, final String defaultStrategyName)
    {
        checkNotNull(strategyProvider, "strategyProvider is null");
        checkNotNull(resolvers, "resolvers is null");
        checkNotNull(defaultStrategyName, "defaultStrategyName is null");

        this.defaultStrategy = strategyProvider.forName(defaultStrategyName);
        checkState(defaultStrategy != null, "Could not locate default version strategy '%s'", defaultStrategyName);

        final ImmutableMap.Builder<QualifiedNameMatcher, Strategy> builder = ImmutableMap.builder();
        Arrays.stream(resolvers).forEach(r -> {
            final Strategy strategy = strategyProvider.forName(r.getStrategyName());
            checkState(strategy != null, "Could not locate version strategy %s! Check for typos!", r.getStrategyName());
            r.getIncludes().forEach(include -> builder.put(include, strategy));
        });
        this.resolverPatterns = builder.build();
    }

    public Strategy forQualifiedName(final QualifiedName name)
    {
        checkNotNull(name, "name is null");
        try {
            return resolverCache.get(name, () -> {
                for (final Map.Entry<QualifiedNameMatcher, Strategy> entry : resolverPatterns.entrySet()) {
                    if (entry.getKey().matches(name)) {
                        return entry.getValue();
                    }
                }
                return defaultStrategy;
            });
        }
        catch (ExecutionException e) {
            // ignore, never happens.
            return defaultStrategy;
        }
    }
}
