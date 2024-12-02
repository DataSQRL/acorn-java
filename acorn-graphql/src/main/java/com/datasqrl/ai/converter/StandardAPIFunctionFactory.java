package com.datasqrl.ai.converter;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.api.APIQueryExecutor;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.FunctionDefinition;
import java.util.Set;

public record StandardAPIFunctionFactory(APIQueryExecutor apiExecutor, Set<String> contextKeys)
    implements APIFunctionFactory {

  @Override
  public APIFunction create(FunctionDefinition function, APIQuery query) {
    return new APIFunction(function, contextKeys, query, apiExecutor);
  }
}
