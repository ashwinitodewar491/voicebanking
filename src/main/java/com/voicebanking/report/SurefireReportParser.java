package com.voicebanking.report;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Reads Surefire's TEST-*.xml (JUnit-format) reports left behind by a TestNG run into
 * {@link TestResult} rows. Uses only javax.xml — no XML-binding dependency needed for this. */
public class SurefireReportParser {

    public List<TestResult> parse(File surefireReportsDir) throws Exception {
        List<TestResult> results = new ArrayList<>();
        File[] xmlFiles = surefireReportsDir.listFiles((dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        if (xmlFiles == null) return results;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        for (File xmlFile : xmlFiles) {
            Document doc = factory.newDocumentBuilder().parse(xmlFile);
            NodeList testcases = doc.getElementsByTagName("testcase");
            for (int i = 0; i < testcases.getLength(); i++) {
                results.add(toTestResult((Element) testcases.item(i)));
            }
        }
        return results;
    }

    private TestResult toTestResult(Element testcase) {
        TestResult r = new TestResult();
        r.className = testcase.getAttribute("classname");
        r.fullName = testcase.getAttribute("name");
        r.durationSeconds = parseDouble(testcase.getAttribute("time"));

        NodeList failures = testcase.getElementsByTagName("failure");
        NodeList errors = testcase.getElementsByTagName("error");
        NodeList skipped = testcase.getElementsByTagName("skipped");

        if (failures.getLength() > 0 || errors.getLength() > 0) {
            Element failEl = failures.getLength() > 0 ? (Element) failures.item(0) : (Element) errors.item(0);
            r.status = TestResult.Status.FAILED;
            r.failureMessage = failEl.getAttribute("message");
            r.failureType = failEl.getAttribute("type");
            r.stackTrace = failEl.getTextContent();
        } else if (skipped.getLength() > 0) {
            r.status = TestResult.Status.SKIPPED;
        } else {
            r.status = TestResult.Status.PASSED;
        }

        splitNameAndParamLabel(r);
        return r;
    }

    /** TestNG data-provider tests get a Surefire testcase name like
     * {@code testVoiceQuery[Can I Transfer Money, Can I transfer money, ...](1)}; plain tests get
     * just the method name. The first bracketed value is the same string ScreenshotUtil uses as
     * its filename label, so splitting it out here is what lets screenshots be matched back up. */
    private void splitNameAndParamLabel(TestResult r) {
        String raw = r.fullName;
        int bracketIdx = raw.indexOf('[');
        if (bracketIdx == -1) {
            r.methodName = raw;
            return;
        }
        r.methodName = raw.substring(0, bracketIdx);
        String inside = raw.substring(bracketIdx + 1);
        int commaIdx = inside.indexOf(',');
        int closeIdx = inside.indexOf(']');
        int endIdx = commaIdx == -1 ? closeIdx : (closeIdx == -1 ? commaIdx : Math.min(commaIdx, closeIdx));
        if (endIdx == -1) endIdx = inside.length();
        r.paramLabel = inside.substring(0, endIdx).trim();
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
