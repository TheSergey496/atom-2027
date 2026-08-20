package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.*;
import com.blueveery.springrest2ts.extensions.ModelConversionExtension;
import com.blueveery.springrest2ts.extensions.RestConversionExtension;
import com.blueveery.springrest2ts.filters.JavaTypeFilter;
import com.blueveery.springrest2ts.filters.OrFilterOperator;
import com.blueveery.springrest2ts.filters.RejectJavaTypeFilter;
import com.blueveery.springrest2ts.implgens.ImplementationGenerator;
import com.blueveery.springrest2ts.tsmodel.*;
import com.blueveery.springrest2ts.tsmodel.generics.TSFormalTypeParameter;
import com.google.common.collect.ImmutableSet;
import org.prolegacy.atom2027.common.annotation.TypescriptIgnore;
import org.prolegacy.atom2027.common.util.ClassUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Rest2tsGenerator {
    static Logger logger = LoggerFactory.getLogger("gen-logger");
    @Getter
    private final Map<Class<?>, TSType> customTypeMapping = new HashMap<>();
    @Getter
    private static final Map<Class<?>, MappingAction> customTypeMappingActions = new HashMap<>();
    @Getter
    private final Map<Class<?>, TSComplexElement> customTypeMappingForClassHierarchy = new HashMap<>();
    @Getter
    private final Map<Class<?>, TSComplexElement> externalClassMappings = new HashMap<>();
    @Setter
    @Getter
    private JavaTypeFilter modelClassesCondition = new RejectJavaTypeFilter();
    @Setter
    @Getter
    private JavaTypeFilter restClassesCondition = new RejectJavaTypeFilter();
    @Setter
    @Getter
    private JavaTypeFilter restMethodsCondition = new RejectJavaTypeFilter();
    @Setter
    @Getter
    private NullableTypesStrategy nullableTypesStrategy = new DefaultNullableTypesStrategy();
    @Setter
    @Getter
    private JavaPackageToTsModuleConverter javaPackageToTsModuleConverter = new TsModuleCreatorConverter(2);
    @Setter
    @Getter
    private ComplexTypeConverter enumConverter = new JavaEnumToTsEnumConverter();
    @Setter
    @Getter
    private ModelClassesAbstractConverter modelClassesConverter;
    @Setter
    @Getter
    private RestClassConverter restClassesConverter;

    public Rest2tsGenerator() {
    }

    public void registerExternalClassMapping(Class<?> javaType, String importFrom, String className, TSFormalTypeParameter... templateClassParameters) {
        TSComplexElement tsElement = this.externalClassMappings.values()
                .stream()
                .filter(el -> Objects.equals(el.getName(), className) && Objects.equals(el.getModule().getName(), importFrom))
                .findFirst()
                .orElseGet(() -> {
                            TSModule tsModule = this.externalClassMappings.values()
                                    .stream().map(TSScopedElement::getModule)
                                    .filter(m -> Objects.equals(m.getName(), importFrom))
                                    .findFirst()
                                    .orElseGet(() -> new TSModule(importFrom, null, true));
                            return new TSClass(className, tsModule, null, templateClassParameters);
                        }
                );
        this.externalClassMappings.put(javaType, tsElement);
        this.customTypeMapping.put(javaType, this.externalClassMappings.get(javaType));
    }

    public SortedSet<TSModule> convert(Set<String> inputPackagesNames) throws IOException {
        Set<Class<?>> modelClasses = new HashSet<>();
        Set<Class<?>> restClasses = new HashSet<>();
        Set<Class<?>> enumClasses = new HashSet<>();
        Set<String> packagesNames = new HashSet<>(inputPackagesNames);
        this.applyConversionExtension(packagesNames);
        logger.info("Scanning model classes");
        List<Class<?>> loadedClasses = this.loadClasses(packagesNames);
        this.searchClasses(loadedClasses, restClasses, modelClasses, enumClasses);
        this.registerCustomTypesMapping(this.customTypeMapping);
        this.registerCustomTypesMappingActions();
        TypeMapper.complexTypeMapForClassHierarchy.putAll(this.customTypeMappingForClassHierarchy);
        this.exploreRestClasses();
        this.exploreModelClasses();
        this.convertModules(enumClasses, this.javaPackageToTsModuleConverter);
        this.convertModules(modelClasses, this.javaPackageToTsModuleConverter);
        this.convertModules(restClasses, this.javaPackageToTsModuleConverter);
        this.convertTypes(enumClasses, this.javaPackageToTsModuleConverter, this.enumConverter);
        if (!modelClasses.isEmpty()) {
            if (this.modelClassesConverter == null) {
                throw new IllegalStateException("Model classes converter is not set");
            }

            this.convertTypes(modelClasses, this.javaPackageToTsModuleConverter, this.modelClassesConverter);
        }

        if (!restClasses.isEmpty()) {
            if (this.restClassesConverter == null) {
                throw new IllegalStateException("Rest classes converter is not set");
            }

            this.convertTypes(restClasses, this.javaPackageToTsModuleConverter, this.restClassesConverter);
        }

        return this.javaPackageToTsModuleConverter.getTsModules();
    }

    public SortedSet<TSModule> generate(Set<String> inputPackagesNames, Path outputDir) throws IOException {
        SortedSet<TSModule> tsModules = this.convert(inputPackagesNames);
        this.writeTSModules(this.javaPackageToTsModuleConverter.getTsModules(), outputDir, logger);
        return tsModules;
    }

    public void writeTSModules(SortedSet<TSModule> tsModuleSortedSet, Path outputDir, Logger logger) throws IOException {
        for (TSModule tsModule : tsModuleSortedSet) {
            tsModule.writeModule(outputDir, logger);
        }
    }

    private void applyConversionExtension(Set<String> packagesNames) {
        List<JavaTypeFilter> modelClassFilterList = new ArrayList<>();
        List<ModelConversionExtension> modelConversionExtensionList = this.getModelConversionExtensions();

        for (ModelConversionExtension extension : modelConversionExtensionList) {
            if (extension.getJavaTypeFilter() != null) {
                modelClassFilterList.add(extension.getJavaTypeFilter());
            }

            packagesNames.addAll(extension.getAdditionalJavaPackages());
            this.modelClassesConverter.getObjectMapperMap().putAll(extension.getObjectMapperMap());
            this.modelClassesConverter.getConversionListener().getConversionListenerSet().add(extension);
        }

        List<JavaTypeFilter> restClassFilterList = new ArrayList<>();
        if (this.restClassesConverter != null) {
            for (RestConversionExtension extension : this.restClassesConverter.getConversionExtensionList()) {
                if (extension.getJavaTypeFilter() != null) {
                    restClassFilterList.add(extension.getJavaTypeFilter());
                }

                packagesNames.addAll(extension.getAdditionalJavaPackages());
                this.restClassesConverter.getConversionListener().getConversionListenerSet().add(extension);
            }

            ImplementationGenerator implementationGenerator = this.restClassesConverter.getImplementationGenerator();
            implementationGenerator.setExtensions(this.restClassesConverter.getConversionExtensionList());
        }

        OrFilterOperator orFilterOperator;
        if (!modelClassFilterList.isEmpty()) {
            if (this.modelClassesConverter == null) {
                throw new IllegalStateException("There is installed extension which requires model classes converter");
            }

            modelClassFilterList.add(this.modelClassesCondition);
            orFilterOperator = new OrFilterOperator(modelClassFilterList);
            this.modelClassesCondition = orFilterOperator;
        }

        if (!restClassFilterList.isEmpty()) {
            if (this.restClassesConverter == null) {
                throw new IllegalStateException("There is installed extension which requires REST classes converter");
            }

            restClassFilterList.add(this.restClassesCondition);
            orFilterOperator = new OrFilterOperator(restClassFilterList);
            this.restClassesCondition = orFilterOperator;
        }

    }

    private List<ModelConversionExtension> getModelConversionExtensions() {
        List<ModelConversionExtension> modelConversionExtensionList = new ArrayList<>();
        if (this.restClassesConverter != null) {
            for (RestConversionExtension restConversionExtension : this.restClassesConverter.getConversionExtensionList()) {
                ModelConversionExtension modelConversionExtension = restConversionExtension.getModelConversionExtension();
                if (modelConversionExtension != null) {
                    modelConversionExtensionList.add(modelConversionExtension);
                }
            }
        }

        if (this.modelClassesConverter != null) {
            modelConversionExtensionList.addAll(this.modelClassesConverter.getConversionExtensionList());
        }

        return modelConversionExtensionList;
    }

    private void registerCustomTypesMapping(Map<Class<?>, TSType> customTypeMapping) {
        for (Class<?> aClass : customTypeMapping.keySet()) {
            TSType tsType = customTypeMapping.get(aClass);
            TypeMapper.registerTsType(aClass, tsType);
        }

    }

    private void registerCustomTypesMappingActions() {
        for (Class<?> aClass : Rest2tsGenerator.customTypeMappingActions.keySet()) {
            MappingAction mappingAction = Rest2tsGenerator.customTypeMappingActions.get(aClass);
            TypeMapper.registerMappingAction(aClass, mappingAction);
        }

    }

    private void convertModules(Set<Class<?>> javaClasses, JavaPackageToTsModuleConverter javaPackageToTsModuleConverter) {
        for (Class<?> javaClass : javaClasses) {
            if (customTypeMapping.containsKey(javaClass) || externalClassMappings.containsKey(javaClass)) continue;
            javaPackageToTsModuleConverter.mapJavaTypeToTsModule(javaClass);
        }

    }

    private void convertTypes(Set<Class<?>> javaTypes, JavaPackageToTsModuleConverter tsModuleSortedMap, ComplexTypeConverter complexTypeConverter) {
        Set<Class<?>> preConvertedTypes = new HashSet<>();
        for (Class<?> javaType : javaTypes) {
            if (complexTypeConverter.preConverted(tsModuleSortedMap, javaType)) {
                preConvertedTypes.add(javaType);
            }
        }

        for (Class<?> javaType : preConvertedTypes) {
            complexTypeConverter.convertInheritance(javaType);
            complexTypeConverter.convert(javaType, nullableTypesStrategy);
        }
    }

    private void exploreModelClasses() {
    }

    private void exploreRestClasses() {
    }

    private void searchClasses(List<Class<?>> loadedClasses, Set<Class<?>> restClassSet, Set<Class<?>> modelClassSet, Set<Class<?>> enumClassSet) {
        for (Class<?> loadedClass : loadedClasses) {
            if (!restClassesCondition.accept(loadedClass)) {
                if (modelClassesCondition.accept(loadedClass)) {
                    modelClassSet.add(loadedClass);
                }
                continue;
            }
            restClassSet.add(loadedClass);
            for (Method classMethod : loadedClass.getMethods()) {
                if (!(classMethod.isAnnotationPresent(RequestMapping.class)
                        || Arrays.stream(classMethod.getAnnotations()).anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class))
                ) || classMethod.isAnnotationPresent(TypescriptIgnore.class)) {
                    continue;
                }

                Type returnType = classMethod.getGenericReturnType();
                Type[] parameterTypes = classMethod.getGenericParameterTypes();
                ArrayDeque<Type> types = new ArrayDeque<>(SetUtils.union(ImmutableSet.of(returnType), ImmutableSet.copyOf(parameterTypes)));
                Set<Type> visitedTypes = new HashSet<>();
                for (; !types.isEmpty(); types.removeIf(visitedTypes::contains)) {
                    Type type = types.pop();
                    visitedTypes.add(type);

                    if (this.isExternalType(type)) {
                        continue;
                    }

                    if (type instanceof ParameterizedType parameterizedType) {
                        if (!ClassUtils.isSimpleParameterizedType(parameterizedType)) {
                            types.add(parameterizedType.getRawType());
                        }

                        types.addAll(List.of((parameterizedType).getActualTypeArguments()));
                        continue;
                    }

                    if (type instanceof Class<?> clazz) {
                        if (ClassUtils.isSimpleClass(clazz) && !Enum.class.isAssignableFrom(clazz)) {
                            continue;
                        }

                        if (!loadedClasses.contains(clazz)) {
                            try {
                                this.getClass().getClassLoader().loadClass((clazz).getName());
                            } catch (ClassNotFoundException e) {
                                throw new IllegalStateException(e);
                            }
                        }

                        if (Enum.class.isAssignableFrom(clazz)) {
                            enumClassSet.add(clazz);
                        } else {
                            if (!(clazz.getSuperclass() == null
                                    || clazz.getSuperclass().equals(Object.class)
                                    || clazz.getSuperclass().equals(Record.class)
                                    || customTypeMapping.containsKey(clazz)
                                    || externalClassMappings.containsKey(clazz))
                            ) {
                                types.add(clazz.getSuperclass());
                            }
                            modelClassSet.add(clazz);
                            types.addAll(Arrays.stream(clazz.getDeclaredFields()).map(Field::getGenericType).toList());
                        }
                    }
                }
            }
        }
    }

    private boolean isExternalType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return this.externalClassMappings.containsKey((Class<?>) parameterizedType.getRawType());
        } else if (type instanceof Class<?> clazz) {
            return this.externalClassMappings.containsKey(clazz);
        }
        return false;
    }

    private List<Class<?>> loadClasses(Set<String> packageSet) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();

        List<Class<?>> classList = new ArrayList<>();
        for (String packageName : packageSet) {
            Enumeration<URL> urlEnumeration = classLoader.getResources(packageName.replace(".", "/"));
            while (urlEnumeration.hasMoreElements()) {
                URL url = urlEnumeration.nextElement();
                URI uri;
                try {
                    uri = url.toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(e);
                }
                Path path = Paths.get(uri);
                scanPackagesRecursively(classLoader, path, packageName, classList);
            }
        }
        return classList;
    }

    private void scanPackagesRecursively(ClassLoader classLoader, Path currentPath, String
            packageName, List<Class<?>> classList) throws IOException {
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(currentPath)) {
            for (Path nextPath : paths) {
                if (Files.isDirectory(nextPath)) {
                    scanPackagesRecursively(classLoader, nextPath, packageName + "." + nextPath.getFileName(), classList);
                } else if (nextPath.toString().endsWith(".class")) {
                    String className = (packageName + "/" + nextPath.getFileName().toString()).replace(".class", "").replace("/", ".");
                    try {
                        Class<?> loadedClass = classLoader.loadClass(className);
                        if (!loadedClass.isAnnotation()) {
                            addNestedClasses(loadedClass.getDeclaredClasses(), classList);
                            classList.add(loadedClass);
                        }
                    } catch (Error | Exception e) {
                        System.out.printf("Failed to load class %s due to error %s:%s%n", className, e.getClass().getSimpleName(), e.getMessage());
                    }
                }
            }
        }
    }

    private void addNestedClasses(Class<?>[] nestedClasses, List<Class<?>> classList) {
        for (Class<?> nestedClass : nestedClasses) {
            if (!nestedClass.isAnnotation()) {
                classList.add(nestedClass);
            }

            this.addNestedClasses(nestedClass.getDeclaredClasses(), classList);
        }
    }
}
