package org.springframework.context;

import org.springframework.context.lookup.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author meisen
 * @date 2019-12-20
 */
public class LoadXmlTest {

	private Long id;

	private String name;

	private int age;

	public LoadXmlTest( String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("services.xml");
//		GenericApplicationContext context = new GenericApplicationContext();
//		new XmlBeanDefinitionReader(context).loadBeanDefinitions("service.xml");
//		context.refresh();
//		LoadXmlTest test = context.getBean("xmlTest", LoadXmlTest.class);
//		test.print();

		User bean = (User) context.getBean("testBean");
		bean.print();
//		System.out.println(Introspector.decapitalize("LoadXmlTest") );

//		XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("services.xml"));
//		System.out.println(factory.getBeanDefinitionCount());
//		LoadXmlTest test = factory.getBean("xmlTest", LoadXmlTest.class);
//		System.out.println(test.name);
	}

	private void print() {
		System.out.println("1231231231`23");
	}

}
