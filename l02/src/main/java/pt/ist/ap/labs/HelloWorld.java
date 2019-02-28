package pt.ist.ap.labs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HelloWorld implements Message {

	public static void main(String[] args) {
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		try {
			String className = input.readLine();
			Class clazz = Class.forName(className);

			Message msg = (Message) clazz.newInstance();
			msg.say();

		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void say() {
		System.out.println("Hello World!");
	}
}
