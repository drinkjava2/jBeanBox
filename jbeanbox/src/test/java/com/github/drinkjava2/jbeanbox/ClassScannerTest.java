/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.drinkjava2.jbeanbox;

import java.util.List;

import org.junit.Test;

import com.github.drinkjava2.jbeanbox.aop.AnnotationAopTest.BeanAop;

/**
 * @author Yong Zhu
 * @since 2.5.0
 */
@SuppressWarnings("rawtypes")
@BeanAop
public class ClassScannerTest {

	@Test
	public void prototypeTest() {
		List<Class> cList1 = ClassScanner.scan("com.github.drinkjava2.jbeanbox");
		for (Class c : cList1)
			System.out.println(c);

		System.out.println("======================");
		List<Class> cList2 = ClassScanner.scanByAnno(BeanAop.class, "com.github.drinkjava2.jbeanbox");
		for (Class c : cList2)
			System.out.println(c);
		
		System.out.println("======================");
		List<Class> cList3 = ClassScanner.scanByName("com.github.drinkjava2.jbeanbox.annotation.PR*", "com.github.drinkjava2.jbeanbox");
		for (Class c : cList3)
			System.out.println(c);
	}
}