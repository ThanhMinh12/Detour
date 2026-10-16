let csrf: {headerName: string; token: string} | null = null;

export async function api<T>(path: string, method = 'GET', payload?: unknown): Promise<T> {
  const headers: Record<string, string> = {Accept: 'application/json'};
  if (payload !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET' && csrf) headers[csrf.headerName] = csrf.token;
  const response = await fetch(path, {method, headers, credentials: 'same-origin',
    body: payload !== undefined ? JSON.stringify(payload) : undefined, signal: AbortSignal.timeout(15000)});
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      csrf = null;
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('detour:session-expired'));
    }
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.message || `Request failed (${response.status})`);
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export async function refreshCsrf() {csrf = await api('/api/auth/csrf');}

export function moneyToCents(text: string, precision = 2): number {
  const pattern = precision === 0 ? /^\d+$/ : new RegExp(`^\\d+(\\.\\d{1,${precision}})?$`);
  if (!pattern.test(text)) throw new Error(`Use a nonnegative amount with at most ${precision} decimal places.`);
  const [whole, fraction = ''] = text.split('.');
  const cents = Number(whole) * 10 ** precision + Number(fraction.padEnd(precision, '0'));
  if (!Number.isSafeInteger(cents) || cents > 100_000_000) throw new Error('Amount is too large.');
  return cents;
}

export const today = () => new Date().toLocaleDateString('en-CA');
export const currencyPrecision = (currency: string) => new Intl.NumberFormat('en-US', {style:'currency',currency}).resolvedOptions().maximumFractionDigits || 0;
export function safeBookingUrl(value?: string): string | undefined {
  if (!value) return undefined;
  try {const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) ? url.href : undefined;}
  catch {return undefined;}
}
