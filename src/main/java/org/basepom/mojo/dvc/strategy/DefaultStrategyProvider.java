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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.function.Function.identity;

/**
 * Default implementation for {@link StrategyProvider}.
 */
@Named("default")
@Singleton
public class DefaultStrategyProvider
        implements StrategyProvider
{
    @Inject
    protected List<Strategy> resolverDefinitions = ImmutableList.of();

    @Override
    public ImmutableMap<String, Strategy> getStrategies()
    {
        return resolverDefinitions.stream().collect(ImmutableMap.toImmutableMap(r -> r.getName().toLowerCase(Locale.ENGLISH), identity()));
    }

    @Override
    public Strategy forName(final String name)
    {
        checkNotNull(name, "name is null");

        final Map<String, Strategy> strategies = getStrategies();
        return strategies.get(name.toLowerCase(Locale.ENGLISH));
    }
}

