/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.feed.server.olingo.processor;

import jetbrains.buildServer.nuget.feed.server.index.NuGetIndexEntry;
import jetbrains.buildServer.nuget.feed.server.olingo.data.NuGetDataSource;
import jetbrains.buildServer.nuget.feed.server.olingo.model.NuGetMapper;
import jetbrains.buildServer.nuget.feed.server.olingo.model.V2FeedPackage;
import jetbrains.buildServer.util.CollectionsUtil;
import org.apache.olingo.odata2.api.batch.*;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.commons.InlineCount;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.ep.*;
import org.apache.olingo.odata2.api.exception.*;
import org.apache.olingo.odata2.api.processor.*;
import org.apache.olingo.odata2.api.uri.ExpandSelectTreeNode;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.PathInfo;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.expression.*;
import org.apache.olingo.odata2.api.uri.info.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Implementation of the centralized parts of OData processing,
 * allowing to use the simplified DataSource for the
 * actual data handling.
 */
public class NuGetPackagesProcessor extends ODataSingleProcessor {

  private static final int SERVER_PAGING_SIZE = 100;
  private final BeanPropertyAccess valueAccess;
  private final NuGetDataSource dataSource;

  public NuGetPackagesProcessor(@NotNull final NuGetDataSource dataSource) {
    this(dataSource, new BeanPropertyAccess());
  }

  public NuGetPackagesProcessor(final NuGetDataSource dataSource, final BeanPropertyAccess valueAccess) {
    this.dataSource = dataSource;
    this.valueAccess = valueAccess;
  }

  @Override
  public ODataResponse readEntitySet(final GetEntitySetUriInfo uriInfo, final String contentType)
          throws ODataException {
    final ODataContext context = getContext();
    final PathInfo pathInfo = context.getPathInfo();
    final ArrayList<V2FeedPackage> data = new ArrayList<>();

    try {
      data.addAll(CollectionsUtil.convertCollection((List<?>) retrieveData(
              uriInfo.getStartEntitySet(),
              uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters())
      ), source -> NuGetMapper.mapPackage((NuGetIndexEntry) source, pathInfo.getServiceRoot())));
    } catch (final ODataNotFoundException e) {
      data.clear();
    }

    final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
    final InlineCount inlineCountType = uriInfo.getInlineCount();
    final Integer count = applySystemQueryOptions(
            entitySet,
            data,
            uriInfo.getFilter(),
            inlineCountType,
            uriInfo.getOrderBy(),
            uriInfo.getSkipToken(),
            uriInfo.getSkip(),
            uriInfo.getTop());


    String nextLink = null;

    // Limit the number of returned entities and provide a "next" link
    // if there are further entities.
    // Almost all system query options in the current request must be carried
    // over to the URI for the "next" link, with the exception of $skiptoken
    // and $skip.
    if (data.size() > SERVER_PAGING_SIZE) {
      if (uriInfo.getOrderBy() == null
              && uriInfo.getSkipToken() == null
              && uriInfo.getSkip() == null
              && uriInfo.getTop() == null) {
        sortInDefaultOrder(entitySet, data);
      }

      nextLink = pathInfo.getServiceRoot().relativize(pathInfo.getRequestUri()).toString();
      nextLink = percentEncodeNextLink(nextLink);

      nextLink += (nextLink.contains("?") ? "&" : "?")
              + "$skiptoken=" + getSkipToken(entitySet, data.get(SERVER_PAGING_SIZE));

      while (data.size() > SERVER_PAGING_SIZE) {
        data.remove(SERVER_PAGING_SIZE);
      }
    }

    final EdmEntityType entityType = entitySet.getEntityType();
    final List<Map<String, Object>> values = new ArrayList<>();
    for (final Object entryData : data) {
      values.add(getStructuralTypeValueMap(entryData, entityType));
    }

    final EntityProviderWriteProperties feedProperties = EntityProviderWriteProperties
            .serviceRoot(pathInfo.getServiceRoot())
            .inlineCountType(inlineCountType)
            .inlineCount(count)
            .expandSelectTree(UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand()))
            .nextLink(nextLink)
            .build();

    final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeFeed");
    final ODataResponse response = EntityProvider.writeFeed(contentType, entitySet, values, feedProperties);
    context.stopRuntimeMeasurement(timingHandle);

    return ODataResponse.fromResponse(response).build();
  }

  private static String percentEncodeNextLink(final String link) {
    if (link == null) {
      return null;
    }

    // Remove potentially trailing "?" or "&" left over from remove actions
    return link.replaceAll("\\$skiptoken=.+?(?:&|$)", "")
            .replaceAll("\\$skip=.+?(?:&|$)", "")
            .replaceFirst("(?:\\?|&)$", "");
  }

  @Override
  public ODataResponse countEntitySet(final GetEntitySetCountUriInfo uriInfo, final String contentType)
          throws ODataException {
    final URI serviceRoot = getContext().getPathInfo().getServiceRoot();
    final List<V2FeedPackage> data = new ArrayList<>();
    try {
      data.addAll(CollectionsUtil.convertCollection((List<?>) retrieveData(
              uriInfo.getStartEntitySet(),
              uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters())
      ), source -> NuGetMapper.mapPackage((NuGetIndexEntry) source, serviceRoot)));
    } catch (final ODataNotFoundException e) {
      data.clear();
    }

    applySystemQueryOptions(
            uriInfo.getTargetEntitySet(),
            data,
            uriInfo.getFilter(),
            null,
            null,
            null,
            uriInfo.getSkip(),
            uriInfo.getTop());

    return ODataResponse.fromResponse(EntityProvider.writeText(String.valueOf(data.size()))).build();
  }

  @Override
  public ODataResponse readEntityLinks(final GetEntitySetLinksUriInfo uriInfo, final String contentType)
          throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse countEntityLinks(final GetEntitySetLinksCountUriInfo uriInfo, final String contentType)
          throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse readEntity(final GetEntityUriInfo uriInfo, final String contentType) throws ODataException {
    final Object data = retrieveData(
            uriInfo.getStartEntitySet(),
            uriInfo.getKeyPredicates(),
            uriInfo.getFunctionImport(),
            mapFunctionParameters(uriInfo.getFunctionImportParameters())
    );

    if (!appliesFilter(data, uriInfo.getFilter())) {
      throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
    }

    final ExpandSelectTreeNode expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
    return ODataResponse.fromResponse(writeEntry(uriInfo.getTargetEntitySet(), expandSelectTreeNode, data, contentType)).build();
  }

  @Override
  public ODataResponse existsEntity(final GetEntityCountUriInfo uriInfo, final String contentType)
          throws ODataException {
    final Object data = retrieveData(
            uriInfo.getStartEntitySet(),
            uriInfo.getKeyPredicates(),
            uriInfo.getFunctionImport(),
            mapFunctionParameters(uriInfo.getFunctionImportParameters())
    );

    return ODataResponse.fromResponse(EntityProvider.writeText(appliesFilter(data, uriInfo.getFilter()) ? "1" : "0")).build();
  }

  @Override
  public ODataResponse deleteEntity(final DeleteUriInfo uriInfo, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse createEntity(final PostUriInfo uriInfo, final InputStream content,
                                    final String requestContentType, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse updateEntity(final PutMergePatchUriInfo uriInfo, final InputStream content,
                                    final String requestContentType, final boolean merge, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse readEntityLink(final GetEntityLinkUriInfo uriInfo, final String contentType)
          throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse existsEntityLink(final GetEntityLinkCountUriInfo uriInfo, final String contentType)
          throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse deleteEntityLink(final DeleteUriInfo uriInfo, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse createEntityLink(final PostUriInfo uriInfo, final InputStream content,
                                        final String requestContentType, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse updateEntityLink(final PutMergePatchUriInfo uriInfo, final InputStream content,
                                        final String requestContentType, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse readEntityComplexProperty(final GetComplexPropertyUriInfo uriInfo, final String contentType)
          throws ODataException {
    final ODataContext context = getContext();
    final V2FeedPackage data = NuGetMapper.mapPackage((NuGetIndexEntry) retrieveData(
            uriInfo.getStartEntitySet(),
            uriInfo.getKeyPredicates(),
            uriInfo.getFunctionImport(),
            mapFunctionParameters(uriInfo.getFunctionImportParameters())
    ), context.getPathInfo().getServiceRoot());

    // if (!appliesFilter(data, uriInfo.getFilter()))
    if (data == null) {
      throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
    }

    final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
    final EdmProperty property = propertyPath.get(propertyPath.size() - 1);
    final Object value;
    if (property.isSimple()) {
      value = property.getMapping() == null || property.getMapping().getMediaResourceMimeTypeKey() == null
              ? getPropertyValue(data, propertyPath)
              : getSimpleTypeValueMap(data, propertyPath);
    } else {
      value = getStructuralTypeValueMap(getPropertyValue(data, propertyPath), (EdmStructuralType) property.getType());
    }

    final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeProperty");
    final ODataResponse response = EntityProvider.writeProperty(contentType, property, value);
    context.stopRuntimeMeasurement(timingHandle);

    return ODataResponse.fromResponse(response).eTag(constructETag(uriInfo.getTargetEntitySet(), data)).build();
  }

  @Override
  public ODataResponse readEntitySimpleProperty(final GetSimplePropertyUriInfo uriInfo, final String contentType)
          throws ODataException {
    return readEntityComplexProperty((GetComplexPropertyUriInfo) uriInfo, contentType);
  }

  @Override
  public ODataResponse readEntitySimplePropertyValue(final GetSimplePropertyUriInfo uriInfo, final String contentType)
          throws ODataException {
    final ODataContext context = getContext();
    final V2FeedPackage data = NuGetMapper.mapPackage((NuGetIndexEntry) retrieveData(
            uriInfo.getStartEntitySet(),
            uriInfo.getKeyPredicates(),
            uriInfo.getFunctionImport(),
            mapFunctionParameters(uriInfo.getFunctionImportParameters())
    ), context.getPathInfo().getServiceRoot());

    // if (!appliesFilter(data, uriInfo.getFilter()))
    if (data == null) {
      throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
    }

    final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
    final EdmProperty property = propertyPath.get(propertyPath.size() - 1);
    final Object value = property.getMapping() == null || property.getMapping().getMediaResourceMimeTypeKey() == null
            ? getPropertyValue(data, propertyPath)
            : getSimpleTypeValueMap(data, propertyPath);

    return ODataResponse.fromResponse(EntityProvider.writePropertyValue(property, value)).eTag(
            constructETag(uriInfo.getTargetEntitySet(), data)).build();
  }

  @Override
  public ODataResponse deleteEntitySimplePropertyValue(final DeleteUriInfo uriInfo, final String contentType)
          throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse updateEntityComplexProperty(final PutMergePatchUriInfo uriInfo, final InputStream content,
                                                   final String requestContentType, final boolean merge, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse updateEntitySimpleProperty(final PutMergePatchUriInfo uriInfo, final InputStream content,
                                                  final String requestContentType, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse updateEntitySimplePropertyValue(final PutMergePatchUriInfo uriInfo, final InputStream content,
                                                       final String requestContentType, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse readEntityMedia(final GetMediaResourceUriInfo uriInfo, final String contentType)
          throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse deleteEntityMedia(final DeleteUriInfo uriInfo, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse updateEntityMedia(final PutMergePatchUriInfo uriInfo, final InputStream content,
                                         final String requestContentType, final String contentType) throws ODataException {
    throw new ODataNotImplementedException();
  }

  @Override
  public ODataResponse executeFunctionImport(final GetFunctionImportUriInfo uriInfo, final String contentType)
          throws ODataException {
    final ODataContext context = getContext();
    final URI serviceRoot = context.getPathInfo().getServiceRoot();
    final EdmFunctionImport functionImport = uriInfo.getFunctionImport();
    final EdmType type = functionImport.getReturnType().getType();

    final Object data = dataSource.readData(
            functionImport,
            mapFunctionParameters(uriInfo.getFunctionImportParameters()),
            null);

    if (data == null) {
      throw new ODataNotFoundException(ODataHttpException.COMMON);
    }

    Object value;
    if (type.getKind() == EdmTypeKind.SIMPLE) {
      value = NuGetMapper.mapPackage((NuGetIndexEntry) data, serviceRoot);
    } else if (functionImport.getReturnType().getMultiplicity() == EdmMultiplicity.MANY) {
      final List<Map<String, Object>> values = new ArrayList<>();
      for (final Object typeData : (List<?>) data) {
        Object entry = NuGetMapper.mapPackage((NuGetIndexEntry) typeData, serviceRoot);
        values.add(getStructuralTypeValueMap(entry, (EdmStructuralType) type));
      }
      value = values;
    } else {
      Object entry = NuGetMapper.mapPackage((NuGetIndexEntry) data, serviceRoot);
      value = getStructuralTypeValueMap(entry, (EdmStructuralType) type);
    }

    final EntityProviderWriteProperties entryProperties = EntityProviderWriteProperties
            .serviceRoot(serviceRoot).build();

    final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeFunctionImport");
    final ODataResponse response = EntityProvider.writeFunctionImport(contentType, functionImport, value, entryProperties);
    context.stopRuntimeMeasurement(timingHandle);

    return ODataResponse.fromResponse(response).build();
  }

  @Override
  public ODataResponse executeFunctionImportValue(final GetFunctionImportUriInfo uriInfo, final String contentType)
          throws ODataException {
    final ODataContext context = getContext();
    final EdmFunctionImport functionImport = uriInfo.getFunctionImport();
    final EdmSimpleType type = (EdmSimpleType) functionImport.getReturnType().getType();

    final Object data = NuGetMapper.mapPackage((NuGetIndexEntry) dataSource.readData(
            functionImport,
            mapFunctionParameters(uriInfo.getFunctionImportParameters()),
            null), context.getPathInfo().getServiceRoot());

    if (data == null) {
      throw new ODataNotFoundException(ODataHttpException.COMMON);
    }

    final String value = type.valueToString(data, EdmLiteralKind.DEFAULT, null);
    final ODataResponse response = EntityProvider.writeText(value == null ? "" : value);

    return ODataResponse.fromResponse(response).build();
  }

  private static Map<String, Object> mapKey(final List<KeyPredicate> keys) throws EdmException {
    final Map<String, Object> keyMap = new HashMap<>();
    for (final KeyPredicate key : keys) {
      final EdmProperty property = key.getProperty();
      final EdmSimpleType type = (EdmSimpleType) property.getType();
      final Object value = type.valueOfString(key.getLiteral(), EdmLiteralKind.DEFAULT, property.getFacets(), type.getDefaultType());
      keyMap.put(property.getName(), value);
    }

    return keyMap;
  }

  private static Map<String, Object> mapFunctionParameters(final Map<String, EdmLiteral> functionImportParameters)
          throws EdmSimpleTypeException {
    if (functionImportParameters == null) {
      return Collections.emptyMap();
    } else {
      final Map<String, Object> parameterMap = new HashMap<>();
      for (final String parameterName : functionImportParameters.keySet()) {
        final EdmLiteral literal = functionImportParameters.get(parameterName);
        final EdmSimpleType type = literal.getType();
        final Object value = type.valueOfString(literal.getLiteral(), EdmLiteralKind.DEFAULT, null, type.getDefaultType());
        parameterMap.put(parameterName, value);
      }

      return parameterMap;
    }
  }

  private Object retrieveData(final EdmEntitySet startEntitySet, final List<KeyPredicate> keyPredicates,
                              final EdmFunctionImport functionImport, final Map<String, Object> functionImportParameters) throws ODataException {
    final Map<String, Object> keys = mapKey(keyPredicates);
    final ODataContext context = getContext();
    Object data;

    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "retrieveData");
    try {
      if (functionImport != null) {
        data = dataSource.readData(functionImport, functionImportParameters, keys);
      } else {
        if (keys.isEmpty()) {
          data = dataSource.readData(startEntitySet);
        } else {
          data = dataSource.readData(startEntitySet, keys);
        }
      }
    } finally {
      context.stopRuntimeMeasurement(timingHandle);
    }

    return data;
  }

  private <T> String constructETag(final EdmEntitySet entitySet, final T data) throws ODataException {
    final EdmEntityType entityType = entitySet.getEntityType();
    String eTag = null;
    for (final String propertyName : entityType.getPropertyNames()) {
      final EdmProperty property = (EdmProperty) entityType.getProperty(propertyName);
      if (property.getFacets() != null && property.getFacets().getConcurrencyMode() == EdmConcurrencyMode.Fixed) {
        final EdmSimpleType type = (EdmSimpleType) property.getType();
        final Object value = valueAccess.getPropertyValue(data, property);
        final String component = type.valueToString(value, EdmLiteralKind.DEFAULT, property.getFacets());
        eTag = eTag == null ? component : eTag + Edm.DELIMITER + component;
      }
    }

    return eTag == null ? null : "W/\"" + eTag + "\"";
  }

  private <T> ODataResponse writeEntry(final EdmEntitySet entitySet, final ExpandSelectTreeNode expandSelectTree,
                                       final T data, final String contentType) throws ODataException {
    final EdmEntityType entityType = entitySet.getEntityType();
    final Map<String, Object> values = getStructuralTypeValueMap(data, entityType);

    final ODataContext context = getContext();
    EntityProviderWriteProperties writeProperties = EntityProviderWriteProperties
            .serviceRoot(context.getPathInfo().getServiceRoot())
            .expandSelectTree(expandSelectTree)
            .build();

    final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeEntry");
    final ODataResponse response = EntityProvider.writeEntry(contentType, entitySet, values, writeProperties);
    context.stopRuntimeMeasurement(timingHandle);

    return response;
  }

  private <T> Integer applySystemQueryOptions(final EdmEntitySet entitySet, final List<T> data,
                                              final FilterExpression filter, final InlineCount inlineCount, final OrderByExpression orderBy,
                                              final String skipToken, final Integer skip, final Integer top) throws ODataException {
    final ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "applySystemQueryOptions");

    if (filter != null) {
      // Remove all elements the filter does not apply for.
      // A for-each loop would not work with "remove", see Java documentation.
      for (Iterator<T> iterator = data.iterator(); iterator.hasNext(); ) {
        if (!appliesFilter(iterator.next(), filter)) {
          iterator.remove();
        }
      }
    }

    final Integer count = inlineCount == InlineCount.ALLPAGES ? data.size() : null;

    if (orderBy != null) {
      sort(data, orderBy);
    } else if (skipToken != null || skip != null || top != null) {
      sortInDefaultOrder(entitySet, data);
    }

    if (skipToken != null) {
      while (!data.isEmpty() && !getSkipToken(entitySet, data.get(0)).equals(skipToken)) {
        data.remove(0);
      }
    }

    if (skip != null) {
      if (skip >= data.size()) {
        data.clear();
      } else {
        for (int i = 0; i < skip; i++) {
          data.remove(0);
        }
      }
    }

    if (top != null) {
      while (data.size() > top) {
        data.remove(top.intValue());
      }
    }

    context.stopRuntimeMeasurement(timingHandle);

    return count;
  }

  private <T> void sort(final List<T> data, final OrderByExpression orderBy) {
    Collections.sort(data, (entity1, entity2) -> {
      try {
        int result = 0;
        for (final OrderExpression expression : orderBy.getOrders()) {
          final String first = evaluateExpression(entity1, expression.getExpression());
          final String second = evaluateExpression(entity2, expression.getExpression());

          if (first != null && second != null) {
            result = first.compareTo(second);
          } else if (first == null && second != null) {
            result = 1;
          } else if (first != null) {
            result = -1;
          }

          if (expression.getSortOrder() == SortOrder.desc) {
            result = -result;
          }

          if (result != 0) {
            break;
          }
        }
        return result;
      } catch (final ODataException e) {
        return 0;
      }
    });
  }

  private <T> void sortInDefaultOrder(final EdmEntitySet entitySet, final List<T> data) {
    Collections.sort(data, (entity1, entity2) -> {
      try {
        return getSkipToken(entitySet, entity1).compareTo(getSkipToken(entitySet, entity2));
      } catch (final ODataException e) {
        return 0;
      }
    });
  }

  private <T> boolean appliesFilter(final T data, final FilterExpression filter) throws ODataException {
    final ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "appliesFilter");

    try {
      return data != null && (filter == null || evaluateExpression(data, filter.getExpression()).equals("true"));
    } catch (final RuntimeException e) {
      return false;
    } finally {
      context.stopRuntimeMeasurement(timingHandle);
    }
  }

  private <T> String evaluateExpression(final T data, final CommonExpression expression) throws ODataException {
    switch (expression.getKind()) {
      case UNARY:
        final UnaryExpression unaryExpression = (UnaryExpression) expression;
        final String operand = evaluateExpression(data, unaryExpression.getOperand());

        switch (unaryExpression.getOperator()) {
          case NOT:
            return Boolean.toString(!Boolean.parseBoolean(operand));
          case MINUS:
            return operand.startsWith("-") ? operand.substring(1) : "-" + operand;
          default:
            throw new ODataNotImplementedException();
        }

      case BINARY:
        final BinaryExpression binaryExpression = (BinaryExpression) expression;
        final EdmSimpleType type = (EdmSimpleType) binaryExpression.getLeftOperand().getEdmType();
        final String left = evaluateExpression(data, binaryExpression.getLeftOperand());
        final String right = evaluateExpression(data, binaryExpression.getRightOperand());

        switch (binaryExpression.getOperator()) {
          case ADD:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) + Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) + Long.valueOf(right));
            }
          case SUB:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) - Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) - Long.valueOf(right));
            }
          case MUL:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) * Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) * Long.valueOf(right));
            }
          case DIV:
            final String number = Double.toString(Double.valueOf(left) / Double.valueOf(right));
            return number.endsWith(".0") ? number.replace(".0", "") : number;
          case MODULO:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) % Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) % Long.valueOf(right));
            }
          case AND:
            return Boolean.toString(left.equals("true") && right.equals("true"));
          case OR:
            return Boolean.toString(left.equals("true") || right.equals("true"));
          case EQ:
            return Boolean.toString(left.equals(right));
          case NE:
            return Boolean.toString(!left.equals(right));
          case LT:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) < 0);
            } else {
              return Boolean.toString(Double.valueOf(left) < Double.valueOf(right));
            }
          case LE:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) <= 0);
            } else {
              return Boolean.toString(Double.valueOf(left) <= Double.valueOf(right));
            }
          case GT:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) > 0);
            } else {
              return Boolean.toString(Double.valueOf(left) > Double.valueOf(right));
            }
          case GE:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) >= 0);
            } else {
              return Boolean.toString(Double.valueOf(left) >= Double.valueOf(right));
            }
          case PROPERTY_ACCESS:
            throw new ODataNotImplementedException();
          default:
            throw new ODataNotImplementedException();
        }

      case PROPERTY:
        final EdmProperty property = (EdmProperty) ((PropertyExpression) expression).getEdmProperty();
        final EdmSimpleType propertyType = (EdmSimpleType) property.getType();
        return propertyType.valueToString(valueAccess.getPropertyValue(data, property), EdmLiteralKind.DEFAULT,
                property.getFacets());

      case MEMBER:
        final MemberExpression memberExpression = (MemberExpression) expression;
        final PropertyExpression propertyExpression = (PropertyExpression) memberExpression.getProperty();
        final EdmProperty memberProperty = (EdmProperty) propertyExpression.getEdmProperty();
        final EdmSimpleType memberType = (EdmSimpleType) memberExpression.getEdmType();
        final List<EdmProperty> propertyPath = new ArrayList<>();
        CommonExpression currentExpression = memberExpression;

        while (currentExpression != null) {
          final PropertyExpression currentPropertyExpression =
                  (PropertyExpression) (currentExpression.getKind() == ExpressionKind.MEMBER ?
                          ((MemberExpression) currentExpression).getProperty() : currentExpression);
          final EdmTyped currentProperty = currentPropertyExpression.getEdmProperty();
          final EdmTypeKind kind = currentProperty.getType().getKind();
          if (kind == EdmTypeKind.SIMPLE || kind == EdmTypeKind.COMPLEX) {
            propertyPath.add(0, (EdmProperty) currentProperty);
          } else {
            throw new ODataNotImplementedException();
          }

          currentExpression = currentExpression.getKind() == ExpressionKind.MEMBER
                  ? ((MemberExpression) currentExpression).getPath()
                  : null;
        }

        final Object memberValue = getPropertyValue(data, propertyPath);
        return memberType.valueToString(memberValue, EdmLiteralKind.DEFAULT, memberProperty.getFacets());

      case LITERAL:
        final LiteralExpression literal = (LiteralExpression) expression;
        final EdmSimpleType literalType = (EdmSimpleType) literal.getEdmType();
        final Object literalValue = literalType.valueOfString(literal.getUriLiteral(),
                EdmLiteralKind.URI, null, literalType.getDefaultType());
        return literalType.valueToString(literalValue, EdmLiteralKind.DEFAULT, null);

      case METHOD:
        final MethodExpression methodExpression = (MethodExpression) expression;
        final String first = evaluateExpression(data, methodExpression.getParameters().get(0));
        final String second = methodExpression.getParameterCount() > 1 ?
                evaluateExpression(data, methodExpression.getParameters().get(1)) : "";
        final String third = methodExpression.getParameterCount() > 2 ?
                evaluateExpression(data, methodExpression.getParameters().get(2)) : "";

        switch (methodExpression.getMethod()) {
          case ENDSWITH:
            return Boolean.toString(first.endsWith(second));
          case INDEXOF:
            return Integer.toString(first.indexOf(second));
          case STARTSWITH:
            return Boolean.toString(first.startsWith(second));
          case TOLOWER:
            return first.toLowerCase(Locale.ROOT);
          case TOUPPER:
            return first.toUpperCase(Locale.ROOT);
          case TRIM:
            return first.trim();
          case SUBSTRING:
            final int offset = second.length() == 0 ? 0 : Integer.parseInt(second);
            final int length = third.length() == 0 ? 0 : Integer.parseInt(second);
            return first.substring(offset, offset + length);
          case SUBSTRINGOF:
            return Boolean.toString(second.contains(first));
          case CONCAT:
            return first + second;
          case LENGTH:
            return Integer.toString(first.length());
          case YEAR:
            return String.valueOf(Integer.parseInt(first.substring(0, 4)));
          case MONTH:
            return String.valueOf(Integer.parseInt(first.substring(5, 7)));
          case DAY:
            return String.valueOf(Integer.parseInt(first.substring(8, 10)));
          case HOUR:
            return String.valueOf(Integer.parseInt(first.substring(11, 13)));
          case MINUTE:
            return String.valueOf(Integer.parseInt(first.substring(14, 16)));
          case SECOND:
            return String.valueOf(Integer.parseInt(first.substring(17, 19)));
          case ROUND:
            return Long.toString(Math.round(Double.valueOf(first)));
          case FLOOR:
            return Long.toString(Math.round(Math.floor(Double.valueOf(first))));
          case CEILING:
            return Long.toString(Math.round(Math.ceil(Double.valueOf(first))));
          default:
            throw new ODataNotImplementedException();
        }

      default:
        throw new ODataNotImplementedException();
    }
  }

  private <T> String getSkipToken(final EdmEntitySet entitySet, final T data) throws ODataException {
    String skipToken = "";
    for (final EdmProperty property : entitySet.getEntityType().getKeyProperties()) {
      final EdmSimpleType type = (EdmSimpleType) property.getType();
      final Object value = valueAccess.getPropertyValue(data, property);
      skipToken = skipToken.concat(type.valueToString(value, EdmLiteralKind.DEFAULT, property.getFacets()));
    }

    return skipToken;
  }

  private <T> Object getPropertyValue(final T data, final List<EdmProperty> propertyPath) throws ODataException {
    Object dataObject = data;
    for (final EdmProperty property : propertyPath) {
      if (dataObject != null) {
        dataObject = valueAccess.getPropertyValue(dataObject, property);
      }
    }

    return dataObject;
  }

  private void handleMapping(final Object data, final EdmMapping mapping, final Map<String, Object> valueMap)
          throws ODataException {
    final String mimeTypeName = mapping.getMediaResourceMimeTypeKey();
    if (mimeTypeName != null) {
      Object value = valueAccess.getPropertyValue(data, mimeTypeName);
      valueMap.put(mimeTypeName, value);
    }

    final String sourceKey = mapping.getMediaResourceSourceKey();
    if (sourceKey != null) {
      Object value = valueAccess.getPropertyValue(data, sourceKey);
      valueMap.put(sourceKey, value);
    }
  }

  private <T> Map<String, Object> getSimpleTypeValueMap(final T data, final List<EdmProperty> propertyPath)
          throws ODataException {
    final EdmProperty property = propertyPath.get(propertyPath.size() - 1);
    final Map<String, Object> valueWithMimeType = new HashMap<>();
    valueWithMimeType.put(property.getName(), getPropertyValue(data, propertyPath));
    handleMapping(data, property.getMapping(), valueWithMimeType);

    return valueWithMimeType;
  }

  private <T> Map<String, Object> getStructuralTypeValueMap(final T data, final EdmStructuralType type)
          throws ODataException {
    final ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "getStructuralTypeValueMap");

    Map<String, Object> valueMap = new HashMap<>();
    EdmMapping mapping = type.getMapping();
    if (mapping != null) {
      handleMapping(data, mapping, valueMap);
    }

    for (final String propertyName : type.getPropertyNames()) {
      final EdmProperty property = (EdmProperty) type.getProperty(propertyName);
      final Object value = valueAccess.getPropertyValue(data, property);

      if (property.isSimple()) {
        if (property.getMapping() == null || property.getMapping().getMediaResourceMimeTypeKey() == null) {
          valueMap.put(propertyName, value);
        } else {
          // TODO: enable MIME type mapping outside the current subtree
          valueMap.put(propertyName, getSimpleTypeValueMap(data, Collections.singletonList(property)));
        }
      } else {
        valueMap.put(propertyName, getStructuralTypeValueMap(value, (EdmStructuralType) property.getType()));
      }
    }

    context.stopRuntimeMeasurement(timingHandle);

    return valueMap;
  }

  @Override
  public ODataResponse executeBatch(final BatchHandler handler, final String contentType, final InputStream content)
          throws ODataException {
    final List<BatchResponsePart> batchResponseParts = new ArrayList<>();
    final PathInfo pathInfo = getContext().getPathInfo();
    final EntityProviderBatchProperties batchProperties = EntityProviderBatchProperties.init().pathInfo(pathInfo).build();
    final List<BatchRequestPart> batchParts = EntityProvider.parseBatchRequest(contentType, content, batchProperties);
    for (BatchRequestPart batchPart : batchParts) {
      batchResponseParts.add(handler.handleBatchPart(batchPart));
    }

    return EntityProvider.writeBatchResponse(batchResponseParts);
  }

  @Override
  public BatchResponsePart executeChangeSet(final BatchHandler handler, final List<ODataRequest> requests)
          throws ODataException {
    final List<ODataResponse> responses = new ArrayList<>();
    for (ODataRequest request : requests) {
      final ODataResponse response = handler.handleRequest(request);
      if (response.getStatus().getStatusCode() >= HttpStatusCodes.BAD_REQUEST.getStatusCode()) {
        // Rollback
        final List<ODataResponse> errorResponses = new ArrayList<>(1);
        errorResponses.add(response);
        return BatchResponsePart.responses(errorResponses).changeSet(false).build();
      }
      responses.add(response);
    }

    return BatchResponsePart.responses(responses).changeSet(true).build();
  }
}
