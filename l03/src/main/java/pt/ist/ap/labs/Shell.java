package pt.ist.ap.labs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;

public class Shell {

	private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	private Object top = null;

	private HashMap<String, Object> memory = new HashMap<>();


	public void repl() throws IOException {

		String[] parts;
		String command;
		String[] args;
		while (true) {
			System.out.print("Command:> ");

			String raw;
			raw = reader.readLine();
			if (raw == null)
				break;
			parts = raw.split("[ \t]");
			command = parts[0];
			args = Arrays.copyOfRange(parts, 1, parts.length);

			switch (command) {
				case "Class":
					assert parts.length == 2;
					try {
						top = Class.forName(args[0]);
						showTop();
					} catch (ClassNotFoundException e) {
						System.out.println("Class not found.");
					}
					break;


				case "Set":
					memory.put(args[0], top);
					System.out.println("Saved name for object of type: class " + top.getClass());
					showTop();
					assert parts.length == 2;
					break;


				case "Get":
					assert parts.length == 2;
					top = memory.get(args[0]);
					showTop();
					break;


				case "Index":
					assert parts.length == 2;
					assert top.getClass().isArray();

					top = Array.get(top, Integer.parseInt(args[0]));
					showTop();
					break;


				case "Exit":
					System.out.println("Goodbye!");
					return;


				default:
					System.out.println("Trying generic command: " + command);
					Class[] types = new Class[args.length / 2];
					try {
						for (int i = 0; i < args.length; i += 2) {
							types[i / 2] = Class.forName(args[i]);
						}
					} catch (ClassNotFoundException e) {
						System.out.println("Argument type unknown!");
						break;
					}

					try {
						top = top.getClass().getMethod(command, types).invoke(top);
						showTop();
					} catch (NoSuchMethodException e) {
						System.out.println("Method not found.");
						break;
					} catch (IllegalAccessException e) {
						System.out.println("Illegal access: " + e.getCause());
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}

			}


		}

	}

	private void showTop() {
		show(top);
	}

	private void show(Object o) {
		if (o instanceof Class)
			System.out.println("class " + ((Class) o).getName());

		else if (o.getClass().isArray())
			for (int i = 0; i < Array.getLength(o); i++) {
				show(Array.get(o, i));
			}

		else if (o instanceof Executable)
			System.out.println(((Executable) o).toGenericString());

		else if (o instanceof Package)
			System.out.println("package " + ((Package) o).getName() +
					", " + ((Package) o).getImplementationTitle() +
					", version " + ((Package) o).getImplementationVersion());

		else
			System.out.println(o.toString());
	}


}
