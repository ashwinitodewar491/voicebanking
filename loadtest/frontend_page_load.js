/**
 * k6 BROWSER-MODULE load test — real Chromium instances (via CDP), simulating concurrent
 * users loading and navigating the Voice Banking UI. This is the lightweight complement to
 * loadtest/../ConcurrentVoiceLoadTest.java: it can spin up more concurrent sessions than a
 * full Playwright-driven voice flow can, but it CANNOT replicate the mic/fake-audio-capture
 * flow (k6 browser has no equivalent of Chromium's --use-file-for-fake-audio-capture flag),
 * so it only measures page-load/navigation performance under concurrency, not voice queries.
 *
 * Use this to answer "does the page itself hold up under N concurrent users hitting it,"
 * and ConcurrentVoiceLoadTest.java to answer "does the voice pipeline hold up under N
 * concurrent users talking to it." They're deliberately separate tools for that reason.
 *
 * Requires the k6 browser module (bundled in k6 >= v0.53, or run via the k6-browser-enabled
 * Docker image grafana/k6:master-with-browser).
 *
 * Usage:
 *   k6 run loadtest/frontend_page_load.js
 *   k6 run -e UI_BASE_URL=https://voicebank-stage.joshsoftware.com -e VUS=10 -e DURATION=1m loadtest/frontend_page_load.js
 *   K6_BROWSER_HEADLESS=false k6 run loadtest/frontend_page_load.js   # watch it run
 */

import { browser } from 'k6/browser';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const UI_BASE_URL = __ENV.UI_BASE_URL || 'https://voicebank-stage.joshsoftware.com';
const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '1m';

const pageLoadTime = new Trend('welcome_page_load_ms', true);
const otpScreenTime = new Trend('otp_screen_reach_ms', true);

export const options = {
    scenarios: {
        ui_concurrent_users: {
            executor: 'ramping-vus',
            exec: 'openAndNavigate',
            startVUs: 0,
            stages: [
                { duration: '20s', target: VUS },
                { duration: DURATION, target: VUS },
                { duration: '20s', target: 0 },
            ],
            options: {
                browser: {
                    type: 'chromium',
                },
            },
        },
    },
    thresholds: {
        welcome_page_load_ms: ['p(95)<3000'],
        checks: ['rate>0.95'],
    },
};

export async function openAndNavigate() {
    const page = await browser.newPage();

    try {
        const loadStart = Date.now();
        const response = await page.goto(UI_BASE_URL, { waitUntil: 'networkidle' });
        pageLoadTime.add(Date.now() - loadStart);

        check(response, {
            'welcome page status is 200': (r) => r && r.status() === 200,
        });

        // Dismiss the PWA-install popup if present — same data-testid WelcomePage.java uses
        // (dismissPwaPopupIfPresent / PWA_NOT_NOW_BTN), best-effort, ignored if not shown.
        try {
            const dismissButton = page.locator("[data-testid='pwa-not-now-btn']");
            if (await dismissButton.isVisible({ timeout: 3000 })) {
                await dismissButton.click();
            }
        } catch (ignored) {
            // popup wasn't present — fine
        }

        // Enter a phone number and reach the OTP screen — the first real concurrency-sensitive
        // hop in the login flow (OTP issuance is a backend call, not just static page render).
        // Same data-testid selectors as WelcomePage.java (PHONE_INPUT / SEND_OTP_BTN) and
        // OtpPage.java's own OTP-input testid, kept in lockstep with the Java page objects so
        // this script doesn't drift from the real DOM if those change.
        const otpStart = Date.now();
        try {
            await page.locator("[data-testid='welcome-phone-input']").fill('9898989898', { timeout: 5000 });
            await page.locator("[data-testid='welcome-send-otp-btn']").click({ timeout: 5000 });
            await page.waitForSelector("[data-testid='otp-digit-input-1']", { timeout: 10000 });
            otpScreenTime.add(Date.now() - otpStart);
            check(true, { 'reached OTP screen': () => true });
        } catch (e) {
            check(false, { 'reached OTP screen': () => false });
        }
    } finally {
        await page.close();
    }

    sleep(1);
}
