package spoon.gosu;

import spoon.compiler.Environment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.TokenWriter;

import java.util.List;

/**
 * A Spoon pretty-printer that emits Gosu instead of Java: types are
 * {@code class}/{@code enhancement}, fields are {@code var name : type},
 * functions are {@code function name(..) : type} and statements carry no
 * trailing semicolons.
 */
public class GosuPrettyPrinter extends DefaultJavaPrettyPrinter {

    public GosuPrettyPrinter(Environment env) {
        super(env);
    }

    /** Prints a single top-level type (no compilation-unit package header). */
    public String printType(CtType<?> type) {
        reset();
        type.accept(this);
        return getResult();
    }

    /** Gosu has no statement-terminating semicolons. */
    @Override
    protected void exitCtStatement(CtStatement statement) {
        // no-op
    }

    @Override
    public <T> void visitCtClass(CtClass<T> type) {
        getContext().pushCurrentThis(type);
        if (!type.isImplicit()) {
            TokenWriter w = getPrinterTokenWriter();
            boolean enhancement = isGosuEnhancement(type);
            w.writeKeyword(enhancement ? "enhancement" : "class");
            w.writeSpace();
            w.writeIdentifier(stripLeadingDigits(type.getSimpleName()));

            CtTypeReference<?> superclass = type.getSuperclass();
            if (superclass != null && !"java.lang.Object".equals(superclass.getQualifiedName())) {
                w.writeSeparator(" : ");
                scan(superclass);
            }
            w.writeSpace();
            w.writeSeparator("{");
            w.incTab();
        }
        getElementPrinterHelper().writeElementList(type.getTypeMembers());
        if (!type.isImplicit()) {
            getPrinterTokenWriter().decTab();
            getPrinterTokenWriter().writeSeparator("}");
        }
        getContext().popCurrentThis();
    }

    @Override
    public <T> void visitCtField(CtField<T> field) {
        TokenWriter w = getPrinterTokenWriter();
        if (field.isStatic()) {
            w.writeKeyword("static");
            w.writeSpace();
        }
        w.writeKeyword("var");
        w.writeSpace();
        w.writeIdentifier(field.getSimpleName());
        w.writeSeparator(" : ");
        scan(field.getType());
        if (field.getDefaultExpression() != null) {
            w.writeSpace();
            w.writeOperator("=");
            w.writeSpace();
            scan(field.getDefaultExpression());
        }
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method) {
        TokenWriter w = getPrinterTokenWriter();
        w.writeKeyword("function");
        w.writeSpace();
        w.writeIdentifier(stripLeadingDigits(method.getSimpleName()));
        w.writeSeparator("(");
        printCommaList(method.getParameters());
        w.writeSeparator(")");
        if (!isVoid(method.getType())) {
            w.writeSeparator(" : ");
            scan(method.getType());
        }
        w.writeSpace();
        scan(method.getBody());
    }

    @Override
    public <T> void visitCtConstructor(CtConstructor<T> constructor) {
        TokenWriter w = getPrinterTokenWriter();
        w.writeKeyword("construct");
        w.writeSeparator("(");
        printCommaList(constructor.getParameters());
        w.writeSeparator(")");
        w.writeSpace();
        scan(constructor.getBody());
    }

    @Override
    public <T> void visitCtParameter(CtParameter<T> parameter) {
        getPrinterTokenWriter().writeIdentifier(parameter.getSimpleName());
        getPrinterTokenWriter().writeSeparator(" : ");
        scan(parameter.getType());
    }

    @Override
    public void visitCtForEach(CtForEach foreach) {
        TokenWriter w = getPrinterTokenWriter();
        w.writeKeyword("for");
        w.writeSpace();
        w.writeSeparator("(");
        CtLocalVariable<?> variable = foreach.getVariable();
        if (variable != null) {
            w.writeIdentifier(variable.getSimpleName());
        }
        w.writeSpace();
        w.writeKeyword("in");
        w.writeSpace();
        if (foreach.getExpression() != null) {
            scan(foreach.getExpression());
        }
        w.writeSeparator(")");
        w.writeSpace();
        scan(foreach.getBody());
    }

    @Override
    public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
        TokenWriter w = getPrinterTokenWriter();
        w.writeKeyword("var");
        w.writeSpace();
        w.writeIdentifier(localVariable.getSimpleName());
        if (localVariable.getType() != null) {
            w.writeSpace();
            w.writeSeparator(":");
            w.writeSpace();
            scan(localVariable.getType());
        }
        if (localVariable.getDefaultExpression() != null) {
            w.writeSpace();
            w.writeOperator("=");
            w.writeSpace();
            scan(localVariable.getDefaultExpression());
        }
        w.writeSeparator(";");
    }

    @Override
    public <T> void visitCtConstructorCall(CtConstructorCall<T> constructorCall) {
        TokenWriter w = getPrinterTokenWriter();
        w.writeKeyword("new");
        w.writeSpace();
        scan(constructorCall.getType());
        w.writeSeparator("(");
        java.util.List<CtExpression<?>> args = constructorCall.getArguments();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                w.writeSeparator(",");
            }
            w.writeSpace();
            scan(args.get(i));
        }
        if (!args.isEmpty()) {
            w.writeSpace();
        }
        w.writeSeparator(")");
    }

    @Override
    public <T> void visitCtNewArray(CtNewArray<T> newArray) {
        TokenWriter w = getPrinterTokenWriter();
        w.writeSeparator("{");
        boolean first = true;
        for (CtExpression<?> element : newArray.getElements()) {
            if (!first) {
                w.writeSeparator(",");
            }
            w.writeSpace();
            scan(element);
            first = false;
        }
        if (!newArray.getElements().isEmpty()) {
            w.writeSpace();
        }
        w.writeSeparator("}");
    }

    private void printCommaList(List<? extends CtElement> elements) {
        TokenWriter w = getPrinterTokenWriter();
        for (int i = 0; i < elements.size(); i++) {
            scan(elements.get(i));
            if (i < elements.size() - 1) {
                w.writeSeparator(", ");
            }
        }
    }

    /** True when the Ct type carries the Gosu enhancement marker annotation. */
    public static boolean isGosuEnhancement(CtType<?> type) {
        if (type == null) {
            return false;
        }
        for (CtAnnotation<?> annotation : type.getAnnotations()) {
            if (GosuModelBuilder.GOSU_KIND_ANNOTATION.equals(
                    annotation.getAnnotationType().getQualifiedName())) {
                Object value = annotation.getValue("value");
                if (value instanceof spoon.reflect.code.CtLiteral<?>) {
                    value = ((spoon.reflect.code.CtLiteral<?>) value).getValue();
                }
                return GosuModelBuilder.GOSU_KIND_ENHANCEMENT.equals(String.valueOf(value));
            }
        }
        return false;
    }

    private static boolean isVoid(CtTypeReference<?> type) {
        return type != null && "void".equals(type.getQualifiedName());
    }
}