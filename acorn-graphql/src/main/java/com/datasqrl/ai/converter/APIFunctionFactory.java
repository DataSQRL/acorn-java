package com.datasqrl.ai.converter;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.FunctionDefinition;

/** Factory for {@link APIFunction} given a {@link FunctionDefinition} and {@link APIQuery} */
@FunctionalInterface
public interface APIFunctionFactory {

  APIFunction create(FunctionDefinition function, APIQuery query);
}
