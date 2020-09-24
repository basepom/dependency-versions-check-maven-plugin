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

import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public final class PluginLog
{
    private final Logger logger;

    public PluginLog(final Class<?> clazz)
    {
        checkNotNull(clazz, "clazz is null");
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public void debug(final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        synchronized (logger) {
            logger.debug(format(fmt, args));
        }
    }

    public void debug(final Throwable t, final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        synchronized (logger) {
            logger.debug(format(fmt, args), t);
        }
    }

    public void info(final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        synchronized (logger) {
            logger.info(format(fmt, args));
        }
    }

    public void info(final Throwable t, final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        synchronized (logger) {
            logger.info(format(fmt, args), t);
        }
    }

    public void warn(final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        MessageBuilder mb = MessageUtils.buffer();
        synchronized (logger) {
            logger.warn(mb.warning(format(fmt, args)).toString());
        }
    }

    public void warn(final Throwable t, final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        MessageBuilder mb = MessageUtils.buffer();
        synchronized (logger) {
            logger.warn(mb.warning(format(fmt, args)).toString(), t);
        }
    }

    public void error(final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        MessageBuilder mb = MessageUtils.buffer();
        synchronized (logger) {
            logger.error(mb.failure(format(fmt, args)).toString());
        }
    }

    public void error(final Throwable t, final String fmt, final Object... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        MessageBuilder mb = MessageUtils.buffer();
        synchronized (logger) {
            logger.error(mb.failure(format(fmt, args)).toString(), t);
        }
    }

    public void report(final boolean quiet, final String fmt, final Object... args)
    {
        if (quiet) {
            debug(fmt, args);
        }
        else {
            info(fmt, args);
        }
    }
}
