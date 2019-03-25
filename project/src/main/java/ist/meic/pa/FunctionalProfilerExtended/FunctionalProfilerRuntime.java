package ist.meic.pa.FunctionalProfilerExtended;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class should not be referenced by any other.
 * It will be loaded at runtime.
 */
@SuppressWarnings("unused")
public class FunctionalProfilerRuntime {

	private static class IntPair {
		int reads = 0;
		int writes = 0;

		void incRead() {
			reads++;
		}

		void incWrite() {
			writes++;
		}
	}

	private static TreeMap<Class, IntPair> counts = new TreeMap<>(Comparator.comparing(Class::getName));
	private static TreeMap<String, IntPair> detailedCounts = new TreeMap<>();
	private static int readCount = 0;
	private static int writeCount = 0;
	public static Object currentConstructor = null;

	public static void addRead(Class c, String field) {
		counts.putIfAbsent(c, new IntPair());
		counts.get(c).incRead();
		detailedCounts.putIfAbsent(field, new IntPair());
		detailedCounts.get(field).incRead();
		readCount++;
	}

	public static void addWrite(Class c, String field) {
		counts.putIfAbsent(c, new IntPair());
		counts.get(c).incWrite();
		detailedCounts.putIfAbsent(field, new IntPair());
		detailedCounts.get(field).incWrite();
		writeCount++;
	}

	public static void print() {
		System.out.println("Total reads: " + readCount + " Total writes: " + writeCount);

		counts.forEach((clazz, intPair) ->
				System.out.println("class " + clazz.getName() + " ->" +
						           " reads: " + intPair.reads +
				                   " writes: " + intPair.writes));

		detailedCounts.forEach((field, intPair) ->
				System.out.println("field " + field + " ->" +
								   " reads: " + intPair.reads +
								   " writes: " + intPair.writes));
	}
}
