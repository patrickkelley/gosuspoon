package spoon.gosu.phase0;

import spoon.Launcher;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.PrettyPrinter;

import static spoon.reflect.code.BinaryOperatorKind.PLUS;

/**
 * Minimal proof-of-concept: map the handful of Gosu AST facts collected by
 * {@link Phase0Probe} onto the Spoon Ct metamodel and pretty-print it.
 */
final class MinimalCtModel {

    private final Factory factory = new Launcher().getFactory();
    private CtClass<Object> clazz;
    private String methodName;

    void build(Phase0Probe.CompiledProbe probe, String className) {
        clazz = factory.Core().createClass();
        clazz.setSimpleName(className);

        // Map every parsed Gosu function onto a CtMethod.
        for (String fn : probe.functionNames) {
            // fn is rendered like "add(a:int, b:int)" — re-derive a name for the demo.
            String name = fn.substring(0, fn.indexOf('(')).trim();
            methodName = name;
            CtMethod<Object> m = factory.Core().createMethod();
            m.setSimpleName(name);
            m.setType(factory.Type().createReference(int.class));
            CtBlock<?> body = factory.Core().createBlock();
            CtReturn<Integer> ret = factory.Core().createReturn();
            ret.setReturnedExpression(sampleExpression());
            body.addStatement(ret);
            m.setBody(body);
            clazz.addMethod(m);
        }
    }

    private CtBinaryOperator<Integer> sampleExpression() {
        CtLiteral<Integer> a = factory.Code().createLiteral(40);
        CtLiteral<Integer> b = factory.Code().createLiteral(2);
        return factory.Code().createBinaryOperator(a, b, PLUS);
    }

    String methodName() {
        return methodName;
    }

    String print() {
        PrettyPrinter printer = factory.getEnvironment().createPrettyPrinter();
        return printer.printElement(clazz);
    }
}