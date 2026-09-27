const state = {
  trips: [], trip: null, members: [], activities: [], expenses: [], balances: [], settlement: null,
  activeMemberId: null, user: null, csrf: null, authMode: "login"
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const icons = { FOOD: "♨", TRANSIT: "→", ACTIVITY: "◇", LODGING: "⌂", SHOPPING: "◌", OTHER: "·" };

document.addEventListener("DOMContentLoaded", async () => {
  bindEvents();
  seedDateInputs();
  await bootstrapAuth();
});

function bindEvents() {
  document.addEventListener("click", event => {
    const trigger = event.target.closest("[data-open]");
    if (!trigger) return;
    if (!["trip-dialog", "join-dialog"].includes(trigger.dataset.open) && !state.trip) {
      toast("Create or select a trip first", true);
      return;
    }
    populateMemberSelects();
    if (trigger.dataset.open === "expense-dialog") renderSplitEditor();
    document.getElementById(trigger.dataset.open).showModal();
  });

  $("#auth-form").addEventListener("submit", submitAuth);
  $("#auth-toggle").addEventListener("click", toggleAuthMode);
  $("#logout-button").addEventListener("click", logout);
  $("#trip-select").addEventListener("change", event => selectTrip(event.target.value));
  $("#trip-form").addEventListener("submit", createTrip);
  $("#member-form").addEventListener("submit", addMember);
  $("#join-form").addEventListener("submit", joinTrip);
  $("#activity-form").addEventListener("submit", addActivity);
  $("#expense-form").addEventListener("submit", addExpense);
  $("#split-mode").addEventListener("change", renderSplitEditor);
  $$("#expense-subtotal, #expense-form [name=tax], #expense-form [name=tip]")
    .forEach(input => input.addEventListener("input", updateSplitSummary));
  $("#load-demo").addEventListener("click", createDemo);
  $("#copy-invite").addEventListener("click", copyInvite);
  $("#mobile-menu").addEventListener("click", () => toggleSidebar());
  $("#sidebar-backdrop").addEventListener("click", () => toggleSidebar(false));
  $("#retry-load").addEventListener("click", () => loadTrips(state.trip?.id));
  $$(".nav-item").forEach(item => item.addEventListener("click", () => {
    $$(".nav-item").forEach(link => link.classList.remove("active"));
    item.classList.add("active");
    toggleSidebar(false);
  }));
}

async function api(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  const method = (options.method || "GET").toUpperCase();
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (options.body) headers["Content-Type"] = "application/json";
  if (!["GET", "HEAD", "OPTIONS"].includes(method) && state.csrf) {
    headers[state.csrf.headerName] = state.csrf.token;
  }
  try {
    const response = await fetch(path, {
      ...options,
      signal: options.signal || controller.signal,
      headers
    });
    if (!response.ok) {
      let problem = {};
      try { problem = await response.json(); } catch (_) { /* no response body */ }
      if ([401, 403].includes(response.status) && !path.startsWith("/api/auth/")) {
        state.csrf = null;
        try { await refreshCsrf(); } catch (_) { /* the sign-in form will report connectivity errors */ }
        showAuth("Your session expired. Sign in to keep planning.");
      }
      throw new Error(problem.message || `Request failed (${response.status})`);
    }
    if (response.status === 204) return null;
    return response.json();
  } catch (error) {
    if (error.name === "AbortError") throw new Error("The server took too long to respond. Please try again.");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

async function bootstrapAuth() {
  try {
    await refreshCsrf();
    const session = await api("/api/auth/me");
    if (!session.authenticated) {
      showAuth();
      return;
    }
    state.user = session.user;
    showApp();
    await loadTrips();
    openPendingInvite();
  } catch (error) {
    showAuth("Detour could not connect to the server. Please try again.");
  }
}

async function refreshCsrf() {
  state.csrf = await api("/api/auth/csrf");
}

async function submitAuth(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const fields = Object.fromEntries(new FormData(form));
  const submit = $("#auth-submit");
  submit.disabled = true;
  submit.textContent = state.authMode === "register" ? "Creating account…" : "Signing in…";
  $("#auth-error").classList.add("hidden");
  try {
    const session = await api(`/api/auth/${state.authMode}`, {
      method: "POST",
      body: JSON.stringify(fields)
    });
    state.user = session.user;
    form.reset();
    await refreshCsrf();
    showApp();
    await loadTrips();
    openPendingInvite();
  } catch (error) {
    $("#auth-error").textContent = error.message;
    $("#auth-error").classList.remove("hidden");
  } finally {
    submit.disabled = false;
    submit.textContent = state.authMode === "register" ? "Create account" : "Sign in";
  }
}

function toggleAuthMode() {
  state.authMode = state.authMode === "login" ? "register" : "login";
  const registering = state.authMode === "register";
  $("#auth-name-field").classList.toggle("hidden", !registering);
  $("#auth-name-field input").required = registering;
  $("#auth-form [name=password]").autocomplete = registering ? "new-password" : "current-password";
  $("#auth-kicker").textContent = registering ? "LET'S GET GOING" : "WELCOME BACK";
  $("#auth-title").textContent = registering ? "Create your account" : "Sign in to your trips";
  $("#auth-intro").textContent = registering
    ? "One quick account keeps every trip private and in sync."
    : "Your plans and balances are right where you left them.";
  $("#auth-submit").textContent = registering ? "Create account" : "Sign in";
  $("#auth-switch-copy").textContent = registering ? "Already have an account?" : "New to Detour?";
  $("#auth-toggle").textContent = registering ? "Sign in" : "Create an account";
  $("#auth-error").classList.add("hidden");
}

function showAuth(message) {
  state.user = null;
  $("#auth-screen").classList.remove("hidden");
  $(".app-shell").classList.add("hidden");
  if (message) {
    $("#auth-error").textContent = message;
    $("#auth-error").classList.remove("hidden");
  }
}

function showApp() {
  $("#auth-screen").classList.add("hidden");
  $(".app-shell").classList.remove("hidden");
  $("#account-name").textContent = state.user.displayName;
  $("#account-email").textContent = state.user.email;
  $("#account-avatar").textContent = initials(state.user.displayName);
  $("#join-account-name").textContent = state.user.displayName;
}

function openPendingInvite() {
  const inviteCode = new URLSearchParams(location.search).get("invite");
  if (!inviteCode || !state.user) return;
  $("#join-form [name=inviteCode]").value = inviteCode;
  $("#join-dialog").showModal();
}

async function logout() {
  const button = $("#logout-button");
  button.disabled = true;
  try {
    await api("/api/auth/logout", { method: "POST" });
    Object.assign(state, { trips: [], trip: null, members: [], activities: [], expenses: [], balances: [], settlement: null, activeMemberId: null, user: null, csrf: null });
    await refreshCsrf();
    showAuth();
  } catch (error) {
    toast(error.message, true);
  } finally {
    button.disabled = false;
  }
}

async function loadTrips(preferredId) {
  if (!state.trip) showLoading();
  try {
    state.trips = await api("/api/trips");
    renderTripSelect();
    if (!state.trips.length) {
      showEmpty();
      return;
    }
    const remembered = preferredId || localStorage.getItem("detour.trip");
    const selected = state.trips.find(trip => trip.id === remembered) || state.trips[0];
    $("#trip-select").value = selected.id;
    await selectTrip(selected.id);
  } catch (error) {
    showLoadError(error.message);
  }
}

async function selectTrip(tripId) {
  if (!tripId) return;
  setDashboardBusy(true);
  try {
    const trip = state.trips.find(value => value.id === tripId) || await api(`/api/trips/${tripId}`);
    const [members, activities, expenses, balancePayload, settlement] = await Promise.all([
      api(`/api/trips/${tripId}/members`),
      api(`/api/trips/${tripId}/activities`),
      api(`/api/trips/${tripId}/expenses`),
      api(`/api/trips/${tripId}/balances`),
      api(`/api/trips/${tripId}/settlements?strategy=OPTIMAL`)
    ]);
    Object.assign(state, { trip, members, activities, expenses, balances: balancePayload.balances, settlement });
    state.activeMemberId = members.find(value => value.email === state.user.email)?.id || null;
    localStorage.setItem("detour.trip", tripId);
    renderDashboard();
  } catch (error) {
    if (state.trip) toast(error.message, true);
    else showLoadError(error.message);
  } finally {
    setDashboardBusy(false);
  }
}

async function refreshLedger(tripId = state.trip?.id) {
  if (!tripId) return;
  const [balancePayload, settlement] = await Promise.all([
    api(`/api/trips/${tripId}/balances`),
    api(`/api/trips/${tripId}/settlements?strategy=OPTIMAL`)
  ]);
  if (state.trip?.id !== tripId) return;
  Object.assign(state, { balances: balancePayload.balances, settlement });
  renderBalances();
}

function showEmpty() {
  state.trip = null;
  $("#loading-state").classList.add("hidden");
  $("#empty-state").classList.remove("hidden");
  $("#dashboard").classList.add("hidden");
  $("#trip-select").innerHTML = '<option value="">No trips yet</option>';
  $("#sidebar-members").innerHTML = "";
}

function renderTripSelect() {
  $("#trip-select").innerHTML = state.trips
    .map(trip => `<option value="${trip.id}">${escapeHtml(trip.name)}</option>`).join("");
}

function renderDashboard() {
  $("#loading-state").classList.add("hidden");
  $("#empty-state").classList.add("hidden");
  $("#dashboard").classList.remove("hidden");
  const trip = state.trip;
  $("#trip-title").textContent = trip.name;
  $("#trip-meta").textContent = `${trip.destination}  ·  ${dateRange(trip.startDate, trip.endDate)}  ·  ${state.members.length} travelers`;
  $("#trip-kicker").textContent = new Date(`${trip.startDate}T12:00:00`) >= new Date() ? "UPCOMING ESCAPE" : "TRIP ARCHIVE";
  const total = state.expenses.reduce((sum, expense) => sum + expense.totalCents, 0);
  $("#total-spend").textContent = money(total);
  $("#expense-count").textContent = `${state.expenses.length} expense${state.expenses.length === 1 ? "" : "s"} logged`;
  $("#plan-count").textContent = state.activities.length;
  const confirmed = state.activities.filter(activity => activity.status === "CONFIRMED").length;
  $("#confirmed-count").textContent = `${confirmed} confirmed · ${state.activities.length - confirmed} proposed`;
  $("#transfer-count").textContent = state.settlement.transfers.length;
  $("#strategy-label").textContent = state.settlement.fellBackFromOptimal ? "Greedy fallback" : "Optimal";
  renderMembers();
  renderActivities();
  renderExpenses();
  renderBalances();
  populateMemberSelects();
}

function renderMembers() {
  $("#sidebar-members").innerHTML = state.members.map((memberValue, index) => `
    <div class="member-row">
      ${avatar(memberValue.displayName, index)}
      <span>${escapeHtml(memberValue.displayName)}${memberValue.role === "ORGANIZER" ? " · host" : ""}</span>
    </div>`).join("");
}

function renderActivities() {
  const target = $("#itinerary-list");
  if (!state.activities.length) {
    target.innerHTML = '<div class="blank-list">No plans yet. Save a restaurant, activity, stay, or ride.</div>';
    return;
  }
  const days = state.activities.reduce((groups, activityValue) => {
    (groups[activityValue.date] ||= []).push(activityValue);
    return groups;
  }, {});
  target.innerHTML = Object.entries(days).map(([date, activities]) => `
    <div class="day-group">
      <div class="day-label">${formatDay(date)}</div>
      ${activities.map(activityValue => {
        const upvoted = activityValue.currentUserVote === 1;
        return `
        <article class="activity-row">
          <span class="activity-time">${formatTime(activityValue.startTime)}</span>
          <span class="activity-icon ${activityValue.type.toLowerCase()}">${icons[activityValue.type]}</span>
          <div class="activity-copy">
            <strong>${escapeHtml(activityValue.title)}</strong>
            <span>${escapeHtml(activityValue.place || activityValue.status.toLowerCase())}</span>
            ${activityValue.reservationReference ? `<span class="reservation">Booked · ${escapeHtml(activityValue.reservationReference)}</span>` : ""}
          </div>
          <button class="vote-button${upvoted ? " active" : ""}" data-vote="${activityValue.id}" data-vote-value="${activityValue.currentUserVote}" title="${upvoted ? "Remove upvote" : "Upvote this plan"}" aria-label="${upvoted ? "Remove upvote from" : "Upvote"} ${escapeHtml(activityValue.title)}" aria-pressed="${upvoted}">▲ <span>${activityValue.voteScore}</span></button>
        </article>`;
      }).join("")}
    </div>`).join("");
  $$('[data-vote]', target).forEach(button => button.addEventListener("click", () => vote(button)));
}

function renderExpenses() {
  const target = $("#expense-list");
  if (!state.expenses.length) {
    target.innerHTML = '<div class="blank-list">No shared costs yet. Add the first one when someone picks up the tab.</div>';
    return;
  }
  target.innerHTML = state.expenses.map(expense => {
    const payer = member(expense.paidByMemberId);
    return `<article class="expense-row">
      <span class="expense-icon">${icons[expense.category]}</span>
      <div class="expense-copy"><strong>${escapeHtml(expense.description)}</strong><span>${formatDay(expense.occurredOn)} · ${titleCase(expense.splitMode)} split</span></div>
      <div class="expense-payer">Paid by <strong>${escapeHtml(payer?.displayName || "Unknown")}</strong><br>${expense.shares.length} shares</div>
      <div class="expense-total">${money(expense.totalCents)}<small>${expense.category}</small></div>
    </article>`;
  }).join("");
}

function renderBalances() {
  $("#transfer-count").textContent = state.settlement.transfers.length;
  $("#strategy-label").textContent = state.settlement.fellBackFromOptimal ? "Greedy fallback" : "Optimal";
  $("#balance-list").innerHTML = state.balances.map((balance, index) => {
    const positive = balance.amountCents > 0;
    const settled = balance.amountCents === 0;
    return `<div class="balance-row ${balance.memberId === state.activeMemberId ? "current" : ""}">
      ${avatar(balance.displayName, index)}
      <div class="balance-name">${escapeHtml(balance.displayName)}<span class="balance-label">${settled ? "settled" : positive ? "gets back" : "owes"}</span></div>
      <span class="balance-amount ${settled ? "" : positive ? "positive" : "negative"}">${positive ? "+" : balance.amountCents < 0 ? "−" : ""}${money(Math.abs(balance.amountCents))}</span>
    </div>`;
  }).join("");
  const transfers = state.settlement.transfers;
  $("#settlement-list").innerHTML = transfers.length ? transfers.map(value => `
    <div class="transfer"><span>${escapeHtml(value.fromName)}</span><strong>${money(value.amountCents)} →</strong><span>${escapeHtml(value.toName)}</span><button class="mark-paid" data-from="${value.fromMemberId}" data-to="${value.toMemberId}" data-amount="${value.amountCents}">Paid</button></div>`).join("")
    : '<div class="blank-list">Everyone is settled.</div>';
  $$(".mark-paid", $("#settlement-list"))
    .forEach(button => button.addEventListener("click", () => recordPayment(button)));
}

function populateMemberSelects() {
  $$(".member-select").forEach(select => {
    const current = select.value;
    select.innerHTML = state.members.map(memberValue => `<option value="${memberValue.id}">${escapeHtml(memberValue.displayName)}</option>`).join("");
    if (state.members.some(memberValue => memberValue.id === current)) select.value = current;
  });
}

function renderSplitEditor() {
  const editor = $("#split-editor");
  const mode = $("#split-mode").value;
  editor.innerHTML = "";
  if (mode === "ITEMIZED") {
    editor.innerHTML = `<div class="split-title"><span>Receipt items</span><button type="button" class="button text" id="add-line-item">+ Add item</button></div><div id="line-items"></div>`;
    $("#expense-subtotal").readOnly = true;
    addLineItem();
    $("#add-line-item").addEventListener("click", addLineItem);
    return;
  }

  $("#expense-subtotal").readOnly = false;
  const suffix = mode === "EXACT" ? state.trip?.currency || "amount" : mode === "PERCENTAGE" ? "%" : "included";
  editor.innerHTML = `<div class="split-title"><span>Who's included?</span><small>${suffix}</small></div><div class="allocation-grid">${state.members.map((memberValue, index) => `
    <label class="allocation-person">${avatar(memberValue.displayName, index)}<span>${escapeHtml(memberValue.displayName)}</span>
    ${mode === "EQUAL" ? `<input type="checkbox" data-member="${memberValue.id}" checked>` : `<input type="number" data-member="${memberValue.id}" min="0" step="0.01" value="0" aria-label="${escapeHtml(memberValue.displayName)} share">`}
    </label>`).join("")}</div>`;
}

function addLineItem() {
  const row = document.createElement("div");
  row.className = "line-item";
  row.innerHTML = `<div class="line-item-fields">
      <input data-item-name required maxlength="160" placeholder="Item name">
      <input data-item-amount required type="number" min="0.01" step="0.01" placeholder="0.00">
      <button type="button" class="remove-item" aria-label="Remove item">×</button>
    </div><div class="item-people">${state.members.map(memberValue => `<label><input type="checkbox" data-item-member="${memberValue.id}" checked>${escapeHtml(memberValue.displayName)}</label>`).join("")}</div>`;
  $("#line-items").append(row);
  $("[data-item-amount]", row).addEventListener("input", updateItemSubtotal);
  $(".remove-item", row).addEventListener("click", () => { row.remove(); updateItemSubtotal(); });
  updateItemSubtotal();
}

function updateItemSubtotal() {
  const cents = $$("[data-item-amount]").reduce((sum, input) => sum + moneyToCents(input.value || "0"), 0);
  $("#expense-subtotal").value = (cents / 100).toFixed(2);
  updateSplitSummary();
}

function updateSplitSummary() {
  const subtotal = moneyToCents($("#expense-subtotal").value || 0);
  const tax = moneyToCents($("#expense-form [name=tax]").value || 0);
  const tip = moneyToCents($("#expense-form [name=tip]").value || 0);
  $("#split-summary").textContent = `Receipt total ${money(subtotal + tax + tip)}`;
}

async function createTrip(event) {
  if (event.submitter?.value === "cancel") return;
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  try {
    const trip = await api("/api/trips", { method: "POST", body: JSON.stringify(data) });
    form.closest("dialog").close();
    form.reset();
    seedDateInputs();
    toast("Trip created — time to build the crew");
    await loadTrips(trip.id);
  } catch (error) { toast(error.message, true); }
}

async function addMember(event) {
  if (event.submitter?.value === "cancel") return;
  event.preventDefault();
  const form = event.currentTarget;
  try {
    const addedMember = await api(`/api/trips/${state.trip.id}/members`, {
      method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(form)))
    });
    state.members.push(addedMember);
    state.balances.push({ memberId: addedMember.id, displayName: addedMember.displayName, amountCents: 0 });
    form.closest("dialog").close();
    form.reset();
    renderDashboard();
    toast("Traveler added");
  } catch (error) { toast(error.message, true); }
}

async function joinTrip(event) {
  if (event.submitter?.value === "cancel") return;
  event.preventDefault();
  const form = event.currentTarget;
  const fields = Object.fromEntries(new FormData(form));
  const code = fields.inviteCode.trim().toUpperCase();
  try {
    await api(`/api/trips/join/${encodeURIComponent(code)}`, { method: "POST" });
    form.closest("dialog").close();
    form.reset();
    history.replaceState({}, "", location.pathname);
    const trips = await api("/api/trips");
    const joinedTrip = trips.find(trip => trip.inviteCode === code);
    state.trips = trips;
    renderTripSelect();
    await selectTrip(joinedTrip?.id || trips[0]?.id);
    toast("You're in — welcome to the trip");
  } catch (error) { toast(error.message, true); }
}

async function addActivity(event) {
  if (event.submitter?.value === "cancel") return;
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  if (!data.startTime) data.startTime = null;
  data.bookingUrl = null;
  try {
    const activity = await api(`/api/trips/${state.trip.id}/activities`, {
      method: "POST", body: JSON.stringify(data)
    });
    state.activities.push(activity);
    state.activities.sort((left, right) => left.date.localeCompare(right.date)
      || (left.startTime || "").localeCompare(right.startTime || ""));
    form.closest("dialog").close();
    form.reset();
    seedDateInputs();
    renderDashboard();
    toast("Plan added to the itinerary");
  } catch (error) { toast(error.message, true); }
}

async function addExpense(event) {
  if (event.submitter?.value === "cancel") return;
  event.preventDefault();
  const form = event.currentTarget;
  const fields = Object.fromEntries(new FormData(form));
  const mode = fields.splitMode;
  const payload = {
    description: fields.description,
    paidByMemberId: fields.paidByMemberId,
    category: fields.category,
    splitMode: mode,
    occurredOn: fields.occurredOn,
    subtotalCents: moneyToCents(fields.subtotal),
    taxCents: moneyToCents(fields.tax || "0"),
    tipCents: moneyToCents(fields.tip || "0"),
    allocations: [],
    items: []
  };
  try {
    if (mode === "EQUAL") {
      payload.allocations = $$('[data-member]:checked', form).map(input => ({ memberId: input.dataset.member, value: 0 }));
    } else if (mode === "EXACT") {
      payload.allocations = $$('[data-member]', form)
        .map(input => ({ memberId: input.dataset.member, value: moneyToCents(input.value) }))
        .filter(value => value.value > 0);
      const sum = payload.allocations.reduce((total, value) => total + value.value, 0);
      if (sum !== payload.subtotalCents) throw new Error("Exact shares must equal the base subtotal");
    } else if (mode === "PERCENTAGE") {
      payload.allocations = $$('[data-member]', form)
        .map(input => ({ memberId: input.dataset.member, value: Math.round(Number(input.value) * 100) }))
        .filter(value => value.value > 0);
      if (payload.allocations.reduce((total, value) => total + value.value, 0) !== 10000) {
        throw new Error("Percentages must total 100%");
      }
    } else {
      payload.items = $$(".line-item", form).map(row => ({
        name: $("[data-item-name]", row).value,
        amountCents: moneyToCents($("[data-item-amount]", row).value),
        participantIds: $$('[data-item-member]:checked', row).map(input => input.dataset.itemMember)
      }));
      if (payload.items.some(item => !item.participantIds.length)) {
        throw new Error("Every receipt item needs at least one traveler");
      }
    }
    if (!payload.allocations.length && mode !== "ITEMIZED") throw new Error("Choose at least one traveler");
    const tripId = state.trip.id;
    const expense = await api(`/api/trips/${tripId}/expenses`, { method: "POST", body: JSON.stringify(payload) });
    state.expenses.push(expense);
    state.expenses.sort((left, right) => right.occurredOn.localeCompare(left.occurredOn)
      || right.createdAt.localeCompare(left.createdAt));
    form.closest("dialog").close();
    form.reset();
    seedDateInputs();
    renderDashboard();
    try {
      await refreshLedger(tripId);
    } catch (_) {
      toast("Expense saved, but balances could not refresh. Reload to try again.", true);
      return;
    }
    toast("Expense split down to the cent");
  } catch (error) { toast(error.message, true); }
}

async function vote(button) {
  if (!state.activeMemberId) return;
  const tripId = state.trip.id;
  const activityId = button.dataset.vote;
  const previousVote = Number(button.dataset.voteValue);
  const previousScore = Number(button.querySelector("span").textContent);
  const value = previousVote === 1 ? 0 : 1;
  button.disabled = true;
  updateVote(activityId, button, value, previousScore + value - previousVote);
  try {
    const result = await api(`/api/trips/${tripId}/activities/${activityId}/votes`, {
      method: "POST", body: JSON.stringify({ value })
    });
    if (state.trip?.id === tripId) updateVote(activityId, button, result.currentUserVote, result.score);
  } catch (error) {
    if (state.trip?.id === tripId) updateVote(activityId, button, previousVote, previousScore);
    toast(error.message, true);
  } finally {
    button.disabled = false;
  }
}

function updateVote(activityId, button, value, score) {
  const activity = state.activities.find(candidate => candidate.id === activityId);
  if (activity) Object.assign(activity, { currentUserVote: value, voteScore: score });
  if (!button.isConnected) return;
  const upvoted = value === 1;
  button.dataset.voteValue = value;
  button.classList.toggle("active", upvoted);
  button.setAttribute("aria-pressed", String(upvoted));
  button.title = upvoted ? "Remove upvote" : "Upvote this plan";
  button.setAttribute("aria-label", `${upvoted ? "Remove upvote from" : "Upvote"} ${activity?.title || "this plan"}`);
  button.querySelector("span").textContent = score;
}

async function recordPayment(button) {
  button.disabled = true;
  try {
    const tripId = state.trip.id;
    await api(`/api/trips/${tripId}/reimbursements`, {
      method: "POST",
      body: JSON.stringify({
        fromMemberId: button.dataset.from,
        toMemberId: button.dataset.to,
        amountCents: Number(button.dataset.amount),
        note: "Recorded from settlement suggestion",
        paidOn: new Date().toISOString().slice(0, 10)
      })
    });
    await refreshLedger(tripId);
    toast("Payment recorded and balances refreshed");
  } catch (error) {
    button.disabled = false;
    toast(error.message, true);
  }
}

async function createDemo() {
  const button = $("#load-demo");
  button.disabled = true;
  button.textContent = "Building your trip…";
  try {
    const timestamp = Date.now();
    const trip = await api("/api/trips", { method: "POST", body: JSON.stringify({
      name: "Seoul after dark",
      destination: "Seoul, South Korea",
      startDate: "2026-10-02",
      endDate: "2026-10-06",
      currency: "USD"
    }) });
    const createdMembers = await api(`/api/trips/${trip.id}/members`);
    const a = createdMembers[0];
    const [b, c, d] = await Promise.all(["Blair", "Casey", "Devon"].map(name =>
      api(`/api/trips/${trip.id}/members`, {
        method: "POST",
        body: JSON.stringify({ displayName: name, email: `${name.toLowerCase()}.${timestamp}@example.com` })
      })));
    const people = [a, b, c, d];

    await Promise.all([
      activity(trip.id, "2026-10-02", "18:30", "FOOD", "Myeongdong night market", "Myeong-dong", "PROPOSED"),
      activity(trip.id, "2026-10-03", "10:00", "ACTIVITY", "Bukchon photo walk", "Bukchon Hanok Village", "CONFIRMED", "BK-2841"),
      activity(trip.id, "2026-10-03", "19:00", "FOOD", "Dinner at Jaha Son Mandu", "Buam-dong", "CONFIRMED", "DIN-1900"),
      activity(trip.id, "2026-10-04", "14:00", "ACTIVITY", "Leeum Museum", "Hannam-dong", "PROPOSED")
    ]);

    await api(`/api/trips/${trip.id}/expenses`, { method: "POST", body: JSON.stringify({
      description: "Dinner at Jaha Son Mandu",
      paidByMemberId: a.id,
      category: "FOOD",
      splitMode: "ITEMIZED",
      occurredOn: "2026-10-03",
      subtotalCents: 9000,
      taxCents: 900,
      tipCents: 1800,
      items: [
        { name: "Alex's order", amountCents: 2000, participantIds: [a.id] },
        { name: "Blair's order", amountCents: 3000, participantIds: [b.id] },
        { name: "Casey's order", amountCents: 2500, participantIds: [c.id] },
        { name: "Devon's order", amountCents: 1500, participantIds: [d.id] }
      ]
    }) });

    await Promise.all([
      simpleExpense(trip.id, "Airport van", b.id, "TRANSIT", "2026-10-02", 8000, people),
      simpleExpense(trip.id, "Korean barbecue", c.id, "FOOD", "2026-10-04", 16000, people),
      simpleExpense(trip.id, "Hanok stay deposit", d.id, "LODGING", "2026-10-02", 20000, people)
    ]);
    await loadTrips(trip.id);
    toast("Sample trip is ready — every number is live");
  } catch (error) {
    toast(error.message, true);
  } finally {
    button.disabled = false;
    button.textContent = "Explore sample trip";
  }
}

function activity(tripId, date, startTime, type, title, place, status, reservationReference = null) {
  return api(`/api/trips/${tripId}/activities`, { method: "POST", body: JSON.stringify({
    date, startTime, type, status, title, place, notes: null, reservationReference, bookingUrl: null
  }) });
}

function simpleExpense(tripId, description, paidByMemberId, category, occurredOn, subtotalCents, people) {
  return api(`/api/trips/${tripId}/expenses`, { method: "POST", body: JSON.stringify({
    description,
    paidByMemberId,
    category,
    splitMode: "EQUAL",
    occurredOn,
    subtotalCents,
    taxCents: 0,
    tipCents: 0,
    allocations: people.map(person => ({ memberId: person.id, value: 0 }))
  }) });
}

async function copyInvite() {
  const value = `${location.origin}/?invite=${state.trip.inviteCode}`;
  try {
    await navigator.clipboard.writeText(value);
    toast(`Invite ${state.trip.inviteCode} copied`);
  } catch (_) {
    toast(`Invite code: ${state.trip.inviteCode}`);
  }
}

function seedDateInputs() {
  const today = new Date().toISOString().slice(0, 10);
  $$('input[type="date"]').forEach(input => { if (!input.value) input.value = today; });
  const start = $("#trip-form [name=startDate]");
  const end = $("#trip-form [name=endDate]");
  if (start && end && start.value === end.value) {
    const later = new Date();
    later.setDate(later.getDate() + 3);
    end.value = later.toISOString().slice(0, 10);
  }
}

function member(id) {
  return state.members.find(memberValue => memberValue.id === id);
}

function showLoading() {
  $("#loading-title").textContent = "Packing the essentials…";
  $("#loading-message").textContent = "Loading trips, plans, and shared balances.";
  $("#retry-load").classList.add("hidden");
  $("#loading-state").classList.remove("hidden");
  $("#empty-state").classList.add("hidden");
  $("#dashboard").classList.add("hidden");
}

function showLoadError(message) {
  $("#loading-title").textContent = "We hit a roadblock.";
  $("#loading-message").textContent = message;
  $("#retry-load").classList.remove("hidden");
  $("#loading-state").classList.remove("hidden");
  $("#empty-state").classList.add("hidden");
  $("#dashboard").classList.add("hidden");
}

function setDashboardBusy(busy) {
  $("#dashboard").classList.toggle("is-loading", busy && !$("#dashboard").classList.contains("hidden"));
  $("#dashboard").setAttribute("aria-busy", String(busy));
  $("#trip-select").disabled = busy;
}

function toggleSidebar(force) {
  const sidebar = $(".sidebar");
  const open = typeof force === "boolean" ? force : !sidebar.classList.contains("open");
  sidebar.classList.toggle("open", open);
  $("#sidebar-backdrop").classList.toggle("open", open);
  $("#mobile-menu").setAttribute("aria-expanded", String(open));
}

function money(cents) {
  return new Intl.NumberFormat(undefined, {
    style: "currency", currency: state.trip?.currency || "USD", maximumFractionDigits: 2
  }).format(cents / 100);
}

function moneyToCents(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number) || number < 0) throw new Error("Amounts must be valid positive numbers");
  return Math.round((number + Number.EPSILON) * 100);
}

function dateRange(start, end) {
  const options = { month: "short", day: "numeric" };
  const first = new Date(`${start}T12:00:00`).toLocaleDateString(undefined, options);
  const last = new Date(`${end}T12:00:00`).toLocaleDateString(undefined, { ...options, year: "numeric" });
  return `${first} – ${last}`;
}

function formatDay(value) {
  return new Date(`${value}T12:00:00`).toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}

function formatTime(value) {
  if (!value) return "Anytime";
  const [hour, minute] = value.split(":");
  return new Date(2000, 0, 1, Number(hour), Number(minute))
    .toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

function titleCase(value) {
  return value.toLowerCase().replaceAll("_", " ").replace(/^./, letter => letter.toUpperCase());
}

function initials(name) {
  return name.split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase();
}

function avatar(name, index) {
  return `<span class="avatar avatar-${index % 6}">${escapeHtml(initials(name))}</span>`;
}

function escapeHtml(value) {
  const replacements = { "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" };
  return String(value).replace(/[&<>'"]/g, character => replacements[character]);
}

function toast(message, error = false) {
  const element = $("#toast");
  element.textContent = message;
  element.className = error ? "show error" : "show";
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => { element.className = ""; }, 3200);
}
