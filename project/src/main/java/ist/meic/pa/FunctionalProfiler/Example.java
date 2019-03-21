package ist.meic.pa.FunctionalProfiler;

interface Counter {
	public int value();

	public Counter advance();
}

class ImperativeCounter implements Counter {
	int i;

	ImperativeCounter(int start) {
		i = start;
	}

	public int value() {
		return i;
	}

	public Counter advance() {
		i = i + 1;
		return this;
	}
}

class FunctionalCounter implements Counter {
	int i;

	public FunctionalCounter(int start) {
		i = start;
	}

	public int value() {
		return i;
	}

	public Counter advance() {
		return new FunctionalCounter(i + 1);
	}
}

public class Example {
	public static void test(Counter c1, Counter c2) {
		System.out.println(String.format("%s %s", c1.value(), c2.value()));
	}

	public static void main(String[] args) {
		Counter fc = new FunctionalCounter(0);
		test(fc, fc.advance());
		Counter ic = new ImperativeCounter(0);
		test(ic, ic.advance());
	}
}