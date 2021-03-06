﻿### (English instructions please see "README_ENG.md")

# jBeanBox 
**License:** [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)  

jBeanBox是一个微形但功能较齐全的IOC/AOP工具，用于Java6或以上环境。

jBeanBox项目的定位：需要一个功能较全的IOC/AOP工具，但是又不想引入臃肿的Spring。   

### 其它IOC/AOP工具的问题：  
1. Spring: 源码臃肿，Java方式的配置不灵活, 非单例模式时性能差。  
2. Guice: 源码臃肿(200多个类)，手工绑定使用不方便，功能不全，如不支持PostConstruct、不支持类路径扫描。  
3. Feather:源码极简(几百行)，但功能不全，只是DI工具，不支持AOP。  
4. Dagger: 源码略臃肿(300个类)，编译期静态注入，性能最好，但使用不方便，不支持AOP。  
5. Genie: 这是ActFramework项目的内核，只是DI工具，不支持AOP。  
6. Nutz、jFinal等Web开发框架中也有IOC/AOP工具，但它们没有独立出来，并且没有考虑JSR330、AOP联盟等业界标准，通用性差。    

### jBeanBox的主要优点
1. 功能较全，Java配置、注解配置、Bean生命周期支持、循环依赖检测和注入、AOP这些功能都具备。
2. 源码简洁，除了引入的第三方库外，核心源码只有3000行左右，源码短小本身就说明了它架构的合理性。
3. 采用原生Java作为配置，更简单易用。它的配置类BeanBox是一个纯Java类而不是一个代理类，可以作为静态配置存在，支持配置的继承、重写等特性, 也可以在运行期动态生成、修改，比Spring的Java配置方式更强大、灵活。   
4. 兼容性好，支持大多数JSR330、JSR250标准注解，并且兼容主要Spring注解。  

### 如何在项目中使用jBeanBox?  
手工在Maven站下载单个的jbeanbox-x.x.x.jar放到项目的类目录，或在pom.xml中加入以下配置：  
```
<dependency>
    <groupId>com.github.drinkjava2</groupId>
    <artifactId>jbeanbox</artifactId>
    <version>4.0.0</version> <!-- Or newest version -->
</dependency>
```
jBeanBox只有单个jar包，不依赖于任何第三方库，为避免包冲突，它已经将用到的CGLIB等第三方库以源码内嵌的方式包含在了项目中。
jBeanBox的jar包尺寸较大，约为460K, 如果用不到AOP代理功能，可以只使用它的DI内核，称为"jBeanBoxDI", 只有约50k大小，将上面artifactId中的jbeanbox改成jbeanboxdi即可，jBeanBoxDI项目详见jbeanboxdi子目录。   
顺便说一下，jSqlBox从4.0.0版起已经用源码内嵌的方式将jBeanBox包含到jSqlBox中去了，所以使用了jSqlBox的项目就不用再添加jBeanBox依赖了。jSqlBox是一个持久层工具，它利用jBeanBox的AOP功能提供声明式事务，而无需依赖Spring。

### jBeanBox注解方式配置
jBeanBox有Java方式配置和注解方式配置两种使用方式。先用一张图来显示它支持的注解，以及与Spring、Guice的区别：  
![image](compare.png)      
从上图可以看出，jBeanBox的功能较全，它除了自带注解外，还可以直接使用Spring及JSR330、JSR250的一些注解。红字部分为4.0.0版增加的功能。  

jBeanBox自带以下注解(全部是大写)：  
@INJECT  类似JSR中的@Inject注解，但它有个附加的参数是允许添加可选的目标类或BeanBox配置类(见下文介绍)作为参数，如@INJECT(Book.class) 或 @INJECT(BookBox.class)   
@POSTCONSTRUCT  等同于JSR中的@PostConstruct注解  
@PREDESTROY  等同于JSR中的@PreDestroy注解  
@VALUE 等同于Spring中的@Value注解, 参数将被解析为对应的值类型, 如@VALUE("3") int a; 参数将被解析为整数3。但与Spring不同的是jBeanBox不支持在@Value参数中的EL模板语法，需要通过自定义ValueTranslator实现。  
@PROTOTYPE  等同于Spring中的@Prototype注解  
@AOP 用于自定义AOP注解，详见AOP一节  
@COMPONENT 用法等同于Spring中的@Component注解，它需要与jBeanBox的类扫描功能联用，用来自动发现注册指定类下的所有Bean类, 例如JBEANBOX.scanComponents("com.foo")可以扫描包"tom.foo"下的所有被@Component标注的类：  
@NAMED 用法等同于JSR330中的@Named注解  
@QUALIFILER 用法等同于JSR330或Spring中的@Qualifiler注解  
 
jBeanBox默认工作在兼容模式，支持Spring和JSR注解，可以用JBEANBOX.ctx().setAllowSpringJsrAnnotation(false)去禁用JSR、Spring注解，这里ctx()方法返回的是默认bean容器，jBeanBox支持多个bean容器空间，每个容器可以有自已的设定。  
jBeanBox也可以用JBEANBOX.ctx().setAllowAnnotation(false)去禁用包括自带注解在内的所有注解，也就是说只能用Java方式配置了。  

关于@INJECT注解，jBeanBox与其它IOC工具不同点在于，它可以直接在@INJECT注解里指明目标类或配置类，如:  
@INJECT(jdbcUrlBox.class)  private String url; //其中jdbcUrlBox.class是一个BeanBox类，它的返回结果是由jdbcUrlBox这个配置类来决定的。这种方式的优点是支持重构和IDE快速定位到配置类。      
 
jBeanBox对于静态定义的类，如果没有任何注解定义，默认均为单例类，所以每次ctx.getBean(SomeClass.class)都会获得同一个单例对象。   

jBeanBox不建议在项目中使用@Named或@Qualifier，它们的主要问题是不支持IDE定位，如ctx.getBean("jdbcURL")无法利用IDE快速定位到配置文件，不利于维护。jBeanBox新增这两个注解的支持主要是考虑兼容性。  

jBeanBox支持类似Guice中的绑定语法，可以用ctx.bind("xxId",box)方法手工给目标类或配置类绑定一个ID值,然后用getBean("xxId")来获取它。绑定是链式的，键可以是任意对象类型，键本身也可以作为目标，形成一个链式查找。

jBeanBox默认不进行类的自动扫描，所以启动非常快速。如果需要对包进行类扫描，把所有@COMPONENT注解标记的类登记到容器里，必须手工调用ctx.scanComponents("包名1","包名2"...)来进行扫描，包名可以包含一个星号通配符。手工扫描只是进行配置的登记，并不创建Bean的实例，也就是说jBeanBox的所有Bean都是懒初始化的，直到碰到getBean方法或@INJECT注入时，才会开始创建Bean的实例。如果需要在程序启动时初始化单例类，必须手工调用一次getBean()方法，从而生成单例实例并缓存在bean容器里。

因为注解注入方式大家比较熟悉，与Spring/Guice/JSR标准中的命名和用法类似，这里就不作详细介绍了，在jBeanBox\test目录下能找到各种使用演示，如：  
```
AnnotationInjectTest.java 演示各种注解注入方式
JavaInjectTest.java 演示各种Java注入方式配置
QualiferTest.java 演示@Named、@Qualifer注解使用，注意这里还有手工进行类扫描的示例
```
 

### jBeanBox的Java方式配置 
jBeanBox的Java方式配置非常强大灵活，以下先通过一个例子来演示10种不同的注入方式：  
```
public class HelloWorld {
	public static class User {
		String name;
		
		public User() {}
		
		@VALUE("User1")
		public User(String name) {	this.name = name;}
		
		void setName(String name) {	this.name = name;}
		
		void init() {this.name = "User6";}
		
		@PreDestroy
		void end() {this.name= "User10";}
	}

	public static class UserBox extends BeanBox {
		Object create() {return new User("User2");}
	}
	
	public static class UserBox7 extends BeanBox {
		{   setBeanClass(User.class);
			setProperty("name", "User7");
		} 
	}

	public static class H8 extends UserBox {{setAsValue("User8");}}
 
	public static void main(String[] args) {
		User u1 = JBEANBOX.getInstance(User.class);
		User u2 = JBEANBOX.getBean(UserBox.class);
		User u3 = JBEANBOX.getBean(new BeanBox().injectConstruct(User.class, String.class, value("User3")));
		User u4 = JBEANBOX.getBean(new BeanBox(User.class).injectValue("name", "User4" ));
		User u5 = JBEANBOX
				.getBean(new BeanBox(User.class).injectMethod("setName", String.class, value("User5")));
		User u6 = JBEANBOX.getBean(new BeanBox().setBeanClass(User.class).setPostConstruct("init"));
		User u7 = new UserBox7().getBean();
		
		BeanBoxContext ctx = new BeanBoxContext(); 
		Interceptor aop=new MethodInterceptor() { 
			public Object invoke(MethodInvocation invocation) throws Throwable { 
				invocation.getArguments()[0]="User9";
				return invocation.proceed();
			}
		};
		User u8 = ctx.rebind(String.class, "8").bind("8", H8.class)
				.getBean(ctx.getBeanBox(User.class).addMethodAop(aop, "setName",String.class).injectField("name", autowired())); 
		System.out.println(u1.name); //Result: User1
		System.out.println(u2.name); //Result: User2
		System.out.println(u3.name); //Result: User3
		System.out.println(u4.name); //Result: User4
		System.out.println(u5.name); //Result: User5
		System.out.println(u6.name); //Result: User6
		System.out.println(u7.name); //Result: User7
		System.out.println(u8.name); //Result: User8
		u8.setName("");
		System.out.println(u8.name); //Result: User9
		ctx.close();
		System.out.println(u8.name); //Result: User10 
	}
}
```
这个例子的输出结果是依次打印出“User1” 、“User2”...到“User10”。下面遂一解释：
1. 第一个利用了@VALUE("User1")注解，进行了构造器注入。  
2. 第二个利用一个jBeanBox的纯Java配置类UserBox，这是一个纯粹的Java类（不象Spring中的Java配置类是一个特殊的类，它在运行期会产生一个代理类）, 在这个示例里它的create方法手工生成了一个User("User2")对象。  
3. 第三个是动态生成一个BeanBox配置，动态配置它的构造器注入，注入值为"User3"。  
4. 第四个也是动态配置，演示了字段注入，注入值为常量"User4"。  
5. 第五个是方法注入的演示，注入参数依次为：方法名、参数类型们、实际参数们。  
6. 第六个是setPostConstruct注入，等效于@PostConstruct注解，即Bean生成后立即执行的方法为init()方法。  
7. 第七个UserBox7是一个普通的BeanBox配置类，它设定了Bean类型，这种方式将调用它的无参构造器生成实例，然后注入它的name属性为"User7"。  
8. 第八个比较复杂，ctx是一个新的上下文实例，它先获取User.class的固定配置，然后给它的setName方法添加一个AOP切面，然后注入"name"字段为autowired类型，也就是说String类型，不过在此之前String类被绑定到字符串"8",字符串"8"又绑定到H8.class，H8又继承于UserBox，UserBox又返回"User2"，然而都是浮云，因为H8本身被配置成一个值类型"User8"，于是最后输出结果是“User8”。  
9. 第九个比较简单，因为setName方法被添加了一个AOP拦截器，参数被改成了"User9"。  
10.第十个是因为ctx这个上下文结束，所有单例被@PreDestroy标注的方法会执行。  

上例除了一头一尾外，主要演示了jBeanBox的Java方法配置，Java方法即可以动态执行，也可以在定义好的BeanBox类中作为固定配置执行，固定的配置可以打下配置的基调，当固定配置需要变动时可以用同样的Java方法来进行调整(因为本来就是同一个BeanBox对象)甚至临时创建出新的配置，所以jBeanBox同时具有了固定配置和动态配置的优点。另外当没有源码时，例如配置第三方库的实例，这时所有的注解方式配置都用不上，唯一能用的只有Java配置方式。  

上例中的value()方法是从JBEANBOX类中静态引入的全局方法，这个示例的源码位于单元测试目录下的HelloWorld.java。  

jBeanBox特有的Java配置方式可以写出非常清晰易读的配置类来，例如下面这些配置层层继承：
``` 
	public static class HikariCPBox extends BeanBox { 
		public HikariDataSource create() {
			HikariDataSource ds = new HikariDataSource();
			ds.addDataSourceProperty("cachePrepStmts", true);
			ds.addDataSourceProperty("prepStmtCacheSize", 250); 
			ds.setMaximumPoolSize(3);
			ds.setConnectionTimeout(5000);
			this.setPreDestroy("close");// jBeanBox will close pool
			return ds;
		}
	}

	public static class MySqlDataSourceBox extends HikariCPBox {
		{
			injectValue("jdbcUrl", "jdbc:mysql://127.0.0.1:3306/test?rewriteBatchedStatements=true&useSSL=false");
			injectValue("driverClassName", "com.mysql.jdbc.Driver");
			injectValue("username", "root");
			injectValue("password", "password");
		}
	}

	public static class OracleDataSourceBox extends MySqlDataSourceBox {//继承父类的username和password
		{
			injectValue("jdbcUrl", "jdbc:oracle:thin:@127.0.0.1:1521:XE");
			injectValue("driverClassName", "oracle.jdbc.OracleDriver"); 
		}
	}

       public static class DataSourceBox extends OracleDataSourceBox {
	}
```
最后在程序的任意地方调用JBEANBOX.getBean(DataSourceBox.class)就可以获取一个数据源，数据源的类型由配置文件来决定。和Spring利用方法进行的Java配置不同，jBeanBox的配置是利用Java类本身，所以配置可以写在任意地方，不需要加任何注解，因为配置就是普通Java类。  
 
### jBeanBox的Java方式配置详解
以上只是笼统演示了一下jBeanBox的Java方式配置，以下是它的Java配置方法详解：  
* setAsValue(Object) 将当前BeanBox配置成一个常量值，等同于setTarget(Obj)+setPureVale(true)  
* setPrototype(boolean) 如参数为true时表示它是一个非单例，与setSingleton方法正好相反  
* injectConstruct(Class<?>, Object...) 设定构造器注入，参数分别是类、构造器参数类型们、参数们  
* injectMethod(String, Object...) 设定某个方法注入，参数分别是方法名、参数类型们、参数们  
* addMethodAop(Object, Method) 对某个方法添加AOP，参数分别是AOP类或实例、方法  
* addMethodAop(Object, String, Class<?>...) 对某个方法添加AOP，参数分别是AOP类或实例、方法名、参数类型们  
* addBeanAop(Object, String) 对整个Bean添加AOP,参数分别是AOP类或实例、方法规则(如"setUser*")，  
* setPostConstruct(String) 设定一个PostConstruct方法名，效果等同与@PostConstruct注解  
* setPreDestroy(String) 设定一个PreDestroy方法名，效果等同与@PreDestroy注解  
* injectField(String, BeanBox) 注入一个字段，参数是字段名、BeanBox实例，它的等效注解是@INJECT   
* setProperty(String, Object) 等同于injectValue方法  
* injectValue(String, Object) 注入一个字段，参数是字段名、对象实例，可与它类比的注解是@VALUE  
* setTarget(Object) 注定当前Bean的目标，另外当bind("7",User.class)时，setTarget("7")就等同于setTarget(User.class)  
* setPureValue(boolean) 表示target不再是目标了，而是作为纯值返回，上行的"7"就会返回字符串"7"  
* setBeanClass(Class<?>) 设定当前BeanBox的最终目标类，所有的配置都是基于这个类展开  
* setSingleton(Boolean) 如参数为true时表示它是一个单例，与setPrototype方法正好相反  
* setConstructor(Constructor<?>) 设定一个构造器  
* setConstructorParams(BeanBox[]) 设定构造器的参数，与上行联用  
* setPostConstruct(Method) 设定一个PostConstruct方法，效果等同与@PostConstruct注解  
* setPreDestroy(Method) 设定一个PreDestroy方法名，效果等同与@PreDestroy注解   

Java方式配置，对于BeanBox来说，还有两个特殊的方法create和config，如下示例：
```
public static class DemoBox extends BeanBox {

		public Object create() {
			return new Book();
		}

		public void config(Object o) {
			(Book)o.setTitle("Foo");
		}
	}
```
上例表示DemoBox中创建的Bean是由create方法来生成，由config方法来修改。  
另外提一下，create方法和config方法可以带BeanBoxContext参数，用在比较特殊的需要使用上下文的场合。  

### jBeanBox的AOP(面向切面编程)
jBeanBox功能大都可以用Java配置或注解配置两种方式来实现，同样地，它对AOP的支持也有两种方式：

#### Java方式AOP配置
* someBeanBox.addMethodAop(Object, String, Class<?>...) 对某个方法添加AOP，参数分别是AOP类或实例、方法名、参数类型们。  
* someBeanBox.addBeanAop(Object, String) 对整个Bean添加AOP,参数分别是AOP类或实例、方法规则(如"setUser＊|getName＊")。  
* someBeanBoxContext.addContextAop(Object, Object, String);对整个上下文添加AOP规则，参数分别是AOP类或实例、类或类名规则、方法名规则。  
以上三个方法分别对应三种不同级别的AOP规则，第一个方法只针对方法，第二个方法针对整个类，第三个方法针对整个上下文。以下是一个AOP的Java配置示例：
```
public static class AopDemo1 {
		String name;
		String address;
		String email;
        //getter & setters...
	}

	public static class MethodAOP implements MethodInterceptor { 
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invocation.getArguments()[0] = "1";
			return invocation.proceed();
		}
	}

	public static class BeanAOP implements MethodInterceptor { 
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invocation.getArguments()[0] = "2";
			return invocation.proceed();
		}
	}

	public static class ContextAOP implements MethodInterceptor {
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invocation.getArguments()[0] = "3";
			return invocation.proceed();
		}
	}

	public static class AopDemo1Box extends BeanBox {
		{
			this.injectConstruct(AopDemo1.class, String.class, value("0"));
			this.addMethodAop(MethodAOP.class, "setName", String.class);
			this.addBeanAop(BeanAOP.class, "setAddr*");
		}
	}

	@Test
	public void aopTest1() {
		JBEANBOX.ctx().addContextAop(ContextAOP.class, AopDemo1.class, "setEm*");
		AopDemo1 demo = JBEANBOX.getBean(AopDemo1Box.class);
		demo.setName("--");
		Assert.assertEquals("1", demo.name);
		demo.setAddress("--");
		Assert.assertEquals("2", demo.address);
		demo.setEmail("--");
		Assert.assertEquals("3", demo.email);
	}
```
jBeanBox中的命名匹配规则采用星号做为模糊匹配字符，代表任意长度、任意字符，但一个规则里只允许出现一个星号，多个规则之间用"|"号分隔。

#### 注解方式AOP配置
注解方式AOP只有两种类型，针对方法的和针对类的，没有针对上下文的。注解方式配置使用方便，但前提是必须要有源码存在。
注解方式需要用到一个特殊的注解@AOP，它是用来自定义自已的AOP注解用的，使用示例如下：
```
public static class Interceptor1 implements MethodInterceptor {//标准AOP联盟接口
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invocation.getArguments()[0] = "1";
			return invocation.proceed();
		}
	}

	public static class Interceptor2 implements MethodInterceptor {//标准AOP联盟接口
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invocation.getArguments()[0] = "2";
			return invocation.proceed();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@AOP
	public static @interface MyAop1 {//这个是自定义的切面注解，放在类上
		public Class<?> value() default Interceptor1.class;

		public String method() default "setNa*";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	@AOP
	public static @interface MyAop2 {//这个是自定义的切面注解，放在方法上
		public Class<?> value() default Interceptor2.class;
	}

	@MyAop1
	public static class AopDemo1 {
		String name;
		String address;

		public void setName(String name) {
			this.name = name;
		}

		@MyAop2
		public void setAddress(String address) {
			this.address = address;
		}
	}

	@Test
	public void aopTest1() {
		AopDemo1 demo = JBEANBOX.getBean(AopDemo1.class);
		demo.setName("--");//切面生效，把参数改成“1”
		Assert.assertEquals("1", demo.name); 
		demo.setAddress("--");//切面生效，把参数改成“2”
		Assert.assertEquals("2", demo.address);
	}
```
本文所说的AOP是针对Aop alliance联盟标准的接口来说的，它已经被包含在jBeanBox中，无需再单独引入(当然重复引入也不会有问题)。Aop alliance联盟标准是比较有用的一个接口，实现了各种AOP实现之间的互换性，基于它，jBeanBox可以替换掉Spring的内核而使用它的声明式事务，这种互换性能够实现的前提就是因为Spring的声明式事务实现(如TransactionInterceptor)也实现了Aop alliance联盟标准接口MethodInterceptor。

jBeanBox的最新版本，AOP功能大幅削减，去掉了不常用的前置、后置、异常切面功能，只保留了支持AOP alliance联盟标准接口MethodInterceptor(注意在CGLIB中有一个同名的接口，不要混淆)。实现了MethodInterceptor接口的类，通常称为Interceptor,但在jBeanBox中图省事，也把它称为AOP，毕竟写成addBeanAop要比写成addBeanInterceptor简洁一些。


### 关于循环依赖
jBeanBox具备循环依赖检测功能，如果发现循环依赖注入(如A构造器中注入B,B的构造器中又需要注入A），将会抛出BeanBoxException运行时异常。
但是，以下这种字段或方法中出现的循环依赖注入在jBeanBox中是允许的：
```
public static class A {
		@Inject
		public B b;
	}

public static class B {
		@Inject
		public A a;
	}

A a = JBEANBOX.getBean(A.class);
Assert.assertTrue(a == a.b.a);//true
```
### jBeanBox支持多上下文和Bean生命周期
jBeanBox支持多个上下文实例(BeanBoxContext)，上下文也可以称为Bean容器。每个上下文实例都是互不干拢的。例如一个User.class可以在3个上下文中各自用不同的配置方式(注解、Java)生成3个“单例”，这3个“单例”都是相对于当前上下文唯一的，它们的属性与各自的配置有关。  
  
JBEANBOX.getBean()方法是利用了一个缺省的全局上下文，可以用JBEANBOX.ctx()方法来获取这个全局上下文，所以如果一个项目中不需要用到多个上下文，可以直接使用JBEANBOX.getBean()方法，这样更方便。  
JBEANBOX不是jBeanBox项目的关键类，它只是提供了一组静态全局方法供调用，以操纵默认的缺省全局上下文。

BeanBoxContext的每个实例都在内部维护着配置信息、单例缓存等，在BeanBoxContext实例的close方法被调用后，它的配置信息和单例被清空，当然，在清空之前，所有单例类的PreDestroy方法（如果有的话)被调用运行。所以对于需要回调PreDestroy方法的上下文来说，在关闭时不要忘了手工调用reset方法（或close方法)进行收尾清理。

BeanBoxContext的常用方法详解：
* reset() 这个静态方法重置所有静态全局配置，并调用缺省上下文实例的close方法。
* close() 先调用当前上下文缓存中单例实例的PreDestroy方法(如果有的话)，然后清空当前上下文的缓存。
* getBean(Object) 根据目标对象（可以是任意对象类型），返回一个Bean，如果找不到则抛出异常
* getInstance(Class<T>) 根据目标类T,返回一个T类型的实例, 如果找不到则抛出异常
* getBean(Object, boolean) 根据目标对象，返回一个Bean， 第二个参数为false时如果找不到则返回Empty.class
* getInstance(Class<T>, boolean) 根据目标类T,返回一个T类型的实例, 第二个参数为false时如果找不到则返回Empty.class
* bind(Object, Object) 给目标类绑定一个ID，例如：ctx.bind("A","B").bind("B".C.class)，则以后可以用getBean("A")获取C的实例
* addGlobalAop(Object, String, String) 在当前上下文环境添加一个AOP(详见AOP一节),第二个参数为类名模糊匹配规则，如"com.tom.＊"或"＊.tom"等，＊号只允许出现一次, 第三个参数为方法名模糊匹配规则，如"setUser＊"或"＊user"等。
* addGlobalAop(Object, Class<?>, String) 在当前上下文环境添加一个AOP，第二个参数为指定类(会匹配所有与指定类名称开头相同的类，例如指定类为a.b.C.class, 则a.b.CXX.class也会被匹配)，第三个参数为方法名模糊匹配规则。
* getBeanBox(Class<?>) 获取一个类的BeanBox实例，例如一个注解标注的类，可以用这个方法获取BeanBox实例，然后再添加、修改它的配置，这就是固定配置和动态配置的结合运用。
* setAllowAnnotation(boolean) 设定是否允许读取类中的注解，如果设为flase的话，则jBeanBox只允行使用纯Java配置方式。默认true。
* setAllowSpringJsrAnnotation(boolean) 设定是否允先读取类中JSR330/JSR250和Spring的部分注解，以实现兼容性。默认true。
* setValueTranslator(ValueTranslator) 设定对于@VALUE注解中的字符串参数如何解析它，例如@VALUE("#user")，系统默认返回"#user"字符串，如果需要不同的解析，例如读取属性文本中的值，则需要自已设定一个实现了ValueTranslator接口的实例。  

### jBeanBox的性能
以下为jBeanBox的性能与其它IOC工具的对比，只对比DI注入功能，搭建一个由6个对象组成的实例树,可见jBeanBox创建非单例的速度比Guic慢一倍、比Spring快45倍左右。一般来说，IOC工具多应用在单例场合，因为从缓存中取，性能大家都差不多，所以性能不是关键。但是如果遇到个别需要频繁生成非单例的场合，例如每次访问生成一个新的页面对象实例，这时Spring就有可能成为性能瓶颈。   

测试程序详见：[di-benchmark]（https://github.com/drinkjava2/di-benchmark)  
```
Runtime benchmark, fetch new bean for 500000 times: 
---------------------------------------------------------
                     Vanilla|    31ms
                       Guice|  1154ms
                     Feather|   624ms
                      Dagger|   312ms
                       Genie|   609ms
                        Pico|  4555ms
              jBeanBoxNormal|  2075ms
            jBeanBoxTypeSafe|  2371ms
          jBeanBoxAnnotation|  2059ms
     SpringJavaConfiguration| 92149ms
     SpringAnnotationScanned| 95504ms
     
     
Split Starting up DI containers & instantiating a dependency graph 4999 times:
-------------------------------------------------------------------------------
                     Vanilla| start:     0ms   fetch:     0ms
                       Guice| start:  1046ms   fetch:  1560ms
                     Feather| start:     0ms   fetch:   109ms
                      Dagger| start:    46ms   fetch:   173ms
                        Pico| start:   376ms   fetch:   217ms
                       Genie| start:   766ms   fetch:   247ms
              jBeanBoxNormal| start:    79ms   fetch:   982ms
            jBeanBoxTypeSafe| start:     0ms   fetch:   998ms
          jBeanBoxAnnotation| start:     0ms   fetch:   468ms
     SpringJavaConfiguration| start: 51831ms   fetch:  1834ms
     SpringAnnotationScanned| start: 70712ms   fetch:  4155ms

Runtime benchmark, fetch singleton bean for 5000000 times:
---------------------------------------------------------
                     Vanilla|    47ms
                       Guice|  1950ms
                     Feather|   624ms
                      Dagger|  2746ms
                       Genie|   327ms
                        Pico|  3385ms
              jBeanBoxNormal|   188ms
            jBeanBoxTypeSafe|   187ms
          jBeanBoxAnnotation|   171ms
     SpringJavaConfiguration|  1061ms
     SpringAnnotationScanned|  1045ms
```

以上就是对jBeanBox的介绍，没有别的文档了，因为毕竟它的核心源码也只有3000行(第三方工具如CGLIB、JSR的源码不算在内)，我怕再写下去，使用说明会超过它的源码行数，有问题去看看它的源码或单元测试可能更简单一点。    

更多关于jBeanBox的用法还可以在jSqlBox项目中看到它的运用(数据源的配置、声明式事务示例等)。
