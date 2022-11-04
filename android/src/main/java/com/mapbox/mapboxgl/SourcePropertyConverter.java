package com.mapbox.mapboxgl;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngQuad;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.ImageSource;
import com.mapbox.mapboxsdk.style.sources.RasterDemSource;
import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class SourcePropertyConverter {
  private static final String TAG = "SourcePropertyConverter";

  static TileSet buildTileset(Map<String, Object> data) {
    final Object tiles = data.get("tiles");

    // options are only valid with tiles
    if (tiles == null) {
      return null;
    }

    final TileSet tileSet =
        new TileSet("2.1.0", (String[]) Convert.toList(tiles).toArray(new String[0]));

    final Object bounds = data.get("bounds");
    if (bounds != null) {
      List<Float> boundsFloat = new ArrayList<Float>();
      for (Object item : Convert.toList(bounds)) {
        boundsFloat.add(Convert.toFloat(item));
      }
      tileSet.setBounds(boundsFloat.toArray(new Float[0]));
    }

    final Object scheme = data.get("scheme");
    if (scheme != null) {
      tileSet.setScheme(Convert.toString(scheme));
    }

    final Object minzoom = data.get("minzoom");
    if (minzoom != null) {
      tileSet.setMinZoom(Convert.toFloat(minzoom));
    }

    final Object maxzoom = data.get("maxzoom");
    if (maxzoom != null) {
      tileSet.setMaxZoom(Convert.toFloat(maxzoom));
    }

    final Object attribution = data.get("attribution");
    if (attribution != null) {
      tileSet.setAttribution(Convert.toString(attribution));
    }
    return tileSet;
  }

  static GeoJsonOptions buildGeojsonOptions(Map<String, Object> data) {
    GeoJsonOptions options = new GeoJsonOptions();

    final Object buffer = data.get("buffer");
    if (buffer != null) {
      options = options.withBuffer(Convert.toInt(buffer));
    }

    final Object cluster = data.get("cluster");
    if (cluster != null) {
      options = options.withCluster(Convert.toBoolean(cluster));
    }

    final Object clusterMaxZoom = data.get("clusterMaxZoom");
    if (clusterMaxZoom != null) {
      options = options.withClusterMaxZoom(Convert.toInt(clusterMaxZoom));
    }

    final Object clusterRadius = data.get("clusterRadius");
    if (clusterRadius != null) {
      options = options.withClusterRadius(Convert.toInt(clusterRadius));
    }

    final Object clusterProperties = data.get("clusterProperties");
    if (clusterProperties != null) {
      System.out.println("Properties: " + clusterProperties);

      options = addClusterProperties(options, clusterProperties);
    }

    final Object lineMetrics = data.get("lineMetrics");
    if (lineMetrics != null) {
      options = options.withLineMetrics(Convert.toBoolean(lineMetrics));
    }

    final Object maxZoom = data.get("maxZoom");
    if (maxZoom != null) {
      options = options.withMaxZoom(Convert.toInt(maxZoom));
    }

    final Object minZoom = data.get("minZoom");
    if (minZoom != null) {
      options = options.withMinZoom(Convert.toInt(minZoom));
    }

    final Object tolerance = data.get("tolerance");
    if (tolerance != null) {
      options = options.withTolerance(Convert.toFloat(tolerance));
    }
    return options;
  }

  static GeoJsonOptions addClusterProperties(GeoJsonOptions options, Object clusterProperties) {
    //List<Map.Entry<String, Object>> properties = new ArrayList<>();

    /*Not works..
    ReadableMapKeySetIterator iterator = map.keySetIterator();
    while (iterator.hasNextKey()) {
      String name = iterator.nextKey();
      ReadableArray expressions = map.getArray(name);

      Expression operator;
      if (expressions.getType(0) == ReadableType.Array) {
        operator = ExpressionParser.from(expressions.getArray(0));
      } else {
        operator = Expression.literal(expressions.getString(0));
      }

      Expression mapping = ExpressionParser.from(expressions.getArray(1));

      //properties.add(new AbstractMap.SimpleEntry<>(name, new Object(operator, mapping)));
      options = options.withClusterProperty(name, operator, mapping);
    }*/

    //getkey = has_ghost
    //getValue
    //            [
    //              "any",
    //              [
    //                "==",
    //                ["get", "category"],
    //                'ghost'
    //              ],
    //              "false"
    //              ]
    Map<String, Object> properties = new HashMap();
    for (Map.Entry<String, List> entry : ((Map<String, List>) Convert.toMap(clusterProperties)).entrySet()) {
      System.out.println("----------- KEY Start ---------- ");
      List expressions = entry.getValue();
      ArrayList<Expression> builder = new ArrayList();

      Expression operatorExpr;
      Expression mapExpr = null;
      if (entry.getValue().get(0) instanceof String)
        operatorExpr = Expression.literal(entry.getValue().get(0));
      else
        operatorExpr = Expression.Converter.convert(entry.getValue().get(0).toString());

      for (int iExp = 1, expLength = expressions.size(); iExp < expLength; iExp++) {
        if (entry.getValue().get(iExp) instanceof List) {
          mapExpr = convertToExpression((List) entry.getValue().get(iExp), 0);
        }else {
          mapExpr = Expression.literal(entry.getValue().get(iExp));
        }
        builder.add(mapExpr);
        /*System.out.println("----------- CYCLE IN CYCLE " + iExp + " ---------------");
        Object expression = expressions.get(iExp);
        System.out.println("Expression: " + expression);
        Expression argument;
        if (expression instanceof List) {
          System.out.println("Expression is list: " + expression);
          argument = Expression.Converter.convert(expression.toString());
        } else if (expression instanceof Map) {
          System.out.println("Expression is map: " + expression);
          argument = Expression.Converter.convert((JsonElement) expression);
        } else if (expression instanceof Boolean) {
          System.out.println("Expression is bool: " + expression);
          argument = Expression.literal(expression);
        } else if (expression instanceof Number) {
          System.out.println("Expression is number: " + expression);
          argument = Expression.literal(expression);
        } else {
          System.out.println("Expression is string: " + expression);
          argument = Expression.literal(expression);
        }
        System.out.println("Argument: " + argument);
        builder.add(argument);
        System.out.println("----------- CYCLE IN CYCLE END OF " + iExp + " ---------------");*/
      }
      System.out.println("Key: " + entry.getKey());
      //System.out.println("Builder: " + builder);
      System.out.println("Value: " + entry.getValue());
      System.out.println("Operator operatorExpr: " + operatorExpr);
      if(mapExpr == null) {
        System.out.println("Operator mapExpr: IT IS NULL!!!");
      }else{
        try {
          System.out.println("Operator mapExpr: " + mapExpr);
        }catch (Exception e) {
          System.out.println("Operator mapExpr: catched error: " + e);
        }
      }

      /*for (int _resi = 0; _resi < builder.size(); _resi++){
        try {
          System.out.println("Operator allexpressions: " + builder.get(_resi));
        }catch (Exception e) {
          System.out.println("Operator allexpressions: " + e);
        }
      }*/
      String rawLatest = Arrays.toString(builder.toArray(new Expression[0]));
      System.out.println("AFTER FOR - Map expression 1: " + rawLatest);
      Expression _lastMapExp = Expression.Converter.convert(rawLatest.substring(1, rawLatest.length()-1));
      System.out.println("AFTER FOR - Map expression 2: " + _lastMapExp);
      Expression _latestExp = new Expression(expressions.get(0).toString(), builder.toArray(new Expression[0]));
      System.out.println("AFTER FOR - Latest expression: " + _latestExp);

      //Expression.any();
      //options.withClusterProperty(entry.getKey(), operatorExpr, Expression.switchCase(Expression.get("dsa"), Expression.literal(false)));

      //NOW DISABLED
      // options = options.withClusterProperty(entry.getKey(), Expression.all(Expression.eq(Expression.get("category"), "alien")), Expression.get("category"));
      Expression _all = Expression.all(Expression.eq(Expression.get("category"), "alien"));
      System.out.println("AFTER FOR - All test expression: " + _all);
      options = options.withClusterProperty(entry.getKey(), operatorExpr, _lastMapExp);
      //options = options.withClusterProperty(entry.getKey(), Expression.literal("all"), Expression.eq(Expression.get("category"), "alien"));
      //options = options.withClusterProperty(entry.getKey(), _latestExp, Expression.get("category"));

      //options = options.withClusterProperty(entry.getKey(), operatorExpr, Expression.Converter.convert(rawLatest));
      System.out.println("----------- KEY END ---------- ");
      //properties.put(entry.getKey(), builder);

      //System.out.println("----------- KEY START ---------- ");
      //System.out.println("Key: " + entry.getKey());
      //HashMap<String, Object[]> selfClusterProperties = new HashMap<String, Object[]>();

      /*WORKING ON IT
      Expression foundedExpression = convertToExpression(expressions, 0);

      try {
        Object map = foundedExpression.toString();
        System.out.println("foundedExpression: " + map);

        selfClusterProperties.put(entry.getKey(), new Object[]{map});
        options.put("clusterProperties", selfClusterProperties);
      }catch (Exception e){
        System.out.println("ERROR FOUNDE ON EXCEPTON: " + e);
      }*/

      //System.out.println("----------- KEY END ---------- ");
      //options = options.withClusterProperty(entry.getKey(), operatorExpr, );


      /*System.out.println("-------- START OF A CYCLE --------");
      Expression operatorExpr;
      if (entry.getValue().get(0) instanceof String)
        operatorExpr = Expression.literal(entry.getValue().get(0));
      else
        operatorExpr = Expression.Converter.convert(entry.getValue().get(0).toString());
      System.out.println("The entry: " + entry);
      System.out.println("The entry key: " + entry.getKey());
      System.out.println("The entry value: " + entry.getValue());
      System.out.println("The entry operatorExpr(0): " + operatorExpr);
      System.out.println("The entry mapExp(1): " + entry.getValue().get(1).toString());
      System.out.println("The entry list length: " + entry.getValue().size());

      Expression mapExpr = Expression.any(entry.getValue().get(1).toString());
      System.out.println("The entry mapExp: " + mapExpr);
      System.out.println("-------- END OF A CYCLEX --------");
      options = options.withClusterProperty(entry.getKey(), operatorExpr, mapExpr);*/
    }
    /*Not works too
    Error: java.lang.Error: GeoJSON source clusterProperties member must be an array with length of 2
    properties = (Map<String, Object>) Convert.toMap(clusterProperties);
    System.out.println("----------- END OF ALL OPTOINS PUT ---------- ");
    System.out.println("LAST PROPERTIES: " + properties);
    options.put("clusterProperties", properties);
    System.out.println("----------- END OF ALL OPTOINS PUT ---------- ");*/
    return options;
  }

  static GeoJsonOptions selfWithClusterProperty(GeoJsonOptions options, String propertyName, Expression operatorExpr, Expression mapExpr) {
    HashMap<String, Object[]> properties = options.containsKey("clusterProperties")
            ? (HashMap<String, Object[]>) options.get("clusterProperties") : new HashMap<String, Object[]>();
    Object operator = (operatorExpr instanceof Expression.ExpressionLiteral)
            ? ((Expression.ExpressionLiteral)operatorExpr).toValue() : Arrays.toString(operatorExpr.toArray());
    Object map = Arrays.toString(mapExpr.toArray());
    properties.put(propertyName, new Object[]{operatorExpr, mapExpr});
    options.put("clusterProperties", properties);
    return options;
  }

  static Expression convertToExpression(List expressions, int _cycleIndex) {
    List<Expression> temporaryExpressions = new ArrayList<>();

    System.out.println("************* Convert to expression START " + _cycleIndex + " *************");
    for (int iExp = 1, expLength = expressions.size(); iExp < expLength; iExp++) {
      System.out.println("-----START------ CYCLE: "+_cycleIndex+" IN FOR " + iExp + " -------START--------");
      Object expression = expressions.get(iExp);
      System.out.println("Expression: " + expression);
      Expression argument;
      if (expression instanceof List) {
        System.out.println("Expression is list: " + expression);
        argument = Expression.Converter.convert(expression.toString());
        //argument = new Expression((String) ((List) expression).get(0), convertToExpression((List) expression, _cycleIndex+1));
      } else if (expression instanceof Map) {
        System.out.println("Expression is map: " + expression);
        argument = Expression.Converter.convert((JsonElement) expression);
      } else {
        System.out.println("Expression is string: " + expression);
        argument = Expression.literal(expression);
      }
      System.out.println("------END----- CYCLE: "+_cycleIndex+" IN FOR " + iExp + " -------END--------");
      temporaryExpressions.add(argument);
    }


    /*Expression[] _resultExpressions = new Expression[temporaryExpressions.size() + 1];

    final int N = temporaryExpressions.size();
    _resultExpressions = java.util.Arrays.copyOf(temporaryExpressions, N);

    System.arraycopy(temporaryExpressions, 0, _resultExpressions, temporaryExpressions.size());*/
    //for (int _ri = 0; _ri < temporaryExpressions.size(); _ri++) {
    //  System.out.println(temporaryExpressions.get(_ri));
    //}
    System.out.println("************* Convert to expression END " + _cycleIndex + " *************");
    System.out.println("Key of expression: " + expressions.get(0).toString());
    System.out.println("Map expression: " + Arrays.toString(temporaryExpressions.toArray(new Expression[0])));
    Expression _latestExp = new Expression(expressions.get(0).toString(), temporaryExpressions.toArray(new Expression[0]));
    System.out.println("Latest expression: " + _latestExp);
    return _latestExp;
    //return new Expression(expressions.get(0).toString(), temporaryExpressions.stream().toArray(Expression[]::new));
  }

  static GeoJsonSource buildGeojsonSource(String id, Map<String, Object> properties) {
    final Object data = properties.get("data");
    final GeoJsonOptions options = buildGeojsonOptions(properties);
    if (data != null) {
      if (data instanceof String) {
        try {
          final URI uri = new URI(Convert.toString(data));
          return new GeoJsonSource(id, uri, options);
        } catch (URISyntaxException e) {
        }
      } else {
        Gson gson = new Gson();
        String geojson = gson.toJson(data);
        final FeatureCollection featureCollection = FeatureCollection.fromJson(geojson);
        System.out.println("Options: "+options);
        return new GeoJsonSource(id, featureCollection, options);
      }
    }
    return null;
  }

  static ImageSource buildImageSource(String id, Map<String, Object> properties) {
    final Object url = properties.get("url");
    List<LatLng> coordinates = Convert.toLatLngList(properties.get("coordinates"), true);
    final LatLngQuad quad =
        new LatLngQuad(
            coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3));
    try {
      final URI uri = new URI(Convert.toString(url));
      return new ImageSource(id, quad, uri);
    } catch (URISyntaxException e) {
    }
    return null;
  }

  static VectorSource buildVectorSource(String id, Map<String, Object> properties) {
    final Object url = properties.get("url");
    if (url != null) {
      final Uri uri = Uri.parse(Convert.toString(url));

      if (uri != null) {
        return new VectorSource(id, uri);
      }
      return null;
    }

    final TileSet tileSet = buildTileset(properties);
    return tileSet != null ? new VectorSource(id, tileSet) : null;
  }

  static RasterSource buildRasterSource(String id, Map<String, Object> properties) {
    final Object url = properties.get("url");
    if (url != null) {
      try {
        final URI uri = new URI(Convert.toString(url));
        return new RasterSource(id, uri);
      } catch (URISyntaxException e) {
      }
    }

    final TileSet tileSet = buildTileset(properties);
    return tileSet != null ? new RasterSource(id, tileSet) : null;
  }

  static RasterDemSource buildRasterDemSource(String id, Map<String, Object> properties) {
    final Object url = properties.get("url");
    if (url != null) {
      try {
        final URI uri = new URI(Convert.toString(url));
        return new RasterDemSource(id, uri);
      } catch (URISyntaxException e) {
      }
    }

    final TileSet tileSet = buildTileset(properties);
    return tileSet != null ? new RasterDemSource(id, tileSet) : null;
  }

  static void addSource(String id, Map<String, Object> properties, Style style) {
    final Object type = properties.get("type");
    Source source = null;

    if (type != null) {
      switch (Convert.toString(type)) {
        case "vector":
          source = buildVectorSource(id, properties);
          break;
        case "raster":
          source = buildRasterSource(id, properties);
          break;
        case "raster-dem":
          source = buildRasterDemSource(id, properties);
          break;
        case "image":
          source = buildImageSource(id, properties);
          break;
        case "geojson":
          source = buildGeojsonSource(id, properties);
          break;
        default:
          // unsupported source type
      }
    }

    if (source != null) {
      style.addSource(source);
    }
  }
}
