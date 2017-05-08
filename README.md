# resteasy-client-timeouts

## Usage
```
cd <module>
mvn clean package
docker-compose up --build
```

## Goal
Each module tries to execute HTTP GET request on http://www.fakeresponse.com/api/?api_key=b3898a2e-5afb-481c-a50d-a80b86643c2c&sleep=5. Client timeouts are set to 1s. Expected behaviour is to interrupt HTTP request execution after time limit exceeded.

## Modules
### default-hello
IMHO this implementation is consistent with resteasy guidelines (https://docs.jboss.org/resteasy/docs/3.0.22.Final/userguide/html_single/) but it fails with runtime error on WildFly:
```
default-hello_1  | Caused by: java.lang.LinkageError: loader constraint violation: when resolving method "org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine.<init>(Lorg/apache/http/client/HttpClient;)V" the class loader (instance of org/jboss/modules/ModuleClassLoader) of the current class, org/jboss/resteasy/client/jaxrs/engines/ApacheHttpClient43Engine, and the class loader (instance of org/jboss/modules/ModuleClassLoader) for the method's defining class, org/jboss/resteasy/client/jaxrs/engines/ApacheHttpClient4Engine, have different Class objects for the type org/apache/http/client/HttpClient used in the signature
default-hello_1  | 	at org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine.<init>(ApacheHttpClient43Engine.java:32)
default-hello_1  | 	at xyz.mjdev.resteasy.client.timeouts.Hello.hello(Hello.java:36)
default-hello_1  | 	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
default-hello_1  | 	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
default-hello_1  | 	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
default-hello_1  | 	at java.lang.reflect.Method.invoke(Method.java:498)
default-hello_1  | 	at org.jboss.as.ee.component.ManagedReferenceLifecycleMethodInterceptor.processInvocation(ManagedReferenceLifecycleMethodInterceptor.java:96)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.doLifecycleInterception(Jsr299BindingsInterceptor.java:114)
default-hello_1  | 	at org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.processInvocation(Jsr299BindingsInterceptor.java:103)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
default-hello_1  | 	at org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:73)
default-hello_1  | 	at org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.weld.injection.WeldInjectionInterceptor.processInvocation(WeldInjectionInterceptor.java:53)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.ee.component.AroundConstructInterceptorFactory$1.processInvocation(AroundConstructInterceptorFactory.java:28)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.weld.injection.WeldInterceptorInjectionInterceptor.processInvocation(WeldInterceptorInjectionInterceptor.java:56)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.weld.ejb.Jsr299BindingsCreateInterceptor.processInvocation(Jsr299BindingsCreateInterceptor.java:100)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
default-hello_1  | 	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
default-hello_1  | 	at org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:275)
default-hello_1  | 	... 29 more
```

### httpcomponents-wa-hello
After some digging one the Web I have found workaround and applied it in pom:
```xml
<plugins>
	<plugin>
		<groupId>org.apache.maven.plugins</groupId>
		<artifactId>maven-war-plugin</artifactId>
		<configuration>
			<archive>
				<manifestEntries>
					<Dependencies>org.apache.httpcomponents</Dependencies>
				</manifestEntries>
			</archive>
		</configuration>
	</plugin>
</plugins>
```
http://stackoverflow.com/questions/10598889/jboss7-loader-constraint-violation-with-reasteasy-and-httpclient-with-custom-ht
http://stackoverflow.com/questions/18275551/java-lang-linkageerror-loader-constraint-violation-when-resolving-method-org
https://developer.jboss.org/thread/213682

It solves runtime issue but timeouts still do not work:
```
httpcomponents-wa-hello_1  | 20:50:03,943 INFO  [Hello] (ServerService Thread Pool -- 64) Executing http request...
httpcomponents-wa-hello_1  | 20:50:09,427 INFO  [Hello] (ServerService Thread Pool -- 64) Response code: 200
httpcomponents-wa-hello_1  | 20:50:09,431 INFO  [Hello] (ServerService Thread Pool -- 64) Request duration: PT5.486S

```

### httpengine-wa-hello
Copy and paste from ApacheHttpClient43Engine and ApacheHttpClient4Engine into MyEngineOne.

Works:
```
httpengine-wa-hello_1  | 20:50:49,403 INFO  [Hello] (ServerService Thread Pool -- 61) Executing http request...
httpengine-wa-hello_1  | 20:50:50,651 SEVERE [Hello] (ServerService Thread Pool -- 61) Request failed cause of: Read timed out
httpengine-wa-hello_1  | 20:50:50,655 INFO  [Hello] (ServerService Thread Pool -- 61) Request duration: PT1.248S
```
