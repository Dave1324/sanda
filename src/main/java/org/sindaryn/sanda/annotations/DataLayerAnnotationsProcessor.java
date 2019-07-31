package org.sindaryn.sanda.annotations;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.sindaryn.sanda.GenericDao;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.tools.Diagnostic;
import java.util.*;

import static com.squareup.javapoet.ParameterizedTypeName.get;
import static org.sindaryn.sanda.StaticUtils.toPascalCase;
import static org.sindaryn.sanda.StaticUtils.writeToJavaFile;

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
        setComponentScan(entities);
        return false;
    }

    private void setComponentScan(Set<? extends TypeElement> entities) {
        if(!entities.isEmpty()){
            String className = entities.iterator().next().getQualifiedName().toString();
            int firstDot = className.indexOf('.');
            String basePackageName = className.substring(0, firstDot);
            String simpleClassName = "DatafiConfiguration";
            TypeSpec.Builder builder = TypeSpec.classBuilder(simpleClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Configuration.class)
                    .addAnnotation(AnnotationSpec.builder(ComponentScan.class)
                            .addMember(
                                    "basePackages",
                                    "{$S, $S}",
                                    "org.sindaryn.sanda", basePackageName)
                            .build());
            writeToJavaFile(simpleClassName, basePackageName, builder, processingEnv, "Configuration source file");
        }
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
                                            ClassName.get(annotatedField.asType()),
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
                                    .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(entity)))
                                    .build());
                }
            });
        }
        writeToJavaFile(entity.getSimpleName().toString(), packageName, builder, processingEnv, "JpaRepository");
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
