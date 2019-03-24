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
		if (classname.equals("ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime"))
			return;

		CtClass ctClass = pool.getCtClass(classname);
		instrument(ctClass);
	}

	private void instrument(CtClass ctClass) throws CannotCompileException {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			int m = ctMethod.getModifiers();
			if (Modifier.isPublic(m) && Modifier.isStatic(m) && ctMethod.getName().equals("main")) {
				ctMethod.insertAfter("{ ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.print(); }");
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
			ctConstructor.insertBeforeBody("{" +
					"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.currentConstructor = $0;" +
					"}");

			ctConstructor.insertAfter("{" +
					"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.currentConstructor = null;" +
					"}");

			ctConstructor.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					addReadCounter(f, ctClass);
					addConstructorWriteCounter(f, ctClass);
				}
			});
		}

	}

	private void addReadCounter(FieldAccess f, CtClass ctClass) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isReader()) {
				f.replace("{" +
						"$_ = $proceed();" +
						"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.addRead($0.getClass());" +
						"}");
			}
		}
	}

	private void addWriteCounter(FieldAccess f, CtClass ctClass) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isWriter()) {
				f.replace("{" +
						"$proceed($$);" +
						"ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.addWrite($0.getClass());" +
						"}");
			}
		}
	}

	private void addConstructorWriteCounter(FieldAccess f, CtClass ctClass) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isWriter()) {
				f.replace("{" +
						"$proceed($$);" +
						"if ($0 != ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.currentConstructor)" +
						"	ist.meic.pa.FunctionalProfiler.FunctionalProfilerRuntime.addWrite($0.getClass());" +
						"}");
			}
		}
	}
}
