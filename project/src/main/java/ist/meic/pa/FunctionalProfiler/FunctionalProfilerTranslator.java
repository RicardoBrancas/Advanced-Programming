package ist.meic.pa.FunctionalProfiler;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.Translator;

public class FunctionalProfilerTranslator implements Translator {
	@Override
	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {

	}

	@Override
	public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {

	}
}
