package ist.meic.pa.FunctionalProfiler;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * This class should not be referenced by any other.
 * It will be loaded at runtime.
 */
@SuppressWarnings("unused")
public class FunctionalProfilerRuntime {

	private static HashMap<Class, Integer> reads = new HashMap<>();
	private static HashMap<Class, Integer> writes = new HashMap<>();
	private static int readCount = 0;
	private static int writeCount = 0;
	public static Object currentConstructor = null;

	public static void addRead(Class c) {
		reads.put(c, reads.getOrDefault(c, 0) + 1);
		readCount++;
	}

	public static void addWrite(Class c) {
		writes.put(c, writes.getOrDefault(c, 0) + 1);
		writeCount++;
	}

	public static void print() {
		System.out.println("Total reads: " + readCount + " Total writes: " + writeCount);
		TreeSet<Class> classes = new TreeSet<>(Comparator.comparing(Class::getName));
		classes.addAll(reads.keySet());
		classes.addAll(writes.keySet());
		for (Object key : classes) {
			System.out.println("class " + key.toString() + " ->" +
					" reads: " + reads.getOrDefault(key, 0) +
					" writes: " + writes.getOrDefault(key, 0));
		}
	}
}
