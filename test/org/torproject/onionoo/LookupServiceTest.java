/* Copyright 2013 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.onionoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.torproject.onionoo.LookupService.LookupResult;

public class LookupServiceTest {

  private List<String> manualGeoLiteCityBlocksLines,
      automaticGeoLiteCityBlocksLines, geoLiteCityBlocksLines,
      geoLiteCityLocationLines, iso3166Lines, regionLines,
      geoipASNum2Lines;

  private LookupService lookupService;

  private SortedSet<String> addressStrings = new TreeSet<String>();

  private SortedMap<String, LookupResult> lookupResults;

  private void populateLines() {
    this.manualGeoLiteCityBlocksLines = new ArrayList<String>();
    this.manualGeoLiteCityBlocksLines.add(
        "Copyright (c) 2011 MaxMind Inc.  All Rights Reserved.");
    this.manualGeoLiteCityBlocksLines.add("startIpNum,endIpNum,locId");
    this.manualGeoLiteCityBlocksLines.add("\"134739200\",\"134744063\","
        + "\"223\"");
    this.manualGeoLiteCityBlocksLines.add("\"134744064\",\"134744319\","
        + "\"32191\"");
    this.manualGeoLiteCityBlocksLines.add("\"134744320\",\"134751743\","
        + "\"223\"");
    this.geoLiteCityLocationLines = new ArrayList<String>();
    this.geoLiteCityLocationLines.add("Copyright (c) 2012 MaxMind "
        + "LLC.  All Rights Reserved.");
    this.geoLiteCityLocationLines.add("locId,country,region,city,"
        + "postalCode,latitude,longitude,metroCode,areaCode");
    this.geoLiteCityLocationLines.add("223,\"US\",\"\",\"\",\"\","
        + "38.0000,-97.0000,,");
    this.geoLiteCityLocationLines.add("32191,\"US\",\"CA\","
        + "\"Mountain View\",\"\",37.3860,-122.0838,807,650");
    this.iso3166Lines = new ArrayList<String>();
    this.iso3166Lines.add("US,\"United States\"");
    this.regionLines = new ArrayList<String>();
    this.regionLines.add("US,CA,\"California\"");
    this.geoipASNum2Lines = new ArrayList<String>();
    this.geoipASNum2Lines.add("134743296,134744063,\"AS3356 Level 3 "
        + "Communications\"");
    this.geoipASNum2Lines.add("134744064,134744319,\"AS15169 Google "
        + "Inc.\"");
    this.geoipASNum2Lines.add("134744320,134750463,\"AS3356 Level 3 "
        + "Communications\"");
  }

  private void writeCsvFiles() {
    try {
      this.writeCsvFile(this.manualGeoLiteCityBlocksLines,
          "Manual-GeoLiteCity-Blocks.csv");
      this.writeCsvFile(this.automaticGeoLiteCityBlocksLines,
          "Automatic-GeoLiteCity-Blocks.csv");
      this.writeCsvFile(this.geoLiteCityBlocksLines,
          "GeoLiteCity-Blocks.csv");
      this.writeCsvFile(this.geoLiteCityLocationLines,
          "GeoLiteCity-Location.csv");
      this.writeCsvFile(this.iso3166Lines, "iso3166.csv");
      this.writeCsvFile(this.regionLines, "region.csv");
      this.writeCsvFile(this.geoipASNum2Lines, "GeoIPASNum2.csv");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeCsvFile(List<String> lines, String fileName)
      throws IOException {
    if (lines != null && !lines.isEmpty()) {
      BufferedWriter bw = new BufferedWriter(new FileWriter(
          new File(this.tempGeoipDir, fileName)));
      for (String line : lines) {
        bw.write(line + "\n");
      }
      bw.close();
    }
  }

  private void performLookups() {
    this.lookupService = new LookupService(this.tempGeoipDir);
    this.lookupResults = this.lookupService.lookup(this.addressStrings);
  }

  private void assertLookupResult(
      List<String> manualGeoLiteCityBlocksLines,
      List<String> automaticGeoLiteCityBlocksLines,
      List<String> geoLiteCityBlocksLines,
      List<String> geoLiteCityLocationLines, List<String> iso3166Lines,
      List<String> regionLines, List<String> geoipASNum2Lines,
      String addressString, String countryCode, String countryName,
      String regionName, String cityName, String latitude,
      String longitude, String aSNumber, String aSName) {
    this.addressStrings.add(addressString);
    this.populateLines();
    if (manualGeoLiteCityBlocksLines != null) {
      this.manualGeoLiteCityBlocksLines =
          manualGeoLiteCityBlocksLines;
    }
    if (automaticGeoLiteCityBlocksLines != null) {
      this.automaticGeoLiteCityBlocksLines =
          automaticGeoLiteCityBlocksLines;
    }
    if (geoLiteCityBlocksLines != null) {
      this.geoLiteCityBlocksLines = geoLiteCityBlocksLines;
    }
    if (geoLiteCityLocationLines != null) {
      this.geoLiteCityLocationLines = geoLiteCityLocationLines;
    }
    if (iso3166Lines != null) {
      this.iso3166Lines = iso3166Lines;
    }
    if (regionLines != null) {
      this.regionLines = regionLines;
    }
    if (geoipASNum2Lines != null) {
      this.geoipASNum2Lines = geoipASNum2Lines;
    }
    this.writeCsvFiles();
    /* Disable log messages printed to System.err. */
    System.setErr(new PrintStream(new OutputStream() {
      public void write(int b) {
      }
    }));
    this.performLookups();
    if (countryCode == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).countryCode == null);
    } else {
      assertEquals(countryCode,
          this.lookupResults.get(addressString).countryCode);
    }
    if (countryName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).countryName == null);
    } else {
      assertEquals(countryName,
          this.lookupResults.get(addressString).countryName);
    }
    if (regionName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).regionName == null);
    } else {
      assertEquals(regionName,
          this.lookupResults.get(addressString).regionName);
    }
    if (cityName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).cityName == null);
    } else {
      assertEquals(cityName,
          this.lookupResults.get(addressString).cityName);
    }
    if (latitude == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).latitude == null);
    } else {
      assertEquals(latitude,
          this.lookupResults.get(addressString).latitude);
    }
    if (longitude == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).longitude == null);
    } else {
      assertEquals(longitude,
          this.lookupResults.get(addressString).longitude);
    }
    if (aSNumber == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).aSNumber == null);
    } else {
      assertEquals(aSNumber,
          this.lookupResults.get(addressString).aSNumber);
    }
    if (aSName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString) ||
          this.lookupResults.get(addressString).aSName == null);
    } else {
      assertEquals(aSName,
          this.lookupResults.get(addressString).aSName);
    }
  }

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private File tempGeoipDir;

  @Before
  public void createTempGeoipDir() throws IOException {
    this.tempGeoipDir = this.tempFolder.newFolder("geoip");
  }

  @Test()
  public void testLookup8888() {
    this.assertLookupResult(null,
        null, null, null, null, null, null, "8.8.8.8", "us",
        "United States", "California", "Mountain View", "37.3860",
        "-122.0838", "AS15169", "Google Inc.");
  }

  @Test()
  public void testLookup8880() {
    this.assertLookupResult(null,
        null, null, null, null, null, null, "8.8.8.0", "us",
        "United States", "California", "Mountain View", "37.3860",
        "-122.0838", "AS15169", "Google Inc.");
  }

  @Test()
  public void testLookup888255() {
    this.assertLookupResult(null,
        null, null, null, null, null, null, "8.8.8.255", "us",
        "United States", "California", "Mountain View", "37.3860",
        "-122.0838", "AS15169", "Google Inc.");
  }

  @Test()
  public void testLookup888256() {
    this.assertLookupResult(null,
        null, null, null, null, null, null, "8.8.8.256", null, null, null,
        null, null, null, null, null);
  }

  @Test()
  public void testLookup888Minus1() {
    this.assertLookupResult(null,
        null, null, null, null, null, null, "8.8.8.-1", null, null, null,
        null, null, null, null, null);
  }

  @Test()
  public void testLookup000() {
    this.assertLookupResult(null,
        null, null, null, null, null, null, "0.0.0.0", null, null, null,
        null, null, null, null, null);
  }

  @Test()
  public void testLookupNoBlocksLines() {
    this.assertLookupResult(
        new ArrayList<String>(), null, null, null, null, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoLocationLines() {
    this.assertLookupResult(null,
        null, null, new ArrayList<String>(), null, null, null, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoIso3166Lines() {
    this.assertLookupResult(null,
        null, null, null, new ArrayList<String>(), null, null, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoRegionLines() {
    this.assertLookupResult(null,
        null, null, null, null, new ArrayList<String>(), null, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoGeoipASNum2Lines() {
    this.assertLookupResult(null,
        null, null, null, null, null, new ArrayList<String>(), "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoCorrespondingLocation() {
    List<String> geoLiteCityLocationLines = new ArrayList<String>();
    geoLiteCityLocationLines.add("Copyright (c) 2012 MaxMind LLC.  All "
        + "Rights Reserved.");
    geoLiteCityLocationLines.add("locId,country,region,city,postalCode,"
        + "latitude,longitude,metroCode,areaCode");
    geoLiteCityLocationLines.add("223,\"US\",\"\",\"\",\"\",38.0000,"
        + "-97.0000,,");
    this.assertLookupResult(null,
        null, null, geoLiteCityLocationLines, null, null, null, "8.8.8.8",
        null, null, null, null, null, null, "AS15169", "Google Inc.");
  }

  @Test()
  public void testLookupNoCorrespondingCountryName() {
    List<String> iso3166Lines = new ArrayList<String>();
    iso3166Lines.add("UY,\"Uruguay\"");
    this.assertLookupResult(null,
        null, null, null, iso3166Lines, null, null, "8.8.8.8", "us",
        null, "California", "Mountain View", "37.3860", "-122.0838",
        "AS15169", "Google Inc.");
  }

  @Test()
  public void testLookupNoCorrespondingRegionName() {
    List<String> regionLines = new ArrayList<String>();
    regionLines.add("US,CO,\"Colorado\"");
    this.assertLookupResult(null,
        null, null, null, null, regionLines, null, "8.8.8.8", "us",
        "United States", null, "Mountain View", "37.3860", "-122.0838",
        "AS15169", "Google Inc.");
  }

  @Test()
  public void testLookupBlocksEndBeforeStart() {
    List<String> manualGeoLiteCityBlocksLines = new ArrayList<String>();
    manualGeoLiteCityBlocksLines.add("Copyright (c) 2011 MaxMind Inc.  "
        + "All Rights Reserved.");
    manualGeoLiteCityBlocksLines.add("startIpNum,endIpNum,locId");
    manualGeoLiteCityBlocksLines.add("\"134739200\",\"134744063\","
        + "\"223\"");
    manualGeoLiteCityBlocksLines.add("\"134744319\",\"134744064\","
        + "\"32191\"");
    manualGeoLiteCityBlocksLines.add("\"134744320\",\"134751743\","
        + "\"223\"");
    this.assertLookupResult(
        manualGeoLiteCityBlocksLines, null, null, null, null, null, null,
        "8.8.8.8", null, null, null, null, null, null, "AS15169",
        "Google Inc.");
  }

  @Test()
  public void testLookupBlocksStartNotANumber() {
    List<String> manualGeoLiteCityBlocksLines = new ArrayList<String>();
    manualGeoLiteCityBlocksLines.add("Copyright (c) 2011 MaxMind Inc.  "
        + "All Rights Reserved.");
    manualGeoLiteCityBlocksLines.add("startIpNum,endIpNum,locId");
    manualGeoLiteCityBlocksLines.add("\"one\",\"134744319\","
        + "\"32191\"");
    this.assertLookupResult(
        manualGeoLiteCityBlocksLines, null, null, null, null, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupBlocksStartTooLarge() {
    List<String> manualGeoLiteCityBlocksLines = new ArrayList<String>();
    manualGeoLiteCityBlocksLines.add("Copyright (c) 2011 MaxMind Inc.  "
        + "All Rights Reserved.");
    manualGeoLiteCityBlocksLines.add("startIpNum,endIpNum,locId");
    manualGeoLiteCityBlocksLines.add("\"1"
        + String.valueOf(Long.MAX_VALUE) + "\",\"134744319\",\"32191\"");
    this.assertLookupResult(
        manualGeoLiteCityBlocksLines, null, null, null, null, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupBlocksLocationX() {
    List<String> manualGeoLiteCityBlocksLines = new ArrayList<String>();
    manualGeoLiteCityBlocksLines.add("Copyright (c) 2011 MaxMind Inc.  "
        + "All Rights Reserved.");
    manualGeoLiteCityBlocksLines.add("startIpNum,endIpNum,locId");
    manualGeoLiteCityBlocksLines.add("\"134744064\",\"134744319\",\"X\"");
    this.assertLookupResult(
        manualGeoLiteCityBlocksLines, null, null, null, null, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupBlocksTooFewFields() {
    List<String> manualGeoLiteCityBlocksLines = new ArrayList<String>();
    manualGeoLiteCityBlocksLines.add("Copyright (c) 2011 MaxMind Inc.  "
        + "All Rights Reserved.");
    manualGeoLiteCityBlocksLines.add("startIpNum,endIpNum,locId");
    manualGeoLiteCityBlocksLines.add("\"134744064\",\"134744319\"");
    this.assertLookupResult(
        manualGeoLiteCityBlocksLines, null, null, null, null, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupLocationLocIdNotANumber() {
    List<String> geoLiteCityLocationLines = new ArrayList<String>();
    geoLiteCityLocationLines.add("Copyright (c) 2012 MaxMind LLC.  All "
        + "Rights Reserved.");
    geoLiteCityLocationLines.add("locId,country,region,city,postalCode,"
        + "latitude,longitude,metroCode,areaCode");
    geoLiteCityLocationLines.add("threetwoonenineone,\"US\",\"CA\","
        + "\"Mountain View\",\"\",37.3860,-122.0838,807,650");
    this.assertLookupResult(null,
        null, null, geoLiteCityLocationLines, null, null, null, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupLocationTooFewFields() {
    List<String> geoLiteCityLocationLines = new ArrayList<String>();
    geoLiteCityLocationLines.add("Copyright (c) 2012 MaxMind LLC.  All "
        + "Rights Reserved.");
    geoLiteCityLocationLines.add("locId,country,region,city,postalCode,"
        + "latitude,longitude,metroCode,areaCode");
    geoLiteCityLocationLines.add("32191,\"US\",\"CA\",\"Mountain View\","
        + "\"\",37.3860,-122.0838,807");
    this.assertLookupResult(null,
        null, null, geoLiteCityLocationLines, null, null, null, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupIso3166TooFewFields() {
    List<String> iso3166Lines = new ArrayList<String>();
    iso3166Lines.add("US");
    this.assertLookupResult(null,
        null, null, null, iso3166Lines, null, null, "8.8.8.8", null, null,
        null, null, null, null, null, null);
  }

  @Test()
  public void testLookupRegionTooFewFields() {
    List<String> regionLines = new ArrayList<String>();
    regionLines.add("US,CA");
    this.assertLookupResult(null,
        null, null, null, null, regionLines, null, "8.8.8.8", null, null,
        null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoipASNum2EndBeforeStart() {
    List<String> geoipASNum2Lines = new ArrayList<String>();
    geoipASNum2Lines.add("134743296,134744063,\"AS3356 Level 3 "
        + "Communications\"");
    geoipASNum2Lines.add("134744319,134744064,\"AS15169 Google Inc.\"");
    geoipASNum2Lines.add("134744320,134750463,\"AS3356 Level 3 "
        + "Communications\"");
    this.assertLookupResult(null,
        null, null, null, null, null, geoipASNum2Lines, "8.8.8.8", "us",
        "United States", "California", "Mountain View", "37.3860",
        "-122.0838", null, null);
  }

  @Test()
  public void testLookupGeoipASNum2StartNotANumber() {
    List<String> geoipASNum2Lines = new ArrayList<String>();
    geoipASNum2Lines.add("one,134744319,\"AS15169 Google Inc.\"");
    this.assertLookupResult(null,
        null, null, null, null, null, geoipASNum2Lines, "8.8.8.8", null,
        null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoipASNum2StartTooLarge() {
    List<String> geoipASNum2Lines = new ArrayList<String>();
    geoipASNum2Lines.add("1" + String.valueOf(Long.MAX_VALUE)
        + ",134744319,\"AS15169 Google Inc.\"");
    this.assertLookupResult(null,
        null, null, null, null, null, geoipASNum2Lines, "8.8.8.8", null,
        null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoipASNum2TooFewFields() {
    List<String> geoipASNum2Lines = new ArrayList<String>();
    geoipASNum2Lines.add("134744064,134744319");
    this.assertLookupResult(null,
        null, null, null, null, null, geoipASNum2Lines, "8.8.8.8", null,
        null, null, null, null, null, null, null);
  }
}

