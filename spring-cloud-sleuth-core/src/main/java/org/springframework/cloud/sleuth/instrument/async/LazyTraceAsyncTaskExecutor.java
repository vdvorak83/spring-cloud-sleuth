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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * {@link AsyncTaskExecutor} that wraps {@link Runnable} and {@link Callable} in a trace
 * representation.
 *
 * @author Marcin Grzejszczak
 * @since 2.1.0
 */
public class LazyTraceAsyncTaskExecutor extends TracingRunnableDecorator implements AsyncTaskExecutor {

	private final BeanFactory beanFactory;

	private final AsyncTaskExecutor delegate;

	public LazyTraceAsyncTaskExecutor(BeanFactory beanFactory,
			AsyncTaskExecutor delegate) {
		super(beanFactory);
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	@Override
	public void execute(Runnable task) {
		this.delegate.execute(wrap(task));
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		this.delegate.execute(wrap(task), startTimeout);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.delegate.submit(wrap(task));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		Callable<T> taskToRun = task;
		if (!ContextUtil.isContextInCreation(this.beanFactory)) {
			taskToRun = new TraceCallable<>(tracing(), spanNamer(), task);
		}
		return this.delegate.submit(taskToRun);
	}

}
