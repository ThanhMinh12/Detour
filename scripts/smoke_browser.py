"""Real React UI smoke test for isolated local data or the explicitly approved preview."""
import argparse
import json
import time
import uuid
from pathlib import Path
from urllib.parse import urlsplit

from playwright.sync_api import Error as PlaywrightError, expect, sync_playwright


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', default='http://127.0.0.1:4320')
    parser.add_argument('--allow-disposable-preview', action='store_true',
                        help='Create synthetic QA data in the known, temporary AWS preview')
    parser.add_argument('--evidence-dir', type=Path, default=Path('/tmp/projects-ready-evidence'))
    args = parser.parse_args()
    local = urlsplit(args.url).hostname in {'127.0.0.1','localhost'}
    preview = args.allow_disposable_preview and args.url.rstrip('/') == 'https://mkmuk8wzzz.us-east-2.awsapprunner.com'
    assert local or preview, 'Use isolated local data or explicitly allow the known disposable preview'
    report = {'target': 'disposable_aws_preview' if preview else 'local'}
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch()
        context = browser.new_context(viewport={'width':1440,'height':1000}, base_url=args.url)
        page = context.new_page()
        deadline = time.monotonic() + 90
        while True:
            try:
                if context.request.get('/actuator/health', timeout=2000).status == 200:
                    break
            except PlaywrightError:
                pass
            if time.monotonic() >= deadline:
                raise TimeoutError('Detour did not become healthy within 90 seconds')
            time.sleep(.5)
        errors = []
        page.on('pageerror', lambda error: errors.append(str(error)))
        page.goto(args.url)
        expect(page.get_by_role('heading', level=1)).to_have_text('Trip planning')
        expect(page).to_have_title('Detour — Trip planning')
        assert page.locator('.auth-story').evaluate('(node) => getComputedStyle(node).backgroundColor') == 'rgb(23, 63, 53)'
        expect(page.locator('.auth-story-copy > p')).to_have_count(0)
        assert 'every penny' not in page.locator('body').inner_text().lower()
        page.locator('#auth-toggle').click()
        email = f'qa-{uuid.uuid4().hex}@example.com'
        password = uuid.uuid4().hex
        page.locator('#auth-form [name=displayName]').fill('QA Traveler')
        page.locator('#auth-form [name=email]').fill(email)
        page.locator('#auth-form [name=password]').fill(password)
        page.locator('#auth-submit').click()
        page.locator('#load-demo').click()
        expect(page.locator('#dashboard')).to_be_visible(timeout=30000)
        assert int(page.locator('#plan-count').inner_text()) >= 2
        for mode in ['EQUAL','EXACT','PERCENTAGE','ITEMIZED']:
            page.get_by_role('button', name='+ Add expense', exact=True).click()
            form = page.locator('#expense-form')
            form.locator('[name=description]').fill('QA ' + mode)
            form.locator('#split-mode').select_option(mode)
            if mode == 'ITEMIZED':
                form.get_by_label('Item name', exact=True).fill('Shared rice')
                form.get_by_label('Item amount', exact=True).fill('20')
                for checkbox in form.locator('input[type=checkbox]').all()[2:]: checkbox.uncheck()
            else:
                form.locator('[name=subtotal]').fill('40' if mode == 'EXACT' else '12.01')
                for entry in form.locator('input[name^=share-]').all(): entry.fill('10' if mode == 'EXACT' else '25')
            form.get_by_role('button', name='Save', exact=True).click()
            expect(page.get_by_role('dialog')).not_to_be_visible()
            expect(page.locator('#expense-list')).to_contain_text('QA ' + mode)
        trip_id = page.locator('#trip-select').input_value()
        expenses = context.request.get(f'/api/trips/{trip_id}/expenses').json()
        for expense in expenses:
            assert sum(share['totalCents'] for share in expense['shares']) == expense['totalCents']
        assert next(expense for expense in expenses if expense['description'] == 'QA ITEMIZED')['shares'].__len__() == 2
        report['all_four_split_modes'] = 'passed'
        page.get_by_role('button', name='Add plan', exact=True).click()
        form = page.locator('#activity-form')
        form.locator('[name=title]').fill('QA hotel')
        form.locator('[name=type]').select_option('LODGING')
        form.locator('[name=status]').select_option('CONFIRMED')
        form.locator('[name=reservationReference]').fill('QA-BOOKING')
        form.locator('[name=bookingUrl]').fill('https://example.com/booking')
        form.get_by_role('button', name='Save', exact=True).click()
        row = page.locator('.activity-row').filter(has_text='QA hotel')
        expect(row).to_contain_text('QA-BOOKING')
        expect(row.get_by_role('link', name='Open booking')).to_have_attribute('href','https://example.com/booking')
        row.get_by_role('button', name='Edit', exact=True).click()
        form = page.locator('#activity-form')
        form.locator('[name=reservationReference]').fill('QA-UPDATED')
        form.get_by_role('button', name='Save', exact=True).click()
        expect(row).to_contain_text('QA-UPDATED')
        row.get_by_role('button', name='Upvote QA hotel', exact=True).click()
        expect(row.get_by_role('button', name='Upvote QA hotel', exact=True)).to_have_attribute('aria-pressed','true')
        report['reservation_create_edit_and_vote'] = 'passed'
        before = page.locator('.mark-paid').count()
        page.locator('.mark-paid').first.click()
        expect(page.locator('.mark-paid')).to_have_count(before - 1)
        report['repayment'] = 'passed'
        page.locator('#logout-button').click()
        page.locator('#auth-form [name=email]').fill(email)
        page.locator('#auth-form [name=password]').fill(password)
        page.locator('#auth-submit').click()
        expect(page.locator('#dashboard')).to_be_visible()
        assert page.locator('#trip-select').input_value() == trip_id
        report['logout_login_restores_trip'] = 'passed'
        artifact = args.evidence_dir
        artifact.mkdir(parents=True, exist_ok=True)
        for width in [1440,980,901,768,390,320]:
            page.set_viewport_size({'width':width,'height':1000})
            assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth'), width
            sidebar = page.locator('.sidebar').bounding_box()
            assert sidebar and sidebar['x'] >= 0 and sidebar['x'] + sidebar['width'] <= width, 'Navigation is off screen'
            page.locator('.sidebar .react-actions').get_by_role('button', name='Add traveler', exact=True).click()
            expect(page.get_by_role('dialog')).to_be_visible()
            page.get_by_role('button', name='Close dialog', exact=True).click()
            page.screenshot(path=str(artifact / f'detour-{width}.png'), full_page=True)
        report['responsive_navigation_and_layout'] = 'passed'
        assert not errors, errors
        browser.close()
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
