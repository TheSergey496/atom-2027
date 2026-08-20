package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.DefaultNullableTypesStrategy;
import com.blueveery.springrest2ts.converters.ModelClassesToTsClassesConverter;
import com.blueveery.springrest2ts.converters.TypeMapper;
import com.blueveery.springrest2ts.filters.HasAnnotationJavaTypeFilter;
import com.blueveery.springrest2ts.implgens.EmptyImplementationGenerator;
import com.blueveery.springrest2ts.tsmodel.TSSimpleType;
import com.blueveery.springrest2ts.tsmodel.TSType;
import com.blueveery.springrest2ts.tsmodel.generics.TSFormalTypeParameter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.common.annotation.TypescriptModel;
import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.hibernate.LongId;
import org.prolegacy.atom2027.common.query.lazy.PageQuery;
import org.reflections.Reflections;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class TypescriptEndpointGeneratorPlugin {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Provide directory path for typescript generation");
        }

        String typescriptDirectory = args[0];

        Rest2tsGenerator tsGenerator = setupTsGenerator();

        Map<Class<?>, TSType> customTypeMapping = tsGenerator.getCustomTypeMapping();
        customTypeMapping.put(Instant.class, TypeMapper.tsDate);
        customTypeMapping.put(LocalDate.class, TypeMapper.tsDate);

        Reflections reflections = new Reflections("org.prolegacy.atom2027");
        reflections.getSubTypesOf(LongId.class).forEach(idClass ->
                customTypeMapping.put(idClass, TypeMapper.tsNumber)
        );
        customTypeMapping.put(JsonNode.class, new TSSimpleType("any"));
        customTypeMapping.put(MultipartFile.class, new TSSimpleType("FormData"));

//        tsGenerator.registerExternalClassMapping(PageQuery.class, "primeng/table", "TableLazyLoadEvent", new TSFormalTypeParameter("T"));
//        tsGenerator.registerExternalClassMapping(UserDto.class, "../models/UserDto", "UserDto");
//        tsGenerator.registerExternalClassMapping(RoleDto.class, "../models/RoleDto", "RoleDto");

        // set of java root packages for class scanning
        Set<String> javaPackageSet = Collections.singleton("org.prolegacy.atom2027");
        tsGenerator.generate(javaPackageSet, Paths.get(typescriptDirectory));
    }

    private static @NonNull Rest2tsGenerator setupTsGenerator() {
        Rest2tsGenerator tsGenerator = new Rest2tsGenerator();

        // Rest classes filtering
        tsGenerator.setRestClassesCondition(new HasAnnotationJavaTypeFilter(TypescriptEndpoint.class));

        // Java model classes converter setup
        tsGenerator.setModelClassesCondition(new HasAnnotationJavaTypeFilter(TypescriptModel.class));
        tsGenerator.setModelClassesConverter(new ModelClassesToTsClassesConverter(new EmptyImplementationGenerator(), new NullableFieldsMapper()));
        tsGenerator.setEnumConverter(new EnumConverter());

        DefaultNullableTypesStrategy nullableTypesStrategy = new DefaultNullableTypesStrategy();
        nullableTypesStrategy.setUsePrimitiveTypesWrappers(false);
        tsGenerator.setNullableTypesStrategy(nullableTypesStrategy);

        // Spring REST controllers converter
        tsGenerator.setRestClassesConverter(new MySpringRestToTsConverter());
        return tsGenerator;
    }

}
