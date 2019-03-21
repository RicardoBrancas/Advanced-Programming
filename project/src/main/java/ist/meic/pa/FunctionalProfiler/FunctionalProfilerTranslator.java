package ist.meic.pa.FunctionalProfiler;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public class FunctionalProfilerTranslator implements Translator {
	@Override
	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {

		CtClass counters = pool.makeClass("FunctionProfiler$Counter");

		CtClass ctString = pool.getCtClass("java.lang.String");

		CtField ctReads = new CtField(pool.getCtClass("java.util.HashMap"), "reads", counters);
		ctReads.setModifiers(Modifier.STATIC);
		counters.addField(ctReads, "new java.util.HashMap()");
		CtField ctReadCount = new CtField(pool.getCtClass("java.lang.Integer"), "readCount", counters);
		ctReadCount.setModifiers(Modifier.STATIC);
		counters.addField(ctReadCount, "Integer.valueOf(0)");
		CtField ctWrites = new CtField(pool.getCtClass("java.util.HashMap"), "writes", counters);
		ctWrites.setModifiers(Modifier.STATIC);
		counters.addField(ctWrites, "new java.util.HashMap()");
		CtField ctWriteCount = new CtField(pool.getCtClass("java.lang.Integer"), "writeCount", counters);
		ctWriteCount.setModifiers(Modifier.STATIC);
		counters.addField(ctWriteCount, "Integer.valueOf(0)");

		counters.addMethod(new CtMethod(
				pool.getCtClass("java.lang.Integer"),
				"addRead",
				new CtClass[]{ctString},
				counters
		));
		CtMethod ctAddRead = counters.getDeclaredMethod("addRead");
		ctAddRead.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
		ctAddRead.setBody("{" +
				"reads.putIfAbsent($1, Integer.valueOf(0));" +
				"reads.replace($1, " +
				"              Integer.valueOf(" +
				"                     ((Integer) reads.get($1)).intValue() " +
				"                     + 1));" +
				"readCount = Integer.valueOf(readCount.intValue() + 1);" +
				"return Integer.valueOf(0);" +
				"}");


		counters.addMethod(new CtMethod(
				pool.getCtClass("java.lang.Integer"),
				"addWrite",
				new CtClass[]{ctString},
				counters
		));
		CtMethod ctAddWrite = counters.getDeclaredMethod("addWrite");
		ctAddWrite.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
		ctAddWrite.setBody("{" +
				"writes.putIfAbsent($1, Integer.valueOf(0));" +
				"writes.replace($1, " +
				"              Integer.valueOf(" +
				"                     ((Integer) writes.get($1)).intValue() " +
				"                     + 1));" +
				"writeCount = Integer.valueOf(writeCount.intValue() + 1);" +
				"return Integer.valueOf(0);" +
				"}");

		counters.addMethod(new CtMethod(
				pool.getCtClass("java.lang.Integer"),
				"print",
				new CtClass[]{},
				counters
		));
		CtMethod ctPrint = counters.getDeclaredMethod("print");
		ctPrint.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
		ctPrint.setBody("{" +
				"System.out.println(\"Total reads: \" + readCount + \" Total writes: \" + writeCount);" +
				"java.util.TreeSet classes = new java.util.TreeSet();" +
				"classes.addAll(reads.keySet());" +
				"classes.addAll(writes.keySet());" +
				"for (java.util.Iterator it = classes.iterator(); it.hasNext();) {" +
				"Object key = it.next();" +
				"System.out.println(\"class \" +" +
				"                   key.toString() +" +
				"                   \" -> reads: \" +" +
				"                   reads.getOrDefault(key, Integer.valueOf(0)) +" +
				"\" writes: \" +" +
				"writes.getOrDefault(key, Integer.valueOf(0)));" +
				"};" +
				"return Integer.valueOf(0);" +
				"}");


	}

	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
		if (classname.equals("FunctionProfiler$Counter"))
			return;

		CtClass ctClass = pool.getCtClass(classname);
		instrument(ctClass);
	}

	private void instrument(CtClass ctClass) throws CannotCompileException {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			int m = ctMethod.getModifiers();
			if (Modifier.isPublic(m) && Modifier.isStatic(m) && ctMethod.getName().equals("main")) {
				ctMethod.insertAfter("{ FunctionProfiler$Counter.print(); }");
			}

			ctMethod.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					addReadCounter(f, ctClass);
					addWriteCounter(f, ctClass);
				}
			});
		}

		for (CtConstructor ctConstructor : ctClass.getConstructors()) {
			ctConstructor.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					addReadCounter(f, ctClass);
					if (!f.getClassName().equals(ctClass.getName())) {
						addWriteCounter(f, ctClass);
					}
				}
			});
		}

	}

	private void addReadCounter(FieldAccess f, CtClass ctClass) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isReader()) {
				f.replace("{" +
						"$_ = $proceed();" +
						"FunctionProfiler$Counter.addRead(\"" + f.getClassName() + "\");" +
						"}");
			}
		}
	}

	private void addWriteCounter(FieldAccess f, CtClass ctClass) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isWriter()) {
				f.replace("{" +
						"$proceed($$);" +
						"FunctionProfiler$Counter.addWrite(\"" + ctClass.getName() + "\");" +
						"}");
			}
		}
	}
}
