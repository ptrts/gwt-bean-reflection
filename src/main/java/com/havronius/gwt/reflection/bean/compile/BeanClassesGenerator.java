package com.havronius.gwt.reflection.bean.compile;

import com.google.gwt.core.ext.*;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BeanClassesGenerator extends Generator {

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {

        try {

            TypeOracle typeOracle = context.getTypeOracle();
            PropertyOracle propertyOracle = context.getPropertyOracle();

            JClassType classType = typeOracle.getType(typeName);

            SourceWriter sourceWriter = getSourceWriter(logger, context, classType);

            List<String> reflectionClassNames = propertyOracle.getConfigurationProperty("com.havronius.reflection.bean.classes").getValues();

            final List<BeanData> classes = buildClassesData(typeOracle, reflectionClassNames);

            JMethod methodFill = classType.getMethod("fill", new JType[0]);

            genMethod(logger, methodFill, sourceWriter, new MethodBodyFiller() {

                @Override
                public void fill(TreeLogger branch, SourceWriter writer) {

                    for (BeanData beanData : classes) {

                        String T = beanData.type.getQualifiedSourceName();

                        writer.println("BeanClass clazz = new BeanClass(\"" + T + "\") {");
                        {
                            writer.indent();

                            writer.println("protected void fillProperties() {");
                            {
                                writer.indent();

                                for (BeanData.PropertyData propertyData : beanData) {

                                    String V = propertyData.type.getQualifiedSourceName();

                                    writer.println("{");
                                    {
                                        writer.indent();

                                        writer.println("BeanProperty<" + T + ", " + V + "> property = new BeanProperty<" + T + ", " + V + ">(\"" + propertyData.name + "\", " + V + ".class) {");
                                        {
                                            writer.indent();

                                            writer.println("public " + V + " getValue(" + T + " instance) {");
                                            {
                                                writer.indent();
                                                writer.println("return instance." + propertyData.getter.getName() + "();");
                                                writer.outdent();
                                            }
                                            writer.println("}//getValue");

                                            writer.println("public void setValue(" + T + " instance, " + V + " value) {");
                                            {
                                                writer.indent();
                                                writer.println("instance." + propertyData.setter.getName() + "(value);");
                                                writer.outdent();
                                            }
                                            writer.println("}//setValue");

                                            writer.outdent();
                                        }
                                        writer.println("};//property");
                                        writer.println("addProperty(property);");

                                        writer.outdent();
                                    }
                                    writer.println("}//property {} block");
                                }

                                writer.outdent();
                            }
                            writer.println("}//fillProperties");

                            writer.outdent();
                        }
                        writer.println("};//clazz");
                        writer.println("addClass(clazz);");
                    }
                }
            });

            sourceWriter.commit(logger);

            return typeName + "Generated";

        } catch (NotFoundException | BadPropertyValueException e) {
            e.printStackTrace();
            throw new UnableToCompleteException();
        }
    }

    protected void genMethod(TreeLogger logger, JMethod method, SourceWriter sourceWriter, MethodBodyFiller bodyFiller) throws UnableToCompleteException {

        String name = method.getName();
        String returnType = method.getReturnType().getParameterizedQualifiedSourceName();

        String access = "";
        if (method.isPublic()) {
            access = "public ";
        } else if (method.isProtected()) {
            access = "protected ";
        }

        sourceWriter.print(access + returnType + " " + name + "(");
        JParameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (i != 0) {
                sourceWriter.print(",");
            }
            if (method.isVarArgs() && i == params.length - 1) {
                JArrayType arrayType = params[i].getType().isArray();
                sourceWriter.print(
                    arrayType.getComponentType().getParameterizedQualifiedSourceName()
                        + "... arg" + (i));
            } else {
                sourceWriter.print(
                    params[i].getType().getParameterizedQualifiedSourceName() + " arg"
                        + (i));
            }
        }

        sourceWriter.println(") {");
        sourceWriter.indent();
        String methodName = method.getName();

        TreeLogger branch = logger.branch(TreeLogger.INFO, "Generating method body for " + methodName + "()", null);

        bodyFiller.fill(branch, sourceWriter);

        sourceWriter.outdent();
        sourceWriter.println("}");
    }

    protected interface MethodBodyFiller {
        void fill(TreeLogger branch, SourceWriter sourceWriter);
    }

    private List<BeanData> buildClassesData(TypeOracle typeOracle, List<String> reflectionClassNames) throws NotFoundException {

        List<BeanData> classes = new ArrayList<>(reflectionClassNames.size());

        for (String reflectionClassName : reflectionClassNames) {

            JClassType reflectionClass = typeOracle.getType(reflectionClassName);

            BeanData beanData = new BeanData(reflectionClass);

            classes.add(beanData);
        }

        return classes;
    }

    public SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext context, JClassType classType) throws UnableToCompleteException {

        // Генерируем в том же пакете, что и суперкласс
        String packageName = classType.getPackage().getName();

        // Имя имплементации будет такое же, как и у суперкласса, с добавлением "Generated" в конце
        String simpleName = classType.getSimpleSourceName() + "Generated";

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Делаем принт райтер в который можно будет нагенерить класс для отложенного связывания
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
        if (printWriter == null) {
            logger.log(TreeLogger.Type.ERROR, "Can't get print writer");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Делаем билдер композитора нашего класса
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Делаем билдер композитора для класса с таким именем, и в таком пакете
        ClassSourceFileComposerFactory composerBuilder = new ClassSourceFileComposerFactory(packageName, simpleName);

        composerBuilder.setSuperclass("com.havronius.gwt.reflection.bean.client.BeanClasses");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Делаем уже SourceWriter, который умеет заполнять начинку класса
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        SourceWriter sourceWriter = composerBuilder.createSourceWriter(context, printWriter);

        return sourceWriter;
    }
}
