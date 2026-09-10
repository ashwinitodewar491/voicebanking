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
        r.description = lookupTestDescription(r.className, r.methodName);
        return r;
    }

    /** Surefire's own TEST-*.xml (JUnit format, parsed above) has no field for TestNG's {@code
     * @Test(description = ...)} — the actual richer test metadata TestNG tracks internally never
     * makes it into that generic schema. Looks the annotation up directly via reflection instead,
     * rather than depending on TestNG's own separate XML report's exact shape. Uses reflection
     * (not a direct {@code org.testng.annotations.Test} import) because this class lives under
     * src/main, which compiles without TestNG on its classpath (a test-scoped dependency) — this
     * only needs TestNG present at the runtime this actually executes at (this report generator
     * is invoked via exec:java with {@code classpathScope=test}, see pom.xml), not at compile
     * time. Returns null (silently) if the class/method can't be found or has no description —
     * {@link TestResult#displayName()} falls back to the method name either way. */
    private static String lookupTestDescription(String className, String methodName) {
        try {
            Class<?> testClass = Class.forName(className);
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> testAnnotationClass =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName("org.testng.annotations.Test");

            for (java.lang.reflect.Method m : testClass.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                java.lang.annotation.Annotation testAnnotation = m.getAnnotation(testAnnotationClass);
                if (testAnnotation == null) continue;
                Object value = testAnnotationClass.getMethod("description").invoke(testAnnotation);
                return value != null ? value.toString() : null;
            }
        } catch (Exception ignored) {
            // Class not on the classpath, method not found, or reflection failed — no description.
        }
        return null;
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
