const API_BASE = window.ONS_API_BASE || "http://127.0.0.1:8000/api/v1";
const WHATSAPP_NUMBER = "9833348296";
const SELECTED_PRODUCTS_KEY = "ons_gold_selected_products";
const MAX_SELECTED_PRODUCTS = 25;
const PRODUCT_CARD_SLIDE_INTERVAL_MS = 2800;

const state = {
  products: [],
  selected: loadSelectedProducts(),
  categories: new Set(),
  purities: new Set(),
  currentPage: 1,
  totalProducts: 0,
  pageSize: 24,
  modalProduct: null,
  contactMode: "catalog",
};

const productCardSliderTimers = [];

function qs(selector, root = document) {
  return root.querySelector(selector);
}

function qsa(selector, root = document) {
  return Array.from(root.querySelectorAll(selector));
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function stockLabel(value) {
  return String(value || "")
    .replaceAll("_", " ")
    .replace(/\b\w/g, char => char.toUpperCase());
}

function cleanPhone(value) {
  return String(value || "").replace(/[^\d+]/g, "");
}

function productImage(product) {
  return product?.images?.[0] || product?.image || "assets/images/demo (2).jpg";
}

function clearProductCardSliders() {
  while (productCardSliderTimers.length) {
    clearInterval(productCardSliderTimers.pop());
  }
}

function setProductCardSlide(card, index) {
  const track = qs("[data-slider-track]", card);
  const slides = qsa(".product-media-slide", card);
  const dots = qsa(".product-media-dot", card);
  if (!track || !slides.length) return;
  const safeIndex = ((index % slides.length) + slides.length) % slides.length;
  track.style.transform = `translateX(-${safeIndex * 100}%)`;
  card.dataset.slideIndex = String(safeIndex);
  dots.forEach((dot, dotIndex) => {
    dot.classList.toggle("active", dotIndex === safeIndex);
  });
}

function initProductCardSliders(root = document) {
  clearProductCardSliders();
  qsa("[data-card-slider]", root).forEach(card => {
    const slides = qsa(".product-media-slide", card);
    if (slides.length < 2) return;

    let index = 0;
    setProductCardSlide(card, index);
    const timer = window.setInterval(() => {
      index = (index + 1) % slides.length;
      setProductCardSlide(card, index);
    }, PRODUCT_CARD_SLIDE_INTERVAL_MS);
    productCardSliderTimers.push(timer);
  });
}

function selectedCount() {
  return state.selected.reduce((sum, item) => sum + (Number(item.quantity) || 1), 0);
}

function loadSelectedProducts() {
  try {
    const parsed = JSON.parse(localStorage.getItem(SELECTED_PRODUCTS_KEY) || "[]");
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter(item => item && item.product_id)
      .map(item => ({
        product_id: String(item.product_id),
        title: String(item.title || item.product_id),
        image: item.image || "",
        quantity: Math.min(Math.max(Number(item.quantity) || 1, 1), 999),
      }));
  } catch {
    return [];
  }
}

function saveSelectedProducts() {
  localStorage.setItem(SELECTED_PRODUCTS_KEY, JSON.stringify(state.selected));
  updateSelectionUI();
}

function showStatus(node, message, type = "") {
  if (!node) return;
  node.textContent = message || "";
  node.className = `status-message ${type}`.trim();
}

function getErrorMessage(data, fallback = "Request failed.") {
  const detail = data?.detail;
  if (typeof detail === "string" && detail.trim()) return detail;
  if (Array.isArray(detail) && detail.length) {
    const messages = detail
      .map(item => {
        const field = Array.isArray(item?.loc) ? item.loc[item.loc.length - 1] : "";
        const message = String(item?.msg || item?.message || "").trim();
        return message ? `${field ? `${stockLabel(field)}: ` : ""}${message}` : "";
      })
      .filter(Boolean);
    if (messages.length) return messages.join(". ");
  }
  if (typeof data?.message === "string" && data.message.trim()) return data.message;
  return fallback;
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method || "GET",
    headers: {
      ...(options.body && !(options.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {}),
    },
    body: options.body instanceof FormData ? options.body : options.body ? JSON.stringify(options.body) : null,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(getErrorMessage(data, "Unable to connect to ONS Gold."));
  }
  return data;
}

function setButtonLoading(button, label) {
  if (!button) return () => {};
  const original = button.textContent;
  button.disabled = true;
  button.textContent = label;
  return () => {
    button.disabled = false;
    button.textContent = original;
  };
}

function setupNav() {
  const navbar = qs("#navbar");
  const hamburger = qs("#hamburger");
  const mobileMenu = qs("#mobile-menu");

  if (navbar) {
    window.addEventListener("scroll", () => {
      navbar.classList.toggle("scrolled", window.scrollY > 36);
    }, { passive: true });
  }

  if (hamburger && mobileMenu) {
    hamburger.addEventListener("click", () => {
      const open = mobileMenu.classList.toggle("open");
      hamburger.classList.toggle("open", open);
      hamburger.setAttribute("aria-expanded", String(open));
    });
    qsa("a", mobileMenu).forEach(link => {
      link.addEventListener("click", () => {
        mobileMenu.classList.remove("open");
        hamburger.classList.remove("open");
        hamburger.setAttribute("aria-expanded", "false");
      });
    });
  }
}

function setupReveal() {
  const revealEls = qsa(".reveal");
  if (!("IntersectionObserver" in window)) {
    revealEls.forEach(el => el.classList.add("visible"));
    return;
  }

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  revealEls.forEach(el => observer.observe(el));
}

function openModal(id) {
  const modal = qs(`#${id}`);
  if (!modal) return;
  modal.classList.add("open");
  document.body.classList.add("modal-open");
}

function closeModal(id) {
  const modal = qs(`#${id}`);
  if (!modal) return;
  modal.classList.remove("open");
  if (!qs(".modal.open")) {
    document.body.classList.remove("modal-open");
  }
}

function setupModals() {
  qsa("[data-close-modal]").forEach(button => {
    button.addEventListener("click", () => closeModal(button.dataset.closeModal));
  });

  qsa(".modal").forEach(modal => {
    modal.addEventListener("click", event => {
      if (event.target === modal) closeModal(modal.id);
    });
  });

  document.addEventListener("keydown", event => {
    if (event.key !== "Escape") return;
    const open = qsa(".modal.open").at(-1);
    if (open) closeModal(open.id);
  });
}

function setupChatWidget() {
  qsa(".floating-whatsapp, #ai-chat-widget").forEach(node => node.remove());
  if (document.body.classList.contains("catalog-page")) return;
  if (qs("#ai-chat-widget")) return;

  const widget = document.createElement("div");
  widget.className = "ai-chat-widget";
  widget.id = "ai-chat-widget";
  widget.innerHTML = `
    <div class="ai-chat-panel" id="ai-chat-panel" aria-label="ONS Gold AI chat" aria-hidden="true">
      <div class="ai-chat-header">
        <div class="ai-chat-avatar" aria-hidden="true">O</div>
        <div>
          <div class="ai-chat-title">ONS Gold Assistant</div>
          <div class="ai-chat-status is-offline"><span></span> Offline</div>
        </div>
        <button class="ai-chat-close" type="button" id="ai-chat-close" aria-label="Close chat">&times;</button>
      </div>
      <div class="ai-chat-body">
        <div class="ai-chat-message">AI Chat will be available soon.</div>
      </div>
      <form class="ai-chat-input-row" id="ai-chat-form">
        <input type="text" aria-label="Chat message" placeholder="Type your message..." disabled />
        <button type="submit" aria-label="Send message" disabled>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 12h13"></path>
            <path d="m13 6 6 6-6 6"></path>
          </svg>
        </button>
      </form>
    </div>
    <button class="ai-chat-launcher" type="button" id="ai-chat-launcher" aria-label="Open AI chat" aria-expanded="false">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 6.5A3.5 3.5 0 0 1 8.5 3h7A3.5 3.5 0 0 1 19 6.5v5A3.5 3.5 0 0 1 15.5 15H12l-4.5 4v-4A3.5 3.5 0 0 1 4 11.5v-5Z"></path>
      </svg>
    </button>
  `;
  document.body.appendChild(widget);

  const launcher = qs("#ai-chat-launcher", widget);
  const panel = qs("#ai-chat-panel", widget);
  const close = qs("#ai-chat-close", widget);

  function setOpen(isOpen) {
    widget.classList.toggle("open", isOpen);
    launcher.setAttribute("aria-expanded", String(isOpen));
    panel.setAttribute("aria-hidden", String(!isOpen));
  }

  launcher.addEventListener("click", () => setOpen(!widget.classList.contains("open")));
  close.addEventListener("click", () => setOpen(false));
  qsa("[data-open-chat]").forEach(trigger => {
    trigger.addEventListener("click", event => {
      event.preventDefault();
      setOpen(true);
    });
  });
  qs("#ai-chat-form", widget)?.addEventListener("submit", event => event.preventDefault());
}

function setSelectOptions(selectId, values, label) {
  const select = qs(`#${selectId}`);
  if (!select) return;
  const current = select.value;
  const items = Array.from(values).filter(Boolean).sort((a, b) => a.localeCompare(b));
  select.innerHTML = [`<option value="">${label}</option>`]
    .concat(items.map(value => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`))
    .join("");
  if (current) select.value = current;
}

function buildProductQuery(page) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("page_size", String(state.pageSize));

  const fields = [
    ["search", "filter-search"],
    ["category", "filter-category"],
    ["metal", "filter-metal"],
    ["purity", "filter-purity"],
    ["min_weight", "filter-min-weight"],
    ["max_weight", "filter-max-weight"],
    ["sort", "filter-sort"],
  ];

  fields.forEach(([param, id]) => {
    const value = qs(`#${id}`)?.value?.trim();
    if (value) params.set(param, value);
  });

  return params.toString();
}

async function loadProducts(page = 1) {
  const grid = qs("#products-grid");
  const status = qs("#catalog-status");
  if (!grid) return;

  state.currentPage = page;
  showStatus(status, "Loading products...");

  try {
    const data = await api(`/products?${buildProductQuery(page)}`);
    state.products = data.items || [];
    state.totalProducts = Number(data.total) || 0;

    state.products.forEach(product => {
      if (product.category) state.categories.add(product.category);
      if (product.purity) state.purities.add(product.purity);
    });

    setSelectOptions("filter-category", state.categories, "All Categories");
    setSelectOptions("filter-purity", state.purities, "All Purity");
    renderProducts();
    updatePagination();

    const foundLabel = `${state.totalProducts} product${state.totalProducts === 1 ? "" : "s"} found`;
    showStatus(status, foundLabel);
  } catch (error) {
    grid.innerHTML = `<div class="empty-state">Unable to load products. Please check the backend connection.</div>`;
    showStatus(status, error.message, "error");
    updatePagination();
  }
}

function renderProducts() {
  const grid = qs("#products-grid");
  if (!grid) return;

  clearProductCardSliders();

  if (!state.products.length) {
    grid.innerHTML = `<div class="empty-state">No products found for the selected filters.</div>`;
    return;
  }

  grid.innerHTML = state.products.map(product => {
    const selected = isProductSelected(product.product_id);
    const isOut = product.stock_status === "out_of_stock";
    const images = Array.isArray(product.images) && product.images.length
      ? product.images
      : [productImage(product)];
    const price = product.price_on_request || product.price === null || product.price === undefined
      ? "Price on request"
      : `₹ ${Number(product.price).toLocaleString("en-IN")}`;
    return `
      <article class="product-card ${selected ? "selected" : ""}">
        <button class="product-media" type="button" data-view="${escapeHtml(product.product_id)}" aria-label="View ${escapeHtml(product.title)}" ${images.length > 1 ? "data-card-slider" : ""}>
          ${images.length > 1 ? `
            <div class="product-media-track" data-slider-track>
              ${images.map((image, imageIndex) => `
                <span class="product-media-slide">
                  <img src="${escapeHtml(image)}" alt="${escapeHtml(product.title)} image ${imageIndex + 1}" loading="lazy" />
                </span>
              `).join("")}
            </div>
            <div class="product-media-dots" aria-hidden="true">
              ${images.map((_, imageIndex) => `
                <span class="product-media-dot ${imageIndex === 0 ? "active" : ""}"></span>
              `).join("")}
            </div>
          ` : `
            <img src="${escapeHtml(images[0])}" alt="${escapeHtml(product.title)}" loading="lazy" />
          `}
          <div class="product-badges">
            <span class="badge">${escapeHtml(product.category)}</span>
            <span class="badge">${escapeHtml(product.purity)}</span>
          </div>
        </button>
        <div class="product-body">
          <div>
            <h2 class="product-title">${escapeHtml(product.title)}</h2>
            <div class="product-meta">
              <span class="pill">${escapeHtml(product.product_id)}</span>
              <span class="pill">${escapeHtml(stockLabel(product.metal))}</span>
              <span class="pill">${Number(product.weight).toLocaleString("en-IN")} g</span>
              <span class="pill">${escapeHtml(stockLabel(product.stock_status))}</span>
            </div>
          </div>
          <p class="product-desc">${escapeHtml(truncate(product.description, 150))}</p>
          <div class="tag-row">
            <span class="tag">${escapeHtml(price)}</span>
            ${(product.tags || []).slice(0, 3).map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}
          </div>
          <div class="product-actions">
            <button type="button" class="btn-quiet" data-view="${escapeHtml(product.product_id)}">View</button>
            <button type="button" class="btn-secondary select-btn ${selected ? "is-selected" : ""}" data-select="${escapeHtml(product.product_id)}" ${isOut ? "disabled" : ""}>
              ${isOut ? "Unavailable" : selected ? "Selected" : "Select"}
            </button>
          </div>
        </div>
      </article>
    `;
  }).join("");

  qsa("[data-view]", grid).forEach(button => {
    button.addEventListener("click", () => openProductModal(button.dataset.view));
  });

  qsa("[data-select]", grid).forEach(button => {
    button.addEventListener("click", () => toggleProductSelection(button.dataset.select));
  });

  initProductCardSliders(grid);
}

function truncate(value, maxLength) {
  const text = String(value || "");
  return text.length > maxLength ? `${text.slice(0, Math.max(0, maxLength - 3))}...` : text;
}

function findProduct(productId) {
  return state.products.find(product => product.product_id === productId)
    || state.selected.find(product => product.product_id === productId);
}

function isProductSelected(productId) {
  return state.selected.some(item => item.product_id === productId);
}

function toggleProductSelection(productId) {
  const product = findProduct(productId);
  if (!product) return;

  if (isProductSelected(productId)) {
    state.selected = state.selected.filter(item => item.product_id !== productId);
  } else {
    if (state.selected.length >= MAX_SELECTED_PRODUCTS) {
      showStatus(qs("#catalog-status"), `You can select up to ${MAX_SELECTED_PRODUCTS} products in one order.`, "error");
      return;
    }
    state.selected.push({
      product_id: product.product_id,
      title: product.title,
      image: productImage(product),
      quantity: 1,
    });
  }

  saveSelectedProducts();
  renderProducts();
  if (state.modalProduct?.product_id === productId) updateModalSelectButtons();
}

function clearSelection() {
  state.selected = [];
  saveSelectedProducts();
  renderProducts();
  renderSelectedList();
}

function updateSelectedQuantity(productId, delta) {
  const item = state.selected.find(entry => entry.product_id === productId);
  if (!item) return;
  const nextQuantity = Math.min(Math.max((Number(item.quantity) || 1) + delta, 1), 999);
  item.quantity = nextQuantity;
  saveSelectedProducts();
  renderSelectedList();
}

function removeSelectedProduct(productId) {
  state.selected = state.selected.filter(item => item.product_id !== productId);
  saveSelectedProducts();
  renderProducts();
  renderSelectedList();
}

function updateSelectionUI() {
  const count = selectedCount();
  const orderBar = qs("#order-bar");
  const selectedSummary = qs("#selected-summary");
  const title = qs("#order-bar-title");

  if (selectedSummary) {
    selectedSummary.textContent = `${count} selected`;
  }

  if (title) {
    title.textContent = `${count} product${count === 1 ? "" : "s"} selected`;
  }

  if (orderBar) {
    orderBar.classList.toggle("open", count > 0);
  }
}

function updatePagination() {
  const totalPages = Math.max(1, Math.ceil(state.totalProducts / state.pageSize));
  const info = qs("#products-page-info");
  const prev = qs("#products-prev");
  const next = qs("#products-next");

  if (state.currentPage > totalPages) state.currentPage = totalPages;
  if (info) info.textContent = `Page ${state.currentPage} of ${totalPages}`;
  if (prev) prev.disabled = state.currentPage <= 1;
  if (next) next.disabled = state.currentPage >= totalPages || state.totalProducts === 0;
}

function openProductModal(productId) {
  const product = findProduct(productId);
  if (!product || !qs("#product-modal")) return;
  state.modalProduct = product;

  qs("#modal-product-title").textContent = product.title;
  qs("#modal-description").textContent = product.description || "";
  qs("#modal-product-id").textContent = product.product_id;
  qs("#modal-category").textContent = product.category || "-";
  qs("#modal-metal").textContent = stockLabel(product.metal);
  qs("#modal-purity").textContent = product.purity || "-";
  qs("#modal-weight").textContent = `${Number(product.weight || 0).toLocaleString("en-IN")} g`;
  qs("#modal-stock").textContent = stockLabel(product.stock_status);
  qs("#modal-tags").innerHTML = (product.tags || []).map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join("");

  const images = product.images || [product.image].filter(Boolean);
  qs("#modal-main-image").src = images[0] || productImage(product);
  qs("#modal-main-image").alt = product.title;
  qs("#modal-thumbs").innerHTML = images.map(image => `
    <button type="button" class="modal-thumb" data-modal-image="${escapeHtml(image)}">
      <img src="${escapeHtml(image)}" alt="${escapeHtml(product.title)}" />
    </button>
  `).join("");
  qsa("[data-modal-image]", qs("#modal-thumbs")).forEach(button => {
    button.addEventListener("click", () => {
      qs("#modal-main-image").src = button.dataset.modalImage;
    });
  });

  updateModalSelectButtons();
  openModal("product-modal");
}

function updateModalSelectButtons() {
  const selectButton = qs("#modal-select-btn");
  const orderButton = qs("#modal-order-btn");
  if (!state.modalProduct || !selectButton || !orderButton) return;
  const isOut = state.modalProduct.stock_status === "out_of_stock";
  const selected = isProductSelected(state.modalProduct.product_id);
  selectButton.disabled = isOut;
  orderButton.disabled = isOut;
  selectButton.textContent = isOut ? "Unavailable" : selected ? "Remove Selection" : "Select Product";
}

function renderSelectedList() {
  const list = qs("#selected-list");
  if (!list) return;
  if (!state.selected.length) {
    list.innerHTML = `<div class="empty-state">No products selected.</div>`;
    return;
  }
  list.innerHTML = state.selected.map(item => `
    <div class="selected-item">
      <img src="${escapeHtml(item.image || "assets/images/demo (2).jpg")}" alt="${escapeHtml(item.title)}" />
      <div>
        <strong>${escapeHtml(item.title)}</strong>
        <small>${escapeHtml(item.product_id)}</small>
      </div>
      <div class="qty-actions">
        <button type="button" data-qty-minus="${escapeHtml(item.product_id)}" aria-label="Decrease quantity">-</button>
        <span>${Number(item.quantity) || 1}</span>
        <button type="button" data-qty-plus="${escapeHtml(item.product_id)}" aria-label="Increase quantity">+</button>
        <button type="button" data-remove-selected="${escapeHtml(item.product_id)}" aria-label="Remove product">&times;</button>
      </div>
    </div>
  `).join("");

  qsa("[data-qty-minus]", list).forEach(button => {
    button.addEventListener("click", () => updateSelectedQuantity(button.dataset.qtyMinus, -1));
  });
  qsa("[data-qty-plus]", list).forEach(button => {
    button.addEventListener("click", () => updateSelectedQuantity(button.dataset.qtyPlus, 1));
  });
  qsa("[data-remove-selected]", list).forEach(button => {
    button.addEventListener("click", () => removeSelectedProduct(button.dataset.removeSelected));
  });
}

function openContactModal(mode) {
  const modal = qs("#contact-modal");
  if (!modal) return;
  state.contactMode = mode;
  showStatus(qs("#contact-status"), "");

  const title = qs("#contact-modal-title");
  const phoneLabel = qs("#contact-phone-label");
  const phoneInput = qs("#contact-phone");
  const submit = qs("#contact-submit-btn");

  if (mode === "custom") {
    if (title) title.textContent = "Open Whatsapp";
    if (phoneLabel) phoneLabel.textContent = "Number (required)";
    if (phoneInput) phoneInput.required = true;
    if (submit) submit.textContent = "Save Request and Open Whatsapp";
  } else {
    if (title) title.textContent = "Order on Whatsapp";
    if (phoneLabel) phoneLabel.textContent = "Number (optional)";
    if (phoneInput) phoneInput.required = false;
    if (submit) submit.textContent = "Continue and Open Whatsapp";
    renderSelectedList();
  }

  openModal("contact-modal");
}

async function submitCatalogOrder(name, phone) {
  const status = qs("#contact-status");
  if (!state.selected.length) {
    showStatus(status, "Select at least one product before ordering.", "error");
    return;
  }
  if (phone && phone.length < 8) {
    showStatus(status, "Enter a valid number or leave the number blank.", "error");
    return;
  }

  const button = qs("#contact-submit-btn");
  const stopLoading = setButtonLoading(button, "Saving Order...");
  try {
    const fallbackWhatsappUrl = buildCatalogWhatsappUrl(state.selected);
    const response = await api("/orders", {
      method: "POST",
      body: {
        customer_name: name || null,
        phone: phone || null,
        inquiry_source: "website",
        products: state.selected.map(item => ({
          product_id: item.product_id,
          quantity: Number(item.quantity) || 1,
        })),
      },
    });
    showStatus(status, `Order ${response.inquiry_id} saved. Opening WhatsApp...`, "success");
    clearSelection();
    setTimeout(() => {
      window.location.href = response.whatsapp_url || fallbackWhatsappUrl;
    }, 450);
  } catch (error) {
    showStatus(status, error.message, "error");
  } finally {
    stopLoading();
  }
}

function buildCatalogWhatsappUrl(products) {
  const lines = ["Hey,", "", "I wanted to buy"];
  products.forEach(item => {
    const suffix = Number(item.quantity) > 1 ? ` x${Number(item.quantity)}` : "";
    lines.push(`${item.title}${suffix} - ${item.product_id}`);
  });
  lines.push("", "", "Thank you!");
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(lines.join("\n"))}`;
}

async function uploadFiles(endpoint, files) {
  if (!files || !files.length) return [];
  const formData = new FormData();
  Array.from(files).forEach(file => formData.append("files", file));
  const data = await api(endpoint, { method: "POST", body: formData });
  return data.urls || [];
}

function collectCustomPayload() {
  return {
    customer_name: qs("#custom-name")?.value.trim() || "",
    phone: cleanPhone(qs("#custom-phone")?.value || ""),
    city: qs("#custom-city")?.value.trim() || "",
    jewelry_type: qs("#custom-type")?.value.trim() || "",
    budget: qs("#custom-budget")?.value.trim() || "",
    description: qs("#custom-description")?.value.trim() || "",
    purity: qs("#custom-purity")?.value.trim() || "",
    email: qs("#custom-email")?.value.trim() || null,
    inquiry_source: "custom_order",
  };
}

async function submitCustomRequest() {
  const pageStatus = qs("#custom-status");
  const files = qs("#custom-images")?.files;
  const payload = collectCustomPayload();

  if (!payload.customer_name || payload.customer_name.length < 2) {
    showStatus(pageStatus, "Enter your name for the custom order request.", "error");
    return;
  }
  if (!payload.phone || payload.phone.length < 8) {
    showStatus(pageStatus, "Enter a valid mobile number for the custom order request.", "error");
    return;
  }
  if (!files || !files.length) {
    showStatus(pageStatus, "Upload at least one jewelry reference image.", "error");
    return;
  }

  const button = qs("#custom-review-btn");
  const stopLoading = setButtonLoading(button, "Saving Request...");
  showStatus(pageStatus, "Uploading images and saving request...");

  try {
    const imageUrls = await uploadFiles("/uploads/custom-request-images", files);
    const response = await api("/custom-requests", {
      method: "POST",
      body: {
        ...payload,
        customer_name: payload.customer_name || null,
        phone: payload.phone,
        image_urls: imageUrls,
      },
    });
    showStatus(pageStatus, `Custom request ${response.request_id} saved. Opening WhatsApp...`, "success");
    resetCustomForm();
    setTimeout(() => {
      window.location.href = response.whatsapp_url || `https://wa.me/${WHATSAPP_NUMBER}`;
    }, 450);
  } catch (error) {
    showStatus(pageStatus, error.message, "error");
  } finally {
    stopLoading();
  }
}

function resetCustomForm() {
  qs("#custom-order-form")?.reset();
  const preview = qs("#custom-image-preview");
  if (preview) preview.innerHTML = "";
}

function initProductPage() {
  if (!qs("#products-grid")) return;

  qs("#open-filters-btn")?.addEventListener("click", () => openModal("filters-modal"));

  qs("#catalog-filters")?.addEventListener("submit", event => {
    event.preventDefault();
    loadProducts(1);
    closeModal("filters-modal");
  });

  qs("#filters-reset")?.addEventListener("click", () => {
    qs("#catalog-filters")?.reset();
    loadProducts(1);
  });

  qs("#products-prev")?.addEventListener("click", () => {
    if (state.currentPage > 1) loadProducts(state.currentPage - 1);
  });

  qs("#products-next")?.addEventListener("click", () => {
    const totalPages = Math.max(1, Math.ceil(state.totalProducts / state.pageSize));
    if (state.currentPage < totalPages) loadProducts(state.currentPage + 1);
  });

  qs("#clear-selection-btn")?.addEventListener("click", clearSelection);
  qs("#open-order-modal-btn")?.addEventListener("click", () => openContactModal("catalog"));

  qs("#modal-select-btn")?.addEventListener("click", () => {
    if (!state.modalProduct) return;
    toggleProductSelection(state.modalProduct.product_id);
    updateModalSelectButtons();
  });

  qs("#modal-order-btn")?.addEventListener("click", () => {
    if (!state.modalProduct) return;
    if (!isProductSelected(state.modalProduct.product_id)) {
      toggleProductSelection(state.modalProduct.product_id);
    }
    closeModal("product-modal");
    openContactModal("catalog");
  });

  updateSelectionUI();
  loadProducts(1);
}

function initCustomPage() {
  const form = qs("#custom-order-form");
  if (!form) return;

  qs("#custom-images")?.addEventListener("change", event => {
    const preview = qs("#custom-image-preview");
    if (!preview) return;
    preview.innerHTML = "";
    Array.from(event.target.files || []).forEach(file => {
      const img = document.createElement("img");
      img.src = URL.createObjectURL(file);
      img.alt = file.name;
      preview.appendChild(img);
    });
  });

  form.addEventListener("submit", event => {
    event.preventDefault();
    showStatus(qs("#custom-status"), "");
    submitCustomRequest();
  });
}

function initContactForm() {
  qs("#contact-form")?.addEventListener("submit", event => {
    event.preventDefault();
    const name = qs("#contact-name")?.value.trim() || "";
    const phone = cleanPhone(qs("#contact-phone")?.value || "");
    if (state.contactMode === "custom") {
      submitCustomRequest();
    } else {
      submitCatalogOrder(name, phone);
    }
  });
}

function boot() {
  setupNav();
  setupReveal();
  setupModals();
  setupChatWidget();
  initContactForm();
  initProductPage();
  initCustomPage();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", boot);
} else {
  boot();
}
