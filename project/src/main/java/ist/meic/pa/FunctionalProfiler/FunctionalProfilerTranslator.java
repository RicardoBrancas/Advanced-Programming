package ist.meic.pa.FunctionalProfiler;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public class FunctionalProfilerTranslator implements Translator {

	@Override
	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
	}

	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
		if (classname.equals("ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime") ||
				classname.equals("ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime$IntPair"))
			return;

		CtClass ctClass = pool.getCtClass(classname);
		instrument(ctClass);
	}

	private void instrument(CtClass ctClass) throws CannotCompileException {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			ctMethod.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					addReadCounter(f);
					addWriteCounter(f);
				}
			});
		}

		for (CtConstructor ctConstructor : ctClass.getConstructors()) {
			ctConstructor.insertBeforeBody("{" +
					"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.currentConstructor = $0;" +
					"}");

			ctConstructor.insertAfter("{" +
					"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.currentConstructor = null;" +
					"}");

			ctConstructor.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					addReadCounter(f);
					addConstructorWriteCounter(f);
				}
			});
		}

	}

	private void addReadCounter(FieldAccess f) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isReader()) {
				f.replace("{" +
						"$_ = $proceed();" +
						"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.addRead($0.getClass());" +
						"}");
			}
		}
	}

	private void addWriteCounter(FieldAccess f) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isWriter()) {
				f.replace("{" +
						"$proceed($$);" +
						"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.addWrite($0.getClass());" +
						"}");
			}
		}
	}

	private void addConstructorWriteCounter(FieldAccess f) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isWriter()) {
				f.replace("{" +
						"$proceed($$);" +
						"if ($0 != ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.currentConstructor) {" +
						"	ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.addWrite($0.getClass()); }" +
						"}");
			}
		}
	}
}
