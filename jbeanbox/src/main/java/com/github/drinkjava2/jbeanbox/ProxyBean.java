/**
 * Copyright (C) 2016 Yong Zhu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.drinkjava2.jbeanbox;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.drinkjava2.cglib3_2_0.proxy.MethodInterceptor;
import com.github.drinkjava2.cglib3_2_0.proxy.MethodProxy;

/**
 * ProxyBean created by CGLib
 * 
 * @author Yong Zhu
 * @since 2.4
 *
 */
class ProxyBean implements MethodInterceptor {
	protected CopyOnWriteArrayList<Advisor> myAdvisors = new CopyOnWriteArrayList<Advisor>();

	protected ProxyBean(Class<?> clazz, List<Advisor> globalAdvicors, BeanBoxContext context) {
		String beanClassName = clazz.getName();
		int i = beanClassName.indexOf("$$");// If created by CGLib, use the original class name as bean ID
		if (i > 0)
			beanClassName = beanClassName.substring(0, i); 
		for (Advisor advisor : globalAdvicors) {// Make a copy from global advisors which only belong to this Bean
			Method[] methods = clazz.getMethods();
			for (Method method : methods)
				if (advisor.match(beanClassName, method.getName())) {
					myAdvisors.add(advisor);
					break;
				}
		}
		dealAopAroundAnnotation(clazz, context);
	}

	private void dealAopAroundAnnotation(Class<?> clazz, BeanBoxContext context) {
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {// check if have AopAround annotation
			if (method.isAnnotationPresent(AopAround.class)) {
				AopAround aop = method.getAnnotation(AopAround.class);
				if (!Object.class.equals(aop.value())) {
					BeanBox box = null;
					try {
						box = BeanBoxUtils.getBeanBox(null, aop.value(), null, null, context, true);
 						box.setContext(context);
					} catch (Exception e) {
						BeanBoxException.throwEX(e, "BeanBox ProxyBean create AopAround box error");
					}
					Advisor adv = new Advisor(clazz.getName(), method.getName(), box, "invoke", "AROUND", true); 
					myAdvisors.add(adv);
				}
			}
		}
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy cgLibMethodProxy) throws Throwable {
		if (!myAdvisors.isEmpty())// Start a advice chain call
			return new AdviceCaller(this, obj, method, args, cgLibMethodProxy, myAdvisors).callNextAdvisor();
		else
			return cgLibMethodProxy.invokeSuper(obj, args);
	}
}