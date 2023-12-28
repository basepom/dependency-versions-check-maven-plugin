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

import com.google.common.collect.ImmutableMap;

/**
 * Gives access to the various strategies. The implementation of this API must collect the Strategies registered in Plexus and return them by name or as list.
 */
public interface StrategyProvider {

    Strategy forName(String name);

    ImmutableMap<String, Strategy> getStrategies();
}
