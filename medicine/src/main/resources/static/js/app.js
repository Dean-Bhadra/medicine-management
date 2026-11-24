const $ = (selector) => document.querySelector(selector);
const view = $("#view"),
  modal = $("dialog"),
  body = $("#dialog-body");
const pages = {
  overview: "Overview",
  inventory: "Inventory",
  checkout: "Point of sale",
  customers: "Customers",
  reports: "Sales reports",
};
const paths = [
  "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  "M3 7l9-4 9 4v11l-9 4-9-4z M3 7l9 4 9-4 M12 11v11",
  "M2 4h3l3 12h11l3-9H6 M9 20h1 M17 20h1",
  "M9 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M2 21a7 7 0 0 1 14 0 M17 5a4 4 0 0 1 0 7 M18 15a6 6 0 0 1 4 6",
  "M3 3v18h18 M7 16v-5 M12 16V7 M17 16v-8",
];
$("nav").innerHTML = Object.entries(pages)
  .map(
    ([key, name], i) =>
      `<a href="#${key}"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[i]}"/></svg>${name}</a>`,
  )
  .join("");
const escape = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const money = (value) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(
    value,
  );
function date(offset = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return [
    d.getFullYear(),
    String(d.getMonth() + 1).padStart(2, "0"),
    String(d.getDate()).padStart(2, "0"),
  ].join("-");
}
const range = (start, end) =>
  new URLSearchParams({
    start: start + "T00:00:00",
    end: end + "T23:59:59.999999",
  });
async function getCsrf() {
  const response = await fetch("/api/session");
  if (response.status === 401) {
    location.assign("/login");
    throw new Error("Sign in to continue.");
  }
  if (!response.ok)
    throw new Error("Session unavailable. Refresh and try again.");
  return response.json();
}
async function api(path, method = "GET", data) {
  const headers = data ? { "Content-Type": "application/json" } : {};
  if (method !== "GET") {
    const csrf = await getCsrf();
    headers[csrf.headerName] = csrf.token;
  }
  const response = await fetch("/api/" + path, {
    method,
    headers,
    body: data ? JSON.stringify(data) : undefined,
  });
  if (response.status === 401) {
    location.assign("/login");
    throw new Error("Sign in to continue.");
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(
      error.detail ||
        error.message ||
        "Request failed (" + response.status + ").",
    );
  }
  return response.headers.get("content-type")?.includes("json")
    ? response.json()
    : null;
}
const title = (name, sub, action = "") =>
  `<div class="title"><div><h1>${name}</h1><p class="sub">${sub}</p></div>${action}</div>`;
const empty = (msg) => `<p class="empty">${msg}</p>`;
const button = (action, label, id = "", cls = "link") =>
  `<button type="button" class="${cls}" data-action="${action}" data-id="${id}">${label}</button>`;
function table(headings, rows, none) {
  return rows.length
    ? `<div class="scroll"><table><thead><tr>${headings.map((h) => "<th>" + h + "</th>").join("")}</tr></thead><tbody>${rows.map((row) => "<tr>" + row.map((cell) => "<td>" + cell + "</td>").join("") + "</tr>").join("")}</tbody></table></div>`
    : empty(none);
}
let meds = [],
  sales = [],
  customers = [],
  cart = new Map(),
  page,
  request = 0,
  filter = "all",
  search = "",
  reportStart = date(-29),
  reportEnd = date();
const med = (id) => meds.find((m) => m.id === Number(id));
const low = (m) => (m.stockQuantity ?? 0) < 10;
const expired = (m) => !m.expiryDate || m.expiryDate < date();
const soon = (m) => m.expiryDate >= date() && m.expiryDate <= date(90);
function status(m) {
  if (expired(m)) return ["bad", m.expiryDate ? "Expired" : "Check expiry"];
  if (!m.stockQuantity) return ["bad", "Out of stock"];
  if (soon(m)) return ["warn", "Expiring soon"];
  if (low(m)) return ["warn", "Low stock"];
  return ["", "In stock"];
}
const pill = (m) => {
  const [cls, label] = status(m);
  return `<span class="pill ${cls}">${label}</span>`;
};
const stat = (label, value) =>
  `<div class="stat"><span>${label}</span><strong>${value}</strong></div>`;
function receipts(items) {
  return table(
    ["Receipt", "Customer", "Date", "Payment", "Total", ""],
    items.map((s) => [
      "#" + s.id,
      escape(s.customerName || "Walk-in"),
      escape(s.saleDate.slice(0, 16).replace("T", " ")),
      escape(s.paymentMethod || "—"),
      money(s.totalAmount),
      button("receipt", "View", s.id),
    ]),
    "No sales in this period.",
  );
}
function overview() {
  const days = Array.from({ length: 7 }, (_, i) => date(i - 6));
  const totals = days.map((d) =>
    sales
      .filter((s) => s.saleDate.startsWith(d))
      .reduce((v, s) => v + Number(s.totalAmount), 0),
  );
  const watch = meds
    .filter((m) => expired(m) || soon(m) || low(m))
    .sort((a, b) => (a.expiryDate || "").localeCompare(b.expiryDate || ""));
  const bars = days
    .map(
      (d, i) =>
        `<div class="day" title="${d} · ${money(totals[i])}"><span class="bar" style="height:${(totals[i] / Math.max(...totals, 1)) * 78}%"></span><small>${new Date(d + "T12:00").toLocaleDateString("en-US", { weekday: "short" })}</small></div>`,
    )
    .join("");
  view.innerHTML =
    title(
      "Pharmacy overview",
      "Sales and stock at a glance.",
      '<a class="button primary" href="#checkout">+ New sale</a>',
    ) +
    `<section class="stats">${stat("Today's sales", money(totals[6]))}${stat("Inventory batches", meds.length)}${stat("Low-stock batches", meds.filter(low).length)}${stat("Expiring in 90 days", meds.filter(soon).length)}</section>
    <section class="split"><article class="card"><div class="cardhead"><h2>Sales this week</h2><small>Last 7 days</small></div><strong class="receipt-total">${money(totals.reduce((a, b) => a + b, 0))}</strong><div class="chart" role="img" aria-label="${days.map((d, i) => d + " " + money(totals[i])).join(", ")}">${bars}</div></article>
    <article class="card"><div class="cardhead"><h2>Stock watch</h2>${button("watch", "View all")}</div>${
      watch
        .slice(0, 3)
        .map(
          (m) =>
            `<div class="row list"><div><strong>${escape(m.name)}</strong><small>Batch ${escape(m.batchNumber)} · ${m.stockQuantity ?? 0} units</small></div>${pill(m)}</div>`,
        )
        .join("") || empty("No batches need attention.")
    }</article></section>
    <section class="card"><div class="cardhead"><h2>Recent sales</h2><a class="button link" href="#reports">Sales history</a></div>${receipts(sales.slice(0, 5))}</section>`;
}
function inventory() {
  view.innerHTML =
    title(
      "Inventory",
      "Manage medicine batches, expiry dates and stock levels.",
      button("add", "+ Add batch", "", "primary"),
    ) +
    `<section class="card"><div class="toolbar"><label>Search inventory<input type="search" id="search" placeholder="Medicine, batch or supplier" value="${escape(search)}"></label><label>Stock filter<select id="filter">${[
      ["all", "All batches"],
      ["watch", "Needs attention"],
      ["low", "Low stock"],
      ["soon", "Expiring in 90 days"],
      ["expired", "Expired / missing expiry"],
    ]
      .map(
        ([v, n]) =>
          `<option value="${v}" ${filter === v ? "selected" : ""}>${n}</option>`,
      )
      .join("")}</select></label>${button("reorder", "Reorder list")}</div><div id="batches"></div></section>`;
  batchRows();
}
function batchRows() {
  const rows = meds
    .filter((m) =>
      [m.name, m.batchNumber, m.manufacturer, m.supplier]
        .join(" ")
        .toLowerCase()
        .includes(search.toLowerCase()),
    )
    .filter(
      (m) =>
        ({
          all: true,
          watch: expired(m) || soon(m) || low(m),
          low: low(m),
          soon: soon(m),
          expired: expired(m),
        })[filter],
    );
  $("#batches").innerHTML = table(
    ["Medicine", "Batch", "Expiry", "Stock", "Unit price", "Status", "Actions"],
    rows.map((m) => [
      `<strong>${escape(m.name)}</strong><small>${escape(m.manufacturer)}${m.requiresPrescription ? " · Rx" : ""}</small>`,
      escape(m.batchNumber),
      escape(m.expiryDate ?? "—"),
      m.stockQuantity ?? 0,
      money(m.unitPrice),
      pill(m),
      `<div class="actions">${button("stock", "Adjust", m.id)}${button("edit", "Edit", m.id)}</div>`,
    ]),
    meds.length
      ? "No matching batches."
      : "Add your first medicine batch to start receiving stock.",
  );
}
function openDialog(html) {
  body.innerHTML = html;
  if (!modal.open) modal.showModal();
}
function field(name, label, type = "text", value = "", attrs = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${escape(value)}" ${attrs}></label>`;
}
function batchForm(m = {}) {
  openDialog(`<h2>${m.id ? "Edit" : "Add"} batch</h2><form id="batch" data-id="${m.id || ""}"><div class="grid">
    ${field("name", "Medicine name", "text", m.name, 'required maxlength="255"')}${field("batchNumber", "Batch number", "text", m.batchNumber, 'required maxlength="255"')}
    ${field("manufacturer", "Manufacturer", "text", m.manufacturer, 'maxlength="255"')}${field("expiryDate", "Expiry date", "date", m.expiryDate, "required")}
    ${field("supplier", "Supplier", "text", m.supplier, 'maxlength="255"')}${field("targetStock", "Reorder target (units)", "number", m.targetStock ?? 30, 'min="0" required')}
    ${field("unitPrice", "Unit price ($)", "number", m.unitPrice, 'min="0" step=".01" required')}${field("stockQuantity", "Units on hand", "number", m.stockQuantity ?? 0, m.id ? "readonly" : 'min="0" required')}
    <label class="wide">Description<textarea name="description" maxlength="255">${escape(m.description)}</textarea></label></div>
    <p><input id="rx" name="requiresPrescription" type="checkbox" ${m.requiresPrescription ? "checked" : ""}><label for="rx" style="display:inline"> Prescription required</label></p>
    <div class="message" role="alert"></div><div class="actions"><button class="primary">Save batch</button>${m.id ? button("delete", "Delete batch", m.id) : ""}</div></form>`);
}
function checkout() {
  const available = meds
    .filter((m) => !expired(m) && m.stockQuantity > 0)
    .sort((a, b) => a.expiryDate.localeCompare(b.expiryDate));
  view.innerHTML =
    title(
      "Point of sale",
      "Choose a batch, confirm quantities and issue a receipt.",
    ) +
    `<div class="split"><section class="card"><h2>Sale items</h2><form id="line" class="toolbar"><label>Medicine batch<select name="medicine" required><option value="">Choose a batch</option>${available.map((m) => `<option value="${m.id}">${escape(m.name)} · ${escape(m.batchNumber)} · ${money(m.unitPrice)} (${m.stockQuantity} left)</option>`).join("")}</select></label>${field("quantity", "Units", "number", 1, 'min="1" required')}<button>Add to sale</button></form><div id="cart"></div></section>
    <section class="card"><h2>Checkout</h2><form id="payment"><div class="grid">${field("customerName", "Customer name", "text", "", 'placeholder="Walk-in" maxlength="255"')}${field("customerPhone", "Customer phone", "tel", "", 'maxlength="255"')}<label>Payment method<select name="paymentMethod"><option>Cash</option><option>Card</option><option>Digital payment</option></select></label>${field("prescriptionNumber", "Prescription reference", "text", "", 'maxlength="255"')}</div><div class="message" role="alert"></div><div class="actions"><button class="primary">Complete sale</button></div></form></section></div>`;
  cartRows();
}
function cartRows() {
  let total = 0,
    rx = false;
  const rows = Array.from(cart, ([id, q]) => {
    const m = med(id);
    total += Math.round(m.unitPrice * 100) * q;
    rx ||= m.requiresPrescription;
    return [
      escape(m.name) + "<small>" + escape(m.batchNumber) + "</small>",
      q,
      money(m.unitPrice * q),
      button("remove", "Remove", id),
    ];
  });
  $("#cart").innerHTML =
    table(["Batch", "Units", "Amount", ""], rows, "Your sale is empty.") +
    `<div class="row list"><strong>Total</strong><strong class="receipt-total">${money(total / 100)}</strong></div>`;
  $("#payment button").disabled = !cart.size;
  $("#payment [name=prescriptionNumber]").required = rx;
}
function showReceipt(s) {
  openDialog(
    `<h2>Receipt #${s.id}</h2><p>${escape(s.saleDate.slice(0, 16).replace("T", " "))} · ${escape(s.paymentMethod || "—")}</p><p>${escape(s.customerName || "Walk-in")}</p>${table(
      ["Medicine / batch", "Units", "Unit price", "Amount"],
      s.items.map((i) => [
        escape(i.medicineName || i.medicine.name) +
          "<small>" +
          escape(i.batchNumber || i.medicine.batchNumber) +
          "</small>",
        i.quantity,
        money(i.unitPrice),
        money(i.totalPrice),
      ]),
      "No items.",
    )}<div class="row list"><strong>Total</strong><strong>${money(s.totalAmount)}</strong></div>${button("print", "Print receipt")}</div>`,
  );
}
let history = [];
let pendingCheckout;
function customerRows(query = "") {
  const matches = customers.filter((c) =>
    [c.name, c.phone].join(" ").toLowerCase().includes(query.toLowerCase()),
  );
  $("#directory").innerHTML = table(
    ["Customer", "Phone", ""],
    matches.map((c) => [
      escape(c.name || "Customer"),
      escape(c.phone),
      button("history", "Purchase history", c.id),
    ]),
    "No matching customers.",
  );
}
function directory() {
  view.innerHTML =
    title("Customers", "Customers with a phone number are saved at checkout.") +
    `<section class="card"><div class="toolbar"><label>Find a customer<input type="search" id="customer-search" placeholder="Name or phone"></label></div><div id="directory"></div></section>`;
  customerRows();
}
function reports(report) {
  view.innerHTML =
    title(
      "Sales reports",
      "Review receipts and sales by medicine for a selected period.",
    ) +
    `<form class="toolbar" id="period">${field("start", "From", "date", reportStart, "required")}${field("end", "Through", "date", reportEnd, "required")}<button>Apply dates</button><a class="button" href="/api/sales/report/pdf?${range(reportStart, reportEnd)}">Download PDF</a></form><div class="stats">${stat("Sales revenue", money(report.totalSales))}${stat("Transactions", report.numberOfTransactions)}</div><div class="card"><div class="cardhead"><h2>Sales history</h2></div>${receipts(sales)}</div><br><div class="card"><h2>Top-selling batches</h2>${table(
      ["Medicine", "Quantity", "Revenue"],
      report.topSellingMedicines.map((m) => [
        escape(m.name),
        m.quantity,
        money(m.revenue),
      ]),
      "No sales in this period.",
    )}</div>`;
}
async function loadPage() {
  const token = ++request;
  page = location.hash.slice(1);
  if (!Object.hasOwn(pages, page)) page = "overview";
  const active = page;
  $("#breadcrumb").textContent = pages[page];
  $("time").textContent = new Date().toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  document
    .querySelectorAll("nav a")
    .forEach((a) =>
      a.setAttribute("aria-current", a.hash === "#" + page ? "page" : "false"),
    );
  $("#notice").textContent = "";
  view.innerHTML = '<p class="empty">Loading…</p>';
  try {
    const period =
      active === "reports"
        ? range(reportStart, reportEnd)
        : range(date(-6), date());
    const [stock, transactions, people, report] = await Promise.all([
      ["overview", "inventory", "checkout"].includes(active)
        ? api("medicines")
        : null,
      ["overview", "reports"].includes(active) ? api("sales?" + period) : null,
      active === "customers" ? api("customers") : null,
      active === "reports" ? api("reports/sales?" + period) : null,
    ]);
    if (token !== request) return;
    if (stock) meds = stock.sort((a, b) => a.name.localeCompare(b.name));
    if (transactions)
      sales = transactions.sort((a, b) => b.saleDate.localeCompare(a.saleDate));
    if (people) customers = people;
    for (const id of cart.keys()) if (stock && !med(id)) cart.delete(id);
    ({
      overview,
      inventory,
      checkout,
      customers: directory,
      reports: () => reports(report),
    })[page]();
  } catch (e) {
    if (token === request) {
      view.innerHTML = button("retry", "Try again");
      $("#notice").textContent = e.message;
    }
  }
}
function showError(e) {
  const target = modal.open
    ? body.querySelector(".message")
    : $("#payment .message");
  (target || $("#notice")).textContent = e.message;
}
document.addEventListener("input", (e) => {
  if (e.target.id === "search") {
    search = e.target.value;
    batchRows();
  }
  if (e.target.id === "customer-search") customerRows(e.target.value);
});
document.addEventListener("change", (e) => {
  if (e.target.id === "filter") {
    filter = e.target.value;
    batchRows();
  }
});
$(".close").onclick = () => modal.close();
document.addEventListener("click", async (e) => {
  const el = e.target.closest("[data-action]");
  if (!el) return;
  const id = Number(el.dataset.id);
  try {
    switch (el.dataset.action) {
      case "logout": {
        const csrf = await getCsrf();
        await fetch("/logout", {
          method: "POST",
          headers: { [csrf.headerName]: csrf.token },
        });
        location.assign("/login");
        break;
      }
      case "retry":
        await loadPage();
        break;
      case "watch":
        filter = "watch";
        search = "";
        location.hash = "inventory";
        break;
      case "add":
        batchForm();
        break;
      case "edit":
        batchForm(med(id));
        break;
      case "reorder": {
        const needsOrder = meds.filter(m=>low(m) && (m.targetStock ?? 30) > (m.stockQuantity ?? 0))
          .sort((a,b)=>(a.supplier||"").localeCompare(b.supplier||""));
        const rows = needsOrder.map(m=>[escape(m.supplier || "Unassigned"), escape(m.name) + "<small>" + escape(m.batchNumber) + "</small>", m.stockQuantity ?? 0, (m.targetStock ?? 30) - (m.stockQuantity ?? 0)]);
        openDialog("<h2>Reorder list</h2><p class='sub'>Batches below 10 units, grouped by supplier. Order quantities bring each batch up to its target.</p>" + table(["Supplier", "Medicine / batch", "On hand", "To order"], rows, "No batches need reordering.") + button("print", "Print list"));
        break;
      }
      case "stock":
        openDialog(
          `<h2>Adjust stock</h2><p>${escape(med(id).name)} · Batch ${escape(med(id).batchNumber)} · ${med(id).stockQuantity ?? 0} on hand</p><form id="stock" data-id="${id}">${field("quantity", "Units to add or remove", "number", "", 'required step="1"')}<div class="message"></div><div class="actions"><button class="primary">Apply adjustment</button></div></form>`,
        );
        break;
      case "delete":
        if (confirm("Delete this medicine batch?")) {
          await api("medicines/" + id, "DELETE");
          modal.close();
          await loadPage();
        }
        break;
      case "remove":
        cart.delete(id);
        cartRows();
        break;
      case "print":
        window.print();
        break;
      case "receipt":
        showReceipt([...sales, ...history].find((s) => s.id === id));
        break;
      case "history": {
        const result = await api("sales/customer/" + id);
        if (page !== "customers") break;
        history = result.sort((a, b) => b.saleDate.localeCompare(a.saleDate));
        openDialog(
          "<h2>Purchase history</h2><p>" +
            escape(customers.find((c) => c.id === id).name || "Customer") +
            "</p>" +
            receipts(history),
        );
        break;
      }
    }
  } catch (err) {
    showError(err);
  }
});
document.addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target,
    values = Object.fromEntries(new FormData(form)),
    id = form.dataset.id;
  const submit = e.submitter;
  if (submit) submit.disabled = true;
  try {
    switch (form.id) {
      case "batch":
        values.unitPrice = Number(values.unitPrice);
        values.stockQuantity = Number(values.stockQuantity);
        values.targetStock = Number(values.targetStock);
        values.requiresPrescription = !!values.requiresPrescription;
        if (id) values.version = med(id).version;
        await api(
          id ? "medicines/" + id : "medicines",
          id ? "PUT" : "POST",
          values,
        );
        modal.close();
        await loadPage();
        break;
      case "stock":
        await api(
          "medicines/" +
            id +
            "/stock?quantity=" +
            encodeURIComponent(values.quantity),
          "PUT",
        );
        modal.close();
        await loadPage();
        break;
      case "line": {
        const m = med(values.medicine),
          q = Number(values.quantity) + (cart.get(m.id) || 0);
        if (q > m.stockQuantity)
          throw new Error(
            "Only " + m.stockQuantity + " units available in this batch.",
          );
        cart.set(m.id, q);
        $("#payment .message").textContent = "";
        cartRows();
        break;
      }
      case "payment": {
        values.items = Array.from(cart, ([medicineId, quantity]) => ({
          medicineId,
          expectedUnitPrice: med(medicineId).unitPrice,
          quantity,
        }));
        const signature = JSON.stringify(values);
        if (pendingCheckout?.signature !== signature) {
          pendingCheckout = { signature, key: crypto.randomUUID() };
        }
        values.checkoutKey = pendingCheckout.key;
        const sale = await api("sales", "POST", values);
        pendingCheckout = null;
        cart.clear();
        await loadPage();
        showReceipt(sale);
        break;
      }
      case "period":
        if (values.start > values.end)
          throw new Error("Choose an end date on or after the start date.");
        reportStart = values.start;
        reportEnd = values.end;
        await loadPage();
        break;
    }
  } catch (err) {
    showError(err);
  } finally {
    if (submit && submit.isConnected) submit.disabled = false;
  }
});
window.addEventListener("hashchange", () => {
  modal.close();
  loadPage();
});
loadPage();
