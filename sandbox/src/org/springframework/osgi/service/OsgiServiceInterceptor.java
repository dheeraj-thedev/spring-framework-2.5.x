/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 26-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.target.HotSwappableTargetSource;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
public class OsgiServiceInterceptor implements MethodBeforeAdvice, ServiceListener {

	private static final Log log = LogFactory.getLog(OsgiServiceInterceptor.class);
	
	private final BundleContext bundleContext;
	private final HotSwappableTargetSource targetSource;
	private final Class serviceType;
	private final String lookupFilter;

	private ServiceReference serviceReference;
	private boolean serviceUnavailable = false;
	private int maxRetries = OsgiServiceProxyFactoryBean.DEFAULT_MAX_RETRIES;
	private long retryIntervalMillis = OsgiServiceProxyFactoryBean.DEFAULT_MILLIS_BETWEEN_RETRIES;

	/**
	 * @param context
	 * @param reference
	 * @param targetSource
	 */
	public OsgiServiceInterceptor(
			BundleContext context, 
			ServiceReference reference, 
			HotSwappableTargetSource targetSource,
			Class  serviceType,
			String lookupFilter) {
		this.bundleContext = context;
		this.serviceReference = reference;
		this.targetSource = targetSource;
		this.serviceType = serviceType;
		this.lookupFilter = lookupFilter;
		registerAsServiceListener();
	}
	

	/**
	 * The maximum number of times that we should attempt to rebind to a
	 * service that has been unregistered.
	 * 
	 * @param maxRetries
	 */
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	/**
	 * Number of milliseconds to wait between retry attempts when the target
	 * service has been unregistered.
	 * 
	 * @param interval
	 */
	public void setRetryIntervalMillis(long interval) {
		this.retryIntervalMillis = interval;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method, java.lang.Object[], java.lang.Object)
	 */
	public synchronized void before(Method method, Object[] args, Object target) throws Throwable {
		if (this.serviceUnavailable) {
			int numAttempts = 0;
			while ((numAttempts++ < this.maxRetries) && !rebindToService()) {
				Thread.sleep(this.retryIntervalMillis);
			}
			if (this.serviceUnavailable) {
				// no luck!
				throw new ServiceUnavailableException(
						 "The target OSGi service of type '" + this.serviceType + 
						 "' matching filter '" + this.lookupFilter + "' was unregistered " +
						 "and no suitable replacement was found after retrying " +
						 this.maxRetries + " times.",
						 this.serviceType,
						 this.lookupFilter);
			}
		}
	}

	/**
	 * Start listening to service events in case the service we are proxying
	 * becomes unavailable.
	 */
	private void registerAsServiceListener() {
		this.bundleContext.addServiceListener(this);
	}

	
	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
		if (event.getServiceReference().equals(this.serviceReference)) {
			// something has changed in the target service
			switch (event.getType()) {
				case ServiceEvent.REGISTERED:
					// no-op for now maybe we will need to do something here in time?
					break;
				case ServiceEvent.UNREGISTERING:
					handleServiceUnavailable();
					break;
				case ServiceEvent.MODIFIED:
					handleServiceModified();
					break;
				default:
					throw new IllegalStateException("Unrecognised OSGi ServiceEvent type: " + event.getType());
			}
		}
	}

	/**
	 * The target service has been modified, rebind to it
	 */
	private void handleServiceModified() {
		Object newTarget = this.bundleContext.getService(this.serviceReference);
		this.targetSource.swap(newTarget);
		log.info("Target OSGi service of type '" + this.serviceType + 
				 "' matched by filter '" + this.lookupFilter + "' was rebound.");
	}

	/**
	 * 
	 */
	private synchronized void handleServiceUnavailable() {
		if (log.isInfoEnabled()) {
			log.info("Target OSGi service of type '" + this.serviceType + 
					 "' matched by filter '" + this.lookupFilter + "' has been unregistered.");
		}
		this.serviceUnavailable  = true;
	}
	
	private synchronized boolean rebindToService() {
		log.info("Attempting to rebind to OSGi service of type '" + this.serviceType + 
				 "' using filter '" + this.lookupFilter + "'.");
		ServiceReference[] sRefs = OsgiServiceUtils.getServices(this.bundleContext, this.serviceType, this.lookupFilter);
		if (sRefs.length == 0) {
			if (log.isInfoEnabled()) {
				log.info("No target OSGi service of type '" + this.serviceType + 
						 "' matching filter '" + this.lookupFilter + "' is available.");
			}
			return false;
		}
		
		this.serviceReference = sRefs[0];
		handleServiceModified();
		this.serviceUnavailable = false;
		return true;
	}

}
