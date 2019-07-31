package org.sindaryn.sanda.annotations;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.sindaryn.sanda.dao.GenericDao;
import org.springframework.stereotype.Repository;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

import static com.squareup.javapoet.ParameterizedTypeName.get;
import static org.sindaryn.sanda.StaticUtils.toPascalCase;

@SuppressWarnings("unchecked")
@SupportedAnnotationTypes("org.sindaryn.sanda.annotations.PersistableEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DataLayerAnnotationsProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<? extends TypeElement> entities = getEntities(roundEnvironment);
        Map<TypeElement, List<VariableElement>> annotatedFieldsMap = new HashMap<>();
        for (TypeElement entity : entities) {
            List<VariableElement> annotatedFields = getAnnotatedFieldsOf(entity);
            if (!annotatedFields.isEmpty()) annotatedFieldsMap.put(entity, annotatedFields);
        }
        entities.forEach(entity -> generateDao(entity, annotatedFieldsMap));
        return false;
    }




    private void generateDao(TypeElement entity, Map<TypeElement, List<VariableElement>> annotatedFieldsMap) {
        String className = entity.getQualifiedName().toString();
        int lastDot = className.lastIndexOf('.');
        String packageName = className.substring(0, lastDot);
        String simpleClassName = className.substring(lastDot + 1);
        String repositoryName = simpleClassName + "Dao";

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Repository.class)
                .addSuperinterface(get(ClassName.get(GenericDao.class), getIdType(entity), ClassName.get(entity)));
        Collection<VariableElement> annotatedFields = annotatedFieldsMap.get(entity);
        if(annotatedFields != null){
            annotatedFields.forEach(annotatedField -> {
                if(annotatedField.getAnnotation(GetBy.class) != null) {
                    builder
                            .addMethod(MethodSpec
                                    .methodBuilder(
                                            "findBy" + toPascalCase(annotatedField.getSimpleName().toString()))
                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                    .addParameter(
                                            ClassName.get(entity),
                                            annotatedField.getSimpleName().toString())
                                    .returns(ClassName.get(entity))
                                    .build());
                }
                else if(annotatedField.getAnnotation(GetAllBy.class) != null){
                    builder
                            .addMethod(MethodSpec
                                    .methodBuilder(
                                            "findAllBy" + toPascalCase(annotatedField.getSimpleName().toString()))
                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                    .addParameter(
                                            ClassName.get(entity),
                                            annotatedField.getSimpleName().toString())
                                    .returns(ClassName.get(entity))
                                    .build());
                }
            });
        }
        final TypeSpec newClass = builder.build();
        final JavaFile javaFile = JavaFile.builder(packageName, newClass).build();

        try {
            javaFile.writeTo(System.out);
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*try {
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className + "Dao");
            PrintWriter out = new PrintWriter(builderFile.openWriter());
            out.println(packageName + ";\n\n");
            String content = builder.build().toString().replaceAll("^package [a-zA-Z0-9]+[.a-zA-Z0-9]*;", "");
            out.print(content);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private ClassName getIdType(TypeElement entity) {
        for(Element field : entity.getEnclosedElements()){
            if(field.getKind() == ElementKind.FIELD &&
                    (
                        field.getAnnotation(Id.class) != null || field.getAnnotation(EmbeddedId.class) != null
                    )){
                return (ClassName) ClassName.get(field.asType());
            }
        }
        processingEnv
                .getMessager()
                .printMessage(Diagnostic.Kind.ERROR,
                        "No id type found for entity " + entity.getSimpleName().toString(), entity);
        return null;
    }

    private List<VariableElement> getAnnotatedFieldsOf(TypeElement entity) {
        List<VariableElement> annotatedFields = new ArrayList<>();
        List<? extends Element> enclosedElements = entity.getEnclosedElements();
        for (Element enclosedElement : enclosedElements)
            if (isAnnotatedField(enclosedElement)) annotatedFields.add((VariableElement) enclosedElement);
        return annotatedFields;
    }

    private boolean isAnnotatedField(Element enclosedElement) {
        return enclosedElement.getKind() == ElementKind.FIELD &&
                (enclosedElement.getAnnotation(GetBy.class) != null ||
                enclosedElement.getAnnotation(GetAllBy.class) != null);
    }

    private Set<? extends TypeElement> getEntities(RoundEnvironment roundEnvironment) {
        return (Set<? extends TypeElement>)
                roundEnvironment.getElementsAnnotatedWith(PersistableEntity.class);
    }


}
