package ist.meic.pa.FunctionalProfiler;

import javassist.*;

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
			classLoader.addTranslator(pool, new FunctionalProfilerTranslator(args[0]));
		} catch (NotFoundException | CannotCompileException e) {
			e.printStackTrace();
		}

		String[] restArgs = Arrays.copyOfRange(args, 1, args.length);

		try {
			classLoader.run(args[0], restArgs);
		} catch (Throwable throwable) {
			System.err.println("Class threw " + throwable.getMessage());
			throwable.printStackTrace();
		}

	}
}
