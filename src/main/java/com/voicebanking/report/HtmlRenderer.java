package com.voicebanking.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Renders the parsed results into one self-contained index.html — all data, CSS and JS inline,
 * no fetch/XHR calls, so it opens correctly straight off disk (file://) with no local server.
 * The pass/fail donut is plain CSS conic-gradient rather than a JS charting library. */
public class HtmlRenderer {

    public String render(List<TestResult> results, int sessionEndedCount) {
        return render(results, sessionEndedCount, List.of(), 0, List.of());
    }

    public String render(List<TestResult> results, int sessionEndedCount, List<String> sessionEndedDetails) {
        return render(results, sessionEndedCount, sessionEndedDetails, 0, List.of());
    }

    public String render(List<TestResult> results, int sessionEndedCount, List<String> sessionEndedDetails,
                          int noResponseCount, List<String> noResponseDetails) {
        long total = results.size();
        long passed = countStatus(results, TestResult.Status.PASSED);
        long failed = countStatus(results, TestResult.Status.FAILED);
        long skipped = countStatus(results, TestResult.Status.SKIPPED);
        double durationSeconds = results.stream().mapToDouble(r -> r.durationSeconds).sum();
        double passRate = total == 0 ? 0 : (passed * 100.0 / total);

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        html.append("<title>QA Automation Dashboard</title>");
        html.append("<style>").append(css()).append("</style></head><body>");

        html.append(renderHeader(passRate));
        html.append(renderSummary(total, passed, failed, skipped, durationSeconds, sessionEndedCount, noResponseCount));
        html.append(renderDetailsList("Session Drop Details", "session-drops", sessionEndedDetails));
        html.append(renderDetailsList("Bot No-Response Details", "no-response", noResponseDetails));
        html.append(renderDonut(total, passed, failed, skipped));
        html.append(renderModuleBreakdown(results));
        html.append(renderFailureReasons(results));
        html.append(renderTable(results));

        html.append("<script>").append(js()).append("</script>");
        html.append("</body></html>");
        return html.toString();
    }

    private long countStatus(List<TestResult> results, TestResult.Status status) {
        return results.stream().filter(r -> r.status == status).count();
    }

    private String renderHeader(double passRate) {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        return "<header><h1>QA Automation Dashboard</h1>"
                + "<p class=\"generated\">Generated " + esc(generatedAt) + " &middot; Pass rate "
                + String.format("%.1f", passRate) + "%</p></header>";
    }

    private String renderSummary(long total, long passed, long failed, long skipped,
                                  double durationSeconds, int sessionEndedCount, int noResponseCount) {
        StringBuilder sb = new StringBuilder("<section class=\"summary\">");
        sb.append(statCard("Total", String.valueOf(total), "neutral"));
        sb.append(statCard("Passed", String.valueOf(passed), "pass"));
        sb.append(statCard("Failed", String.valueOf(failed), "fail"));
        sb.append(statCard("Skipped", String.valueOf(skipped), "skip"));
        sb.append(statCard("Duration", formatDuration(durationSeconds), "neutral"));
        // Environment-stability signals, not test outcomes — either one that gets successfully
        // recovered from (session reconnect, or a later re-ask finally getting an answer — see
        // BaseVoiceTest.runVoiceQuery) never shows up as a failure at all, so this is the only
        // place either count is visible.
        sb.append(statCard("Session Drops", String.valueOf(sessionEndedCount),
                sessionEndedCount > 0 ? "warn" : "neutral"));
        sb.append(statCard("Bot No-Response", String.valueOf(noResponseCount),
                noResponseCount > 0 ? "warn" : "neutral"));
        sb.append("</section>");
        return sb.toString();
    }

    /** Lists each occurrence (time + query) of an environment-stability signal below the summary
     * cards, so a non-zero count can be traced back to exactly where it happened rather than just
     * how many. Shared by "Session Ended" and "bot never responded" — same shape, different
     * source. Renders nothing when the list is empty. */
    private String renderDetailsList(String title, String cssClass, List<String> details) {
        if (details.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<section class=\"").append(cssClass).append("\">");
        sb.append("<h2>").append(esc(title)).append(" (").append(details.size()).append(")</h2><ul>");
        for (String detail : details) {
            sb.append("<li>").append(esc(detail)).append("</li>");
        }
        sb.append("</ul></section>");
        return sb.toString();
    }

    private String statCard(String label, String value, String cssClass) {
        return "<div class=\"card " + cssClass + "\"><div class=\"value\">" + esc(value) + "</div>"
                + "<div class=\"label\">" + esc(label) + "</div></div>";
    }

    private String renderDonut(long total, long passed, long failed, long skipped) {
        if (total == 0) return "";
        double passPct = passed * 100.0 / total;
        double failPct = failed * 100.0 / total;
        double skipPct = skipped * 100.0 / total;
        double failEnd = passPct + failPct;
        String gradient = String.format(
                "conic-gradient(var(--pass) 0%% %.3f%%, var(--fail) %.3f%% %.3f%%, var(--skip) %.3f%% 100%%)",
                passPct, passPct, failEnd, failEnd);
        return "<section class=\"donut-section\">"
                + "<div class=\"donut\" style=\"background:" + gradient + "\"><div class=\"donut-hole\">"
                + String.format("%.1f", passPct) + "%<span>passed</span></div></div>"
                + "<ul class=\"legend\">"
                + "<li><span class=\"dot pass\"></span>Passed (" + passed + ")</li>"
                + "<li><span class=\"dot fail\"></span>Failed (" + failed + ")</li>"
                + "<li><span class=\"dot skip\"></span>Skipped (" + skipped + ")</li>"
                + "</ul></section>";
    }

    /** One card per module (see TestResult#module), sorted worst-pass-rate-first so the modules
     * that need attention are the ones you see without scrolling. Clicking a card filters the
     * test table below to just that module, reusing the same search box as free-text search. */
    private String renderModuleBreakdown(List<TestResult> results) {
        if (results.isEmpty()) return "";

        Map<String, long[]> stats = new LinkedHashMap<>(); // [total, passed, failed, skipped]
        for (TestResult r : results) {
            long[] s = stats.computeIfAbsent(r.module(), k -> new long[4]);
            s[0]++;
            if (r.status == TestResult.Status.PASSED) s[1]++;
            else if (r.status == TestResult.Status.FAILED) s[2]++;
            else s[3]++;
        }

        List<String> modules = new ArrayList<>(stats.keySet());
        modules.sort(Comparator.comparingDouble(m -> passRate(stats.get(m))));

        StringBuilder sb = new StringBuilder("<section class=\"module-section\"><h2>Module Breakdown</h2>");
        sb.append("<div class=\"module-grid\">");
        for (String module : modules) {
            long[] s = stats.get(module);
            double rate = passRate(s);
            String tier = rate >= 95 ? "good" : rate >= 80 ? "warn" : "bad";
            sb.append("<div class=\"module-card ").append(tier).append("\" onclick=\"filterByModule('")
                    .append(escJs(module)).append("')\">");
            sb.append("<div class=\"module-name\">").append(esc(module)).append("</div>");
            sb.append("<div class=\"module-rate\">").append(String.format("%.1f", rate)).append("%</div>");
            sb.append("<div class=\"module-bar\"><div class=\"module-bar-fill\" style=\"width:")
                    .append(String.format("%.1f", rate)).append("%\"></div></div>");
            sb.append("<div class=\"module-counts\">").append(s[1]).append(" / ").append(s[0])
                    .append(" passed").append(s[2] > 0 ? " &middot; " + s[2] + " failed" : "").append("</div>");
            sb.append("</div>");
        }
        sb.append("</div></section>");
        return sb.toString();
    }

    private double passRate(long[] stats) {
        return stats[0] == 0 ? 0 : (stats[1] * 100.0 / stats[0]);
    }

    /** One bar per distinct failure reason (see TestResult#failureCategory), sorted most-common
     * first — this is what actually answers "is this one weird one-off or a systemic problem,"
     * since e.g. 18 failures all bucketing into "Session Dropped" points at environment
     * instability rather than 18 unrelated bugs. Clicking a bar filters the table to just that
     * reason. Renders nothing when there are no failures. */
    private String renderFailureReasons(List<TestResult> results) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TestResult r : results) {
            String category = r.failureCategory();
            if (category == null) continue;
            counts.merge(category, 1L, Long::sum);
        }
        if (counts.isEmpty()) return "";

        List<String> categories = new ArrayList<>(counts.keySet());
        categories.sort(Comparator.comparingLong((String c) -> counts.get(c)).reversed());
        long maxCount = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);

        StringBuilder sb = new StringBuilder("<section class=\"reason-section\"><h2>Failure Reasons</h2>");
        sb.append("<div class=\"reason-list\">");
        for (String category : categories) {
            long count = counts.get(category);
            double widthPct = count * 100.0 / maxCount;
            sb.append("<div class=\"reason-row\" onclick=\"filterByCategory('")
                    .append(escJs(category)).append("')\">");
            sb.append("<div class=\"reason-name\">").append(esc(category)).append("</div>");
            sb.append("<div class=\"reason-bar\"><div class=\"reason-bar-fill\" style=\"width:")
                    .append(String.format("%.1f", widthPct)).append("%\"></div></div>");
            sb.append("<div class=\"reason-count\">").append(count).append("</div>");
            sb.append("</div>");
        }
        sb.append("</div></section>");
        return sb.toString();
    }

    private String renderTable(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"table-section\">");
        sb.append("<input id=\"search\" type=\"text\" placeholder=\"Search tests...\" oninput=\"filterRows()\">");
        sb.append("<table id=\"results\"><thead><tr>")
                .append("<th>Module</th><th>Class</th><th>Test</th><th>Status</th><th>Reason</th><th>Duration</th>")
                .append("</tr></thead><tbody>");

        int rowIndex = 0;
        for (TestResult r : results) {
            String statusClass = r.status.name().toLowerCase();
            boolean expandable = r.status == TestResult.Status.FAILED;
            sb.append("<tr class=\"row ").append(statusClass).append(expandable ? " expandable" : "")
                    .append("\"").append(expandable ? " onclick=\"toggleDetail(" + rowIndex + ")\"" : "").append(">");
            sb.append("<td>").append(esc(r.module())).append("</td>");
            sb.append("<td>").append(esc(r.simpleClassName())).append("</td>");
            sb.append("<td>").append(esc(r.displayName())).append("</td>");
            sb.append("<td><span class=\"badge ").append(statusClass).append("\">")
                    .append(esc(r.status.name())).append("</span></td>");
            sb.append("<td>").append(esc(r.failureCategory() != null ? r.failureCategory() : "")).append("</td>");
            sb.append("<td>").append(String.format("%.1fs", r.durationSeconds)).append("</td>");
            sb.append("</tr>");

            if (expandable) {
                sb.append("<tr class=\"detail-row\" id=\"detail-").append(rowIndex).append("\" style=\"display:none\">");
                sb.append("<td colspan=\"6\"><div class=\"detail\">");
                if (r.failureType != null && !r.failureType.isBlank()) {
                    sb.append("<div class=\"failure-type\">").append(esc(r.failureType)).append("</div>");
                }
                if (r.failureMessage != null && !r.failureMessage.isBlank()) {
                    sb.append("<div class=\"failure-message\">").append(esc(r.failureMessage)).append("</div>");
                }
                if (r.screenshotDataUri != null) {
                    sb.append("<img class=\"screenshot\" src=\"").append(r.screenshotDataUri)
                            .append("\" alt=\"failure screenshot\">");
                }
                if (r.stackTrace != null && !r.stackTrace.isBlank()) {
                    sb.append("<pre class=\"stacktrace\">").append(esc(r.stackTrace)).append("</pre>");
                }
                sb.append("</div></td></tr>");
            }
            rowIndex++;
        }

        sb.append("</tbody></table></section>");
        return sb.toString();
    }

    private String formatDuration(double totalSeconds) {
        long totalSecs = Math.round(totalSeconds);
        long minutes = totalSecs / 60;
        long seconds = totalSecs % 60;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String css() {
        return """
            :root {
                --bg: #f5f6f8; --card-bg: #ffffff; --text: #1a1d23; --muted: #6b7280;
                --border: #e5e7eb; --pass: #22c55e; --fail: #ef4444; --skip: #a1a1aa; --warn: #eab308;
            }
            @media (prefers-color-scheme: dark) {
                :root { --bg: #14161a; --card-bg: #1e2126; --text: #e5e7eb; --muted: #9ca3af; --border: #2d3138; }
            }
            * { box-sizing: border-box; }
            body { margin: 0; font-family: -apple-system, Segoe UI, Roboto, sans-serif; background: var(--bg); color: var(--text); }
            header { padding: 24px 32px 8px; }
            header h1 { margin: 0; font-size: 22px; }
            .generated { color: var(--muted); font-size: 13px; margin: 4px 0 0; }
            .summary { display: flex; gap: 12px; padding: 16px 32px; flex-wrap: wrap; }
            .card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px; padding: 14px 20px; min-width: 110px; }
            .card .value { font-size: 26px; font-weight: 700; }
            .card .label { color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
            .card.pass .value { color: var(--pass); }
            .card.fail .value { color: var(--fail); }
            .card.skip .value { color: var(--skip); }
            .card.warn .value { color: var(--warn); }
            .donut-section { display: flex; align-items: center; gap: 24px; padding: 8px 32px 24px; }
            .donut { width: 120px; height: 120px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
            .donut-hole { width: 80px; height: 80px; border-radius: 50%; background: var(--card-bg); display: flex; flex-direction: column; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; }
            .donut-hole span { font-weight: 400; font-size: 10px; color: var(--muted); text-transform: uppercase; }
            .legend { list-style: none; margin: 0; padding: 0; font-size: 13px; }
            .legend li { display: flex; align-items: center; gap: 8px; margin: 4px 0; }
            .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
            .dot.pass { background: var(--pass); } .dot.fail { background: var(--fail); } .dot.skip { background: var(--skip); }
            .session-drops, .no-response { padding: 8px 32px 24px; }
            .session-drops h2, .no-response h2 { font-size: 13px; margin: 0 0 12px; color: var(--warn); text-transform: uppercase; letter-spacing: .04em; }
            .session-drops ul, .no-response ul { list-style: none; margin: 0; padding: 0; background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px; }
            .session-drops li, .no-response li { padding: 8px 16px; font-size: 13px; border-bottom: 1px solid var(--border); }
            .session-drops li:last-child, .no-response li:last-child { border-bottom: none; }
            .module-section { padding: 8px 32px 24px; }
            .module-section h2 { font-size: 13px; margin: 0 0 12px; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; }
            .module-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 12px; }
            .module-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px; padding: 12px 16px; cursor: pointer; }
            .module-card:hover { background: rgba(127,127,127,0.08); }
            .module-name { font-weight: 600; font-size: 13px; margin-bottom: 4px; }
            .module-rate { font-size: 22px; font-weight: 700; }
            .module-card.good .module-rate { color: var(--pass); }
            .module-card.warn .module-rate { color: var(--warn); }
            .module-card.bad .module-rate { color: var(--fail); }
            .module-bar { height: 6px; background: var(--border); border-radius: 999px; margin: 6px 0; overflow: hidden; }
            .module-bar-fill { height: 100%; }
            .module-card.good .module-bar-fill { background: var(--pass); }
            .module-card.warn .module-bar-fill { background: var(--warn); }
            .module-card.bad .module-bar-fill { background: var(--fail); }
            .module-counts { font-size: 11px; color: var(--muted); }
            .reason-section { padding: 8px 32px 24px; }
            .reason-section h2 { font-size: 13px; margin: 0 0 12px; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; }
            .reason-list { display: flex; flex-direction: column; gap: 8px; max-width: 640px; }
            .reason-row { display: grid; grid-template-columns: 160px 1fr 32px; align-items: center; gap: 10px; cursor: pointer; padding: 6px 10px; border-radius: 8px; }
            .reason-row:hover { background: rgba(127,127,127,0.08); }
            .reason-name { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            .reason-bar { height: 10px; background: var(--border); border-radius: 999px; overflow: hidden; }
            .reason-bar-fill { height: 100%; background: var(--fail); }
            .reason-count { font-size: 12px; color: var(--muted); text-align: right; }
            .table-section { padding: 0 32px 40px; }
            #search { width: 100%; max-width: 320px; padding: 8px 12px; margin-bottom: 12px; border: 1px solid var(--border); border-radius: 8px; background: var(--card-bg); color: var(--text); }
            table { width: 100%; border-collapse: collapse; background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
            th, td { text-align: left; padding: 10px 14px; border-bottom: 1px solid var(--border); font-size: 13px; }
            th { color: var(--muted); font-weight: 600; text-transform: uppercase; font-size: 11px; }
            .row.expandable { cursor: pointer; }
            .row.expandable:hover { background: rgba(127,127,127,0.08); }
            .badge { padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 600; }
            .badge.passed { background: rgba(34,197,94,.15); color: var(--pass); }
            .badge.failed { background: rgba(239,68,68,.15); color: var(--fail); }
            .badge.skipped { background: rgba(161,161,170,.15); color: var(--skip); }
            .detail { padding: 12px 0; }
            .failure-type { font-weight: 700; color: var(--fail); font-size: 13px; }
            .failure-message { margin: 4px 0 10px; }
            .screenshot { max-width: 480px; width: 100%; border: 1px solid var(--border); border-radius: 6px; margin-bottom: 10px; display: block; }
            .stacktrace { background: var(--bg); border: 1px solid var(--border); border-radius: 6px; padding: 10px; font-size: 11px; overflow-x: auto; max-height: 260px; overflow-y: auto; white-space: pre-wrap; }
            """;
    }

    private String js() {
        return """
            function toggleDetail(i) {
                var row = document.getElementById('detail-' + i);
                if (row) row.style.display = row.style.display === 'none' ? 'table-row' : 'none';
            }
            function filterRows() {
                var q = document.getElementById('search').value.toLowerCase();
                var rows = document.querySelectorAll('#results tbody tr.row');
                rows.forEach(function(row) {
                    var match = row.textContent.toLowerCase().indexOf(q) !== -1;
                    row.style.display = match ? '' : 'none';
                    var next = row.nextElementSibling;
                    if (next && next.classList.contains('detail-row') && !match) next.style.display = 'none';
                });
            }
            function filterByModule(module) {
                document.getElementById('search').value = module;
                filterRows();
            }
            function filterByCategory(category) {
                document.getElementById('search').value = category;
                filterRows();
            }
            """;
    }
}
