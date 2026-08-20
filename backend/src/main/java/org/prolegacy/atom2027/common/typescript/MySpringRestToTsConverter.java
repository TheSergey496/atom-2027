package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.SpringRestToTsConverter;
import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.common.annotation.TypescriptIgnore;

import java.lang.reflect.Method;

class MySpringRestToTsConverter extends SpringRestToTsConverter {
    public MySpringRestToTsConverter() {
        super(new MyAngular4ImplementationGenerator());
    }

    @Override
    protected boolean isRestMethod(Method method) {
        return !method.isAnnotationPresent(TypescriptIgnore.class) && super.isRestMethod(method);
    }

    @Override
    protected String createTsClassName(Class javaClass) {
        String tsClassName = super.createTsClassName(javaClass);
        Class<?> clazz = (Class<?>) javaClass;
        if (clazz.isAnnotationPresent(TypescriptEndpoint.class)) {
            TypescriptEndpoint annotation = clazz.getAnnotation(TypescriptEndpoint.class);
            if (annotation.value() != null && !annotation.value().isEmpty()) {
                return annotation.value();
            } else if (javaClass.getSimpleName().endsWith("Controller")) {
                return javaClass.getSimpleName().replaceFirst("Controller$", "Service");
            }
        }
        return tsClassName;
    }
}
