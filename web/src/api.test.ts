import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { moneyToCents, safeBookingUrl } from './api.ts';

describe('exact-cent inputs', () => {
  it('does not round floating point currency', () => {
    assert.equal(moneyToCents('0.29'), 29);
    assert.equal(moneyToCents('11.7'), 1170);
    assert.equal(moneyToCents('0'), 0);
  });
  it('rejects negative, nonfinite, overprecise and oversized values', () => {
    for (const value of ['-1', 'NaN', '1.001', '1e3', '1000001']) assert.throws(() => moneyToCents(value));
  });
  it('supports zero-decimal currencies without dropping minor units', () => {
    assert.equal(moneyToCents('12000', 0), 12000);
    assert.throws(() => moneyToCents('12.34', 0));
  });
});
it('only opens http(s) booking links', () => {
  assert.equal(safeBookingUrl('javascript:alert(1)'), undefined);
  assert.equal(safeBookingUrl('https://example.com/booking'), 'https://example.com/booking');
});
