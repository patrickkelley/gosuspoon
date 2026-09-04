/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import gw.internal.gosu.parser.DynamicFunctionSymbol;
import gw.internal.gosu.parser.IGosuClassInternal;
import gw.internal.gosu.parser.IGosuEnhancementInternal;
import gw.internal.gosu.parser.MemberFieldSymbol;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.IParseTree;
import gw.lang.parser.ISymbol;
import gw.lang.parser.IStatement;
import gw.lang.parser.expressions.IBooleanLiteralExpression;
import gw.lang.parser.expressions.IBeanMethodCallExpression;
import gw.lang.parser.expressions.IBlockExpression;
import gw.lang.parser.expressions.IBlockInvocation;
import gw.lang.parser.expressions.ICharLiteralExpression;
import gw.lang.parser.expressions.ICollectionInitializerExpression;
import gw.lang.parser.expressions.IConditionalAndExpression;
import gw.lang.parser.expressions.IConditionalExpression;
import gw.lang.parser.expressions.IConditionalOrExpression;
import gw.lang.parser.expressions.IConditionalTernaryExpression;
import gw.lang.parser.expressions.IEqualityExpression;
import gw.lang.parser.expressions.IIdentifierExpression;
import gw.lang.parser.expressions.IImplicitTypeAsExpression;
import gw.lang.parser.expressions.IIntervalExpression;
import gw.lang.parser.expressions.IMapAccessExpression;
import gw.lang.parser.expressions.IMemberAccessExpression;
import gw.lang.parser.expressions.INewExpression;
import gw.lang.parser.expressions.INullExpression;
import gw.lang.parser.expressions.INumericLiteralExpression;
import gw.lang.parser.expressions.IParameterDeclaration;
import gw.lang.parser.expressions.IParenthesizedExpression;
import gw.lang.parser.expressions.IRelationalExpression;
import gw.lang.parser.expressions.IStringLiteralExpression;
import gw.lang.parser.expressions.ITypeAsExpression;
import gw.lang.parser.expressions.IUnaryExpression;
import gw.lang.parser.expressions.IUnaryNotPlusMinusExpression;
import gw.lang.parser.expressions.IVarStatement;
import gw.lang.parser.statements.IAssertStatement;
import gw.lang.parser.statements.IAssignmentStatement;
import gw.lang.parser.statements.IBeanMethodCallStatement;
import gw.lang.parser.statements.IBreakStatement;
import gw.lang.parser.statements.ICaseClause;
import gw.lang.parser.statements.ICatchClause;
import gw.lang.parser.statements.IContinueStatement;
import gw.lang.parser.statements.IDoWhileStatement;
import gw.lang.parser.statements.IExpressionStatement;
import gw.lang.parser.statements.IForEachStatement;
import gw.lang.parser.statements.IFunctionStatement;
import gw.lang.parser.statements.IIfStatement;
import gw.lang.parser.statements.IMapAssignmentStatement;
import gw.lang.parser.statements.IMemberAssignmentStatement;
import gw.lang.parser.statements.IMethodCallStatement;
import gw.lang.parser.statements.IReturnStatement;
import gw.lang.parser.statements.IStatementList;
import gw.lang.parser.statements.ISwitchStatement;
import gw.lang.parser.statements.IThrowStatement;
import gw.lang.parser.statements.ITryCatchFinallyStatement;
import gw.internal.gosu.parser.IGosuAnnotation;
import gw.internal.gosu.parser.expressions.AnnotationExpression;
import gw.internal.gosu.parser.expressions.ParameterDeclaration;
import gw.lang.parser.statements.IUsingStatement;
import gw.lang.reflect.IModifierInfo;
import gw.lang.parser.statements.IWhileStatement;
import gw.lang.reflect.IType;
import gw.lang.reflect.IPropertyInfo;
import gw.lang.reflect.gs.ICompilableType;
import gw.lang.reflect.gs.IGenericTypeVariable;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.IGosuMethodInfo;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtResource;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtIntersectionTypeReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Builds a Spoon Ct model from Gosu source, mirroring Spoon's
* {@code ModelBuilder} for Java. Type loading goes through
* {@link GosuEnvironment}; member ASTs are read from each compiled type's
* parse-info (fields, constructors, functions) and from the bodies' statement
* trees. Types are attached to their {@code CtPackage}; {@code uses} clauses
* are captured as real {@code CtImport} elements.
*/
public class GosuModelBuilder {

	/** Marker annotation attached to Ct types and methods for Gosu-specific constructs. */
	public static final String GOSU_KIND_ANNOTATION = "spoon.gosu.meta.GosuKind";
	public static final String GOSU_KIND_ENHANCEMENT = "ENHANCEMENT";
	public static final String GOSU_KIND_CLASS = "CLASS";
	public static final String GOSU_KIND_STRUCTURE = "STRUCTURE";
	public static final String GOSU_KIND_PROPERTY_GET = "PROPERTY_GET";
	public static final String GOSU_KIND_PROPERTY_SET = "PROPERTY_SET";

	/**
	* Annotation on an enhancement Ct type carrying the {@code CtTypeReference}
	* of the type it enhances, so analysis never sees a fake superclass.
	*/
	public static final String GOSU_ENHANCED_TYPE_ANNOTATION = "spoon.gosu.meta.GosuEnhancedType";

	private static final Pattern USES_PATTERN =
			Pattern.compile("(?m)^\\s*uses\\s+([\\w.]+(?:\\.[*])?)\\s*(?://.*)?$");

	private static final Pattern JAVA_LANG_PREFIX =
			Pattern.compile("\\bjava\\.lang\\.([A-Z]\\w*)");

	private final Factory factory;
	private final GosuEnvironment gosu;

	private final Map<CtType<?>, List<CtImport>> typeImports = new LinkedHashMap<>();

	private CtType<?> currentClass;
	private CtTypeReference<Object> currentThisType;
	private Set<String> currentFields = new LinkedHashSet<>();
	private final Map<String, IType> currentFieldTypes = new LinkedHashMap<>();
	private List<String> currentUses = new ArrayList<>();
	private CharSequence currentSource;

	public GosuModelBuilder(Factory factory, GosuEnvironment gosu) {
		this.factory = factory;
		this.gosu = gosu;
	}

	public Factory getFactory() {
		return factory;
	}

	public GosuEnvironment getGosuEnvironment() {
		return gosu;
	}

	/** The {@code uses} clauses of each built type, as Ct import elements. */
	public Map<CtType<?>, List<CtImport>> getTypeImports() {
		return typeImports;
	}

	/** Scans the source directory and builds a Ct type for every {@code .gs}/{@code .gsx} file. */
	public List<CtType<?>> buildAll(java.io.File sourceDir) {
		List<CtType<?>> result = new ArrayList<>();
		for (String name : gosu.scanTypeNames(sourceDir)) {
			ICompilableType ct = gosu.loadType(name);
			if (!(ct instanceof IGosuClass)) {
				continue;
			}
			CtType<?> built = buildType((IGosuClass) ct);
			if (built != null) {
				result.add(built);
			}
		}
		return result;
	}

	/** Builds a Ct type for a single already-loaded Gosu class/enhancement. */
	public CtType<?> buildType(IGosuClass gosuClass) {
		if (!(gosuClass instanceof IGosuClassInternal)) {
			return null;
		}
		IGosuClassInternal gs = (IGosuClassInternal) gosuClass;
		gs.compileDeclarationsIfNeeded();
		try {
			gs.compileDefinitionsIfNeeded();
		} catch (Throwable ignored) {
			// bodies are best-effort; declarations still get built
		}

		String fqn = gs.getName();
		int dot = fqn.lastIndexOf('.');
		String simpleName = dot < 0 ? fqn : fqn.substring(dot + 1);
		String pkgName = dot < 0 ? null : fqn.substring(0, dot);

		boolean enhancement = gosuClass instanceof IGosuEnhancementInternal;
		IType enhancedType = enhancement
				? ((IGosuEnhancementInternal) gosuClass).getEnhancedType()
				: null;

		currentClass = null;
		currentThisType = null;
		currentFields.clear();
		currentFieldTypes.clear();
		currentUses.clear();
		currentSource = null;

		try {
			CtType<?> ctType;
			if (gs.isEnum()) {
				@SuppressWarnings("rawtypes")
				CtEnum ctEnum = factory.createEnum();
				for (String constant : gs.getEnumConstants()) {
					@SuppressWarnings("rawtypes")
					CtEnumValue ev = factory.createEnumValue();
					ev.setSimpleName(constant);
					ctEnum.addEnumValue(ev);
				}
				ctType = ctEnum;
			} else if (gs.isAnnotation()) {
				@SuppressWarnings("rawtypes")
				CtAnnotationType ctAnno = factory.createAnnotationType();
				ctType = ctAnno;
			} else if (gs.isStructure()) {
				CtInterface<Object> ctStruct = factory.createInterface();
				addGosuKind(ctStruct, GOSU_KIND_STRUCTURE);
				ctType = ctStruct;
			} else if (gs.isInterface()) {
				ctType = factory.createInterface();
			} else {
				CtClass<Object> ctClass = factory.createClass();
				addGosuKind(ctClass, enhancement ? GOSU_KIND_ENHANCEMENT : GOSU_KIND_CLASS);
				ctType = ctClass;
			}

			ctType.setSimpleName(simpleName);
			if (gs.getGenericTypeVariables() != null) {
				for (IGenericTypeVariable gtv : gs.getGenericTypeVariables()) {
					ctType.addFormalCtTypeParameter(buildTypeParameter(gtv));
				}
			}
			if (gs.getModifierInfo() != null) {
				addAnnotations(gs.getModifierInfo(), ctType);
			}
			currentClass = ctType;
			currentThisType = null;

			if (pkgName != null) {
				CtPackage pkg = factory.Package().getOrCreate(pkgName);
				pkg.addType(ctType);
			}

			if (enhancement) {
				if (enhancedType != null) {
					CtTypeReference<Object> enhancedRef = mapType(enhancedType);
					addEnhancedType(ctType, enhancedRef);
					currentThisType = enhancedRef;
				}
			} else if (ctType instanceof CtClass<?>) {
				IGosuClassInternal superClass = gs.getSuperClass();
				if (superClass != null && !"java.lang.Object".equals(superClass.getName())) {
					((CtClass<?>) ctType).setSuperclass(
							factory.Type().createReference(superClass.getName()));
				}
			}

			currentUses = captureUses(gs.getSource());
			currentSource = gs.getSource();
			typeImports.put(ctType, buildImports(currentUses));

			// members
			Map<String, gw.internal.gosu.parser.statements.VarStatement> fields =
					new LinkedHashMap<>(gs.getParseInfo().getMemberFields());
			for (gw.internal.gosu.parser.statements.VarStatement staticField
					: gs.getParseInfo().getStaticFields().values()) {
				fields.putIfAbsent(staticField.getIdentifierName(), staticField);
			}
			currentFields = new LinkedHashSet<>(fields.keySet());
			if (enhancement && enhancedType != null) {
				addEnhancedTypeFields(enhancedType);
			}
			for (gw.internal.gosu.parser.statements.VarStatement field : fields.values()) {
				if (field.isEnumConstant()) {
					continue;
				}
				ctType.addField(buildField(field));
				if (field.getSymbol() != null) {
					currentFieldTypes.put(
							field.getIdentifierName(), field.getSymbol().getType());
				}
			}

			if (ctType instanceof CtClass<?>) {
				for (DynamicFunctionSymbol dfs : gs.getConstructorFunctions()) {
					IFunctionStatement decl = dfs.getDeclFunctionStmt();
					if (decl != null) {
						@SuppressWarnings({"rawtypes", "unchecked"})
						CtClass rawClass = (CtClass) ctType;
						rawClass.addConstructor(buildConstructor(dfs, decl));
					}
				}
			}

			for (DynamicFunctionSymbol dfs : gs.getParseInfo().getMemberFunctions().values()) {
				IFunctionStatement decl = dfs.getDeclFunctionStmt();
				if (decl != null) {
					ctType.addMethod(buildMethod(dfs, decl));
				}
			}

			return ctType;
		} finally {
			currentClass = null;
			currentThisType = null;
			currentFields.clear();
			currentFieldTypes.clear();
			currentUses.clear();
			currentSource = null;
		}
	}

	private void addEnhancedTypeFields(IType enhancedType) {
		if (enhancedType instanceof IGosuClassInternal) {
			IGosuClassInternal gs = (IGosuClassInternal) enhancedType;
			Map<String, gw.internal.gosu.parser.statements.VarStatement> fields =
					new LinkedHashMap<>(gs.getParseInfo().getMemberFields());
			for (gw.internal.gosu.parser.statements.VarStatement staticField
					: gs.getParseInfo().getStaticFields().values()) {
				fields.putIfAbsent(staticField.getIdentifierName(), staticField);
			}
			for (gw.internal.gosu.parser.statements.VarStatement field : fields.values()) {
				if (!field.isEnumConstant() && currentFields.add(field.getIdentifierName())
						&& field.getSymbol() != null) {
					currentFieldTypes.put(field.getIdentifierName(), field.getSymbol().getType());
				}
			}
		} else {
			for (IPropertyInfo property : enhancedType.getTypeInfo().getProperties()) {
				if (property.isReadable() && currentFields.add(property.getName())) {
					currentFieldTypes.put(property.getName(), property.getFeatureType());
				}
			}
		}
	}

	private void addEnhancedType(CtType<?> ctType, CtTypeReference<Object> enhancedType) {
		CtTypeReference<Annotation> ref =
				factory.Type().createReference(GOSU_ENHANCED_TYPE_ANNOTATION);
		CtAnnotation<Annotation> marker = factory.createAnnotation(ref);
		marker.addValue("value", enhancedType.getQualifiedName());
		ctType.addAnnotation(marker);
	}

	private void addGosuKind(CtElement element, String kind) {
		CtTypeReference<Annotation> ref =
				factory.Type().createReference(GOSU_KIND_ANNOTATION);
		CtAnnotation<Annotation> marker = factory.createAnnotation(ref);
		marker.addValue("value", kind);
		element.addAnnotation(marker);
	}

	private List<CtImport> buildImports(List<String> uses) {
		List<CtImport> imports = new ArrayList<>();
		for (String use : uses) {
			CtImport imp;
			if (use.endsWith(".*")) {
				CtPackageReference pkgRef = factory.Package()
						.createReference(use.substring(0, use.length() - 2));
				imp = factory.createImport(pkgRef);
			} else {
				imp = factory.createImport(factory.Type().createReference(use));
			}
			imports.add(imp);
		}
		return imports;
	}

	private static List<String> captureUses(String source) {
		List<String> uses = new ArrayList<>();
		if (source == null) {
			return uses;
		}
		Matcher m = USES_PATTERN.matcher(source);
		while (m.find()) {
			uses.add(m.group(1));
		}
		return uses;
	}

	// ------------------------------------------------------------------
	// members
	// ------------------------------------------------------------------

	private CtField<?> buildField(gw.internal.gosu.parser.statements.VarStatement var) {
		CtField<Object> field = factory.createField();
		field.setSimpleName(var.getIdentifierName());
		field.setType(mapType(var.getType()));
		copyModifiers(var, field);
		if (var.getModifierInfo() != null) {
			addAnnotations(var.getModifierInfo(), field);
		}
		if (var.getAsExpression() != null) {
			field.setDefaultExpression(mapExpression(var.getAsExpression(), field));
		}
		return field;
	}

	private CtConstructor<Object> buildConstructor(DynamicFunctionSymbol dfs,
											IFunctionStatement decl) {
		CtConstructor<Object> ctor = factory.createConstructor();
		copyModifiers(dfs, ctor);
		if (dfs.getModifierInfo() != null) {
			addAnnotations(dfs.getModifierInfo(), ctor);
		}
		for (IParameterDeclaration p : decl.getParameters()) {
			ctor.addParameter(buildParameter(p));
		}
		ctor.setBody(buildBody(decl));
		return ctor;
	}

	private CtMethod<?> buildMethod(DynamicFunctionSymbol dfs, IFunctionStatement decl) {
		CtMethod<Object> method = factory.createMethod();
		String name = dfs.getDisplayName();
		boolean prop = name.startsWith("@");
		if (prop) {
			name = name.substring(1);
			boolean isSetter = dfs.getReturnType() == null
					|| "void".equals(dfs.getReturnType().getName());
			addGosuKind(method, isSetter ? GOSU_KIND_PROPERTY_SET : GOSU_KIND_PROPERTY_GET);
		} else if (dfs.hasTypeVariables()) {
			Object mi = dfs.getMethodOrConstructorInfo();
			if (mi instanceof IGosuMethodInfo) {
				IGenericTypeVariable[] tvs = ((IGosuMethodInfo) mi).getTypeVariables();
				if (tvs != null) {
					for (IGenericTypeVariable gtv : tvs) {
						method.addFormalCtTypeParameter(buildTypeParameter(gtv));
					}
				}
			}
		}
		copyModifiers(dfs, method);
		if (dfs.getModifierInfo() != null) {
			addAnnotations(dfs.getModifierInfo(), method);
		}
		method.setSimpleName(name);
		method.setType(mapType(dfs.getReturnType()));
		for (IParameterDeclaration p : decl.getParameters()) {
			method.addParameter(buildParameter(p));
		}
		CtBlock<?> body = buildBody(decl);
		if (body != null && (!body.getStatements().isEmpty()
				|| (decl.getGosuClass() != null && !decl.getGosuClass().isInterface()))) {
			method.setBody(body);
		}
		return method;
	}

	private void addAnnotations(IModifierInfo mi, CtElement element) {
		if (mi == null || mi.getAnnotations() == null) {
			return;
		}
		for (Object obj : mi.getAnnotations()) {
			if (obj instanceof IGosuAnnotation) {
				element.addAnnotation(buildAnnotation((IGosuAnnotation) obj, element));
			}
		}
	}

	private CtAnnotation<Annotation> buildAnnotation(IGosuAnnotation ga, CtElement context) {
		CtTypeReference<Annotation> annoType = mapType(ga.getType());
		CtAnnotation<Annotation> ctAnno = factory.createAnnotation(annoType);
		gw.lang.parser.IExpression expr = ga.getExpression();
		if (expr instanceof AnnotationExpression) {
			AnnotationExpression ae = (AnnotationExpression) expr;
			gw.lang.parser.IExpression[] args = ae.getArgs();
			if (args != null && args.length > 0) {
				if (args.length == 1) {
					ctAnno.addValue("value", mapExpression(args[0], ctAnno));
				} else {
					for (int i = 0; i < args.length; i++) {
						ctAnno.addValue("param" + i, mapExpression(args[i], ctAnno));
					}
				}
			}
		}
		return ctAnno;
	}

	private CtTypeParameter buildTypeParameter(IGenericTypeVariable gtv) {
		CtTypeParameter tp = factory.createTypeParameter();
		tp.setSimpleName(gtv.getName());
		IType bound = gtv.getBoundingType();
		if (bound != null && !"java.lang.Object".equals(bound.getName())) {
			if (bound.isCompoundType()) {
				@SuppressWarnings("rawtypes")
				CtIntersectionTypeReference inter = factory.createIntersectionTypeReference();
				for (IType comp : bound.getCompoundTypeComponents()) {
					inter.addBound(mapType(comp));
				}
				tp.setSuperclass(inter);
			} else {
				tp.setSuperclass(mapType(bound));
			}
		}
		return tp;
	}

	private CtParameter<?> buildParameter(IParameterDeclaration p) {
		CtParameter<Object> param = factory.createParameter();
		param.setSimpleName(p.getLocalVarName().toString());
		param.setType(mapType(p.getType()));
		if (p instanceof ParameterDeclaration) {
			List<?> annos = ((ParameterDeclaration) p).getAnnotations();
			if (annos != null) {
				for (Object obj : annos) {
					if (obj instanceof IGosuAnnotation) {
						param.addAnnotation(buildAnnotation((IGosuAnnotation) obj, param));
					}
				}
			}
		}
		return param;
	}

	// ------------------------------------------------------------------
	// statements
	// ------------------------------------------------------------------

	private CtBlock<?> buildBody(IFunctionStatement decl) {
		CtBlock<Object> block = factory.createBlock();
		List<IStatement> top = new ArrayList<>();
		decl.getContainedParsedElementsByType(IStatement.class, top);
		for (IStatement child : top) {
			if (child.getParent() != decl) {
				continue;
			}
			if (child instanceof IStatementList) {
				for (IStatement s : ((IStatementList) child).getStatements()) {
					CtStatement mapped = mapStatementTree(s, block);
					if (mapped != null) {
						block.addStatement(mapped);
					}
				}
			} else {
				CtStatement mapped = mapStatementTree(child, block);
				if (mapped != null) {
					block.addStatement(mapped);
				}
			}
		}
		return block;
	}

	private CtStatement mapStatementTree(IParsedElement el, CtElement context) {
		if (el instanceof IStatementList) {
			return mapBlock((IStatementList) el);
		}
		if (el instanceof IIfStatement) {
			IIfStatement iff = (IIfStatement) el;
			CtIf ctIf = factory.createIf();
			ctIf.setCondition(castBool(mapExpression(iff.getExpression(), ctIf)));
			ctIf.setThenStatement(mapBodyOrStatement(iff.getStatement(), ctIf));
			if (iff.hasElseStatement()) {
				ctIf.setElseStatement(mapBodyOrStatement(iff.getElseStatement(), ctIf));
			}
			return ctIf;
		}
		if (el instanceof IWhileStatement) {
			IWhileStatement wh = (IWhileStatement) el;
			CtWhile ctWhile = factory.createWhile();
			ctWhile.setLoopingExpression(
					castBool(mapExpression(wh.getExpression(), ctWhile)));
			ctWhile.setBody(mapBodyOrStatement(wh.getStatement(), ctWhile));
			return ctWhile;
		}
		if (el instanceof IDoWhileStatement) {
			IDoWhileStatement dw = (IDoWhileStatement) el;
			CtDo ctDo = factory.createDo();
			ctDo.setLoopingExpression(
					castBool(mapExpression(dw.getExpression(), ctDo)));
			ctDo.setBody(mapBodyOrStatement(dw.getStatement(), ctDo));
			return ctDo;
		}
		if (el instanceof ISwitchStatement) {
			ISwitchStatement sw = (ISwitchStatement) el;
			CtSwitch<Object> ctSw = factory.Core().createSwitch();
			ctSw.setSelector(cast(mapExpression(sw.getSwitchExpression(), ctSw)));
			for (ICaseClause clause : sw.getCases()) {
				CtCase<Object> ctCase = factory.Core().createCase();
				ctCase.setCaseExpression(cast(mapExpression(clause.getExpression(), ctCase)));
				for (IStatement caseStmt : clause.getStatements()) {
					CtStatement mapped = mapStatementTree(caseStmt, ctCase);
					if (mapped != null) {
						ctCase.addStatement(mapped);
					}
				}
				ctSw.addCase(ctCase);
			}
			if (!sw.getDefaultStatements().isEmpty()) {
				CtCase<Object> ctDefault = factory.Core().createCase();
				ctDefault.setIncludesDefault(true);
				for (IStatement defStmt : sw.getDefaultStatements()) {
					CtStatement mapped = mapStatementTree(defStmt, ctDefault);
					if (mapped != null) {
						ctDefault.addStatement(mapped);
					}
				}
				ctSw.addCase(ctDefault);
			}
			return ctSw;
		}
		if (el instanceof IThrowStatement) {
			IThrowStatement thr = (IThrowStatement) el;
			CtThrow ctThrow = factory.Core().createThrow();
			ctThrow.setThrownExpression(
					(CtExpression<? extends Throwable>) (CtExpression<?>)
							mapExpression(thr.getExpression(), ctThrow));
			return ctThrow;
		}
		if (el instanceof ITryCatchFinallyStatement) {
			ITryCatchFinallyStatement tcf = (ITryCatchFinallyStatement) el;
			CtTry ctTry = factory.Core().createTry();
			ctTry.setBody((CtBlock<?>) mapBodyOrStatement(tcf.getTryStatement(), ctTry));
			for (ICatchClause clause : tcf.getCatchStatements()) {
				CtCatch ctCatch = factory.Core().createCatch();
				CtCatchVariable<Object> variable = factory.Core().createCatchVariable();
				ISymbol symbol = clause.getSymbol();
				variable.setSimpleName(symbol == null ? "" : symbol.getName());
				variable.setType(mapType(clause.getCatchType()));
				ctCatch.setParameter((CtCatchVariable) variable);
				ctCatch.setBody((CtBlock<?>) mapBodyOrStatement(clause.getCatchStmt(), ctCatch));
				ctTry.addCatcher(ctCatch);
			}
			if (tcf.getFinallyStatement() != null) {
				ctTry.setFinalizer((CtBlock<?>) mapBodyOrStatement(
						tcf.getFinallyStatement(), ctTry));
			}
			return ctTry;
		}
		if (el instanceof IForEachStatement) {
			IForEachStatement fe = (IForEachStatement) el;
			CtForEach ctFor = factory.createForEach();
			CtLocalVariable<Object> var = factory.createLocalVariable();
			ISymbol symbol = fe.getIdentifier();
			var.setSimpleName(symbol == null ? "" : symbol.getName());
			var.setType(mapType(symbol == null ? null : symbol.getType()));
			ctFor.setVariable(var);
			ctFor.setExpression(mapExpression(fe.getInExpression(), ctFor));
			ctFor.setBody(mapBodyOrStatement(fe.getStatement(), ctFor));
			return ctFor;
		}
		if (el instanceof IReturnStatement) {
			IReturnStatement ret = (IReturnStatement) el;
			CtReturn<Object> r = factory.createReturn();
			if (ret.getValue() != null) {
				r.setReturnedExpression(mapExpression(ret.getValue(), r));
			}
			return r;
		}
		if (el instanceof IBreakStatement) {
			return factory.createBreak();
		}
		if (el instanceof IContinueStatement) {
			return factory.createContinue();
		}
		if (el instanceof IBeanMethodCallStatement) {
			return asStatementExpression(mapExpression(
					((IBeanMethodCallStatement) el).getBeanMethodCall(), context));
		}
		if (el instanceof IMethodCallStatement) {
			return asStatementExpression(mapExpression(
					((IMethodCallStatement) el).getMethodCall(), context));
		}
		if (el instanceof IExpressionStatement) {
			return asStatementExpression(mapExpression(
					((IExpressionStatement) el).getExpression(), context));
		}
		if (el instanceof IMapAssignmentStatement) {
			IMapAssignmentStatement assign = (IMapAssignmentStatement) el;
			IMapAccessExpression acc = assign.getMapAccessExpression();
			CtArrayRead<Object> lhs = factory.createArrayRead();
			lhs.setTarget(mapExpression(acc.getRootExpression(), lhs));
			lhs.setIndexExpression(castInt(mapExpression(acc.getKeyExpression(), lhs)));
			lhs.setType(mapType(acc.getType()));
			CtAssignment<Object, Object> a = factory.createAssignment();
			a.setAssigned(lhs);
			a.setAssignment(cast(mapExpression(assign.getExpression(), a)));
			return a;
		}
		if (el instanceof IMemberAssignmentStatement) {
			IMemberAssignmentStatement assign = (IMemberAssignmentStatement) el;
			CtAssignment<Object, Object> a = factory.createAssignment();
			a.setAssigned(memberAccess(assign.getMemberAccess(), true, null));
			a.setAssignment(mapExpression(assign.getExpression(), a));
			return a;
		}
		if (el instanceof IAssignmentStatement) {
			IAssignmentStatement assign = (IAssignmentStatement) el;
			CtAssignment<Object, Object> a = factory.createAssignment();
			a.setAssigned(mapLValue(assign.getIdentifier()));
			a.setAssignment(mapExpression(assign.getExpression(), a));
			return a;
		}
		if (el instanceof IVarStatement) {
			IVarStatement local = (IVarStatement) el;
			CtLocalVariable<Object> v = factory.createLocalVariable();
			v.setSimpleName(local.getIdentifierName());
			v.setType(mapType(local.getType()));
			if (local.getAsExpression() != null) {
				v.setDefaultExpression(mapExpression(local.getAsExpression(), v));
			}
			return v;
		}
		if (el instanceof IAssertStatement) {
			IAssertStatement as = (IAssertStatement) el;
			CtAssert<Object> ctAssert = factory.createAssert();
			ctAssert.setAssertExpression(castBool(mapExpression(as.getCondition(), ctAssert)));
			if (as.getDetail() != null) {
				ctAssert.setExpression(mapExpression(as.getDetail(), ctAssert));
			}
			return ctAssert;
		}
		if (el instanceof gw.internal.gosu.parser.statements.UsingStatement) {
			gw.internal.gosu.parser.statements.UsingStatement us =
					(gw.internal.gosu.parser.statements.UsingStatement) el;
			CtTryWithResource ctTry = factory.createTryWithResource();
			if (us.getVarStatements() != null) {
				for (IStatement v : us.getVarStatements()) {
					CtStatement mapped = mapStatementTree(v, ctTry);
					if (mapped instanceof CtLocalVariable<?>) {
						ctTry.addResource((CtLocalVariable<?>) mapped);
					}
				}
			}
			if (us.getExpression() != null) {
				CtExpression<?> expr = mapExpression(us.getExpression(), ctTry);
				if (expr instanceof CtResource<?>) {
					ctTry.addResource((CtResource<?>) expr);
				} else if (expr != null) {
					CtLocalVariable<Object> wrap = factory.createLocalVariable();
					wrap.setImplicit(true);
					wrap.setDefaultExpression(cast(expr));
					wrap.setType(expr.getType());
					ctTry.addResource(wrap);
				}
			}
			ctTry.setBody(mapBodyOrStatement(us.getStatement(), ctTry));
			return ctTry;
		}
		if (el instanceof gw.internal.gosu.parser.statements.EvalStatement) {
			gw.internal.gosu.parser.statements.EvalStatement es =
					(gw.internal.gosu.parser.statements.EvalStatement) el;
			return asStatementExpression(mapExpression(es.getEvalExpression(), context));
		}
		if (el instanceof IUsingStatement) {
			return factory.createCodeSnippetStatement(sourceSlice(el));
		}
		return null;
	}

	private CtStatement asStatementExpression(CtExpression<?> expression) {
		return expression instanceof CtStatement ? (CtStatement) expression : null;
	}

	private CtStatement mapBodyOrStatement(IStatement body, CtElement context) {
		CtBlock<Object> block = factory.createBlock();
		IStatement[] statements = body instanceof IStatementList
				? ((IStatementList) body).getStatements()
				: new IStatement[]{body};
		for (IStatement child : statements) {
			CtStatement mapped = mapStatementTree(child, block);
			if (mapped != null) {
				block.addStatement(mapped);
			}
		}
		return block;
	}

	private CtBlock<?> mapBlock(IStatementList list) {
		CtBlock<Object> block = factory.createBlock();
		for (IStatement child : list.getStatements()) {
			CtStatement mapped = mapStatementTree(child, block);
			if (mapped != null) {
				block.addStatement(mapped);
			}
		}
		return block;
	}

	// ------------------------------------------------------------------
	// expressions
	// ------------------------------------------------------------------

	private CtExpression<Object> mapExpression(IParsedElement el, CtElement context) {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (el instanceof gw.lang.parser.IExpression
				&& ((gw.lang.parser.IExpression) el).isNullSafe()) {
			// Null-safe member chains (l.?size()) have no JLS counterpart;
			// keep the source text verbatim as a snippet.
			return snippet(sourceSlice(el));
		}
		if (el instanceof IStringLiteralExpression) {
			return expr(literal(((IStringLiteralExpression) el).getValue()));
		}
		if (el instanceof INumericLiteralExpression) {
			return expr(literal(((INumericLiteralExpression) el).getValue()));
		}
		if (el instanceof IBooleanLiteralExpression) {
			return expr(literal(((IBooleanLiteralExpression) el).getValue()));
		}
		if (el instanceof INullExpression) {
			return expr(literal(null));
		}
		if (el instanceof ICharLiteralExpression) {
			return expr(literal(((ICharLiteralExpression) el).getValue()));
		}
		if (el instanceof IParenthesizedExpression) {
			return mapExpression(((IParenthesizedExpression) el).getExpression(), context);
		}
		if (el instanceof IImplicitTypeAsExpression) {
			return mapExpression(((ITypeAsExpression) el).getLHS(), context);
		}
		if (el instanceof gw.internal.gosu.parser.expressions.TypeIsExpression) {
			gw.internal.gosu.parser.expressions.TypeIsExpression tis =
					(gw.internal.gosu.parser.expressions.TypeIsExpression) el;
			CtBinaryOperator<Boolean> bin = factory.createBinaryOperator();
			bin.setKind(BinaryOperatorKind.INSTANCEOF);
			bin.setLeftHandOperand(cast(mapExpression(tis.getLHS(), bin)));
			IType rhsType = tis.getRHS().getType();
			if (rhsType instanceof gw.lang.reflect.IMetaType) {
				rhsType = ((gw.lang.reflect.IMetaType) rhsType).getType();
			}
			bin.setRightHandOperand(factory.Code().createTypeAccess(mapType(rhsType)));
			bin.setType(factory.Type().booleanPrimitiveType());
			return cast(bin);
		}
		if (el instanceof gw.internal.gosu.parser.expressions.TypeOfExpression) {
			gw.internal.gosu.parser.expressions.TypeOfExpression tof =
					(gw.internal.gosu.parser.expressions.TypeOfExpression) el;
			CtInvocation<Object> inv = factory.createInvocation();
			CtExecutableReference<Object> ref = factory.createExecutableReference();
			ref.setSimpleName("typeof");
			inv.setExecutable(ref);
			inv.addArgument(cast(mapExpression(tof.getExpression(), inv)));
			inv.setType(mapType(tof.getType()));
			return inv;
		}
		if (el instanceof gw.internal.gosu.parser.expressions.TypeAsExpression) {
			gw.internal.gosu.parser.expressions.TypeAsExpression tas =
					(gw.internal.gosu.parser.expressions.TypeAsExpression) el;
			CtExpression<Object> expr = cast(mapExpression(tas.getLHS(), context));
			expr.addTypeCast(mapType(tas.getType()));
			return expr;
		}
		if (el instanceof gw.internal.gosu.parser.expressions.EvalExpression) {
			gw.internal.gosu.parser.expressions.EvalExpression ev =
					(gw.internal.gosu.parser.expressions.EvalExpression) el;
			CtInvocation<Object> inv = factory.createInvocation();
			CtExecutableReference<Object> ref = factory.createExecutableReference();
			ref.setSimpleName("eval");
			inv.setExecutable(ref);
			inv.addArgument(cast(mapExpression(ev.getExpression(), inv)));
			inv.setType(mapType(ev.getType()));
			return inv;
		}
		if (el instanceof IIntervalExpression) {
			CtExpression<Object> slice = snippet(sourceSlice(el));
			slice.setType(mapType(((gw.lang.parser.IExpression) el).getType()));
			return slice;
		}
		if (el instanceof IConditionalTernaryExpression) {
			IConditionalTernaryExpression t = (IConditionalTernaryExpression) el;
			CtConditional<Object> c = factory.createConditional();
			c.setCondition(castBool(mapExpression(t.getCondition(), c)));
			c.setThenExpression(cast(mapExpression(t.getFirst(), c)));
			c.setElseExpression(cast(mapExpression(t.getSecond(), c)));
			c.setType(mapType(t.getType()));
			return c;
		}
		if (el instanceof IEqualityExpression) {
			IEqualityExpression eq = (IEqualityExpression) el;
			return binary(eq, eq.isEquals() ? BinaryOperatorKind.EQ : BinaryOperatorKind.NE,
					context);
		}
		if (el instanceof IRelationalExpression) {
			IRelationalExpression rel = (IRelationalExpression) el;
			return binary(rel, relationalKind(rel.getOperator()), context);
		}
		if (el instanceof IConditionalAndExpression) {
			return binary((IConditionalExpression) el, BinaryOperatorKind.AND, context);
		}
		if (el instanceof IConditionalOrExpression) {
			return binary((IConditionalExpression) el, BinaryOperatorKind.OR, context);
		}
		if (el instanceof IUnaryNotPlusMinusExpression) {
			IUnaryNotPlusMinusExpression u = (IUnaryNotPlusMinusExpression) el;
			CtUnaryOperator<Object> op = factory.createUnaryOperator();
			op.setOperand(cast(mapExpression(u.getExpression(), op)));
			op.setKind(u.isNot() ? UnaryOperatorKind.NOT
					: u.isBitNot() ? UnaryOperatorKind.COMPL
					: UnaryOperatorKind.POS);
			op.setType(mapType(u.getType()));
			return op;
		}
		if (el instanceof IUnaryExpression) {
			IUnaryExpression u = (IUnaryExpression) el;
			CtUnaryOperator<Object> op = factory.createUnaryOperator();
			op.setOperand(cast(mapExpression(u.getExpression(), op)));
			op.setKind(UnaryOperatorKind.NEG);
			op.setType(mapType(u.getType()));
			return op;
		}
		if (el instanceof INewExpression) {
			return construct((INewExpression) el, context);
		}
		if (el instanceof IIdentifierExpression) {
			IIdentifierExpression id = (IIdentifierExpression) el;
			ISymbol symbol = id.getSymbol();
			String name = symbol == null ? null : symbol.getName();
			if ("this".equals(name)) {
				CtExpression<Object> thisAccess =
						factory.createThisAccess(thisTypeRef(), false);
				thisAccess.setType(thisTypeRef());
				return thisAccess;
			}
			boolean field = name != null
					&& (currentFields.contains(name) || symbol instanceof MemberFieldSymbol);
			if (field) {
				return fieldAccess(name, false);
			}
			return variableAccess(name, symbol == null ? null : symbol.getType());
		}
		if (el instanceof IBeanMethodCallExpression) {
			return beanCall((IBeanMethodCallExpression) el, context);
		}
		if (el instanceof gw.lang.parser.expressions.IArithmeticExpression) {
			gw.lang.parser.expressions.IArithmeticExpression arith =
					(gw.lang.parser.expressions.IArithmeticExpression) el;
			CtExpression<Object> result = factory.createBinaryOperator(
					mapExpression(arith.getLHS(), context),
					mapExpression(arith.getRHS(), context),
					binaryKind(arith.getOperator()));
			result.setType(mapType(arith.getType()));
			return result;
		}
		if (el instanceof gw.lang.parser.expressions.IMethodCallExpression) {
			return plainCall((gw.lang.parser.expressions.IMethodCallExpression) el, context);
		}
		if (el instanceof IMemberAccessExpression) {
			return memberAccess((IMemberAccessExpression) el, false, context);
		}
		if (el instanceof gw.lang.parser.expressions.IArrayAccessExpression) {
			gw.lang.parser.expressions.IArrayAccessExpression acc =
					(gw.lang.parser.expressions.IArrayAccessExpression) el;
			CtArrayRead<Object> read = factory.createArrayRead();
			read.setTarget(mapExpression(acc.getRootExpression(), read));
			read.setIndexExpression(castInt(mapExpression(acc.getMemberExpression(), read)));
			read.setType(mapType(acc.getType()));
			return read;
		}
		if (el instanceof IMapAccessExpression) {
			IMapAccessExpression acc = (IMapAccessExpression) el;
			CtArrayRead<Object> read = factory.createArrayRead();
			read.setTarget(mapExpression(acc.getRootExpression(), read));
			read.setIndexExpression(castInt(mapExpression(acc.getKeyExpression(), read)));
			read.setType(mapType(acc.getType()));
			return read;
		}
		if (el instanceof gw.lang.parser.expressions.INotAWordExpression) {
			// Gosu wraps some call expressions (e.g. block invocations) in a
			// "not a word" placeholder; map the real contained expression.
			List<gw.lang.parser.IExpression> kids = new ArrayList<>();
			el.getContainedParsedElementsByType(gw.lang.parser.IExpression.class, kids);
			for (gw.lang.parser.IExpression kid : kids) {
				if (!(kid instanceof gw.lang.parser.expressions.INotAWordExpression)) {
					return mapExpression(kid, context);
				}
			}
		}
		if (el instanceof IBlockExpression) {
			return buildLambda((IBlockExpression) el, context);
		}
		if (el instanceof IBlockInvocation) {
			// A direct block call (b(3)) is not a member invocation; keep it verbatim.
			return snippet(sourceSlice(el));
		}
		throw new UnsupportedOperationException(
				"unsupported Gosu expression " + el.getClass().getName()
						+ " text=[" + sourceSlice(el) + "]");
	}

	private CtExpression<Object> buildLambda(IBlockExpression block, CtElement context) {
		CtLambda<Object> lambda = factory.createLambda();
		for (ISymbol param : block.getArgs()) {
			CtParameter<Object> p = factory.createParameter();
			p.setSimpleName(param.getName());
			p.setType(mapType(param.getType()));
			lambda.addParameter(p);
		}
		IParsedElement body = block.getBody();
		if (body instanceof gw.lang.parser.IExpression) {
			lambda.setExpression(cast(mapExpression((gw.lang.parser.IExpression) body, lambda)));
		} else {
			lambda.setExpression(cast(snippet(sourceSlice(body))));
		}
		CtTypeReference<Object> ret = mapType(
				((gw.lang.reflect.IFunctionType) block.getType()).getReturnType());
		lambda.setType(ret);
		return lambda;
	}

	private CtExpression<Object> construct(INewExpression nw, CtElement context) {
		if (nw instanceof gw.internal.gosu.parser.expressions.InferredNewExpression) {
			gw.internal.gosu.parser.expressions.InferredNewExpression inf =
					(gw.internal.gosu.parser.expressions.InferredNewExpression) nw;
			if (inf.getValueExpressions() != null) {
				CtNewArray<Object> arr = factory.createNewArray();
				arr.setType(mapType(nw.getType()));
				for (gw.lang.parser.IExpression item : inf.getValueExpressions()) {
					arr.addElement(cast(mapExpression(item, arr)));
				}
				return arr;
			}
		}
		if (nw.getInitializer() instanceof ICollectionInitializerExpression) {
			ICollectionInitializerExpression ini =
					(ICollectionInitializerExpression) nw.getInitializer();
			CtNewArray<Object> arr = factory.createNewArray();
			arr.setType(mapType(nw.getType()));
			for (gw.lang.parser.IExpression item : ini.getValues()) {
				arr.addElement(cast(mapExpression(item, arr)));
			}
			return arr;
		}
		IType meta = nw.getTypeLiteral() == null || nw.getTypeLiteral().getType() == null
				? null
				: nw.getTypeLiteral().getType();
		IType type = meta instanceof gw.lang.reflect.IMetaType
				? ((gw.lang.reflect.IMetaType) meta).getType()
				: meta;
		gw.lang.parser.IExpression[] gosuArgs =
				nw.getArgs() == null ? new gw.lang.parser.IExpression[0] : nw.getArgs();
		CtExpression<?>[] args = new CtExpression<?>[gosuArgs.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = mapExpression(gosuArgs[i], context);
		}
		return factory.createConstructorCall(
				type == null
						? factory.Type().createReference("java.lang.Object")
						: mapType(type), args);
	}

	private CtExpression<Object> snippet(String code) {
		return factory.<Object>createCodeSnippetExpression(code);
	}

	/** The source text spanned by this element's parse tree (offset + length). */
	private String sourceSlice(IParsedElement el) {
		if (currentSource == null || el.getLocation() == null) {
			return null;
		}
		IParseTree tree = el.getLocation();
		return currentSource.subSequence(
				tree.getOffset(), tree.getOffset() + tree.getLength()).toString();
	}

	private CtExpression<Object> binary(IConditionalExpression expr,
										BinaryOperatorKind kind,
										CtElement context) {
		CtExpression<Object> result = factory.createBinaryOperator(
				mapExpression(expr.getLHS(), context),
				mapExpression(expr.getRHS(), context),
				kind);
		result.setType(mapType(expr.getType()));
		return result;
	}

	private CtExpression<Object> beanCall(IBeanMethodCallExpression bean, CtElement context) {
		gw.lang.parser.IExpression root = bean.getRootExpression();
		CtTypeReference<Object> declaring = isThis(root) || root == null
				|| root.getType() == null
				? thisTypeRef()
				: mapType(root.getType());
		CtExecutableReference<Object> ref = factory.Executable().createReference(
				declaring,
				mapType(bean.getType()),
				bean.getMemberName());
		List<CtExpression<?>> args = new ArrayList<>();
		for (gw.lang.parser.IExpression arg
				: bean.getArgs() == null ? new gw.lang.parser.IExpression[0] : bean.getArgs()) {
			args.add(mapExpression(arg, context));
		}
		return factory.createInvocation(
				mapExpression(bean.getRootExpression(), context), ref, args);
	}

	private CtExpression<Object> plainCall(
			gw.lang.parser.expressions.IMethodCallExpression call, CtElement context) {
		String name = call.getFunctionSymbol() == null
				? null : call.getFunctionSymbol().getDisplayName();
		CtExecutableReference<Object> ref = factory.Executable().createReference(
				thisTypeRef(),
				mapType(call.getType()),
				name == null ? "call" : name);
		List<CtExpression<?>> args = new ArrayList<>();
		for (gw.lang.parser.IExpression arg : call.getArgs()) {
			args.add(mapExpression(arg, context));
		}
		return factory.createInvocation(null, ref, args);
	}

	private CtExpression<Object> mapLValue(IParsedElement el) {
		if (el instanceof IMemberAccessExpression) {
			return memberAccess((IMemberAccessExpression) el, true, null);
		}
		IIdentifierExpression id = (IIdentifierExpression) el;
		ISymbol symbol = id.getSymbol();
		String name = symbol == null ? null : symbol.getName();
		if ("this".equals(name)) {
			return factory.createThisAccess(thisTypeRef(), false);
		}
		if (name != null && currentFields.contains(name)) {
			return fieldAccess(name, true);
		}
		return variableAccess(name, symbol == null ? null : symbol.getType());
	}

	private CtExpression<Object> variableAccess(String name, IType type) {
		CtVariableReference<Object> ref = factory.createParameterReference();
		ref.setSimpleName(name);
		if (type != null) {
			ref.setType(mapType(type));
		}
		return factory.createVariableRead(ref, false);
	}

	private CtExpression<Object> fieldAccess(String name, boolean write) {
		CtFieldReference<Object> ref = factory.Field().createReference(
				thisTypeRef(), mapType(currentFieldTypes.get(name)), name);
		return variableAccess(ref, write);
	}

	private CtExpression<Object> memberAccess(IMemberAccessExpression member,
											boolean write,
											CtElement context) {
		gw.lang.parser.IExpression root = member.getRootExpression();
		CtTypeReference<Object> declaring = isThis(root) || root == null
				|| root.getType() == null
				? thisTypeRef()
				: mapType(root.getType());
		CtFieldReference<Object> ref = factory.Field().createReference(
				declaring, mapType(member.getType()), member.getMemberName());
		CtExpression<Object> access = variableAccess(ref, write);
		if (root != null && isThis(root)) {
			((spoon.reflect.code.CtFieldAccess<Object>) (CtExpression<?>) access)
					.setTarget(factory.createThisAccess(thisTypeRef(), false));
		} else if (root != null) {
			((spoon.reflect.code.CtFieldAccess<Object>) (CtExpression<?>) access)
					.setTarget(mapExpression(root, context == null ? access : context));
		}
		return access;
	}

	private CtExpression<Object> variableAccess(CtVariableReference<Object> ref, boolean write) {
		CtVariableAccess<Object> access = write
				? factory.createVariableWrite(ref, false)
				: factory.createVariableRead(ref, false);
		return (CtExpression<Object>) (CtExpression<?>) access;
	}

	private static boolean isThis(gw.lang.parser.IExpression expr) {
		return expr instanceof IIdentifierExpression
				&& ((IIdentifierExpression) expr).getSymbol() != null
				&& "this".equals(((IIdentifierExpression) expr).getSymbol().getName());
	}

	private static BinaryOperatorKind binaryKind(String operator) {
		if (operator == null) {
			return BinaryOperatorKind.PLUS;
		}
		switch (operator.trim()) {
			case "+": return BinaryOperatorKind.PLUS;
			case "-": return BinaryOperatorKind.MINUS;
			case "*": return BinaryOperatorKind.MUL;
			case "/": return BinaryOperatorKind.DIV;
			case "%": return BinaryOperatorKind.MOD;
			default: return BinaryOperatorKind.PLUS;
		}
	}

	private static BinaryOperatorKind relationalKind(String operator) {
		switch (operator == null ? "" : operator.trim()) {
			case "<": return BinaryOperatorKind.LT;
			case "<=": return BinaryOperatorKind.LE;
			case ">": return BinaryOperatorKind.GT;
			case ">=": return BinaryOperatorKind.GE;
			default: return BinaryOperatorKind.EQ;
		}
	}

	/** The type {@code this} refers to: the enhanced type in an enhancement body. */
	private CtTypeReference<Object> thisTypeRef() {
		return currentThisType != null ? currentThisType : currentTypeRef();
	}

	private CtTypeReference<Object> currentTypeRef() {
		return factory.Type().createReference(currentClass.getQualifiedName());
	}

	private static <T> CtExpression<Object> expr(CtLiteral<T> literal) {
		return (CtExpression<Object>) (CtExpression<?>) literal;
	}

	@SuppressWarnings("unchecked")
	private static CtExpression<Object> cast(CtExpression<?> expression) {
		return (CtExpression<Object>) expression;
	}

	@SuppressWarnings("unchecked")
	private static CtExpression<Boolean> castBool(CtExpression<?> expression) {
		return (CtExpression<Boolean>) expression;
	}

	@SuppressWarnings("unchecked")
	private static CtExpression<Integer> castInt(CtExpression<?> expression) {
		return (CtExpression<Integer>) expression;
	}

	private <T> CtLiteral<T> literal(T value) {
		return factory.createLiteral(value);
	}

	// ------------------------------------------------------------------
	// types / modifiers
	// ------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private <T> CtTypeReference<T> mapType(IType type) {
		if (type == null) {
			return (CtTypeReference<T>) factory.Type().createReference("java.lang.Object");
		}
		String name = abbreviate(type.getName());
		if (name.indexOf('(') >= 0) {
			// The block/closure type (e.g. block(int):int) is not JLS-valid,
			// so it must be held verbatim, bypassing identifier validation.
			return (CtTypeReference<T>) nonJlsReference(name);
		}
		return (CtTypeReference<T>) createGenericRef(name);
	}

	/** Creates a type reference whose name is not a JLS identifier (block types). */
	private CtTypeReference<Object> nonJlsReference(String name) {
		boolean ignore = factory.getEnvironment().getIgnoreSyntaxErrors();
		factory.getEnvironment().setIgnoreSyntaxErrors(true);
		try {
			return factory.Type().createReference(name);
		} finally {
			factory.getEnvironment().setIgnoreSyntaxErrors(ignore);
		}
	}

	/** Creates a type reference from a possibly generic name, with real type args. */
	@SuppressWarnings("unchecked")
	private <T> CtTypeReference<T> createGenericRef(String name) {
		if (name == null) {
			return (CtTypeReference<T>) factory.Type().createReference("java.lang.Object");
		}
		int lt = name.indexOf('<');
		if (lt < 0) {
			return (CtTypeReference<T>) factory.Type().createReference(name);
		}
		String base = name.substring(0, lt).trim();
		String inner = name.substring(lt + 1, name.lastIndexOf('>'));
		CtTypeReference<Object> ref = factory.Type().createReference(base);
		for (String part : splitGenericArgs(inner)) {
			ref.addActualTypeArgument(createGenericRef(part));
		}
		return (CtTypeReference<T>) (CtTypeReference<?>) ref;
	}

	private static List<String> splitGenericArgs(String inner) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < inner.length(); i++) {
			char c = inner.charAt(i);
			if (c == '<') {
				depth++;
			} else if (c == '>') {
				depth--;
			} else if (c == ',' && depth == 0) {
				parts.add(inner.substring(start, i).trim());
				start = i + 1;
			}
		}
		parts.add(inner.substring(start).trim());
		return parts;
	}

	/** Abbreviates fully-qualified names to the used simple names. */
	private String abbreviate(String typeName) {
		if (typeName == null) {
			return null;
		}
		String s = JAVA_LANG_PREFIX.matcher(typeName).replaceAll("$1");
		for (String use : currentUses) {
			if (use.endsWith(".*")) {
				String prefix = use.substring(0, use.length() - 1);
				s = Pattern.compile("\\b" + Pattern.quote(prefix) + "([A-Z]\\w*)")
						.matcher(s).replaceAll("$1");
			} else {
				s = s.replaceAll("\\b" + Pattern.quote(use) + "\\b", simpleName(use));
			}
		}
		return s;
	}

	private static String simpleName(String fqn) {
		int dot = fqn.lastIndexOf('.');
		return dot < 0 ? fqn : fqn.substring(dot + 1);
	}

	private void copyModifiers(IVarStatement source, CtModifiable target) {
		Set<ModifierKind> mods = new LinkedHashSet<>();
		if (source.isPublic()) {
			mods.add(ModifierKind.PUBLIC);
		} else if (source.isPrivate()) {
			mods.add(ModifierKind.PRIVATE);
		} else if (source.isProtected()) {
			mods.add(ModifierKind.PROTECTED);
		} else if (source.isInternal()) {
			// package-private -> no visibility modifier
		}
		if (source.isAbstract()) {
			mods.add(ModifierKind.ABSTRACT);
		}
		if (source.isStatic()) {
			mods.add(ModifierKind.STATIC);
		}
		if (source.isFinal()) {
			mods.add(ModifierKind.FINAL);
		}
		target.setModifiers(mods);
	}

	private void copyModifiers(gw.internal.gosu.parser.Symbol source, CtModifiable target) {
		Set<ModifierKind> mods = new LinkedHashSet<>();
		if (source.isPublic()) {
			mods.add(ModifierKind.PUBLIC);
		} else if (source.isPrivate()) {
			mods.add(ModifierKind.PRIVATE);
		} else if (source.isProtected()) {
			mods.add(ModifierKind.PROTECTED);
		} else if (source.isInternal()) {
			// package-private
		}
		if (source.isAbstract()) {
			mods.add(ModifierKind.ABSTRACT);
		}
		if (source.isStatic()) {
			mods.add(ModifierKind.STATIC);
		}
		if (source.isFinal()) {
			mods.add(ModifierKind.FINAL);
		}
		target.setModifiers(mods);
	}
}
