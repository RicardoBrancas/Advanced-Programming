package ist.meic.pa.FunctionalProfiler;

import java.util.HashMap;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class FunctionalProfilerCounter {

	private static HashMap<String, Integer> reads = new HashMap<>();
	private static HashMap<String, Integer> writes = new HashMap<>();
	private static int readCount = 0;
	private static int writeCount = 0;

	public static void addRead(String c) {
		reads.put(c, reads.getOrDefault(c, 0) + 1);
		readCount++;
	}

	public static void addWrite(String c) {
		writes.put(c, writes.getOrDefault(c, 0) + 1);
		writeCount++;
	}

	public static void print() {
		System.out.println("Total reads: " + readCount + " Total writes: " + writeCount);
		TreeSet<String> classes = new TreeSet<>();
		classes.addAll(reads.keySet());
		classes.addAll(writes.keySet());
		for (Object key : classes) {
			System.out.println("class " + key.toString() + " ->" +
					" reads: " + reads.getOrDefault(key, 0) +
					" writes: " + writes.getOrDefault(key, 0));
		}
	}
}
