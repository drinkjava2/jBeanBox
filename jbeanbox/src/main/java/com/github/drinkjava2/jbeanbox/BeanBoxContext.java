/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.drinkjava2.jbeanbox;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Qualifier;

import org.springframework.stereotype.Component;

import com.github.drinkjava2.jbeanbox.ValueTranslator.DefaultValueTranslator;
import com.github.drinkjava2.jbeanbox.annotation.COMPONENT;
import com.github.drinkjava2.jbeanbox.annotation.NAMED;
import com.github.drinkjava2.jbeanbox.annotation.QUALIFILER;

/**
 * BeanBoxContext is the Context to create beans
 * 
 * @author Yong Zhu
 * @since 2.4
 *
 */
public class BeanBoxContext {
	public static String CREATE_METHOD = "create"; // as title
	public static String CONFIG_METHOD = "config"; // as title

	protected static boolean globalNextAllowAnnotation = true; // as title
	protected static boolean globalNextAllowSpringJsrAnnotation = true; // as title
	protected static ValueTranslator globalNextValueTranslator = new DefaultValueTranslator(); // see user manual

	protected boolean allowAnnotation = globalNextAllowAnnotation;
	protected boolean allowSpringJsrAnnotation = globalNextAllowSpringJsrAnnotation;
	protected ValueTranslator valueTranslator = globalNextValueTranslator;

	protected Map<Object, Object> bindCache = new ConcurrentHashMap<>();// bind cache
	protected Map<Class<?>, BeanBox> beanBoxCache = new ConcurrentHashMap<>(); // BeanBox Cache
	protected Map<Object, Object> singletonCache = new ConcurrentHashMap<>(); // class or BeanBox as key
	protected Set<Class<?>> componentCache = new HashSet<>(); // component cache
	protected Map<Class<?>, Boolean> componentExistCache = new ConcurrentHashMap<>();// as title

	protected static BeanBoxContext globalBeanBoxContext = new BeanBoxContext();// Global Bean context

	// ==========AOP about=========
	protected List<Object[]> aopRules;

	public BeanBoxContext() {
		bind(Object.class, EMPTY.class);
		bind(String.class, EMPTY.class);
		bind(Integer.class, EMPTY.class);
		bind(Boolean.class, EMPTY.class);
		bind(Byte.class, EMPTY.class);
		bind(Long.class, EMPTY.class);
		bind(Short.class, EMPTY.class);
		bind(Float.class, EMPTY.class);
		bind(Double.class, EMPTY.class);
		bind(Character.class, EMPTY.class);
		bind(List.class, EMPTY.class);
		bind(Map.class, EMPTY.class);
		bind(Set.class, EMPTY.class);

		bind(int.class, EMPTY.class);
		bind(boolean.class, EMPTY.class);
		bind(byte.class, EMPTY.class);
		bind(long.class, EMPTY.class);
		bind(short.class, EMPTY.class);
		bind(float.class, EMPTY.class);
		bind(double.class, EMPTY.class);
		bind(char.class, EMPTY.class);
	}

	/**
	 * Reset global variants setting , note this method only close
	 * globalBeanBoxContext, if created many BeanBoxContext instance need close them
	 * manually
	 */
	public static void reset() {
		globalBeanBoxContext.close();
		globalNextAllowAnnotation = true;
		globalNextAllowSpringJsrAnnotation = true;
		globalNextValueTranslator = new DefaultValueTranslator();
		CREATE_METHOD = "create";
		CONFIG_METHOD = "config";
		globalBeanBoxContext = new BeanBoxContext();
	}

	/**
	 * Close current BeanBoxContext, clear singlton cache, call predestory methods
	 * for each singleton if they have
	 */
	public void close() {
		for (Entry<Object, Object> singletons : singletonCache.entrySet()) {
			Object key = singletons.getKey();
			Object obj = singletons.getValue();
			if (key instanceof BeanBox) {
				BeanBox box = (BeanBox) key;
				if (box.getPreDestroy() != null)
					try {
						box.getPreDestroy().invoke(obj);
					} catch (Exception e) {
						// Eat it here, but usually need log it
					}
			}
		}
		bindCache.clear();
		beanBoxCache.clear();
		singletonCache.clear();
	}

	public Object getObject(Object obj) {
		return getBean(new Require(obj));
	}

	public <T> T getBean(Object obj) {
		return getBean(new Require(obj, true)); // first step of changzheng
	}

	public <T> T getInstance(Class<T> target) {
		return getBean(new Require(target, true));
	}

	public <T> T getBean(Object obj, boolean required) {
		return getBean(new Require(obj, required));
	}

	public <T> T getInstance(Class<T> target, boolean required) {
		return getBean(new Require(target, required));
	}

	// protected <T> T getBean(Object target, boolean required, Set<Object> history)

	@SuppressWarnings("unchecked")
	protected <T> T getBean(Require req) {// NOSONAR
		if (req.target != null && singletonCache.containsKey(req.target))
			return (T) singletonCache.get(req.target);

		if (req.target == null || EMPTY.class == req.target)
			return (T) notfoundOrException(req.target, req.required);

		if (req.target instanceof BeanBox) {
			BeanBox bx = (BeanBox) req.target;
			if (bx.isSingleton()) { // BeanBox already in singleton cache?
				Object id = bx.getSingletonId();
				if (id != null) {
					Object existed = singletonCache.get(id);
					if (existed != null && EMPTY.class != existed)
						return (T) existed;
				}
			}
			if (req.history != null && req.history.contains(req.target)) {
				if (bx.getTarget() != null)
					BeanBoxException
							.throwEX("Fail to build bean, circular dependency found on target: " + bx.getTarget());
				if (bx.getBeanClass() != null)
					BeanBoxException.throwEX(
							"Fail to build bean, circular dependency found on beanClass: " + bx.getBeanClass());
				if (bx.getType() != null)
					BeanBoxException.throwEX("Fail to build bean, circular dependency found on type: " + bx.getType());
				if (bx.getCreateMethod() != null)
					BeanBoxException.throwEX(
							"Fail to build bean, circular dependency found on method: " + bx.getCreateMethod());
				BeanBoxException.throwEX("Fail to build bean, circular dependency found on: " + bx);
			}
		}

		Object result = null;
		req.setCtx(this);
		if (req.history == null)
			req.history = new HashSet<Object>();
		req.history.add(req.target);
		if (bindCache.containsKey(req.target)) {
			result = getBean(req.setTarget(bindCache.get(req.target)));
		} else if (req.target instanceof BeanBox) { // is a BeanBox instance?
			result = getBeanFromBox((BeanBox) req.target, req);
		} else if (req.target instanceof Class) { // is a class?
			BeanBox box = searchComponent((Class<?>) req.target); // first search in components
			if (box == null) // if not a component, directly create the instance for this class
				box = BeanBoxUtils.getUniqueBeanBox(this, (Class<?>) req.target);
			result = getBean(req.setTarget(box));
			if (EMPTY.class != result && box.isSingleton()) {
				singletonCache.put(req.target, result);
			}
		} else
			result = notfoundOrException(req.target, req.required);
		req.history.remove(req.target);
		return (T) result;
	}

	private BeanBox searchComponent(Class<?> claz) {
		if (Boolean.FALSE.equals(componentExistCache.get(claz))) // if already know no component exist
			return null;
		for (Class<?> compClaz : componentCache)
			if (claz.isAssignableFrom(compClaz)) {

			}
		return null;
	}

	/** Get Bean From BeanBox instance */
	private Object getBeanFromBox(BeanBox box, Require req) {// NOSONAR
		// NOSONAR System.out.println(" Box=> box=" + box + " history=" + history);
		BeanBoxException.assureNotNull(box, "Fail to build instance for a null beanBox");
		Object bean = null;
		if (box.isSingleton()) { // Check if singleton in cache
			bean = singletonCache.get(box);
			if (bean != null)
				return bean;
		}

		if (box.isPureValue()) // if constant?
			return box.getTarget();
		if (box.getTarget() != null) {// if target?
			if (EMPTY.class != box.getTarget())
				return getBean(req.setTarget(box.getTarget()).setRequired(box.isRequired()));
			if (box.getType() != null)
				return getBean(req.setTarget(box.getType()).setRequired(box.isRequired()));
			else
				return notfoundOrException(box.getTarget(), box.isRequired());
		}

		boolean aopFound = false;// is AOP?
		if (box.getAopRules() != null || box.getMethodAops() != null)
			aopFound = true;
		else if (this.getAopRules() != null && box.getBeanClass() != null)
			for (Object[] aops : this.getAopRules()) // global AOP
				if (NameMatchUtil.nameMatch((String) aops[1], box.getBeanClass().getName())) {
					aopFound = true;
					break;
				}
		if (aopFound)
			bean = AopUtils.createProxyBean(box.getBeanClass(), box, this);
		else {
			Object created = box.create();
			if (created != null)
				bean = created;
		}
		if (bean == null)
			if (box.getCreateMethod() != null) // if have create method?
				try {
					Method m = box.getCreateMethod();
					if (m.getParameterTypes().length == 1) {
						bean = m.invoke(box, req);
					} else if (m.getParameterTypes().length == 0)
						bean = m.invoke(box);
					else
						BeanBoxException.throwEX("Create method can only have 0 or 1 parameter");
					BeanBoxException.assureNotNull(bean, "Create method created a null object.");
				} catch (Exception e) {
					return BeanBoxException.throwEX(e);
				}
			else if (box.getConstructor() != null) { // has constructor?
				if (box.getConstructorParams() != null && box.getConstructorParams().length > 0) {
					Object[] initargs = param2RealObjects(req, box.getConstructorParams());
					try {
						bean = box.getConstructor().newInstance(initargs);
					} catch (Exception e) {
						return BeanBoxException.throwEX(e);
					}
				} else // 0 param constructor
					try {
						bean = box.getConstructor().newInstance();
					} catch (Exception e) {
						return BeanBoxException.throwEX(e);
					}
			} else if (box.getBeanClass() != null) { // is normal bean
				if (EMPTY.class == box.getBeanClass())
					return notfoundOrException(EMPTY.class, req.required);
				try {
					bean = box.getBeanClass().newInstance();
				} catch (Exception e) {
					BeanBoxException.throwEX("Failed to call 0 parameter constructor of: " + box.getBeanClass(), e);
				}
			} else
				return notfoundOrException(null, req.required); // return null or throw EX

		// Now Bean is ready

		// Cache bean or proxy bean right now for circular dependency use
		if (box.isSingleton()) {
			Object id = box.getSingletonId();
			if (id != null)
				singletonCache.put(box, bean);
		} // NOW BEAN IS CREATED

		box.config(bean, req);

		if (box.getPostConstruct() != null) // PostConstructor
			ReflectionUtils.invokeMethod(box.getPostConstruct(), bean);

		if (box.getFieldInjects() != null) // Fields inject
			for (Entry<Field, BeanBox> entry : box.getFieldInjects().entrySet()) {
				Field f = entry.getKey();
				BeanBox b = entry.getValue();
				Object fieldValue = this.getBeanFromBox(b, req);// TODO: here is false should?
				if (EMPTY.class == fieldValue) {
					if (b.isRequired())
						BeanBoxException.throwEX("Not found required value for field: " + f.getName() + " in "
								+ f.getDeclaringClass().getName());
				} else {
					if (fieldValue != null && fieldValue instanceof String)
						fieldValue = this.valueTranslator.translate((String) fieldValue, b.getType());
					ReflectionUtils.setField(f, bean, fieldValue);
				}
			}

		if (box.getMethodInjects() != null) { // Methods inject
			for (Entry<Method, BeanBox[]> methods : box.getMethodInjects().entrySet()) {
				Method m = methods.getKey();
				BeanBox[] paramBoxs = methods.getValue();
				if (paramBoxs != null && paramBoxs.length > 0) {
					Object[] methodParams = param2RealObjects(req, paramBoxs);
					ReflectionUtils.invokeMethod(m, bean, methodParams);
				} else // method has no parameter
					ReflectionUtils.invokeMethod(m, bean);
			}
		}
		return bean;
	}

	/**
	 * Scan classes with &#064;COMPONENT or &#064;Component annotation, for
	 * autowiring purpose
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void scanComponents(String... packages) {
		List<Class> classes = ClassScanner.scan(packages);
		for (Class claz : classes)
			for (Annotation anno : claz.getAnnotations()) {
				Class<? extends Annotation> aType = anno.annotationType();
				if (BeanBoxUtils.ifSameOrChildAnno(aType, COMPONENT.class, Component.class)) {
					componentCache.add(claz);// add class as component
					BeanBox box = getBeanBox(claz);
					Map<String, Object> values = BeanBoxUtils.changeAnnotationValuesToMap(anno);
					if (!"".equals(values.get("value")))// use given bean name
						this.bind(values.get("value"), box);
					else {
						String s = claz.getSimpleName(); // else use first char lower case class name as bean name
						if (!Character.isLowerCase(s.charAt(0)))
							s = (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1))
									.toString();
						bind(s, box);
					}

					for (Annotation otherAnno : claz.getAnnotations()) {// check qualifiler or named annotation family
						Class<? extends Annotation> qualiAnno = otherAnno.annotationType();
						if (BeanBoxUtils.ifSameOrChildAnno(qualiAnno, NAMED.class, Named.class, QUALIFILER.class,
								Qualifier.class, org.springframework.beans.factory.annotation.Qualifier.class)) {
							Map<String, Object> v = BeanBoxUtils.changeAnnotationValuesToMap(otherAnno);
							if (v.size() > 1)
								BeanBoxException
										.throwEX("jBeanBox does not support multiple property in Qualifier annotation: "
												+ qualiAnno);
							box.setQualifierAnno(qualiAnno)
									.setQualifierValue(v.isEmpty() ? null : v.values().iterator().next());
						}
					}
				}
			}
	}

	/** Bind a targe on a bean id, if id already exist, throw BeanBoxException */
	public BeanBoxContext bind(Object id, Object target) {
		BeanBoxException.assureNotNull(id, "bind id can not be empty");
		if (bindCache.containsKey(id))
			BeanBoxException.throwEX("Binding already exists on bean id '" + id
					+ "', consider use 'rebind' method to allow override existed binding");
		bindCache.put(id, target);
		return this;
	}

	/** Bind a targe on a bean id, if id already exist, override it */
	public BeanBoxContext rebind(Object id, Object target) {
		BeanBoxException.assureNotNull(id, "bind id can not be empty");
		bindCache.put(id, target);
		return this;
	}

	public BeanBoxContext addContextAop(Object aop, String classNameRegex, String methodNameRegex) {
		if (aopRules == null)
			aopRules = new ArrayList<Object[]>();
		aopRules.add(new Object[] { BeanBoxUtils.checkAOP(aop), classNameRegex, methodNameRegex });
		return this;
	}

	public BeanBoxContext addContextAop(Object aop, Class<?> clazz, String methodNameRegex) {
		return addContextAop(aop, clazz.getName() + "*", methodNameRegex);
	}

	public BeanBox getBeanBox(Class<?> clazz) {
		return BeanBoxUtils.getUniqueBeanBox(this, clazz);
	}

	protected void staticMethods________________________() {// NOSONAR
	}

	private static Object[] param2RealObjects(Require req, BeanBox[] boxes) {
		Object[] result = new Object[boxes.length];
		for (int i = 0; i < boxes.length; i++) {
			result[i] = req.ctx.getBeanFromBox(boxes[i], req);
			if (result[i] != null && result[i] instanceof String)
				result[i] = req.ctx.valueTranslator.translate((String) result[i], boxes[i].getType());
		}
		return result;
	}

	private static Object notfoundOrException(Object target, boolean required) {
		if (required)
			return BeanBoxException.throwEX("BeanBox target not found: " + target);
		else
			return EMPTY.class;
	}

	protected void notExistMethod() {// this mark a not exist Method

	}

	protected static Method NOT_EXIST_METHOD = null; // NOSONAR

	static {
		try {
			NOT_EXIST_METHOD = BeanBoxContext.class.getDeclaredMethod("notExistMethod");
		} catch (Exception e) {// NOSONAR
		}
	}

	protected void staticGetterAndSetters________________________() {// NOSONAR
	}

	public static BeanBoxContext getGlobalBeanBoxContext() {
		return globalBeanBoxContext;
	}

	public static void setGlobalBeanBoxContext(BeanBoxContext globalBeanBoxContext) {
		BeanBoxContext.globalBeanBoxContext = globalBeanBoxContext;
	}

	public static boolean isGlobalNextAllowAnnotation() {
		return globalNextAllowAnnotation;
	}

	public static void setGlobalNextAllowAnnotation(boolean globalNextAllowAnnotation) {
		BeanBoxContext.globalNextAllowAnnotation = globalNextAllowAnnotation;
	}

	public static boolean isGlobalNextAllowSpringJsrAnnotation() {
		return globalNextAllowSpringJsrAnnotation;
	}

	public static void setGlobalNextAllowSpringJsrAnnotation(boolean globalNextAllowSpringJsrAnnotation) {
		BeanBoxContext.globalNextAllowSpringJsrAnnotation = globalNextAllowSpringJsrAnnotation;
	}

	public static ValueTranslator getGlobalNextParamTranslator() {
		return globalNextValueTranslator;
	}

	public static void setGlobalNextParamTranslator(ValueTranslator globalNextParamTranslator) {
		BeanBoxContext.globalNextValueTranslator = globalNextParamTranslator;
	}

	public static Method getNotExistMethod() {
		return NOT_EXIST_METHOD;
	}

	public static void setNotExistMethod(Method notExistMethod) {
		BeanBoxContext.NOT_EXIST_METHOD = notExistMethod;
	}

	protected void getterAndSetters________________________() {// NOSONAR
	}

	public boolean isAllowAnnotation() {
		return allowAnnotation;
	}

	public BeanBoxContext setAllowAnnotation(boolean allowAnnotation) {
		this.allowAnnotation = allowAnnotation;
		return this;
	}

	public boolean isAllowSpringJsrAnnotation() {
		return allowSpringJsrAnnotation;
	}

	public BeanBoxContext setAllowSpringJsrAnnotation(boolean allowSpringJsrAnnotation) {
		this.allowSpringJsrAnnotation = allowSpringJsrAnnotation;
		return this;
	}

	public ValueTranslator getValueTranslator() {
		return valueTranslator;
	}

	public BeanBoxContext setValueTranslator(ValueTranslator valueTranslator) {
		this.valueTranslator = valueTranslator;
		return this;
	}

	public Map<Object, Object> getBindCache() {
		return bindCache;
	}

	public BeanBoxContext setBindCache(Map<Object, Object> bindCache) {
		this.bindCache = bindCache;
		return this;
	}

	public Map<Class<?>, BeanBox> getBeanBoxCache() {
		return beanBoxCache;
	}

	public BeanBoxContext setBeanBoxCache(Map<Class<?>, BeanBox> beanBoxMetaCache) {
		this.beanBoxCache = beanBoxMetaCache;
		return this;
	}

	public Map<Object, Object> getSingletonCache() {
		return singletonCache;
	}

	public BeanBoxContext setSingletonCache(Map<Object, Object> singletonCache) {
		this.singletonCache = singletonCache;
		return this;
	}

	public List<Object[]> getAopRules() {
		return aopRules;
	}

	public BeanBoxContext setAopRules(List<Object[]> aopRules) {
		this.aopRules = aopRules;
		return this;
	}

}
