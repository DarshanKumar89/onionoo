/* Copyright 2011, 2012 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.onionoo.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.torproject.onionoo.util.ApplicationFactory;
import org.torproject.onionoo.util.DateTimeHelper;
import org.torproject.onionoo.util.Time;

public class ResourceServlet extends HttpServlet {

  private static final long serialVersionUID = 7236658979947465319L;

  private boolean maintenanceMode = false;

  /* Called by servlet container, not by test class. */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    this.maintenanceMode =
        config.getInitParameter("maintenance") != null &&
        config.getInitParameter("maintenance").equals("1");
  }

  public long getLastModified(HttpServletRequest request) {
    if (this.maintenanceMode) {
      return super.getLastModified(request);
    } else {
      return ApplicationFactory.getNodeIndexer().getLastIndexed(
          DateTimeHelper.TEN_SECONDS);
    }
  }

  public void doGet(HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    HttpServletRequestWrapper requestWrapper =
        new HttpServletRequestWrapper(request);
    HttpServletResponseWrapper responseWrapper =
        new HttpServletResponseWrapper(response);
    this.doGet(requestWrapper, responseWrapper);
  }

  public void doGet(HttpServletRequestWrapper request,
      HttpServletResponseWrapper response) throws IOException {

    if (this.maintenanceMode) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    if (ApplicationFactory.getNodeIndexer().getLastIndexed(
        DateTimeHelper.TEN_SECONDS) + DateTimeHelper.SIX_HOURS
        < ApplicationFactory.getTime().currentTimeMillis()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    NodeIndex nodeIndex = ApplicationFactory.getNodeIndexer().
        getLatestNodeIndex(DateTimeHelper.TEN_SECONDS);
    if (nodeIndex == null) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    Time time = ApplicationFactory.getTime();
    long receivedRequestMillis = time.currentTimeMillis();

    String uri = request.getRequestURI();
    if (uri.startsWith("/onionoo/")) {
      uri = uri.substring("/onionoo".length());
    }
    String resourceType = null;
    if (uri.startsWith("/summary")) {
      resourceType = "summary";
    } else if (uri.startsWith("/details")) {
      resourceType = "details";
    } else if (uri.startsWith("/bandwidth")) {
      resourceType = "bandwidth";
    } else if (uri.startsWith("/weights")) {
      resourceType = "weights";
    } else if (uri.startsWith("/clients")) {
      resourceType = "clients";
    } else if (uri.startsWith("/uptime")) {
      resourceType = "uptime";
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    RequestHandler rh = new RequestHandler(nodeIndex);
    rh.setResourceType(resourceType);

    /* Extract parameters either from the old-style URI or from request
     * parameters. */
    Map<String, String> parameterMap = new HashMap<String, String>();
    for (Object parameterKey : request.getParameterMap().keySet()) {
      String[] parameterValues =
          request.getParameterValues((String) parameterKey);
      parameterMap.put((String) parameterKey, parameterValues[0]);
    }

    /* Make sure that the request doesn't contain any unknown
     * parameters. */
    Set<String> knownParameters = new HashSet<String>(Arrays.asList((
        "type,running,search,lookup,fingerprint,country,as,flag,"
        + "first_seen_days,last_seen_days,contact,order,limit,offset,"
        + "fields,family").split(",")));
    for (String parameterKey : parameterMap.keySet()) {
      if (!knownParameters.contains(parameterKey)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    }

    /* Filter relays and bridges matching the request. */
    if (parameterMap.containsKey("type")) {
      String typeParameterValue = parameterMap.get("type").toLowerCase();
      boolean relaysRequested = true;
      if (typeParameterValue.equals("bridge")) {
        relaysRequested = false;
      } else if (!typeParameterValue.equals("relay")) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setType(relaysRequested ? "relay" : "bridge");
    }
    if (parameterMap.containsKey("running")) {
      String runningParameterValue =
          parameterMap.get("running").toLowerCase();
      boolean runningRequested = true;
      if (runningParameterValue.equals("false")) {
        runningRequested = false;
      } else if (!runningParameterValue.equals("true")) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setRunning(runningRequested ? "true" : "false");
    }
    if (parameterMap.containsKey("search")) {
      String[] searchTerms = this.parseSearchParameters(
          parameterMap.get("search"));
      if (searchTerms == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setSearch(searchTerms);
    }
    if (parameterMap.containsKey("lookup")) {
      String lookupParameter = this.parseFingerprintParameter(
          parameterMap.get("lookup"));
      if (lookupParameter == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      String fingerprint = lookupParameter.toUpperCase();
      rh.setLookup(fingerprint);
    }
    if (parameterMap.containsKey("fingerprint")) {
      String fingerprintParameter = this.parseFingerprintParameter(
          parameterMap.get("fingerprint"));
      if (fingerprintParameter == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      String fingerprint = fingerprintParameter.toUpperCase();
      rh.setFingerprint(fingerprint);
    }
    if (parameterMap.containsKey("country")) {
      String countryCodeParameter = this.parseCountryCodeParameter(
          parameterMap.get("country"));
      if (countryCodeParameter == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setCountry(countryCodeParameter);
    }
    if (parameterMap.containsKey("as")) {
      String aSNumberParameter = this.parseASNumberParameter(
          parameterMap.get("as"));
      if (aSNumberParameter == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setAs(aSNumberParameter);
    }
    if (parameterMap.containsKey("flag")) {
      String flagParameter = this.parseFlagParameter(
          parameterMap.get("flag"));
      if (flagParameter == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setFlag(flagParameter);
    }
    if (parameterMap.containsKey("first_seen_days")) {
      int[] days = this.parseDaysParameter(
          parameterMap.get("first_seen_days"));
      if (days == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setFirstSeenDays(days);
    }
    if (parameterMap.containsKey("last_seen_days")) {
      int[] days = this.parseDaysParameter(
          parameterMap.get("last_seen_days"));
      if (days == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setLastSeenDays(days);
    }
    if (parameterMap.containsKey("contact")) {
      String[] contactParts = this.parseContactParameter(
          parameterMap.get("contact"));
      if (contactParts == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setContact(contactParts);
    }
    if (parameterMap.containsKey("order")) {
      String orderParameter = parameterMap.get("order").toLowerCase();
      String orderByField = orderParameter;
      if (orderByField.startsWith("-")) {
        orderByField = orderByField.substring(1);
      }
      if (!orderByField.equals("consensus_weight")) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setOrder(new String[] { orderParameter });
    }
    if (parameterMap.containsKey("offset")) {
      String offsetParameter = parameterMap.get("offset");
      if (offsetParameter.length() > 6) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      try {
        Integer.parseInt(offsetParameter);
      } catch (NumberFormatException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setOffset(offsetParameter);
    }
    if (parameterMap.containsKey("limit")) {
      String limitParameter = parameterMap.get("limit");
      if (limitParameter.length() > 6) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      try {
        Integer.parseInt(limitParameter);
      } catch (NumberFormatException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rh.setLimit(limitParameter);
    }
    if (parameterMap.containsKey("family")) {
      String familyParameter = this.parseFingerprintParameter(
          parameterMap.get("family"));
      if (familyParameter == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      String family = familyParameter.toUpperCase();
      rh.setFamily(family);
    }
    rh.handleRequest();
    long parsedRequestMillis = time.currentTimeMillis();

    ResponseBuilder rb = new ResponseBuilder();
    rb.setResourceType(resourceType);
    rb.setRelaysPublishedString(rh.getRelaysPublishedString());
    rb.setBridgesPublishedString(rh.getBridgesPublishedString());
    rb.setOrderedRelays(rh.getOrderedRelays());
    rb.setOrderedBridges(rh.getOrderedBridges());
    String[] fields = null;
    if (parameterMap.containsKey("fields")) {
      fields = this.parseFieldsParameter(parameterMap.get("fields"));
      if (fields == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      rb.setFields(fields);
    }

    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    PrintWriter pw = response.getWriter();
    rb.buildResponse(pw);
    int relayDocumentsWritten = rh.getOrderedRelays().size();
    int bridgeDocumentsWritten = rh.getOrderedBridges().size();
    int charsWritten = rb.getCharsWritten();
    pw.flush();
    pw.close();
    long writtenResponseMillis = time.currentTimeMillis();
    PerformanceMetrics.logStatistics(receivedRequestMillis, resourceType,
        parameterMap.keySet(), parsedRequestMillis, relayDocumentsWritten,
        bridgeDocumentsWritten, charsWritten, writtenResponseMillis);
  }

  private static Pattern searchParameterPattern =
      Pattern.compile("^\\$?[0-9a-fA-F]{1,40}$|" /* Fingerprint. */
      + "^[0-9a-zA-Z\\.]{1,19}$|" /* Nickname or IPv4 address. */
      + "^\\[[0-9a-fA-F:\\.]{1,39}\\]?$"); /* IPv6 address. */
  private String[] parseSearchParameters(String parameter) {
    String[] searchParameters;
    if (parameter.contains(" ")) {
      searchParameters = parameter.split(" ");
    } else {
      searchParameters = new String[] { parameter };
    }
    for (String searchParameter : searchParameters) {
      if (!searchParameterPattern.matcher(searchParameter).matches()) {
        return null;
      }
    }
    return searchParameters;
  }

  private static Pattern fingerprintParameterPattern =
      Pattern.compile("^[0-9a-zA-Z]{1,40}$");
  private String parseFingerprintParameter(String parameter) {
    if (!fingerprintParameterPattern.matcher(parameter).matches()) {
      return null;
    }
    if (parameter.length() != 40) {
      return null;
    }
    return parameter;
  }

  private static Pattern countryCodeParameterPattern =
      Pattern.compile("^[0-9a-zA-Z]{2}$");
  private String parseCountryCodeParameter(String parameter) {
    if (!countryCodeParameterPattern.matcher(parameter).matches()) {
      return null;
    }
    return parameter;
  }

  private static Pattern aSNumberParameterPattern =
      Pattern.compile("^[asAS]{0,2}[0-9]{1,10}$");
  private String parseASNumberParameter(String parameter) {
    if (!aSNumberParameterPattern.matcher(parameter).matches()) {
      return null;
    }
    return parameter;
  }

  private static Pattern flagPattern =
      Pattern.compile("^[a-zA-Z0-9]{1,20}$");
  private String parseFlagParameter(String parameter) {
    if (!flagPattern.matcher(parameter).matches()) {
      return null;
    }
    return parameter;
  }

  private static Pattern daysPattern = Pattern.compile("^[0-9-]{1,10}$");
  private int[] parseDaysParameter(String parameter) {
    if (!daysPattern.matcher(parameter).matches()) {
      return null;
    }
    int x = 0, y = Integer.MAX_VALUE;
    try {
      if (!parameter.contains("-")) {
        x = Integer.parseInt(parameter);
        y = x;
      } else {
        String[] parts = parameter.split("-", 2);
        if (parts[0].length() > 0) {
          x = Integer.parseInt(parts[0]);
        }
        if (parts.length > 1 && parts[1].length() > 0) {
          y = Integer.parseInt(parts[1]);
        }
      }
    } catch (NumberFormatException e) {
      return null;
    }
    if (x > y) {
      return null;
    }
    return new int[] { x, y };
  }

  private String[] parseContactParameter(String parameter) {
    for (char c : parameter.toCharArray()) {
      if (c < 32 || c >= 127) {
        return null;
      }
    }
    return parameter.split(" ");
  }

  private static Pattern fieldsParameterPattern =
      Pattern.compile("^[0-9a-zA-Z_,]*$");
  private String[] parseFieldsParameter(String parameter) {
    if (!fieldsParameterPattern.matcher(parameter).matches()) {
      return null;
    }
    return parameter.split(",");
  }
}
