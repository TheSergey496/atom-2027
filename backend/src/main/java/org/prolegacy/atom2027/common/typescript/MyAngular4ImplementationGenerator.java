package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.implgens.Angular4ImplementationGenerator;
import com.blueveery.springrest2ts.tsmodel.*;
import com.google.common.collect.Sets;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class MyAngular4ImplementationGenerator extends Angular4ImplementationGenerator {
    public MyAngular4ImplementationGenerator() {
        this(null);
    }

    public MyAngular4ImplementationGenerator(Path urlServicePath) {
        super(urlServicePath);
        TSModule angularCoreModule = new TSModule("@angular/core", null, true);
        this.injectableDecorator = new TSDecorator(new TSFunction("Service", angularCoreModule));
    }

    @Override
    public void addComplexTypeUsage(TSClass tsClass) {
        super.addComplexTypeUsage(tsClass);
        TSModule angularCoreModule = new TSModule("@angular/core", null, true);
        tsClass.addScopedTypeUsage(new TSClass("inject", angularCoreModule));
    }

    @Override
    public List<TSParameter> getImplementationSpecificParameters(TSMethod method) {
        return Collections.emptyList();
    }

    @Override
    protected void writeConstructorImplementation(BufferedWriter writer, TSClass tsClass) throws IOException {
        if (tsClass.getExtendsClass() == null) {
            final Set<String> implementationSpecificFieldNames = Sets.newHashSet(getImplementationSpecificFieldNames());
            StringBuilder classFieldBuilder = new StringBuilder();
            for (TSField tsField : tsClass.getTsFields()) {
                if (!implementationSpecificFieldNames.contains(tsField.getName())) {
                    continue;
                }
                classFieldBuilder.append("    this.").append(tsField.getName()).append(" = ").append("inject(").append(tsField.getType().getName()).append(")").append(";\n");
            }
            String classField = classFieldBuilder.toString();
            writer.write(!classField.isEmpty() ? classField.substring(0, classField.length() - 1) : "");
        } else {
            writer.write("super(");
            writer.write(String.join(",", getImplementationSpecificFieldNames()));
            writer.write(");");
        }
    }

    @Override
    protected String composeRequestOptions(String requestHeadersVar, String requestParamsVar, boolean isRequestParamDefined, boolean isRequestHeaderDefined, String requestOptions, boolean isJsonParsingRequired) {
        if (isRequestHeaderDefined || isRequestParamDefined || isJsonParsingRequired) {
            List<String> requestOptionsList = new ArrayList<>();
            if (isRequestHeaderDefined) {
                requestOptionsList.add(requestHeadersVar);
            }
            if (isRequestParamDefined) {
                requestOptionsList.add(requestParamsVar);
            }
            if (isJsonParsingRequired) {
                requestOptionsList.add("responseType: 'json'");
            }
            requestOptions += ", {";
            requestOptions += String.join(", ", requestOptionsList);
            requestOptions += "}";
        }
        return requestOptions;
    }

    @Override
    protected String getGenericType(TSMethod method, boolean isRequestOptionRequired) {
        return super.getGenericType(method, false);
    }

    @Override
    protected String getParseResponseFunction(boolean isJsonResponse, TSMethod method) {
        return super.getParseResponseFunction(false, method);
    }
}
