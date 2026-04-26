const API_BASE = window.ONS_API_BASE || "http://127.0.0.1:8000/api/v1";
const ADMIN_TOKEN_KEY = "ons_gold_admin_token";
const ADMIN_USER_KEY = "ons_gold_admin_user";

function readStoredValue(key) {
  try {
    return sessionStorage.getItem(key) || localStorage.getItem(key) || "";
  } catch (_) {
    return "";
  }
}

function readStoredUser() {
  const raw = readStoredValue(ADMIN_USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (_) {
    return null;
  }
}

function persistSession() {
  try {
    if (token) {
      sessionStorage.setItem(ADMIN_TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(ADMIN_TOKEN_KEY);
      localStorage.removeItem(ADMIN_TOKEN_KEY);
    }
    if (currentUser) {
      sessionStorage.setItem(ADMIN_USER_KEY, JSON.stringify(currentUser));
    } else {
      sessionStorage.removeItem(ADMIN_USER_KEY);
      localStorage.removeItem(ADMIN_USER_KEY);
    }
  } catch (_) {}
}

function clearPersistedSession() {
  try {
    sessionStorage.removeItem(ADMIN_TOKEN_KEY);
    localStorage.removeItem(ADMIN_TOKEN_KEY);
    sessionStorage.removeItem(ADMIN_USER_KEY);
    localStorage.removeItem(ADMIN_USER_KEY);
  } catch (_) {}
}

let token = readStoredValue(ADMIN_TOKEN_KEY);
let currentUser = readStoredUser();
let allProducts = [];
let allOrders = [];
let allCustomRequests = [];
let allAdminOrders = [];
let allAdmins = [];
let allActivityLogs = [];
let activeProductModalId = null;
let productCurrentPage = 1;
let productTotalItems = 0;
const PRODUCTS_PAGE_SIZE = 25;
let orderCurrentPage = 1;
const ORDERS_PAGE_SIZE = 25;
let activityCurrentPage = 1;
const ACTIVITY_PAGE_SIZE = 25;
let activeOrderModalRef = null;
let activeOrderModalKind = null;
let activeActivityLogId = null;
let editingAdminEmail = null;
let activeAdminModalEmail = null;
let bugImageBase64 = null;
let bugImageMime = null;

function showToast(message, type = "") {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = `show ${type}`.trim();
  setTimeout(() => { toast.className = ""; }, 3000);
}

function showMsg(node, message, type = "error") {
  if (!node) return;
  node.textContent = message;
  node.style.display = "block";
  node.style.color = type === "success" ? "var(--success)" : "var(--danger)";
  node.style.background = type === "success" ? "rgba(22,163,74,.07)" : "rgba(220,38,38,.07)";
  node.style.border = type === "success" ? "1.5px solid rgba(22,163,74,.2)" : "1.5px solid rgba(220,38,38,.2)";
}

function clearMsg(node) {
  if (!node) return;
  node.style.display = "none";
  node.textContent = "";
}

function setButtonLoading(button, label = "Please wait...") {
  if (!button) return () => {};
  const originalHtml = button.innerHTML;
  button.disabled = true;
  button.classList.add("is-loading");
  button.innerHTML = `<span class="btn-spinner" aria-hidden="true"></span><span>${label}</span>`;
  return (nextHtml = null) => {
    button.disabled = false;
    button.classList.remove("is-loading");
    button.innerHTML = nextHtml ?? originalHtml;
  };
}

function authHeaders() {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function api(path, method = "GET", body = null, extra = {}) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10000);
  try {
    const response = await fetch(API_BASE + path, {
      method,
      headers: {
        ...(body && !(body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
        ...authHeaders(),
        ...(extra.headers || {}),
      },
      body: body instanceof FormData ? body : body ? JSON.stringify(body) : null,
      signal: controller.signal,
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const detail = data?.detail;
      let message = "Request failed.";
      if (typeof detail === "string" && detail.trim()) message = detail;
      else if (typeof data?.message === "string" && data.message.trim()) message = data.message;
      else if (Array.isArray(detail) && detail.length) {
        const formatted = detail
          .map(item => String(item?.msg || item?.message || "").trim())
          .filter(Boolean);
        if (formatted.length) {
          message = formatted.length === 1
            ? formatted[0]
            : `${formatted.slice(0, -1).join(", ")} and ${formatted.slice(-1)}`;
        }
      }
      throw new Error(message);
    }
    return data;
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error("Unable to reach the admin portal server. Please try again.");
    }
    if (error instanceof Error) throw error;
    throw new Error("Unable to connect to the admin portal server.");
  } finally {
    clearTimeout(timeoutId);
  }
}

function stockLabel(value) {
  return String(value || "").replaceAll("_", " ").replace(/\b\w/g, c => c.toUpperCase());
}

function statusBadge(value) {
  const key = String(value || "new").toLowerCase();
  return `<span class="status-badge status-${key}">${stockLabel(key)}</span>`;
}

function truncateText(value, maxLength = 140) {
  const text = String(value || "").trim();
  if (text.length <= maxLength) return text;
  return `${text.slice(0, Math.max(0, maxLength - 3))}...`;
}

function parseApiDate(value) {
  if (!value) return null;
  if (value instanceof Date) return value;
  const raw = String(value).trim();
  if (!raw) return null;
  const hasTimezone = /(?:Z|[+-]\d{2}:\d{2})$/i.test(raw);
  const normalized = hasTimezone ? raw : `${raw}Z`;
  const parsed = new Date(normalized);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function normalizeAdminOrders(orders, customRequests) {
  const catalogOrders = (orders || []).map(order => ({
    ...order,
    order_kind: "catalog_order",
    order_ref: order.inquiry_id,
  }));
  const customOrders = (customRequests || []).map(order => ({
    ...order,
    order_kind: "custom_order",
    order_ref: order.request_id,
    status: order.status || "new",
  }));
  return [...catalogOrders, ...customOrders].sort((left, right) => {
    const leftTime = parseApiDate(left.created_at)?.getTime() || 0;
    const rightTime = parseApiDate(right.created_at)?.getTime() || 0;
    return rightTime - leftTime;
  });
}

function orderTypeBadge(orderKind) {
  const variant = orderKind === "custom_order" ? "custom" : "catalog";
  const label = orderKind === "custom_order" ? "Custom Order" : "Catalog Order";
  return `<span class="order-type-badge ${variant}">${label}</span>`;
}

function orderSourceBadge(source) {
  return `<span class="order-source-badge">${stockLabel(source)}</span>`;
}

function formatDateTime(value) {
  const parsed = parseApiDate(value);
  return parsed ? parsed.toLocaleString() : "-";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatActionLabel(action) {
  return stockLabel(String(action || "").toLowerCase());
}

function getActivityActionCategory(action) {
  const key = String(action || "").toLowerCase();
  if (key.includes("login")) return "login";
  if (key.includes("order") || key.includes("request")) return "order";
  if (key.includes("product")) return "product";
  if (key.includes("profile") || key.includes("password") || key.includes("email")) return "profile";
  if (key.includes("admin")) return "admin";
  if (key.includes("bug")) return "bug";
  return "generic";
}

function activityActionBadge(action) {
  const category = getActivityActionCategory(action);
  return `<span class="activity-action-badge activity-action-${category}">${formatActionLabel(action)}</span>`;
}

function formatActivityActor(log) {
  const name = log?.performed_by_name || "-";
  const email = log?.performed_by_email || "-";
  return `<div class="activity-performer"><strong>${escapeHtml(name)}</strong><small>${escapeHtml(email)}</small></div>`;
}

function stringifyActivityValue(value) {
  if (value === null || value === undefined || value === "") return "";
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) return "";
    return trimmed;
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch (_) {
    return String(value);
  }
}

function getFilteredActivityLogs() {
  const searchInput = document.getElementById("activity-search");
  const q = searchInput?.value.trim().toLowerCase() || "";
  if (!q) return allActivityLogs;
  return allActivityLogs.filter(log => {
    return [
      log.action,
      log.performed_by_name,
      log.performed_by_email,
      log.target,
      log.detail,
      stringifyActivityValue(log.old_value),
      stringifyActivityValue(log.new_value),
      stringifyActivityValue(log.extra),
    ].some(value => String(value || "").toLowerCase().includes(q));
  });
}

function updateActivityPagination(total = 0) {
  const infoEl = document.getElementById("activity-page-info");
  const prevBtn = document.getElementById("activity-prev-btn");
  const nextBtn = document.getElementById("activity-next-btn");
  if (!infoEl || !prevBtn || !nextBtn) return;
  const totalPages = Math.max(1, Math.ceil(total / ACTIVITY_PAGE_SIZE));
  if (activityCurrentPage > totalPages) activityCurrentPage = totalPages;
  infoEl.textContent = `Page ${activityCurrentPage} of ${totalPages}`;
  prevBtn.disabled = activityCurrentPage <= 1;
  nextBtn.disabled = activityCurrentPage >= totalPages || total === 0;
}

function renderActivityLogs(list) {
  const tbody = document.getElementById("activity-table-body");
  const count = document.getElementById("activity-log-count");
  if (count) {
    count.textContent = list.length === allActivityLogs.length
      ? `${allActivityLogs.length} Log${allActivityLogs.length === 1 ? "" : "s"}`
      : `${list.length} of ${allActivityLogs.length} Logs`;
  }
  if (!tbody) return;
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state">No activity logs found.</div></td></tr>`;
    updateActivityPagination(0);
    return;
  }
  const totalPages = Math.max(1, Math.ceil(list.length / ACTIVITY_PAGE_SIZE));
  if (activityCurrentPage > totalPages) activityCurrentPage = totalPages;
  const start = (activityCurrentPage - 1) * ACTIVITY_PAGE_SIZE;
  const slice = list.slice(start, start + ACTIVITY_PAGE_SIZE);
  tbody.innerHTML = slice.map(log => `
    <tr>
      <td>${activityActionBadge(log.action)}</td>
      <td>${formatActivityActor(log)}</td>
      <td><div class="activity-target">${escapeHtml(log.target || "-")}</div></td>
      <td><div class="activity-detail-copy">${escapeHtml(truncateText(log.detail || "-", 140))}</div></td>
      <td>${formatDateTime(log.created_at)}</td>
      <td><button class="btn-sm btn-outline activity-view-btn" data-activity-view="${log.id}">View</button></td>
    </tr>
  `).join("");
  tbody.querySelectorAll("[data-activity-view]").forEach(button => {
    button.addEventListener("click", () => openActivityLogModal(button.dataset.activityView));
  });
  updateActivityPagination(list.length);
}

function openActivityLogModal(activityId) {
  const log = allActivityLogs.find(item => item.id === activityId);
  const modal = document.getElementById("activity-detail-modal");
  if (!log || !modal) return;
  activeActivityLogId = activityId;
  document.getElementById("activity-modal-title").textContent = `${formatActionLabel(log.action)} Detail`;
  document.getElementById("activity-modal-action").innerHTML = activityActionBadge(log.action);
  document.getElementById("activity-modal-time").textContent = formatDateTime(log.created_at);
  document.getElementById("activity-modal-performed-by").innerHTML = formatActivityActor(log);
  document.getElementById("activity-modal-target").textContent = log.target || "-";
  document.getElementById("activity-modal-detail").textContent = log.detail || "-";

  [
    ["activity-modal-old-wrap", "activity-modal-old", log.old_value],
    ["activity-modal-new-wrap", "activity-modal-new", log.new_value],
    ["activity-modal-extra-wrap", "activity-modal-extra", log.extra],
  ].forEach(([wrapId, valueId, value]) => {
    const wrap = document.getElementById(wrapId);
    const node = document.getElementById(valueId);
    const formatted = stringifyActivityValue(value);
    if (formatted) {
      wrap.style.display = "block";
      node.textContent = formatted;
    } else {
      wrap.style.display = "none";
      node.textContent = "";
    }
  });

  modal.classList.add("open");
}

function closeActivityLogModal() {
  activeActivityLogId = null;
  document.getElementById("activity-detail-modal")?.classList.remove("open");
}

function setLogoutIcon() {
  const button = document.getElementById("logout-btn");
  if (!button) return;
  button.innerHTML = `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M15 3h-4a2 2 0 0 0-2 2v3h2V5h4v14h-4v-3H9v3a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2Z"></path>
      <path d="M10 12H3"></path>
      <path d="m6 9-3 3 3 3"></path>
    </svg>
  `;
}

function renderOrderDetails(order) {
  if (order.order_kind === "custom_order") {
    return `
      <div class="order-details">
        <div class="order-detail-line">
          <strong>${order.jewelry_type}</strong>
          <span>${order.city} | ${order.budget} | ${order.purity}</span>
        </div>
        <div class="order-detail-note">${truncateText(order.description, 160)}</div>
      </div>
    `;
  }
  const productLines = (order.products || []).map(product => `
    <div class="order-detail-line">
      <strong>${product.title}</strong>
      <span>${product.product_id}${product.quantity > 1 ? ` x${product.quantity}` : ""}</span>
    </div>
  `).join("");
  const notes = order.notes ? `<div class="order-detail-note">Note: ${truncateText(order.notes, 120)}</div>` : "";
  return `<div class="order-details">${productLines}${notes}</div>`;
}

function getFilteredOrders() {
  const searchInput = document.getElementById("orders-search");
  const q = searchInput?.value.trim().toLowerCase() || "";
  if (!q) return allAdminOrders;
  return allAdminOrders.filter(item => {
    const baseMatch = [
      item.order_ref,
      item.customer_name,
      item.phone,
      item.city,
      item.jewelry_type,
      item.budget,
      item.purity,
      item.description,
      item.notes,
      item.order_kind === "custom_order" ? "custom" : "catalog",
    ].some(value => String(value || "").toLowerCase().includes(q));
    const productMatch = (item.products || []).some(product =>
      [product.product_id, product.title].some(value => String(value || "").toLowerCase().includes(q))
    );
    return baseMatch || productMatch;
  });
}

function updateOrdersPagination(total = 0) {
  const infoEl = document.getElementById("orders-page-info");
  const prevBtn = document.getElementById("orders-prev-btn");
  const nextBtn = document.getElementById("orders-next-btn");
  if (!infoEl || !prevBtn || !nextBtn) return;
  const totalPages = Math.max(1, Math.ceil(total / ORDERS_PAGE_SIZE));
  if (orderCurrentPage > totalPages) orderCurrentPage = totalPages;
  infoEl.textContent = `Page ${orderCurrentPage} of ${totalPages}`;
  prevBtn.disabled = orderCurrentPage <= 1;
  nextBtn.disabled = orderCurrentPage >= totalPages || total === 0;
}

function renderOrderCommentList(order) {
  const list = document.getElementById("order-modal-comments");
  if (!list) return;
  const comments = order?.comments || [];
  list.innerHTML = comments.length
    ? comments.map(comment => `
      <div class="comment-item">
        <div class="comment-meta">${comment.added_by} · ${formatDateTime(comment.created_at)}</div>
        <div class="comment-text">${comment.comment}</div>
      </div>
    `).join("")
    : `<div style="font-size:13px;color:var(--muted);margin-bottom:8px;">No comments yet.</div>`;
}

function getCommentAuthorLabel(comment) {
  const explicitName = String(comment?.added_by_name || "").trim();
  if (explicitName) return explicitName;

  const legacyLabel = String(comment?.added_by || "").trim();
  if (legacyLabel && !/\S+@\S+\.\S+/.test(legacyLabel)) return legacyLabel;

  return "Admin";
}

function renderOrderCommentList(order) {
  const list = document.getElementById("order-modal-comments");
  if (!list) return;
  const comments = order?.comments || [];
  list.innerHTML = comments.length
    ? comments.map(comment => `
      <div class="comment-item">
        <div class="comment-meta">${escapeHtml(getCommentAuthorLabel(comment))} &middot; ${formatDateTime(comment.created_at)}</div>
        <div class="comment-text">${escapeHtml(comment.comment)}</div>
      </div>
    `).join("")
    : `<div style="font-size:13px;color:var(--muted);margin-bottom:8px;">No comments yet.</div>`;
}

function renderOrderImageGallery(order) {
  if (order.order_kind === "custom_order") {
    const urls = order.image_urls || [];
    if (!urls.length) return "";
    return `
      <div class="order-image-grid">
        ${urls.map((url, index) => `
          <a class="order-image-card" href="${url}" target="_blank" rel="noopener noreferrer">
            <img src="${url}" alt="Reference image ${index + 1}">
            <div class="order-image-caption">Image ${index + 1}</div>
          </a>
        `).join("")}
      </div>
    `;
  }
  const productImages = (order.products || [])
    .filter(product => product.image)
    .map((product, index) => `
      <a class="order-image-card" href="${product.image}" target="_blank" rel="noopener noreferrer">
        <img src="${product.image}" alt="${product.title}">
        <div class="order-image-caption">${product.title || `Product ${index + 1}`}</div>
      </a>
    `)
    .join("");
  return productImages ? `<div class="order-image-grid">${productImages}</div>` : "";
}

function openOrderDetailModal(orderRef, orderKind) {
  const order = allAdminOrders.find(item => item.order_ref === orderRef && item.order_kind === orderKind);
  const modal = document.getElementById("order-detail-modal");
  if (!order || !modal) return;
  activeOrderModalRef = orderRef;
  activeOrderModalKind = orderKind;
  document.getElementById("order-modal-title").textContent = `${orderKind === "custom_order" ? "Custom Order" : "Catalog Order"} Details`;
  document.getElementById("order-modal-id").textContent = order.order_ref;
  document.getElementById("order-modal-type").innerHTML = orderTypeBadge(order.order_kind);
  document.getElementById("order-modal-customer").innerHTML = `${order.customer_name}<br><span style="font-size:12px;color:var(--muted);">${order.phone}</span>`;
  document.getElementById("order-modal-created").textContent = formatDateTime(order.created_at);
  const statusSelect = document.getElementById("order-modal-status");
  statusSelect.innerHTML = ["new", "contacted", "quoted", "closed"]
    .map(status => `<option value="${status}" ${order.status === status ? "selected" : ""}>${stockLabel(status)}</option>`)
    .join("");
  statusSelect.onchange = () => updateAdminOrderStatus(activeOrderModalKind, activeOrderModalRef, statusSelect.value);
  document.getElementById("order-modal-details").innerHTML = renderOrderDetails(order);
  const imageWrap = document.getElementById("order-modal-images-wrap");
  const imageContainer = document.getElementById("order-modal-images");
  const imageGallery = renderOrderImageGallery(order);
  const imageLabel = imageWrap?.querySelector(".modal-label");
  if (imageGallery) {
    imageWrap.style.display = "block";
    if (imageLabel) imageLabel.textContent = order.order_kind === "custom_order" ? "Reference Images" : "Product Images";
    imageContainer.innerHTML = imageGallery;
  } else {
    imageWrap.style.display = "none";
    imageContainer.innerHTML = "-";
  }
  document.getElementById("order-comment-text").value = "";
  clearMsg(document.getElementById("order-comment-msg"));
  renderOrderCommentList(order);
  modal.classList.add("open");
}

function closeOrderDetailModal() {
  activeOrderModalRef = null;
  activeOrderModalKind = null;
  document.getElementById("order-comment-text").value = "";
  clearMsg(document.getElementById("order-comment-msg"));
  document.getElementById("order-detail-modal")?.classList.remove("open");
}

function roleTag(role) {
  const key = String(role || "admin").toLowerCase();
  const extra = key === "super_admin" ? " super" : "";
  return `<span class="admin-card-role-tag${extra}">${stockLabel(key)}</span>`;
}

function adminRoles(admin) {
  if (Array.isArray(admin?.roles) && admin.roles.length) return admin.roles;
  if (admin?.role === "super_admin") return ["super_admin", "admin"];
  return [admin?.role || "admin"];
}

function adminRoleTags(admin) {
  return adminRoles(admin).map(role => roleTag(role)).join(" ");
}

function ensureSelectHasOption(select, value) {
  if (!select || !value) return;
  const hasOption = Array.from(select.options).some(option => option.value === value);
  if (!hasOption) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = value;
    select.appendChild(option);
  }
}

function getPrimaryRoleLabel(user) {
  const roles = adminRoles(user);
  if (roles.includes("super_admin")) return "super admin";
  return String(roles[0] || "admin").replaceAll("_", " ");
}

function setUserUI() {
  const name = currentUser?.name || "Admin";
  const initial = name[0]?.toUpperCase() || "A";
  document.getElementById("sidebar-name").textContent = name;
  document.getElementById("sidebar-role").textContent = getPrimaryRoleLabel(currentUser);
  document.getElementById("sidebar-avatar").textContent = initial;
  document.getElementById("topbar-avatar").textContent = initial;
  document.getElementById("topbar-name").textContent = name;
  populateProfilePanel();
}

function isSuperAdmin() {
  const roles = adminRoles(currentUser);
  if (roles.includes("super_admin")) return true;
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const tokenRole = payload?.role;
    const tokenRoles = Array.isArray(payload?.roles) ? payload.roles : tokenRole ? [tokenRole] : [];
    return tokenRoles.includes("super_admin");
  } catch (_) {
    return false;
  }
}

function showDashboard() {
  setSidebarOpen(false);
  document.getElementById("login-screen").style.display = "none";
  document.getElementById("dashboard").style.display = "block";
  setUserUI();
}

function showLogin() {
  setSidebarOpen(false);
  document.getElementById("dashboard").style.display = "none";
  document.getElementById("login-screen").style.display = "flex";
}

function logout() {
  token = "";
  currentUser = null;
  clearPersistedSession();
  showLogin();
}

async function fetchPublicSummaryForLogin() {
  try {
    const summary = await fetch(API_BASE + "/dashboard/login-summary").then(res => res.ok ? res.json() : null);
    if (!summary) return;
    document.getElementById("lv-products").textContent = summary.total_products;
    document.getElementById("lv-orders").textContent = summary.total_orders;
    document.getElementById("lv-custom").textContent = summary.total_custom_requests;
  } catch (_) {}
}

async function login() {
  const email = document.getElementById("l-email").value.trim();
  const password = document.getElementById("l-password").value;
  const errorNode = document.getElementById("login-error");
  clearMsg(errorNode);
  if (!email || !password) {
    showMsg(errorNode, "Please enter your email and password.");
    return;
  }
  const btn = document.getElementById("login-btn");
  const stopLoading = setButtonLoading(btn, "Signing In...");
  try {
    const data = await api("/auth/login", "POST", { email, password });
    token = data.access_token;
    currentUser = data.user;
    persistSession();
    await fetchCurrentUser();
    showDashboard();
    await loadDashboardData();
    showToast("Login successful.", "success");
  } catch (error) {
    showMsg(errorNode, error.message);
  } finally {
    stopLoading();
  }
}

async function fetchCurrentUser() {
  const data = await api("/auth/me");
  currentUser = data;
  persistSession();
  setUserUI();
}

function updateProductsPagination(total = 0) {
  const infoEl = document.getElementById("products-page-info");
  const prevBtn = document.getElementById("products-prev-btn");
  const nextBtn = document.getElementById("products-next-btn");
  if (!infoEl || !prevBtn || !nextBtn) return;
  const totalPages = Math.max(1, Math.ceil(total / PRODUCTS_PAGE_SIZE));
  if (productCurrentPage > totalPages) productCurrentPage = totalPages;
  infoEl.textContent = `Page ${productCurrentPage} of ${totalPages}`;
  prevBtn.disabled = productCurrentPage <= 1;
  nextBtn.disabled = productCurrentPage >= totalPages || total === 0;
}

async function loadProductsPage(page = productCurrentPage) {
  const searchValue = document.getElementById("products-search")?.value.trim() || "";
  const query = new URLSearchParams({
    page: String(page),
    page_size: String(PRODUCTS_PAGE_SIZE),
  });
  if (searchValue) query.set("search", searchValue);

  const products = await api(`/products?${query.toString()}`);
  const totalPages = Math.max(1, Math.ceil((products.total || 0) / PRODUCTS_PAGE_SIZE));
  if (page > totalPages && products.total > 0) {
    productCurrentPage = totalPages;
    return loadProductsPage(totalPages);
  }

  productCurrentPage = products.page || page;
  productTotalItems = products.total || 0;
  allProducts = products.items || [];
  renderProducts(allProducts);
  updateProductsPagination(productTotalItems);
}

async function loadDashboardData() {
  const requests = [
    api("/dashboard/summary"),
    api("/admin/orders"),
    api("/admin/custom-requests"),
  ];
  if (isSuperAdmin()) {
    requests.push(api("/admin/all-admins"));
    requests.push(api("/admin/activity-logs"));
  }
  const [summary, orders, customRequests, admins, activityLogs] = await Promise.all(requests);
  allOrders = orders || [];
  allCustomRequests = customRequests || [];
  allAdminOrders = normalizeAdminOrders(allOrders, allCustomRequests);
  allAdmins = admins?.data || admins || [];
  allActivityLogs = activityLogs || [];
  renderOverview(summary, allAdminOrders);
  renderOrders(getFilteredOrders());
  renderAdmins(allAdmins);
  renderActivityLogs(getFilteredActivityLogs());
  if (activeOrderModalRef && activeOrderModalKind) {
    openOrderDetailModal(activeOrderModalRef, activeOrderModalKind);
  }
  if (activeActivityLogId) {
    openActivityLogModal(activeActivityLogId);
  }
  toggleAdminManagementVisibility();
  await loadProductsPage(productCurrentPage);
}

function renderOverview(summary, orders) {
  document.getElementById("overview-stats").innerHTML = `
    <div class="stat-card stat-blue"><div class="sc-label">Total Products</div><div class="sc-num">${summary.total_products}</div></div>
    <div class="stat-card stat-warn"><div class="sc-label">Catalog Orders</div><div class="sc-num">${summary.total_orders}</div></div>
    <div class="stat-card stat-green"><div class="sc-label">Custom Orders</div><div class="sc-num">${summary.total_custom_requests}</div></div>
    <div class="stat-card stat-red"><div class="sc-label">New Orders</div><div class="sc-num">${summary.new_orders}</div></div>
  `;
  const tbody = document.getElementById("overview-orders-body");
  if (!orders.length) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">No orders yet.</div></td></tr>`;
    return;
  }
  tbody.innerHTML = orders.slice(0, 5).map(order => `
    <tr>
      <td>${order.order_ref}</td>
      <td>${order.customer_name}<br><small>${order.phone}</small></td>
      <td>${orderTypeBadge(order.order_kind)}</td>
      <td>${statusBadge(order.status)}</td>
      <td>${formatDateTime(order.created_at)}</td>
    </tr>
  `).join("");
}

function renderProducts(list) {
  const tbody = document.getElementById("products-table-body");
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state">No products found.</div></td></tr>`;
    updateProductsPagination(productTotalItems);
    return;
  }
  tbody.innerHTML = list.map(product => `
    <tr>
      <td>${product.title}<br><small>${product.product_id}</small></td>
      <td>${product.category}</td>
      <td>${stockLabel(product.metal)}</td>
      <td>${product.purity}</td>
      <td>${product.weight} g</td>
      <td>${stockLabel(product.stock_status)}</td>
      <td>
        <button class="btn-sm btn-outline" data-edit="${product.product_id}">Edit</button>
      </td>
    </tr>
  `).join("");
  tbody.querySelectorAll("[data-edit]").forEach(btn => btn.addEventListener("click", () => openProductDetailModal(btn.dataset.edit)));
}

function renderOrders(list) {
  const tbody = document.getElementById("orders-table-body");
  if (!tbody) return;
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state">No orders found.</div></td></tr>`;
    updateOrdersPagination(0);
    return;
  }
  const totalPages = Math.max(1, Math.ceil(list.length / ORDERS_PAGE_SIZE));
  if (orderCurrentPage > totalPages) orderCurrentPage = totalPages;
  const start = (orderCurrentPage - 1) * ORDERS_PAGE_SIZE;
  const slice = list.slice(start, start + ORDERS_PAGE_SIZE);
  tbody.innerHTML = slice.map(order => `
    <tr>
      <td>${order.order_ref}</td>
      <td><div class="order-customer"><strong>${order.customer_name}</strong><small>${order.phone}</small></div></td>
      <td>${orderTypeBadge(order.order_kind)}</td>
      <td>
        <select class="order-status-select" data-order-status="${order.order_ref}" data-order-kind="${order.order_kind}">
          ${["new", "contacted", "quoted", "closed"].map(status => `<option value="${status}" ${order.status === status ? "selected" : ""}>${stockLabel(status)}</option>`).join("")}
        </select>
      </td>
      <td>${formatDateTime(order.created_at)}</td>
      <td><button class="btn-sm btn-outline order-view-btn" data-order-view="${order.order_ref}" data-order-kind="${order.order_kind}">View</button></td>
    </tr>
  `).join("");
  tbody.querySelectorAll("[data-order-status]").forEach(select => {
    select.addEventListener("change", () => updateAdminOrderStatus(select.dataset.orderKind, select.dataset.orderStatus, select.value));
  });
  tbody.querySelectorAll("[data-order-view]").forEach(button => {
    button.addEventListener("click", () => openOrderDetailModal(button.dataset.orderView, button.dataset.orderKind));
  });
  updateOrdersPagination(list.length);
}

function renderAdmins(list) {
  const container = document.getElementById("admins-list-body");
  if (!container) return;
  if (!isSuperAdmin()) {
    container.innerHTML = `<div class="empty-state">Only super admins can manage admin accounts.</div>`;
    return;
  }
  if (!list.length) {
    container.innerHTML = `<div class="empty-state">No admin accounts found.</div>`;
    return;
  }
  container.innerHTML = `<div class="admin-cards-grid">${
    list.map((admin, idx) => `
      <div class="admin-card">
        <div class="admin-card-avatar">${(admin.name || "A")[0]?.toUpperCase() || "A"}</div>
        <div class="admin-card-info">
          <div class="admin-card-name">${admin.name}</div>
          <div class="admin-card-email">${admin.email}</div>
          <div class="admin-card-roles">${adminRoleTags(admin)} ${admin.is_active ? "" : statusBadge("inactive")}</div>
        </div>
        ${admin.email === currentUser?.email
          ? `<span class="admin-card-you">You</span>`
          : `<button class="btn-card-view" data-admin-view="${idx}" title="View details">
            <svg viewBox="0 0 24 24"><path d="M12 5c5.23 0 9.27 3.11 11 7-1.73 3.89-5.77 7-11 7S2.73 15.89 1 12c1.73-3.89 5.77-7 11-7zm0 2C8.27 7 5.21 9.03 3.3 12 5.21 14.97 8.27 17 12 17s6.79-2.03 8.7-5C18.79 9.03 15.73 7 12 7zm0 2.5A2.5 2.5 0 1 1 9.5 12 2.5 2.5 0 0 1 12 9.5z"/></svg>
          </button>`
        }
      </div>
    `).join("")
  }</div>`;
  container.querySelectorAll("[data-admin-view]").forEach(btn => btn.addEventListener("click", () => openAdminModal(Number(btn.dataset.adminView))));
}

function toggleAdminManagementVisibility() {
  const adminPanel = document.getElementById("panel-admins");
  const activityPanel = document.getElementById("panel-activity-logs");
  const adminNavButton = document.querySelector('.nav-item[data-panel="admins"]');
  const activityNavButton = document.querySelector('.nav-item[data-panel="activity-logs"]');
  const navSection = document.getElementById("super-admin-nav-section");
  if (!adminPanel || !activityPanel || !adminNavButton || !activityNavButton || !navSection) return;
  if (isSuperAdmin()) {
    navSection.style.display = "block";
    adminNavButton.style.display = "flex";
    activityNavButton.style.display = "flex";
  } else {
    navSection.style.display = "none";
    adminNavButton.style.display = "none";
    activityNavButton.style.display = "none";
    if (adminPanel.classList.contains("active") || activityPanel.classList.contains("active")) {
      switchPanel("overview");
    }
  }
}

function resetProductForm() {
  document.getElementById("product-form").reset();
  document.getElementById("product-form-title").textContent = "Create Product";
  clearMsg(document.getElementById("product-form-msg"));
}

function renderProductImageCards(containerId, images, emptyLabel = "No images available.") {
  const container = document.getElementById(containerId);
  if (!container) return;
  if (!images.length) {
    container.innerHTML = `<div class="empty-state" style="padding:18px 12px;">${emptyLabel}</div>`;
    return;
  }
  container.innerHTML = images.map((image, index) => `
    <div class="product-image-card">
      <a href="${image}" target="_blank" rel="noopener noreferrer">
        <img src="${image}" alt="Product image ${index + 1}">
      </a>
      <div class="product-image-caption">Image ${index + 1}</div>
    </div>
  `).join("");
}

function openProductDetailModal(productId) {
  const product = allProducts.find(item => item.product_id === productId);
  const modal = document.getElementById("product-detail-modal");
  if (!product || !modal) return;
  activeProductModalId = product.product_id;
  document.getElementById("pdm-title").textContent = `Edit ${product.title}`;
  document.getElementById("pdm-product-id").textContent = product.product_id;
  document.getElementById("pdm-product-title").value = product.title;
  const categorySelect = document.getElementById("pdm-product-category");
  ensureSelectHasOption(categorySelect, product.category);
  categorySelect.value = product.category;
  document.getElementById("pdm-product-metal").value = product.metal || "gold";
  document.getElementById("pdm-product-purity").value = product.purity;
  document.getElementById("pdm-product-weight").value = product.weight;
  document.getElementById("pdm-product-stock").value = product.stock_status;
  document.getElementById("pdm-product-tags").value = (product.tags || []).join(", ");
  document.getElementById("pdm-product-description").value = product.description;
  document.getElementById("pdm-product-images").value = "";
  renderProductImageCards("pdm-current-images", product.images || []);
  renderProductImageCards("pdm-new-images", [], "No new images selected.");
  clearMsg(document.getElementById("pdm-msg"));
  modal.classList.add("open");
}

function closeProductDetailModal() {
  activeProductModalId = null;
  document.getElementById("product-detail-form")?.reset();
  renderProductImageCards("pdm-current-images", [], "No images available.");
  renderProductImageCards("pdm-new-images", [], "No new images selected.");
  clearMsg(document.getElementById("pdm-msg"));
  document.getElementById("product-detail-modal")?.classList.remove("open");
}

function handleProductModalImageChange(event) {
  const files = Array.from(event.target.files || []);
  if (!files.length) {
    renderProductImageCards("pdm-new-images", [], "No new images selected.");
    return;
  }
  const previews = files.map(file => URL.createObjectURL(file));
  renderProductImageCards("pdm-new-images", previews, "No new images selected.");
}

function openAdminModal(index) {
  const admin = allAdmins[index];
  const modal = document.getElementById("admin-detail-modal");
  if (!admin || !modal) return;
  activeAdminModalEmail = admin.email;
  document.getElementById("adm-avatar").textContent = (admin.name || "A")[0]?.toUpperCase() || "A";
  document.getElementById("adm-modal-name").textContent = admin.name || "-";
  document.getElementById("adm-modal-fullname").textContent = admin.name || "-";
  document.getElementById("adm-modal-email").textContent = admin.email || "-";
  document.getElementById("adm-modal-roles").innerHTML = adminRoleTags(admin);
  document.getElementById("adm-modal-id").textContent = admin.id || admin._id || "-";
  document.getElementById("adm-super-actions").style.display = admin.email === currentUser?.email ? "none" : "block";
  closeAdminActionSections();
  modal.classList.add("open");
}

function closeAdminModal() {
  activeAdminModalEmail = null;
  closeAdminActionSections();
  document.getElementById("admin-detail-modal")?.classList.remove("open");
}

function closeAdminActionSections() {
  ["adm-edit-section", "adm-otp-section", "adm-delete-section", "adm-delete-otp-section"].forEach(id => {
    const node = document.getElementById(id);
    if (node) node.style.display = "none";
  });
  ["adm-edit-msg", "adm-delete-msg"].forEach(id => {
    const node = document.getElementById(id);
    if (node) clearMsg(node);
  });
  ["adm-edit-email", "adm-edit-password", "adm-edit-otp", "adm-delete-otp"].forEach(id => {
    const node = document.getElementById(id);
    if (node) node.value = "";
  });
}

async function uploadImages(files) {
  if (!files.length) return [];
  const form = new FormData();
  Array.from(files).forEach(file => form.append("files", file));
  const response = await fetch(`${API_BASE}/uploads/product-images`, {
    method: "POST",
    headers: authHeaders(),
    body: form,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.detail || "Image upload failed.");
  return data.urls || [];
}

async function saveProduct(event) {
  event.preventDefault();
  const msg = document.getElementById("product-form-msg");
  const btn = document.querySelector('#product-form button[type="submit"]');
  clearMsg(msg);
  const stopLoading = setButtonLoading(btn, "Saving Product...");
  try {
    const imageFiles = document.getElementById("product-images").files;
    const uploadedImages = imageFiles.length ? await uploadImages(imageFiles) : [];
    const payload = {
      title: document.getElementById("product-title").value.trim(),
      category: document.getElementById("product-category").value,
      metal: document.getElementById("product-metal").value,
      purity: document.getElementById("product-purity").value.trim(),
      weight: Number(document.getElementById("product-weight").value),
      price: null,
      price_on_request: false,
      stock_status: document.getElementById("product-stock").value,
      tags: document.getElementById("product-tags").value.split(",").map(item => item.trim()).filter(Boolean),
      description: document.getElementById("product-description").value.trim(),
      featured: false,
      images: uploadedImages,
    };
    if (!payload.images.length) throw new Error("Upload at least one product image.");
    await api("/products", "POST", payload);
    showMsg(msg, "Product created successfully.", "success");
    resetProductForm();
    productCurrentPage = 1;
    await loadDashboardData();
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function saveProductFromModal(event) {
  event.preventDefault();
  if (!activeProductModalId) return;
  const msg = document.getElementById("pdm-msg");
  const btn = document.getElementById("pdm-save-btn");
  clearMsg(msg);
  const stopLoading = setButtonLoading(btn, "Saving Changes...");
  try {
    const imageFiles = document.getElementById("pdm-product-images").files;
    const uploadedImages = imageFiles.length ? await uploadImages(imageFiles) : [];
    const currentProduct = allProducts.find(item => item.product_id === activeProductModalId);
    const payload = {
      title: document.getElementById("pdm-product-title").value.trim(),
      category: document.getElementById("pdm-product-category").value,
      metal: document.getElementById("pdm-product-metal").value,
      purity: document.getElementById("pdm-product-purity").value.trim(),
      weight: Number(document.getElementById("pdm-product-weight").value),
      price: null,
      price_on_request: false,
      stock_status: document.getElementById("pdm-product-stock").value,
      tags: document.getElementById("pdm-product-tags").value.split(",").map(item => item.trim()).filter(Boolean),
      description: document.getElementById("pdm-product-description").value.trim(),
      featured: false,
      images: uploadedImages.length ? uploadedImages : (currentProduct?.images || []),
    };
    if (!payload.images.length) throw new Error("Upload at least one product image.");
    await api(`/products/${activeProductModalId}`, "PUT", payload);
    showToast("Product updated.", "success");
    closeProductDetailModal();
    await loadDashboardData();
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function deleteProductFromModal() {
  if (!activeProductModalId) return;
  if (!confirm(`Delete ${activeProductModalId}?`)) return;
  const btn = document.getElementById("pdm-delete-btn");
  const stopLoading = setButtonLoading(btn, "Deleting...");
  try {
    await api(`/products/${activeProductModalId}`, "DELETE");
    showToast("Product deleted.", "success");
    closeProductDetailModal();
    await loadDashboardData();
  } catch (error) {
    showMsg(document.getElementById("pdm-msg"), error.message, "error");
  } finally {
    stopLoading();
  }
}

async function requestRegisterOtp() {
  const msg = document.getElementById("register-msg");
  const btn = document.getElementById("register-otp-btn");
  clearMsg(msg);
  if (!isSuperAdmin()) {
    showMsg(msg, "Only super admins can manage admin accounts.");
    return;
  }
  const name = document.getElementById("r-name").value.trim();
  const email = document.getElementById("r-email").value.trim();
  if (!name || !email) {
    showMsg(msg, "Enter full name and email address.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Sending OTP...");
  try {
    await api("/admin/request-register-otp", "POST", { name, email });
    document.getElementById("register-otp-section").style.display = "block";
    document.getElementById("register-btn").style.display = "inline-flex";
    stopLoading("Resend OTP");
    showMsg(msg, "OTP sent to your email. Enter it below to confirm admin creation.", "success");
  } catch (error) {
    showMsg(msg, error.message, "error");
    stopLoading();
  }
}

async function confirmRegisterAdmin() {
  const msg = document.getElementById("register-msg");
  const btn = document.getElementById("register-btn");
  clearMsg(msg);
  const name = document.getElementById("r-name").value.trim();
  const email = document.getElementById("r-email").value.trim();
  const otp = document.getElementById("r-otp").value.trim();
  if (!name || !email) {
    showMsg(msg, "Enter full name and email address.");
    return;
  }
  if (!otp) {
    showMsg(msg, "Enter the OTP sent to your email.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Creating Admin...");
  try {
    await api("/admin/register", "POST", { name, email, otp });
    document.getElementById("r-name").value = "";
    document.getElementById("r-email").value = "";
    document.getElementById("r-otp").value = "";
    document.getElementById("register-otp-section").style.display = "none";
    document.getElementById("register-btn").style.display = "none";
    document.getElementById("register-otp-btn").textContent = "Request OTP";
    showMsg(msg, "Admin account created and credentials emailed.", "success");
    await loadDashboardData();
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function requestDeleteOtp() {
  const msg = document.getElementById("da-msg");
  const btn = document.getElementById("da-otp-btn");
  clearMsg(msg);
  const email = document.getElementById("da-email").value.trim();
  if (!email) {
    showMsg(msg, "Enter the admin email address.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Sending OTP...");
  try {
    await api("/admin/request-delete-otp", "POST", { target_email: email });
    document.getElementById("da-otp-section").style.display = "block";
    document.getElementById("da-btn").style.display = "inline-flex";
    stopLoading("Resend OTP");
    showMsg(msg, "OTP sent to your email. Enter it below to confirm deletion.", "success");
  } catch (error) {
    showMsg(msg, error.message, "error");
    stopLoading();
  }
}

async function confirmDeleteAdminFromForm() {
  const msg = document.getElementById("da-msg");
  const btn = document.getElementById("da-btn");
  clearMsg(msg);
  const email = document.getElementById("da-email").value.trim();
  const otp = document.getElementById("da-otp").value.trim();
  if (!otp) {
    showMsg(msg, "Enter the OTP sent to your email.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Deleting...");
  try {
    await api("/admin/delete-admin", "POST", { email, otp });
    document.getElementById("da-email").value = "";
    document.getElementById("da-otp").value = "";
    document.getElementById("da-otp-section").style.display = "none";
    document.getElementById("da-btn").style.display = "none";
    document.getElementById("da-otp-btn").textContent = "Request OTP";
    showMsg(msg, "Admin account removed successfully.", "success");
    await loadDashboardData();
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

function toggleAdminEdit() {
  const section = document.getElementById("adm-edit-section");
  section.style.display = section.style.display === "block" ? "none" : "block";
  document.getElementById("adm-otp-section").style.display = "none";
  clearMsg(document.getElementById("adm-edit-msg"));
}

async function requestAdminEditOtp() {
  const msg = document.getElementById("adm-edit-msg");
  const btn = document.getElementById("adm-edit-otp-btn");
  clearMsg(msg);
  const newEmail = document.getElementById("adm-edit-email").value.trim() || null;
  const newPassword = document.getElementById("adm-edit-password").value || null;
  if (!newEmail && !newPassword) {
    showMsg(msg, "Enter a new email or password.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Sending OTP...");
  try {
    await api("/admin/request-edit-otp", "POST", { target_email: activeAdminModalEmail });
    document.getElementById("adm-otp-section").style.display = "block";
    showMsg(msg, "OTP sent to your email. Enter it below.", "success");
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function saveAdminCredentials() {
  const msg = document.getElementById("adm-edit-msg");
  const btn = document.getElementById("adm-save-creds-btn");
  clearMsg(msg);
  const newEmail = document.getElementById("adm-edit-email").value.trim() || null;
  const newPassword = document.getElementById("adm-edit-password").value || null;
  const otp = document.getElementById("adm-edit-otp").value.trim();
  if (!otp) {
    showMsg(msg, "Enter the OTP sent to your email.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Saving Changes...");
  try {
    await api("/admin/update-admin-credentials", "POST", {
      target_email: activeAdminModalEmail,
      otp,
      new_email: newEmail,
      new_password: newPassword,
    });
    showToast("Admin credentials updated.", "success");
    closeAdminModal();
    await loadDashboardData();
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

function toggleAdminDelete() {
  const section = document.getElementById("adm-delete-section");
  section.style.display = section.style.display === "block" ? "none" : "block";
  document.getElementById("adm-delete-otp-section").style.display = "none";
  clearMsg(document.getElementById("adm-delete-msg"));
}

async function requestModalDeleteOtp() {
  const msg = document.getElementById("adm-delete-msg");
  const btn = document.getElementById("adm-delete-otp-btn");
  clearMsg(msg);
  const stopLoading = setButtonLoading(btn, "Sending OTP...");
  try {
    await api("/admin/request-delete-otp", "POST", { target_email: activeAdminModalEmail });
    document.getElementById("adm-delete-otp-section").style.display = "block";
    showMsg(msg, "OTP sent to your email. Enter it below to confirm permanent deletion.", "success");
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function confirmModalDeleteAdmin() {
  const msg = document.getElementById("adm-delete-msg");
  const btn = document.getElementById("adm-confirm-delete-btn");
  clearMsg(msg);
  const otp = document.getElementById("adm-delete-otp").value.trim();
  if (!otp) {
    showMsg(msg, "Enter the OTP sent to your email.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Deleting...");
  try {
    await api("/admin/delete-admin", "POST", { email: activeAdminModalEmail, otp });
    showToast("Admin account removed.", "success");
    closeAdminModal();
    await loadDashboardData();
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

function populateProfilePanel() {
  const nameEl = document.getElementById("profile-name-display");
  const emailEl = document.getElementById("profile-email-display");
  if (nameEl) nameEl.textContent = currentUser?.name || "-";
  if (emailEl) emailEl.textContent = currentUser?.email || "-";
}

function openProfileModal(section) {
  ["profile-modal-name", "profile-modal-password", "profile-modal-email"].forEach(id => {
    document.getElementById(id).style.display = "none";
  });
  ["pm-name-msg", "pm-pwd-msg", "pm-email-msg"].forEach(id => clearMsg(document.getElementById(id)));
  ["pm-name", "pm-old-pwd", "pm-new-pwd", "pm-confirm-pwd", "pm-email", "pm-email-pwd"].forEach(id => {
    document.getElementById(id).value = "";
  });
  if (section === "name") {
    document.getElementById("pm-name").value = currentUser?.name || "";
    document.getElementById("profile-modal-name").style.display = "block";
  }
  if (section === "password") document.getElementById("profile-modal-password").style.display = "block";
  if (section === "email") {
    document.getElementById("pm-email").value = currentUser?.email || "";
    document.getElementById("profile-modal-email").style.display = "block";
  }
  document.getElementById("profile-modal").classList.add("open");
}

function closeProfileModal() {
  document.getElementById("profile-modal").classList.remove("open");
}

async function submitProfileName() {
  const msg = document.getElementById("pm-name-msg");
  const btn = document.getElementById("pm-name-btn");
  clearMsg(msg);
  const name = document.getElementById("pm-name").value.trim();
  if (name.length < 2) {
    showMsg(msg, "Name must be at least 2 characters.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Saving...");
  try {
    const updated = await api("/auth/me", "PATCH", {
      name,
      email: currentUser.email,
    });
    currentUser = updated;
    setUserUI();
    showMsg(msg, "Name updated successfully.", "success");
    showToast("Name updated.", "success");
    setTimeout(closeProfileModal, 900);
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function submitProfilePassword() {
  const msg = document.getElementById("pm-pwd-msg");
  const btn = document.getElementById("pm-pwd-btn");
  clearMsg(msg);
  const currentPassword = document.getElementById("pm-old-pwd").value;
  const newPassword = document.getElementById("pm-new-pwd").value;
  const confirmPassword = document.getElementById("pm-confirm-pwd").value;
  if (!currentPassword || !newPassword) {
    showMsg(msg, "Please fill in all password fields.");
    return;
  }
  if (newPassword !== confirmPassword) {
    showMsg(msg, "New passwords do not match.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Saving...");
  try {
    await api("/auth/me/password", "PATCH", {
      current_password: currentPassword,
      new_password: newPassword,
    });
    showMsg(msg, "Password updated. Signing you out...", "success");
    showToast("Password updated.", "success");
    setTimeout(logout, 1500);
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function submitProfileEmail() {
  const msg = document.getElementById("pm-email-msg");
  const btn = document.getElementById("pm-email-btn");
  clearMsg(msg);
  const email = document.getElementById("pm-email").value.trim();
  const currentPassword = document.getElementById("pm-email-pwd").value;
  if (!email || !currentPassword) {
    showMsg(msg, "Enter a new email and your current password.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Saving...");
  try {
    await api("/auth/me/email", "PATCH", {
      email,
      current_password: currentPassword,
    });
    showMsg(msg, "Email updated. Signing you out...", "success");
    showToast("Email updated.", "success");
    setTimeout(logout, 1500);
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function updateAdminOrderStatus(orderKind, orderId, status) {
  try {
    const route = orderKind === "custom_order"
      ? `/admin/custom-requests/${orderId}/status`
      : `/admin/orders/${orderId}/status`;
    await api(route, "PATCH", { status });
    showToast("Order status updated.", "success");
    await loadDashboardData();
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function addOrderCommentFromModal() {
  if (!activeOrderModalRef || !activeOrderModalKind) return;
  const text = document.getElementById("order-comment-text").value.trim();
  const msg = document.getElementById("order-comment-msg");
  const btn = document.getElementById("order-add-comment-btn");
  clearMsg(msg);
  if (!text) {
    showMsg(msg, "Enter a comment before submitting.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Adding...");
  try {
    const route = activeOrderModalKind === "custom_order"
      ? `/admin/custom-requests/${activeOrderModalRef}/comments`
      : `/admin/orders/${activeOrderModalRef}/comments`;
    await api(route, "POST", { comment: text });
    const order = allAdminOrders.find(item => item.order_ref === activeOrderModalRef && item.order_kind === activeOrderModalKind);
    if (order) {
      if (!Array.isArray(order.comments)) order.comments = [];
      order.comments.push({
        comment: text,
        added_by: currentUser?.name || currentUser?.email || "Admin",
        added_by_name: currentUser?.name || "Admin",
        added_by_email: currentUser?.email || null,
        created_at: new Date().toISOString(),
      });
      renderOrderCommentList(order);
    }
    document.getElementById("order-comment-text").value = "";
    showMsg(msg, "Comment added.", "success");
    showToast("Comment added.", "success");
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function refreshOrdersData() {
  const btn = document.getElementById("orders-refresh-btn");
  const stopLoading = setButtonLoading(btn, "Refreshing...");
  try {
    await loadDashboardData();
  } finally {
    stopLoading();
  }
}

async function sendForgotOtp() {
  const msg = document.getElementById("fp-msg1");
  const btn = document.getElementById("fp-send-btn");
  clearMsg(msg);
  const stopLoading = setButtonLoading(btn, "Sending OTP...");
  try {
    await api("/auth/forgot-password/request", "POST", { email: document.getElementById("fp-email").value.trim() });
    showMsg(msg, "OTP sent if the account exists.", "success");
    document.getElementById("forgot-step1").classList.remove("active");
    document.getElementById("forgot-step2").classList.add("active");
  } catch (error) {
    showMsg(msg, error.message);
  } finally {
    stopLoading();
  }
}

async function resetForgotPassword() {
  const msg = document.getElementById("fp-msg2");
  const btn = document.getElementById("fp-reset-btn");
  clearMsg(msg);
  const newPassword = document.getElementById("fp-new-password").value;
  const confirmPassword = document.getElementById("fp-confirm-password").value;
  if (newPassword !== confirmPassword) {
    showMsg(msg, "Passwords do not match.");
    return;
  }
  const stopLoading = setButtonLoading(btn, "Resetting...");
  try {
    await api("/auth/forgot-password/reset", "POST", {
      email: document.getElementById("fp-email").value.trim(),
      otp: document.getElementById("fp-otp").value.trim(),
      new_password: newPassword,
    });
    showMsg(msg, "Password reset successful. You can sign in now.", "success");
  } catch (error) {
    showMsg(msg, error.message);
  } finally {
    stopLoading();
  }
}

function filterProducts() {
  productCurrentPage = 1;
  loadProductsPage(1).catch(error => {
    showToast(error.message, "error");
  });
}

function filterOrders() {
  orderCurrentPage = 1;
  renderOrders(getFilteredOrders());
}

function filterActivityLogs() {
  activityCurrentPage = 1;
  renderActivityLogs(getFilteredActivityLogs());
}

async function exportOrdersCsv() {
  const btn = document.getElementById("export-orders-btn");
  const stopLoading = setButtonLoading(btn, "Exporting...");
  try {
    const response = await fetch(`${API_BASE}/admin/orders/export`, { headers: authHeaders() });
    if (!response.ok) throw new Error("Unable to export CSV.");
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "ons-gold-orders.csv";
    a.click();
    URL.revokeObjectURL(url);
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    stopLoading();
  }
}

function openBugModal() {
  document.getElementById("bug-report-modal").classList.add("open");
}

function closeBugModal() {
  document.getElementById("bug-report-modal").classList.remove("open");
  ["bug-title", "bug-severity", "bug-description", "bug-image-input"].forEach(id => {
    const node = document.getElementById(id);
    if (node) node.value = "";
  });
  document.getElementById("bug-img-preview").style.display = "none";
  document.getElementById("bug-img-label-text").textContent = "Attach screenshot (PNG, JPG, WEBP - max 4 MB)";
  clearMsg(document.getElementById("bug-msg"));
  bugImageBase64 = null;
  bugImageMime = null;
}

function handleBugImageChange(event) {
  const file = event.target.files[0];
  const msg = document.getElementById("bug-msg");
  clearMsg(msg);
  if (!file) return;
  if (file.size > 4 * 1024 * 1024) {
    showMsg(msg, "Image too large. Max 4 MB.");
    event.target.value = "";
    return;
  }
  if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
    showMsg(msg, "Unsupported image format. Use PNG, JPG, or WEBP.");
    event.target.value = "";
    return;
  }
  bugImageMime = file.type;
  const reader = new FileReader();
  reader.onload = loadEvent => {
    const dataUrl = String(loadEvent.target.result || "");
    bugImageBase64 = dataUrl.split(",")[1] || null;
    const preview = document.getElementById("bug-img-preview");
    preview.src = dataUrl;
    preview.style.display = "block";
    document.getElementById("bug-img-label-text").textContent = file.name;
  };
  reader.readAsDataURL(file);
}

async function submitBugReport() {
  const title = document.getElementById("bug-title").value.trim();
  const severity = document.getElementById("bug-severity").value;
  const description = document.getElementById("bug-description").value.trim();
  const msg = document.getElementById("bug-msg");
  const btn = document.getElementById("bug-submit-btn");
  clearMsg(msg);
  if (!title || !severity || !description) {
    showMsg(msg, "Please fill in all required fields.");
    return;
  }
  const payload = { title, severity, description };
  if (bugImageBase64) {
    payload.image_base64 = bugImageBase64;
    payload.image_mime = bugImageMime;
  }
  const stopLoading = setButtonLoading(btn, "Submitting...");
  try {
    await api("/admin/report-bug", "POST", payload);
    showMsg(msg, "Bug report submitted. The team has been notified.", "success");
    showToast("Bug report sent.", "success");
    setTimeout(closeBugModal, 1200);
  } catch (error) {
    showMsg(msg, error.message, "error");
  } finally {
    stopLoading();
  }
}

async function refreshDashboardData() {
  const btn = document.getElementById("admins-refresh-btn");
  const stopLoading = setButtonLoading(btn, "Refreshing...");
  try {
    await loadDashboardData();
  } finally {
    stopLoading();
  }
}

async function refreshActivityLogs() {
  const btn = document.getElementById("activity-refresh-btn");
  const stopLoading = setButtonLoading(btn, "Refreshing...");
  try {
    await loadDashboardData();
  } finally {
    stopLoading();
  }
}

function switchPanel(panelName) {
  if (!isSuperAdmin() && (panelName === "admins" || panelName === "activity-logs")) {
    panelName = "overview";
  }
  document.querySelectorAll(".nav-item").forEach(item => item.classList.toggle("active", item.dataset.panel === panelName));
  document.querySelectorAll(".panel").forEach(panel => panel.classList.toggle("active", panel.id === `panel-${panelName}`));
  const labels = {
    overview: "Overview",
    products: "Products",
    orders: "Orders",
    admins: "Manage Admins",
    "activity-logs": "Activity Logs",
    account: "Edit Profile",
  };
  document.getElementById("topbar-title").textContent = labels[panelName] || panelName.replace("-", " ").replace(/\b\w/g, c => c.toUpperCase());
  setSidebarOpen(false);
  closeAdminModal();
  closeOrderDetailModal();
  closeActivityLogModal();
  closeProductDetailModal();
}

function setSidebarOpen(isOpen) {
  document.body.classList.toggle("sidebar-open", Boolean(isOpen));
}

document.getElementById("login-btn").addEventListener("click", login);
["l-email", "l-password"].forEach(id => document.getElementById(id).addEventListener("keydown", event => {
  if (event.key === "Enter") login();
}));
document.querySelectorAll(".password-toggle[data-target]").forEach(toggle => toggle.addEventListener("click", () => {
  const input = document.getElementById(toggle.dataset.target);
  if (!input) return;
  const nextType = input.type === "password" ? "text" : "password";
  input.type = nextType;
  toggle.textContent = nextType === "password" ? "Show" : "Hide";
  toggle.setAttribute("aria-label", nextType === "password" ? "Show password" : "Hide password");
}));
document.getElementById("forgot-link").addEventListener("click", () => {
  document.getElementById("login-form-section").style.display = "none";
  document.getElementById("forgot-form").style.display = "block";
});
document.getElementById("forgot-back-btn").addEventListener("click", () => {
  document.getElementById("forgot-form").style.display = "none";
  document.getElementById("login-form-section").style.display = "block";
});
document.getElementById("forgot-back2-btn").addEventListener("click", () => {
  document.getElementById("forgot-step2").classList.remove("active");
  document.getElementById("forgot-step1").classList.add("active");
});
document.getElementById("fp-send-btn").addEventListener("click", sendForgotOtp);
document.getElementById("fp-reset-btn").addEventListener("click", resetForgotPassword);
document.getElementById("report-bug-btn").addEventListener("click", openBugModal);
document.getElementById("bug-modal-close").addEventListener("click", closeBugModal);
document.getElementById("bug-report-modal").addEventListener("click", event => {
  if (event.target.id === "bug-report-modal") closeBugModal();
});
document.getElementById("bug-image-input").addEventListener("change", handleBugImageChange);
document.getElementById("bug-submit-btn").addEventListener("click", submitBugReport);
document.getElementById("product-form").addEventListener("submit", saveProduct);
document.getElementById("product-detail-form").addEventListener("submit", saveProductFromModal);
document.getElementById("product-detail-close").addEventListener("click", closeProductDetailModal);
document.getElementById("product-detail-modal").addEventListener("click", event => {
  if (event.target.id === "product-detail-modal") closeProductDetailModal();
});
document.getElementById("order-detail-close").addEventListener("click", closeOrderDetailModal);
document.getElementById("order-detail-modal").addEventListener("click", event => {
  if (event.target.id === "order-detail-modal") closeOrderDetailModal();
});
document.getElementById("order-add-comment-btn").addEventListener("click", addOrderCommentFromModal);
document.getElementById("activity-detail-close").addEventListener("click", closeActivityLogModal);
document.getElementById("activity-detail-modal").addEventListener("click", event => {
  if (event.target.id === "activity-detail-modal") closeActivityLogModal();
});
document.getElementById("pdm-product-images").addEventListener("change", handleProductModalImageChange);
document.getElementById("pdm-delete-btn").addEventListener("click", deleteProductFromModal);
document.getElementById("register-otp-btn").addEventListener("click", requestRegisterOtp);
document.getElementById("register-btn").addEventListener("click", confirmRegisterAdmin);
document.getElementById("da-otp-btn").addEventListener("click", requestDeleteOtp);
document.getElementById("da-btn").addEventListener("click", confirmDeleteAdminFromForm);
document.getElementById("admins-refresh-btn").addEventListener("click", refreshDashboardData);
document.querySelectorAll("[data-profile-modal]").forEach(btn => {
  btn.addEventListener("click", () => openProfileModal(btn.dataset.profileModal));
});
document.getElementById("profile-modal-close").addEventListener("click", closeProfileModal);
document.getElementById("profile-modal").addEventListener("click", event => {
  if (event.target.id === "profile-modal") closeProfileModal();
});
document.getElementById("pm-name-btn").addEventListener("click", submitProfileName);
document.getElementById("pm-pwd-btn").addEventListener("click", submitProfilePassword);
document.getElementById("pm-email-btn").addEventListener("click", submitProfileEmail);
document.getElementById("products-search").addEventListener("input", filterProducts);
document.getElementById("products-prev-btn").addEventListener("click", () => {
  if (productCurrentPage <= 1) return;
  loadProductsPage(productCurrentPage - 1).catch(error => showToast(error.message, "error"));
});
document.getElementById("products-next-btn").addEventListener("click", () => {
  const totalPages = Math.max(1, Math.ceil(productTotalItems / PRODUCTS_PAGE_SIZE));
  if (productCurrentPage >= totalPages) return;
  loadProductsPage(productCurrentPage + 1).catch(error => showToast(error.message, "error"));
});
const ordersSearchInput = document.getElementById("orders-search");
if (ordersSearchInput) ordersSearchInput.addEventListener("input", filterOrders);
document.getElementById("orders-refresh-btn").addEventListener("click", refreshOrdersData);
document.getElementById("orders-prev-btn").addEventListener("click", () => {
  if (orderCurrentPage <= 1) return;
  orderCurrentPage -= 1;
  renderOrders(getFilteredOrders());
});
document.getElementById("orders-next-btn").addEventListener("click", () => {
  const totalPages = Math.max(1, Math.ceil(getFilteredOrders().length / ORDERS_PAGE_SIZE));
  if (orderCurrentPage >= totalPages) return;
  orderCurrentPage += 1;
  renderOrders(getFilteredOrders());
});
const activitySearchInput = document.getElementById("activity-search");
if (activitySearchInput) activitySearchInput.addEventListener("input", filterActivityLogs);
document.getElementById("activity-refresh-btn").addEventListener("click", refreshActivityLogs);
document.getElementById("activity-prev-btn").addEventListener("click", () => {
  if (activityCurrentPage <= 1) return;
  activityCurrentPage -= 1;
  renderActivityLogs(getFilteredActivityLogs());
});
document.getElementById("activity-next-btn").addEventListener("click", () => {
  const totalPages = Math.max(1, Math.ceil(getFilteredActivityLogs().length / ACTIVITY_PAGE_SIZE));
  if (activityCurrentPage >= totalPages) return;
  activityCurrentPage += 1;
  renderActivityLogs(getFilteredActivityLogs());
});
document.getElementById("admin-detail-close").addEventListener("click", closeAdminModal);
document.getElementById("admin-detail-modal").addEventListener("click", event => {
  if (event.target.id === "admin-detail-modal") closeAdminModal();
});
document.getElementById("adm-edit-toggle-btn").addEventListener("click", toggleAdminEdit);
document.getElementById("adm-edit-otp-btn").addEventListener("click", requestAdminEditOtp);
document.getElementById("adm-save-creds-btn").addEventListener("click", saveAdminCredentials);
document.getElementById("adm-delete-toggle-btn").addEventListener("click", toggleAdminDelete);
document.getElementById("adm-delete-otp-btn").addEventListener("click", requestModalDeleteOtp);
document.getElementById("adm-confirm-delete-btn").addEventListener("click", confirmModalDeleteAdmin);
const exportOrdersButton = document.getElementById("export-orders-btn");
if (exportOrdersButton) exportOrdersButton.addEventListener("click", exportOrdersCsv);
document.getElementById("logout-btn").addEventListener("click", () => {
  logout();
});
document.querySelectorAll(".nav-item").forEach(item => item.addEventListener("click", () => switchPanel(item.dataset.panel)));
document.getElementById("sidebar-toggle-btn").addEventListener("click", () => setSidebarOpen(true));
document.getElementById("sidebar-close-btn").addEventListener("click", () => setSidebarOpen(false));
document.getElementById("sidebar-overlay").addEventListener("click", () => setSidebarOpen(false));

(async function boot() {
  setLogoutIcon();
  await fetchPublicSummaryForLogin();
  if (token) {
    try {
      await fetchCurrentUser();
      showDashboard();
      await loadDashboardData();
    } catch (_) {
      token = "";
      currentUser = null;
      clearPersistedSession();
      showLogin();
    }
  } else {
    showLogin();
  }
})();
