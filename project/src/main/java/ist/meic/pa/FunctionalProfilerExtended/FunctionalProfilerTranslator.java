package ist.meic.pa.FunctionalProfilerExtended;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public class FunctionalProfilerTranslator implements Translator {
	@Override
	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
	}

	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
		if (classname.equals("ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime") ||
				classname.equals("ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime$IntPair"))
			return;

		CtClass ctClass = pool.getCtClass(classname);
		instrument(ctClass);
	}

	private void instrument(CtClass ctClass) throws CannotCompileException {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			int m = ctMethod.getModifiers();
			if (Modifier.isPublic(m) && Modifier.isStatic(m) && ctMethod.getName().equals("main")) {
				ctMethod.insertAfter("{ ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.print(); }");
			}

			ctMethod.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					try {
						addReadCounter(f);
						addWriteCounter(f);
					} catch (NotFoundException e) {
						System.err.println("Not found: " + e.getCause());
					}
				}
			});
		}

		for (CtConstructor ctConstructor : ctClass.getConstructors()) {
			ctConstructor.insertBeforeBody("{" +
					"ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.currentConstructor = $0;" +
					"}");

			ctConstructor.insertAfter("{" +
					"ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.currentConstructor = null;" +
					"}");

			ctConstructor.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					try {
						addReadCounter(f);
						addConstructorWriteCounter(f);
					} catch (NotFoundException e) {
						System.err.println("Not found: " + e.getCause());
					}
				}
			});
		}

	}

	private void addReadCounter(FieldAccess f) throws CannotCompileException, NotFoundException {
		if (!f.isStatic()) {
			if (f.isReader() && f.getField().hasAnnotation(FunctionalRecord.class.getTypeName())) {
				f.replace("{" +
						"$_ = $proceed();" +
						"ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.addRead($0.getClass(), \"" + f.getClassName() + "." + f.getFieldName() + "\");" +
						"}");
			}
		}
	}

	private void addWriteCounter(FieldAccess f) throws CannotCompileException, NotFoundException {
		if (!f.isStatic()) {
			if (f.isWriter() && f.getField().hasAnnotation(FunctionalRecord.class.getTypeName())) {
				f.replace("{" +
						"$proceed($$);" +
						"ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.addWrite($0.getClass(), \"" + f.getClassName() + "." + f.getFieldName() + "\");" +
						"}");
			}
		}
	}

	private void addConstructorWriteCounter(FieldAccess f) throws CannotCompileException, NotFoundException {
		if (!f.isStatic()) {
			if (f.isWriter() && f.getField().hasAnnotation(FunctionalRecord.class.getTypeName())) {
				f.replace("{" +
						"$proceed($$);" +
						"if ($0 != ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.currentConstructor)" +
						"	ist.meic.pa.FunctionalProfilerExtended.FunctionalProfilerRuntime.addWrite($0.getClass(), \"" + f.getClassName() + "." + f.getFieldName() + "\");" +
						"}");
			}
		}
	}
}
