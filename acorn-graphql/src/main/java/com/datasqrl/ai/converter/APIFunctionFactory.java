package com.datasqrl.ai.converter;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionDefinition;

@FunctionalInterface
public interface APIFunctionFactory {

  APIFunction create(FunctionDefinition function, Context query);

}
