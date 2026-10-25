import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { api, currencyPrecision, moneyToCents, refreshCsrf, safeBookingUrl, today } from './api';
import type { Activity, Balance, Expense, Member, Session, Settlement, Transfer, Trip, TripData, User } from './types';

const categories = ['FOOD', 'TRANSIT', 'ACTIVITY', 'LODGING', 'SHOPPING', 'OTHER'];
const label = (value: string) => value.charAt(0).toUpperCase() + value.slice(1).toLowerCase().replaceAll('_', ' ');
const message = (error: unknown) => error instanceof Error ? error.message : 'Something went wrong. Please try again.';
const fields = (form: HTMLFormElement) => Object.fromEntries(new FormData(form).entries()) as Record<string, string>;
const initials = (name: string) => name.split(' ').map(part => part[0]).slice(0, 2).join('').toUpperCase();

function Brand() {return <a className="brand" href="/" aria-label="Detour home"><span className="brand-mark">↗</span><span>detour</span></a>;}
function Avatar({name}: {name: string}) {return <span className="avatar avatar-0">{initials(name)}</span>;}

function Modal({title, children, close}: {title: string; children: ReactNode; close: () => void}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {ref.current?.showModal();}, []);
  return <dialog ref={ref} className="react-dialog" onCancel={close} aria-label={title}>
    <div className="dialog-header"><h2>{title}</h2><button type="button" className="close-dialog" onClick={close} aria-label="Close dialog">×</button></div>{children}
  </dialog>;
}

function Form({id, children, save}: {id?: string; children: ReactNode; save: (form: HTMLFormElement) => Promise<void>}) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('');
    try {await save(event.currentTarget);} catch (error) {setError(message(error));} finally {setBusy(false);}
  }
  return <form id={id} className="react-form" onSubmit={event => void submit(event)}>
    {children}{error && <p className="react-error" role="alert">{error}</p>}
    <button className="button primary" disabled={busy} type="submit">{busy ? 'Saving…' : 'Save'}</button>
  </form>;
}

function ExpenseForm({data, save}: {data: TripData; save: (payload: unknown) => Promise<void>}) {
  const [mode, setMode] = useState('EQUAL');
  const [items, setItems] = useState([crypto.randomUUID()]);
  const amount = (text: string) => moneyToCents(text, currencyPrecision(data.trip.currency));
  return <Form id="expense-form" save={async form => {
    const values = fields(form); const raw = new FormData(form);
    const allocations = data.members.map(member => ({memberId: member.id, value: mode === 'EQUAL' ? 0 : moneyToCents(values[`share-${member.id}`] || '0', mode === 'PERCENTAGE' ? 2 : currencyPrecision(data.trip.currency))}))
      .filter(value => mode === 'EQUAL' ? raw.getAll('participants').includes(value.memberId) : value.value > 0);
    const receipt = mode === 'ITEMIZED' ? items.map(id => ({name: values[`item-${id}-name`], amountCents: amount(values[`item-${id}-amount`]), participantIds: raw.getAll(`item-${id}-members`)})) : [];
    if (mode === 'ITEMIZED' && (!receipt.length || receipt.some(item => !item.participantIds.length))) throw new Error('Every item needs an amount and at least one traveler.');
    if (mode !== 'ITEMIZED' && !allocations.length) throw new Error('Choose at least one traveler.');
    const subtotalCents = mode === 'ITEMIZED' ? receipt.reduce((sum, item) => sum + item.amountCents, 0) : amount(values.subtotal);
    if (mode === 'EXACT' && allocations.reduce((sum, value) => sum + value.value, 0) !== subtotalCents) throw new Error('Exact shares must total the subtotal.');
    if (mode === 'PERCENTAGE' && allocations.reduce((sum, value) => sum + value.value, 0) !== 10000) throw new Error('Percentages must total 100%.');
    await save({description: values.description, paidByMemberId: values.paidByMemberId, category: values.category, splitMode: mode, occurredOn: values.occurredOn, subtotalCents, taxCents: amount(values.tax || '0'), tipCents: amount(values.tip || '0'), allocations, items: receipt});
  }}>
    <label>Description<input name="description" required maxLength={180} placeholder="Dinner with the group" /></label>
    <label>Paid by<select name="paidByMemberId">{data.members.map(member => <option key={member.id} value={member.id}>{member.displayName}</option>)}</select></label>
    <label>Category<select name="category">{categories.map(category => <option key={category}>{category}</option>)}</select></label>
    <label>Date<input name="occurredOn" type="date" defaultValue={today()} required /></label>
    <label>Split<select id="split-mode" name="splitMode" value={mode} onChange={event => setMode(event.target.value)}>{['EQUAL','EXACT','PERCENTAGE','ITEMIZED'].map(value => <option key={value} value={value}>{label(value)}</option>)}</select></label>
    {mode !== 'ITEMIZED' && <label>Subtotal ({data.trip.currency})<input name="subtotal" inputMode="decimal" placeholder="0.00" required pattern="[0-9]+(\.[0-9]{1,2})?" /></label>}
    <label>Tax<input name="tax" inputMode="decimal" defaultValue="0" pattern="[0-9]+(\.[0-9]{1,2})?" /></label>
    <label>Tip<input name="tip" inputMode="decimal" defaultValue="0" pattern="[0-9]+(\.[0-9]{1,2})?" /></label>
    {mode !== 'ITEMIZED' ? <fieldset><legend>{mode === 'EQUAL' ? 'Travelers included' : mode === 'PERCENTAGE' ? 'Percentage per traveler' : 'Subtotal share per traveler'}</legend>
      {data.members.map(member => mode === 'EQUAL' ? <label className="check-row" key={member.id}><input name="participants" type="checkbox" value={member.id} defaultChecked />{member.displayName}</label> : <label key={member.id}>{member.displayName}<input name={`share-${member.id}`} aria-label={`${member.displayName} share`} inputMode="decimal" defaultValue="0" pattern="[0-9]+(\.[0-9]{1,2})?" /></label>)}
    </fieldset> : <fieldset><legend>Receipt items</legend>{items.map(id => <div key={id} className="react-line">
      <label>Item name<input name={`item-${id}-name`} required maxLength={160} /></label>
      <label>Item amount<input name={`item-${id}-amount`} inputMode="decimal" pattern="[0-9]+(\.[0-9]{1,2})?" required /></label>
      {data.members.map(member => <label className="check-row" key={member.id}><input name={`item-${id}-members`} type="checkbox" value={member.id} defaultChecked />{member.displayName}</label>)}
      <button className="button text" type="button" onClick={() => setItems(items.filter(value => value !== id))}>Remove item</button>
    </div>)}<button type="button" className="button secondary" onClick={() => setItems([...items, crypto.randomUUID()])}>Add item</button></fieldset>}
  </Form>;
}

function ActivityForm({activity, date, save}: {activity?: Activity; date: string; save: (payload: unknown) => Promise<void>}) {
  return <Form id="activity-form" save={async form => {
    const values = fields(form);
    if (values.bookingUrl && !safeBookingUrl(values.bookingUrl)) throw new Error('Booking URL must use http or https.');
    await save({...values, startTime: values.startTime || null, bookingUrl: values.bookingUrl || null});
  }}>
    <label>Plan title<input name="title" defaultValue={activity?.title} required maxLength={160} /></label>
    <label>Date<input name="date" type="date" required defaultValue={activity?.date || date} /></label>
    <label>Time<input name="startTime" type="time" defaultValue={activity?.startTime?.slice(0,5)} /></label>
    <label>Type<select name="type" defaultValue={activity?.type || 'ACTIVITY'}>{['ACTIVITY','FOOD','LODGING','TRANSIT'].map(value => <option key={value}>{value}</option>)}</select></label>
    <label>Status<select name="status" defaultValue={activity?.status || 'PROPOSED'}>{['PROPOSED','CONFIRMED','CANCELLED'].map(value => <option key={value}>{value}</option>)}</select></label>
    <label>Place<input name="place" maxLength={240} defaultValue={activity?.place} /></label>
    <label>Reservation reference<input name="reservationReference" maxLength={160} defaultValue={activity?.reservationReference} /></label>
    <label>Booking URL<input name="bookingUrl" type="url" maxLength={500} placeholder="https://…" defaultValue={activity?.bookingUrl} /></label>
    <label>Notes<textarea name="notes" maxLength={2000} defaultValue={activity?.notes} /></label>
  </Form>;
}

type Dialog = 'trip' | 'join' | 'member' | 'expense' | 'activity' | null;

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [register, setRegister] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [trips, setTrips] = useState<Trip[]>([]);
  const [data, setData] = useState<TripData | null>(null);
  const [dialog, setDialog] = useState<Dialog>(null);
  const [editing, setEditing] = useState<Activity>();
  const version = useRef(0);
  const money = (cents: number) => new Intl.NumberFormat('en-US', {style:'currency', currency: data?.trip.currency || 'USD'}).format(cents / 10 ** currencyPrecision(data?.trip.currency || 'USD'));

  async function loadTrip(trip: Trip) {
    const request = ++version.current;
    const base = `/api/trips/${trip.id}`;
    const [members, activities, expenses, ledger, settlement] = await Promise.all([
      api<Member[]>(`${base}/members`), api<Activity[]>(`${base}/activities`), api<Expense[]>(`${base}/expenses`), api<{balances: Balance[]}>(`${base}/balances`), api<Settlement>(`${base}/settlements?strategy=OPTIMAL`),
    ]);
    if (request !== version.current) return;
    setData({trip, members, activities, expenses, balances: ledger.balances, settlement});
    localStorage.setItem('detour.trip', trip.id);
  }

  async function loadTrips(preferred?: string) {
    const available = await api<Trip[]>('/api/trips'); setTrips(available);
    const selected = available.find(trip => trip.id === (preferred || localStorage.getItem('detour.trip'))) || available[0];
    if (selected) await loadTrip(selected); else setData(null);
  }

  useEffect(() => {
    const expired = () => {++version.current; setUser(null); setData(null); setDialog(null); setRegister(false); setError('Your session expired. Sign in again.');};
    window.addEventListener('detour:session-expired', expired);
    void (async () => {
      try {await refreshCsrf(); const session = await api<Session>('/api/auth/me');
        if (session.authenticated && session.user) {setUser(session.user); await loadTrips();}
      } catch (error) {setError(message(error));} finally {setLoading(false);}
    })();
    return () => window.removeEventListener('detour:session-expired', expired);
  }, []);

  useEffect(() => {
    if (user && new URLSearchParams(location.search).get('invite')) setDialog('join');
  }, [user]);

  async function act(task: () => Promise<void>, success?: string) {
    if (busy) return; setBusy(true); setError(''); setNotice('');
    try {await task(); if (success) setNotice(success);} catch (error) {setError(message(error));} finally {setBusy(false);}
  }

  async function mutate(path: string, method: string, payload?: unknown) {
    await api(path, method, payload); await loadTrips(data?.trip.id); setDialog(null);
  }

  async function sampleTrip() {
    const trip = await api<Trip>('/api/trips', 'POST', {name: 'Seoul after dark', destination:'Seoul, South Korea', startDate:today(), endDate:today(), currency:'USD'});
    const [organizer] = await api<Member[]>(`/api/trips/${trip.id}/members`);
    const people = [organizer];
    for (const name of ['Blair','Casey','Devon']) people.push(await api<Member>(`/api/trips/${trip.id}/members`, 'POST', {displayName:name, email:`${name.toLowerCase()}.${crypto.randomUUID()}@example.com`}));
    for (const [title,time,type] of [['Bukchon photo walk','10:00','ACTIVITY'],['Dinner at Jaha Son Mandu','19:00','FOOD']]) {
      await api(`/api/trips/${trip.id}/activities`, 'POST', {title, date:today(), startTime:time, type, status:'CONFIRMED', reservationReference:'SAMPLE-BOOKING', place:'Seoul'});
    }
    await api(`/api/trips/${trip.id}/expenses`, 'POST', {description:'Sample dinner', paidByMemberId:organizer.id, category:'FOOD', splitMode:'EXACT', occurredOn:today(), subtotalCents:9000, taxCents:900, tipCents:1800, allocations:people.map((person,index) => ({memberId:person.id, value:[2000,3000,2500,1500][index]}))});
    await loadTrips(trip.id);
  }

  async function payment(transfer: Transfer) {
    await mutate(`/api/trips/${data!.trip.id}/reimbursements`, 'POST', {...transfer, note:'Recorded from settlement suggestion', paidOn:today()});
  }

  if (loading) return <main className="loading-state" aria-live="polite"><Brand /><h1>Loading your trips…</h1></main>;

  if (!user) return <section id="auth-screen" className="auth-screen" aria-labelledby="auth-title">
    <div className="auth-story"><Brand /><div className="auth-story-copy"><h1>Trip planning</h1></div></div>
    <div className="auth-entry"><form id="auth-form" className="auth-card" onSubmit={event => {
      event.preventDefault(); const values = fields(event.currentTarget);
      void act(async () => {await refreshCsrf(); const session = await api<Session>(`/api/auth/${register ? 'register' : 'login'}`, 'POST', values); await refreshCsrf(); if (!session.user) throw new Error('Sign-in failed'); setUser(session.user); await loadTrips();});
    }}><h2 id="auth-title">{register ? 'Create an account' : 'Sign in'}</h2>
      {register && <label>Your name<input name="displayName" required maxLength={100} autoComplete="name" /></label>}
      <label>Email<input name="email" type="email" maxLength={320} autoComplete="email" required /></label>
      <label>Password<input name="password" type="password" minLength={8} maxLength={72} autoComplete={register ? 'new-password' : 'current-password'} required /></label>
      {error && <p id="auth-error" className="react-error" role="alert">{error}</p>}
      <button id="auth-submit" className="button primary" disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}</button>
      <p className="auth-switch">{register ? 'Already have an account? ' : 'New to Detour? '}<button id="auth-toggle" type="button" onClick={() => {setRegister(!register); setError('');}}>{register ? 'Sign in' : 'Create an account'}</button></p>
    </form></div>
  </section>;

  const base = data ? `/api/trips/${data.trip.id}` : '';
  return <div className="app-shell react-shell">
    <a className="skip-link" href="#overview">Skip to trip overview</a>
    <aside className="sidebar"><Brand />
      <div className="trip-switcher"><label htmlFor="trip-select">Current trip</label><select id="trip-select" value={data?.trip.id || ''} disabled={busy} onChange={event => {const trip = trips.find(trip => trip.id === event.target.value); if (trip) void act(() => loadTrip(trip));}}>{!trips.length && <option value="">No trips yet</option>}{trips.map(trip => <option key={trip.id} value={trip.id}>{trip.name}</option>)}</select></div>
      <nav className="nav-list" aria-label="Trip sections">{['overview','itinerary','expenses','settle'].map(section => <a className="nav-item" href={`#${section}`} key={section}>{label(section === 'settle' ? 'settle up' : section)}</a>)}</nav>
      <div className="react-actions"><button className="button secondary" onClick={() => setDialog('trip')}>New trip</button><button className="button secondary" onClick={() => setDialog('join')}>Join trip</button>{data && <button className="button secondary" onClick={() => setDialog('member')}>Add traveler</button>}</div>
      {data && <div className="people-panel"><div className="section-label"><span>Travelers</span><button className="icon-button" onClick={() => setDialog('member')} aria-label="Add traveler">+</button></div><div id="sidebar-members" className="member-stack">{data.members.map(member => <div className="member-row" key={member.id}><Avatar name={member.displayName} /><span>{member.displayName}{member.role === 'ORGANIZER' ? ' · host' : ''}</span></div>)}</div></div>}
      <div className="account-panel"><Avatar name={user.displayName} /><div><strong>{user.displayName}</strong><small>{user.email}</small></div><button id="logout-button" disabled={busy} onClick={() => void act(async () => {await api('/api/auth/logout','POST'); ++version.current; setUser(null); setData(null); setTrips([]); setRegister(false); await refreshCsrf();})}>Sign out</button></div>
      <div className="sidebar-footer"><a href="/docs" target="_blank" rel="noreferrer">API docs ↗</a><span>React · TypeScript</span></div>
    </aside>
    <main id="overview">
      {error && <div className="react-error" role="alert">{error}<button className="button text" onClick={() => void act(() => loadTrips(data?.trip.id))}>Retry</button></div>}
      {notice && <p className="react-message" role="status">{notice}</p>}
      {!data ? <section id="empty-state" className="empty-state"><h1>Your trips</h1><p>Create a trip or open a sample trip.</p><div className="empty-actions"><button className="button primary" onClick={() => setDialog('trip')}>Create a trip</button><button id="load-demo" className="button secondary" disabled={busy} onClick={() => void act(sampleTrip)}>{busy ? 'Loading…' : 'Open sample trip'}</button></div></section> : <div id="dashboard" className="dashboard" aria-busy={busy}>
        <section className="trip-hero"><div><p className="eyebrow">Trip overview</p><h1 id="trip-title">{data.trip.name}</h1><p className="trip-meta">{data.trip.destination} · {data.trip.startDate} – {data.trip.endDate} · {data.members.length} travelers</p></div><div className="hero-actions"><button className="button secondary" onClick={() => void act(async () => {await navigator.clipboard.writeText(`${location.origin}/?invite=${data.trip.inviteCode}`);}, `Invite copied. Code: ${data.trip.inviteCode}`)}>Copy invite</button><button className="button primary" onClick={() => setDialog('expense')}>+ Add expense</button></div></section>
        <section className="stat-grid" aria-label="Trip summary"><article className="stat-card warm"><span>Group spend</span><strong id="total-spend">{money(data.expenses.reduce((sum, expense) => sum + expense.totalCents, 0))}</strong><small id="expense-count">{data.expenses.length} expenses logged</small></article><article className="stat-card green"><span>Plans saved</span><strong id="plan-count">{data.activities.length}</strong><small>{data.activities.filter(activity => activity.status === 'CONFIRMED').length} confirmed</small></article><article className="stat-card"><span>Settle up</span><strong id="transfer-count">{data.settlement.transfers.length}</strong><small id="strategy-label">{data.settlement.fellBackFromOptimal ? 'Greedy fallback' : 'Optimal'} payments</small></article></section>
        <section id="itinerary" className="react-panel"><div className="panel-header"><h2>Itinerary & reservations</h2><button className="button secondary" onClick={() => {setEditing(undefined); setDialog('activity');}}>Add plan</button></div><div id="itinerary-list">
          {!data.activities.length && <p className="blank-list">No plans yet.</p>}
          {[...data.activities].sort((a,b) => a.date.localeCompare(b.date) || (a.startTime || '').localeCompare(b.startTime || '')).map(activity => <article className="activity-row" key={activity.id}><span className="activity-time">{activity.date}<br/>{activity.startTime?.slice(0,5) || 'Any time'}</span><div className="activity-copy"><strong>{activity.title}</strong><span>{activity.place} · {label(activity.status)}</span>{activity.reservationReference && <span className="reservation">Booked · {activity.reservationReference}</span>}{safeBookingUrl(activity.bookingUrl) && <a className="react-booking" href={safeBookingUrl(activity.bookingUrl)} target="_blank" rel="noopener noreferrer">Open booking ↗</a>}{activity.notes && <span>{activity.notes}</span>}</div><div className="react-actions"><button className={`vote-button ${activity.currentUserVote === 1 ? 'active' : ''}`} aria-pressed={activity.currentUserVote === 1} aria-label={`Upvote ${activity.title}`} disabled={busy} onClick={() => void act(() => mutate(`${base}/activities/${activity.id}/votes`,'POST',{value: activity.currentUserVote === 1 ? 0 : 1}))}>▲ {activity.voteScore}</button><button className="button text" onClick={() => {setEditing(activity); setDialog('activity');}}>Edit</button><button className="button text" disabled={busy} onClick={() => {if (confirm(`Remove “${activity.title}” from this trip?`)) void act(() => mutate(`${base}/activities/${activity.id}`, 'DELETE'));}}>Remove</button></div></article>)}
        </div></section>
        <section id="expenses" className="react-panel"><div className="panel-header"><h2>Shared expenses</h2><button className="button secondary" onClick={() => setDialog('expense')}>Add expense</button></div><div id="expense-list">{!data.expenses.length && <p className="blank-list">No expenses yet.</p>}{data.expenses.map(expense => <article className="expense-row" key={expense.id}><span className="expense-icon">◇</span><div className="expense-copy"><strong>{expense.description}</strong><span>{expense.occurredOn} · {label(expense.splitMode)} split</span></div><div className="expense-payer">Paid by <strong>{data.members.find(member => member.id === expense.paidByMemberId)?.displayName}</strong><br/>{expense.shares.length} shares</div><div className="expense-total">{money(expense.totalCents)}</div></article>)}</div></section>
        <section id="settle" className="react-panel"><div className="panel-header"><h2>Balances & settle up</h2></div><div id="balance-list">{data.balances.map(balance => <div className="balance-row" key={balance.memberId}><Avatar name={balance.displayName}/><div className="balance-name">{balance.displayName}<span className="balance-label">{balance.amountCents > 0 ? 'gets back' : balance.amountCents < 0 ? 'owes' : 'settled'}</span></div><strong className={`balance-amount ${balance.amountCents > 0 ? 'positive' : 'negative'}`}>{money(balance.amountCents)}</strong></div>)}</div><div id="settlement-list">{data.settlement.transfers.map(transfer => <div className="transfer" key={`${transfer.fromMemberId}-${transfer.toMemberId}`}><span>{transfer.fromName}</span><strong>{money(transfer.amountCents)} →</strong><span>{transfer.toName}</span><button className="mark-paid" disabled={busy} onClick={() => void act(() => payment(transfer), 'Payment recorded.')}>Paid</button></div>)}{!data.settlement.transfers.length && <p className="blank-list">Everyone is settled.</p>}</div><p className="react-status">Exact minimum-payment search for up to 12 nonzero balances; larger groups use a greedy fallback. Recording a payment updates the ledger—it does not move money.</p></section>
      </div>}
    </main>
    {dialog && <Modal title={{trip:'Create a trip',join:'Join a trip',member:'Add a traveler',expense:'Add an expense',activity:editing ? 'Edit plan' : 'Add a plan'}[dialog]} close={() => setDialog(null)}>
      {dialog === 'trip' && <Form id="trip-form" save={async form => {const trip = await api<Trip>('/api/trips','POST',fields(form)); await loadTrips(trip.id); setDialog(null);}}><label>Trip name<input name="name" required maxLength={120}/></label><label>Destination<input name="destination" required maxLength={160}/></label><label>Start date<input name="startDate" type="date" defaultValue={today()} required/></label><label>End date<input name="endDate" type="date" defaultValue={today()} required/></label><label>Currency<select name="currency">{['USD','EUR','GBP','KRW','VND','JPY','CAD','AUD'].map(currency => <option key={currency}>{currency}</option>)}</select></label></Form>}
      {dialog === 'join' && <Form id="join-form" save={async form => {const code = fields(form).inviteCode.trim().toUpperCase(); await api(`/api/trips/join/${encodeURIComponent(code)}`,'POST'); const available = await api<Trip[]>('/api/trips'); await loadTrips(available.find(trip => trip.inviteCode === code)?.id); setDialog(null); history.replaceState({},'',location.pathname);}}><label>Invite code<input name="inviteCode" defaultValue={new URLSearchParams(location.search).get('invite') || ''} required maxLength={32}/></label></Form>}
      {dialog === 'member' && data && <Form id="member-form" save={form => mutate(`${base}/members`,'POST',fields(form))}><label>Name<input name="displayName" required maxLength={100}/></label><label>Email<input name="email" type="email" required maxLength={320}/></label></Form>}
      {dialog === 'expense' && data && <ExpenseForm data={data} save={payload => mutate(`${base}/expenses`,'POST',payload)}/>}
      {dialog === 'activity' && data && <ActivityForm activity={editing} date={data.trip.startDate} save={payload => mutate(`${base}/activities${editing ? `/${editing.id}` : ''}`,editing ? 'PUT' : 'POST',payload)}/>}
    </Modal>}
  </div>;
}
