package ist.meic.pa.FunctionalProfiler;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

import java.util.Objects;

public class FunctionalProfilerTranslator implements Translator {
	@Override
	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
	}

	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
		if (classname.equals("ist.meic.pa.FunctionalProfiler.FunctionalProfilerCounter"))
			return;

		CtClass ctClass = pool.getCtClass(classname);
		instrument(ctClass);
	}

	private void instrument(CtClass ctClass) throws CannotCompileException {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			int m = ctMethod.getModifiers();
			if (Modifier.isPublic(m) && Modifier.isStatic(m) && ctMethod.getName().equals("main")) {
				ctMethod.insertAfter("{ ist.meic.pa.FunctionalProfiler.FunctionalProfilerCounter.print(); }");
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

					if (f.getClassName() != null) {
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
						"ist.meic.pa.FunctionalProfiler.FunctionalProfilerCounter.addRead(\"" + f.getClassName() + "\");" +
						"}");
			}
		}
	}

	private void addWriteCounter(FieldAccess f, CtClass ctClass) throws CannotCompileException {
		if (!f.isStatic()) {
			if (f.isWriter()) {
				f.replace("{" +
						"$proceed($$);" +
						"ist.meic.pa.FunctionalProfiler.FunctionalProfilerCounter.addWrite(\"" + f.getClassName() + "\");" +
						"}");
			}
		}
	}
}
