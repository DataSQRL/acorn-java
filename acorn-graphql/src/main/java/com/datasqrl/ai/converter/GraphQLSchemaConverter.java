package com.datasqrl.ai.converter;

import static graphql.Scalars.GraphQLString;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.api.GraphQLQuery;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.FunctionDefinition.Argument;
import com.datasqrl.ai.tool.FunctionDefinition.Parameters;
import com.datasqrl.ai.util.ErrorHandling;
import graphql.language.Comment;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SourceLocation;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.scalars.ExtendedScalars;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;


/**
 * Converts a given GraphQL Schema to a tools configuration for the function backend.
 * It extracts all queries and mutations and converts them into {@link com.datasqrl.ai.tool.APIFunction}.
 */
@Value
@Slf4j
public class GraphQLSchemaConverter {

  APIFunctionFactory functionFactory;
  BiPredicate<Operation, String> includeOperation;
  GraphQLSchema schema;

  public GraphQLSchemaConverter(String schemaString,
      APIFunctionFactory functionFactory) {
    this(schemaString, (x,y) -> true, functionFactory);
  }

  public GraphQLSchemaConverter(String schemaString,
      BiPredicate<Operation, String> includeOperation,
      APIFunctionFactory functionFactory) {
    this.functionFactory = functionFactory;
    this.includeOperation = includeOperation;
    this.schema = getSchema(schemaString);
  }

  private static GraphQLSchema getSchema(String schemaString) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schemaString);
    RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
    getExtendedScalars().forEach(runtimeWiringBuilder::scalar);
    return new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiringBuilder.build());
  }

  SchemaPrinter schemaPrinter = new SchemaPrinter(SchemaPrinter.Options.defaultOptions().descriptionsAsHashComments(true));

  public List<APIFunction> convertOperations(String operationDefinition) {
    Parser parser = new Parser();
    Document document = parser.parseDocument(operationDefinition);
    ErrorHandling.checkArgument(!document.getDefinitions().isEmpty(), "Operation definition contains no definitions");

    List<APIFunction> functions = new ArrayList<>();
    Iterator<Definition> defIter = document.getDefinitions().iterator();
    Definition definition = defIter.next();
    do {
      ErrorHandling.checkArgument(definition instanceof OperationDefinition, "Expected definition to be an operation, but got: %s", operationDefinition);
      FunctionDefinition fctDef = convertOperationDefinition(((OperationDefinition) definition));

      SourceLocation startLocation = definition.getSourceLocation();
      SourceLocation endLocation = new SourceLocation(Integer.MAX_VALUE, Integer.MAX_VALUE);
      definition = null;
      if (defIter.hasNext()) {
        definition = defIter.next();
        endLocation = definition.getSourceLocation();
      }
      //Get string between source and end location
      String queryString = extractOperation(operationDefinition, startLocation.getLine(),
          startLocation.getColumn(), endLocation.getLine(), endLocation.getColumn());
      APIQuery query = new GraphQLQuery(queryString);
      functions.add(functionFactory.create(fctDef, query));
    } while (definition !=null);
    return functions;
  }

  private static String extractOperation(String text, int startLine, int startColumn, int endLine, int endColumn) {
    String[] lines = text.split("\n");
    StringBuilder result = new StringBuilder();
    endLine = Math.min(endLine, lines.length);
    for (int i = startLine - 1; i <= endLine - 1; i++) {
      String line = lines[i];
      String subLine;
      if (i == startLine - 1 && i == endLine - 1) {
        subLine = line.substring(startColumn - 1, Math.min(endColumn - 1, line.length()));
      } else if (i == startLine - 1) {
        subLine = line.substring(startColumn - 1);
      } else if (i == endLine - 1) {
        subLine = (line.substring(0, Math.min(endColumn - 1, line.length())));
      } else {
        subLine = line;
      }
      int index = subLine.indexOf('#');
      subLine = (index != -1) ? subLine.substring(0, index) : subLine;
      if (!subLine.isBlank()) {
        result.append(subLine);
        result.append("\n");
      }
    }

    return result.toString();
  }
  private static String comments2String(List<Comment> comments) {
    return comments.stream().map(Comment::getContent).collect(Collectors.joining(" "));
  }

  public FunctionDefinition convertOperationDefinition(OperationDefinition node) {
    Operation op = node.getOperation();
    ErrorHandling.checkArgument(op == Operation.QUERY || op==Operation.MUTATION, "Do not support subscriptions: %s", node.getName());
    String fctComment = comments2String(node.getComments());
    String fctName = node.getName();

    FunctionDefinition funcDef = initializeFunctionDefinition(fctName, fctComment);
    Parameters params = funcDef.getParameters();

    // Process variable definitions
    List<VariableDefinition> variableDefinitions = node.getVariableDefinitions();
    for (VariableDefinition varDef : variableDefinitions) {
      String description = comments2String(varDef.getComments());
      String argName = varDef.getName();
      Type type = varDef.getType();

      boolean required = false;
      if (type instanceof NonNullType nonNullType) {
        required = true;
        type = nonNullType.getType();
      }
      Argument argDef = convert(type);
      argDef.setDescription(description);
      if (required) params.getRequired().add(argName);
      params.getProperties().put(argName, argDef);
    }
    return funcDef;
  }

  private static record OperationField (Operation op, GraphQLFieldDefinition fieldDefinition) {

  }

  public List<APIFunction> convertSchema() {
    List<APIFunction> functions = new ArrayList<>();

    GraphQLObjectType queryType = schema.getQueryType();
    GraphQLObjectType mutationType = schema.getMutationType();
    Stream.concat(queryType.getFieldDefinitions().stream().map(fieldDef -> new OperationField(Operation.QUERY, fieldDef))
            ,mutationType.getFieldDefinitions().stream().map(fieldDef -> new OperationField(Operation.MUTATION, fieldDef)))
        .flatMap(input -> {
          try {
            if (includeOperation.test(input.op(), input.fieldDefinition().getName())) {
              return Stream.of(convert(input.op(), input.fieldDefinition()));
            }
            return Stream.of();
          } catch (Exception e) {
            log.error("Error converting query: {}", input.fieldDefinition().getName(), e);
            return Stream.of();
          }
        }).forEach(functions::add);
    return functions;
  }

  private Argument convert(Type type) {
    if (type instanceof NonNullType) {
      return convert(((NonNullType) type).getType());
    }
    Argument argument = new Argument();
    if (type instanceof ListType) {
      argument.setType("array");
      argument.setItems(convert(((ListType) type).getType()));
    } else if (type instanceof TypeName) {
      String typeName = ((TypeName) type).getName();
      GraphQLType graphQLType = schema.getType(typeName);
      if (graphQLType instanceof GraphQLInputType graphQLInputType) {
        return GraphQLSchemaConverter.this.convert(graphQLInputType);
      } else {
        throw new UnsupportedOperationException("Unexpected type [" + typeName + "] with class: " + graphQLType);
      }
    } else throw new UnsupportedOperationException("Unexpected type:  " + type);
    return argument;
  }

  public static List<GraphQLScalarType> getExtendedScalars() {
    List<GraphQLScalarType> scalars = new ArrayList<>();

    Field[] fields = ExtendedScalars.class.getFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) &&
          Modifier.isStatic(field.getModifiers()) &&
          GraphQLScalarType.class.isAssignableFrom(field.getType())) {
        try {
          scalars.add((GraphQLScalarType) field.get(null));
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return scalars;
  }

  private record Context(String prefix, int numArgs) {}

  private static FunctionDefinition initializeFunctionDefinition(String name, String description) {
    FunctionDefinition funcDef = new FunctionDefinition();
    Parameters params = new Parameters();
    params.setType("object");
    params.setProperties(new HashMap<>());
    params.setRequired(new ArrayList<>());
    funcDef.setName(name);
    funcDef.setDescription(description);
    funcDef.setParameters(params);
    return funcDef;
  }

  public APIFunction convert(Operation operationType, GraphQLFieldDefinition fieldDef) {
    FunctionDefinition funcDef = initializeFunctionDefinition(fieldDef.getName(), fieldDef.getDescription());
    Parameters params = funcDef.getParameters();

    StringBuilder queryHeader = new StringBuilder(operationType.name().toLowerCase()).append(" ")
        .append(fieldDef.getName()).append("(");
    StringBuilder queryBody = new StringBuilder();

    visit(fieldDef, queryBody, queryHeader, params, new Context("", 0));

    queryHeader.append(") {\n").append(queryBody).append("\n}");
    APIQuery apiQuery = new GraphQLQuery(queryHeader.toString());
    return functionFactory.create(funcDef, apiQuery);
  }

  private static String combineStrings(String prefix, String suffix) {
    return prefix + (prefix.isBlank()? "" : "_") + suffix;
  }


  private record UnwrappedType(GraphQLInputType type, boolean required) {}

  private UnwrappedType convertRequired(GraphQLInputType type) {
    boolean required = false;
    if (type instanceof GraphQLNonNull) {
      required = true;
      type = (GraphQLInputType) ((GraphQLNonNull) type).getWrappedType();
    }
    return new UnwrappedType(type, required);
  }

  private Argument convert(GraphQLInputType graphQLInputType) {
    Argument argument = new Argument();
    if (graphQLInputType instanceof GraphQLScalarType) {
      argument.setType(convertScalarTypeToJsonType((GraphQLScalarType) graphQLInputType));
    } else if (graphQLInputType instanceof GraphQLEnumType enumType) {
      argument.setType("string");
      argument.setEnumValues(enumType.getValues().stream().map(GraphQLEnumValueDefinition::getName).collect(Collectors.toSet()));
    } else if (graphQLInputType instanceof GraphQLList) {
      argument.setType("array");
      argument.setItems(convert(convertRequired((GraphQLInputType) ((GraphQLList) graphQLInputType).getWrappedType()).type()));
    } else {
      throw new UnsupportedOperationException("Unsupported type: " + graphQLInputType);
    }
    return argument;
  }

  public String convertScalarTypeToJsonType(GraphQLScalarType scalarType) {
    return switch (scalarType.getName()) {
      case "Int" -> "integer";
      case "Float" -> "number";
      case "String" -> "string";
      case "Boolean" -> "boolean";
      case "ID" -> "string"; // Typically treated as a string in JSON Schema
      default -> "string"; //We assume that type can be cast from string.
    };
  }


  public void visit(GraphQLFieldDefinition fieldDef, StringBuilder queryBody, StringBuilder queryHeader,
      Parameters params, Context ctx) {
    queryBody.append(fieldDef.getName());
    int numArgs = 0;
    if (!fieldDef.getArguments().isEmpty()) {
      queryBody.append("(");
      for (GraphQLArgument arg : fieldDef.getArguments()) {
        UnwrappedType unwrappedType = convertRequired(arg.getType());
        if (unwrappedType.type() instanceof GraphQLInputObjectType inputType) {
          queryBody.append(arg.getName()).append(": { ");
          for (GraphQLInputObjectField nestedField : inputType.getFieldDefinitions()) {
            String argName = combineStrings(ctx.prefix(), nestedField.getName());
            unwrappedType = convertRequired(nestedField.getType());
            argName = processField(queryBody, queryHeader, params, ctx, numArgs, unwrappedType,
                argName, nestedField.getName(), nestedField.getDescription());
            String typeString = printFieldType(nestedField);
            queryHeader.append(argName).append(": ").append(typeString);
            numArgs++;
          }
          queryBody.append(" }");
        } else {
          String argName = combineStrings(ctx.prefix(), arg.getName());
          argName = processField(queryBody, queryHeader, params, ctx, numArgs, unwrappedType, argName,
              arg.getName(), arg.getDescription());
          String typeString = printArgumentType(arg);
          queryHeader.append(argName).append(": ").append(typeString);
          numArgs++;
        }
      }
      queryBody.append(")");
    }
    GraphQLOutputType type = unwrapType(fieldDef.getType());
    if (type instanceof GraphQLObjectType) {
      queryBody.append(" {\n");
      for (GraphQLFieldDefinition nestedField : ((GraphQLObjectType)type).getFieldDefinitions()) {
        visit(nestedField, queryBody, queryHeader, params, new Context(combineStrings(ctx.prefix(), nestedField.getName()), ctx.numArgs() + numArgs));
      }
      queryBody.append("}\n");
    } else {
      queryBody.append("\n");
    }
  }

  private String processField(StringBuilder queryBody, StringBuilder queryHeader, Parameters params,
      Context ctx, int numArgs, UnwrappedType unwrappedType, String argName, String originalName,
      String description) {
    Argument argDef = convert(unwrappedType.type());
    argDef.setDescription(description);
    if (numArgs>0) queryBody.append(", ");
    if (ctx.numArgs() + numArgs > 0) queryHeader.append(", ");
    if (unwrappedType.required()) params.getRequired().add(argName);
    params.getProperties().put(argName, argDef);
    argName = "$" + argName;
    queryBody.append(originalName).append(": ").append(argName);
    return argName;
  }

  private String printFieldType(GraphQLInputObjectField field) {
    GraphQLInputObjectType type = GraphQLInputObjectType.newInputObject()
        .name("DummyType")
        .field(field)
        .build();
    // Print argument as part of a dummy field in a dummy schema
    String output = schemaPrinter.print(type);
    return extractTypeFromDummy(output, field.getName());
  }

  private String printArgumentType(GraphQLArgument argument) {
    GraphQLArgument argumentWithoutDescription = argument.transform(builder -> builder.description(null));
    GraphQLObjectType type = GraphQLObjectType.newObject()
        .name("DummyType")
        .field(field -> field
            .name("dummyField")
            .type(GraphQLString)
            .argument(argumentWithoutDescription)
        )
        .build();
    // Print argument as part of a dummy field in a dummy schema
    String output = schemaPrinter.print(type);
    return extractTypeFromDummy(output, argument.getName());
  }

  private String extractTypeFromDummy(String output, String fieldName) {
    //Remove comments
    output = Arrays.stream(output.split("\n"))
        .filter(line -> !line.trim().startsWith("#")).collect(Collectors.joining("\n"));
    Pattern pattern = Pattern.compile(fieldName + "\\s*:\\s*([^)}]+)");
    // Print argument as part of a dummy field in a dummy schema
    Matcher matcher = pattern.matcher(output);
    ErrorHandling.checkArgument(matcher.find(), "Could not find type in: %s", output);
    return matcher.group(1).trim();
  }


  private static GraphQLOutputType unwrapType(GraphQLOutputType type) {
    if (type instanceof GraphQLList) {
      return unwrapType((GraphQLOutputType) ((GraphQLList) type).getWrappedType());
    } else if (type instanceof GraphQLNonNull) {
      return unwrapType((GraphQLOutputType)((GraphQLNonNull) type).getWrappedType());
    } else {
      return type;
    }
  }

}