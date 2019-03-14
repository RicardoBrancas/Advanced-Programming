package pt.ist.ap.labs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;

public class RunTests {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: RunTests <ClassName>");
			return;
		}

		Class clazz;
		try {
			clazz = Class.forName(args[0]);
		} catch (ClassNotFoundException e) {
			System.out.println("Class " + args[0] + " not found.");
			return;
		}

		HashMap<String, Method> setups = new HashMap<>();

		Class c = clazz;
		while (c != null) {
			for (Method method : c.getDeclaredMethods()) {
				if (Arrays.stream(method.getDeclaredAnnotations())
						.anyMatch(annotation -> annotation instanceof Setup)) {
					method.trySetAccessible();
					setups.put(method.getAnnotation(Setup.class).value(), method);
				}
			}
			c = c.getSuperclass();
		}


		int passed = 0, failed = 0;
		c = clazz;
		while (c != null) {
			for (Method method : c.getDeclaredMethods()) {
				if (Modifier.isStatic(method.getModifiers())
						&& Arrays.stream(method.getDeclaredAnnotations())
						.anyMatch(annotation -> annotation instanceof Test)) {
					String[] requirements = method.getAnnotation(Test.class).value();

					if (requirements.length == 1 && requirements[0].equals("*"))
						requirements = setups.keySet().toArray(new String[0]);


					try {
						for (String requirement : requirements) {
							setups.get(requirement).invoke(null);
						}

						method.trySetAccessible();
						method.invoke(null);
					} catch (IllegalAccessException | InvocationTargetException e) {
						failed++;
						System.out.println("Test " + method.toGenericString() + " failed");
						continue;
					}
					passed++;
					System.out.println("Test " + method.toGenericString() + " OK!");
				}
			}
			c = c.getSuperclass();
		}

		System.out.println("Passed: " + passed + ", Failed " + failed);
	}


}
