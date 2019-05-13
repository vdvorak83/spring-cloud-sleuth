/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.async;

import brave.Tracing;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.sleuth.DefaultSpanNamer;
import org.springframework.cloud.sleuth.SpanNamer;

import java.util.function.Function;

/**
 * A {@link Function} that wraps {@link Runnable} in a trace
 * representation.
 *
 * @author Marcin Grzejszczak
 * @author Sergei Egorov
 * @since 2.2.0
 */
public class TracingRunnableDecorator implements Function<Runnable, Runnable> {

	private static final Log log = LogFactory.getLog(LazyTraceAsyncTaskExecutor.class);

	private final BeanFactory beanFactory;

	private Tracing tracing;

	private SpanNamer spanNamer;

	public TracingRunnableDecorator(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Runnable apply(Runnable task) {
		return wrap(task);
	}

	Runnable wrap(Runnable task) {
		if (!ContextUtil.isContextInCreation(this.beanFactory)) {
			return new TraceRunnable(tracing(), spanNamer(), task);
		} else {
			return task;
		}
	}

	// due to some race conditions trace keys might not be ready yet
	protected SpanNamer spanNamer() {
		if (this.spanNamer == null) {
			try {
				this.spanNamer = this.beanFactory.getBean(SpanNamer.class);
			}
			catch (NoSuchBeanDefinitionException e) {
				log.warn(
						"SpanNamer bean not found - will provide a manually created instance");
				return new DefaultSpanNamer();
			}
		}
		return this.spanNamer;
	}

	protected Tracing tracing() {
		if (this.tracing == null) {
			try {
				this.tracing = this.beanFactory.getBean(Tracing.class);
			}
			catch (NoSuchBeanDefinitionException e) {
				return null;
			}
		}
		return this.tracing;
	}
}
