package ist.meic.pa.FunctionalProfilerExtended;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class WithFunctionalProfiler {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: WithFunctionalProfiler [CLASS] [ARG]...");
			System.exit(-1);
		}

		ClassPool pool = ClassPool.getDefault();
		Loader classLoader = new Loader(pool);
		try {
			classLoader.addTranslator(pool, new FunctionalProfilerTranslator());
		} catch (NotFoundException | CannotCompileException e) {
			e.printStackTrace();
		}

		try {
			Class c = classLoader.loadClass("ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime");
			Method print = c.getDeclaredMethod("print");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					print.invoke(null);
				} catch (IllegalAccessException | InvocationTargetException e) {
					System.err.println("Error while printing statistics.");
				}
			}));
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			System.err.println("Could not find runtime class!");
			System.exit(1);
		}

		String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
		try {
			classLoader.run(args[0], restArgs);
		} catch (Throwable throwable) {
			System.err.println("Class threw " + throwable.getClass().getName());
			throwable.printStackTrace();
		}

	}
}
