const state = {
  trips: [], trip: null, members: [], activities: [], expenses: [], balances: [], settlement: null
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const icons = { FOOD: "♨", TRANSIT: "→", ACTIVITY: "◇", LODGING: "⌂", SHOPPING: "◌", OTHER: "·" };
const avatarColors = ["#f5d88a", "#efb9a6", "#b9d9ce", "#cfc5e7", "#bcd5ec", "#f2c8d8"];

document.addEventListener("DOMContentLoaded", async () => {
  bindEvents();
  seedDateInputs();
  await loadTrips();
});

function bindEvents() {
  document.addEventListener("click", event => {
    const trigger = event.target.closest("[data-open]");
    if (!trigger) return;
    if (trigger.dataset.open !== "trip-dialog" && !state.trip) {
      toast("Create or select a trip first", true);
      return;
    }
    populateMemberSelects();
    if (trigger.dataset.open === "expense-dialog") renderSplitEditor();
    document.getElementById(trigger.dataset.open).showModal();
  });

  $("#trip-select").addEventListener("change", event => selectTrip(event.target.value));
  $("#trip-form").addEventListener("submit", createTrip);
  $("#member-form").addEventListener("submit", addMember);
  $("#activity-form").addEventListener("submit", addActivity);
  $("#expense-form").addEventListener("submit", addExpense);
  $("#split-mode").addEventListener("change", renderSplitEditor);
  $$("#expense-subtotal, #expense-form [name=tax], #expense-form [name=tip]")
    .forEach(input => input.addEventListener("input", updateSplitSummary));
  $("#load-demo").addEventListener("click", createDemo);
  $("#copy-invite").addEventListener("click", copyInvite);
  $("#mobile-menu").addEventListener("click", () => $(".sidebar").classList.toggle("open"));
  $$(".nav-item").forEach(item => item.addEventListener("click", () => {
    $$(".nav-item").forEach(link => link.classList.remove("active"));
    item.classList.add("active");
    $(".sidebar").classList.remove("open");
  }));
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });
  if (!response.ok) {
    let problem = {};
    try { problem = await response.json(); } catch (_) { /* no response body */ }
    throw new Error(problem.message || `Request failed (${response.status})`);
  }
  if (response.status === 204) return null;
  return response.json();
}

async function loadTrips(preferredId) {
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
    showEmpty();
    toast(error.message, true);
  }
}

async function selectTrip(tripId) {
  if (!tripId) return;
  const trip = state.trips.find(value => value.id === tripId) || await api(`/api/trips/${tripId}`);
  try {
    const [members, activities, expenses, balancePayload, settlement] = await Promise.all([
      api(`/api/trips/${tripId}/members`),
      api(`/api/trips/${tripId}/activities`),
      api(`/api/trips/${tripId}/expenses`),
      api(`/api/trips/${tripId}/balances`),
      api(`/api/trips/${tripId}/settlements?strategy=OPTIMAL`)
    ]);
    Object.assign(state, { trip, members, activities, expenses, balances: balancePayload.balances, settlement });
    localStorage.setItem("detour.trip", tripId);
    renderDashboard();
  } catch (error) {
    toast(error.message, true);
  }
}

function showEmpty() {
  state.trip = null;
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
      ${activities.map(activityValue => `
        <article class="activity-row">
          <span class="activity-time">${formatTime(activityValue.startTime)}</span>
          <span class="activity-icon ${activityValue.type.toLowerCase()}">${icons[activityValue.type]}</span>
          <div class="activity-copy">
            <strong>${escapeHtml(activityValue.title)}</strong>
            <span>${escapeHtml(activityValue.place || activityValue.status.toLowerCase())}</span>
            ${activityValue.reservationReference ? `<span class="reservation">Booked · ${escapeHtml(activityValue.reservationReference)}</span>` : ""}
          </div>
          <button class="vote-button" data-vote="${activityValue.id}" title="Vote as ${escapeHtml(state.members[0]?.displayName || "organizer")}">▲ <span>${activityValue.voteScore}</span></button>
        </article>`).join("")}
    </div>`).join("");
  $$('[data-vote]', target).forEach(button => button.addEventListener("click", () => vote(button.dataset.vote)));
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
  $("#balance-list").innerHTML = state.balances.map((balance, index) => {
    const positive = balance.amountCents > 0;
    const settled = balance.amountCents === 0;
    return `<div class="balance-row">
      ${avatar(balance.displayName, index)}
      <div class="balance-name">${escapeHtml(balance.displayName)}<span class="balance-label">${settled ? "settled" : positive ? "gets back" : "owes"}</span></div>
      <span class="balance-amount ${settled ? "" : positive ? "positive" : "negative"}">${positive ? "+" : balance.amountCents < 0 ? "−" : ""}${money(Math.abs(balance.amountCents))}</span>
    </div>`;
  }).join("");
  const transfers = state.settlement.transfers;
  $("#settlement-list").innerHTML = transfers.length ? transfers.map(value => `
    <div class="transfer"><span>${escapeHtml(value.fromName)}</span><strong>${money(value.amountCents)} →</strong><span>${escapeHtml(value.toName)}</span></div>`).join("")
    : '<div class="blank-list">Everyone is settled.</div>';
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
    await api(`/api/trips/${state.trip.id}/members`, { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(form))) });
    form.closest("dialog").close();
    form.reset();
    await selectTrip(state.trip.id);
    toast("Traveler added");
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
    await api(`/api/trips/${state.trip.id}/activities`, { method: "POST", body: JSON.stringify(data) });
    form.closest("dialog").close();
    form.reset();
    seedDateInputs();
    await selectTrip(state.trip.id);
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
    await api(`/api/trips/${state.trip.id}/expenses`, { method: "POST", body: JSON.stringify(payload) });
    form.closest("dialog").close();
    form.reset();
    seedDateInputs();
    await selectTrip(state.trip.id);
    toast("Expense split down to the cent");
  } catch (error) { toast(error.message, true); }
}

async function vote(activityId) {
  if (!state.members.length) return;
  try {
    await api(`/api/trips/${state.trip.id}/activities/${activityId}/votes`, {
      method: "POST", body: JSON.stringify({ memberId: state.members[0].id, value: 1 })
    });
    await selectTrip(state.trip.id);
  } catch (error) { toast(error.message, true); }
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
      currency: "USD",
      organizerName: "Alex",
      organizerEmail: `alex.${timestamp}@example.com`
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
      activity(trip.id, a.id, "2026-10-02", "18:30", "FOOD", "Myeongdong night market", "Myeong-dong", "PROPOSED"),
      activity(trip.id, b.id, "2026-10-03", "10:00", "ACTIVITY", "Bukchon photo walk", "Bukchon Hanok Village", "CONFIRMED", "BK-2841"),
      activity(trip.id, c.id, "2026-10-03", "19:00", "FOOD", "Dinner at Jaha Son Mandu", "Buam-dong", "CONFIRMED", "DIN-1900"),
      activity(trip.id, d.id, "2026-10-04", "14:00", "ACTIVITY", "Leeum Museum", "Hannam-dong", "PROPOSED")
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

function activity(tripId, proposedByMemberId, date, startTime, type, title, place, status, reservationReference = null) {
  return api(`/api/trips/${tripId}/activities`, { method: "POST", body: JSON.stringify({
    proposedByMemberId, date, startTime, type, status, title, place, notes: null, reservationReference, bookingUrl: null
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
  return `<span class="avatar" style="--avatar:${avatarColors[index % avatarColors.length]}">${escapeHtml(initials(name))}</span>`;
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
