package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.TypeMapper;
import com.blueveery.springrest2ts.implgens.Angular4ImplementationGenerator;
import com.blueveery.springrest2ts.tsmodel.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MyAngular4ImplementationGenerator extends Angular4ImplementationGenerator {
    @Override
    public List<TSDecorator> getDecorators(TSClass tsClass) {
        List<ILiteral> literalList = this.injectableDecorator.getTsLiteralList();
        if (literalList.isEmpty()) {
            TSJsonLiteral jsonLiteral = new TSJsonLiteral();
            jsonLiteral.getFieldMap().put("providedIn", new TSLiteral("root", TypeMapper.tsString, "root"));
            literalList.add(jsonLiteral);
        }
        return Collections.singletonList(this.injectableDecorator);
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
