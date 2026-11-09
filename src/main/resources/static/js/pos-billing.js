/**
 * Showroom POS & Billing Counter
 * --------------------------------
 * Three independent product-selection methods that all add to the SAME billing cart:
 *   1. Camera barcode scanning (laptop webcam / mobile camera) via html5-qrcode
 *   2. Manual barcode entry (also works with USB / Bluetooth keyboard-wedge scanners)
 *   3. Product search (name, brand, model, product code, barcode)
 *
 * Products are always looked up from the existing inventory through
 * /sales/api/products/* — nothing is created or duplicated client-side.
 */
(function () {
    'use strict';

    const form = document.getElementById('billingForm');
    if (!form) return;

    const API_BASE = form.getAttribute('data-api-base') || '/sales/api/products';
    const tbody = document.getElementById('itemsBody');
    const rowTemplate = document.getElementById('itemRowTemplate');
    const feedback = document.getElementById('posFeedback');

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    function formatINR(value) {
        const n = Number(value) || 0;
        const hasFraction = Math.round(n * 100) % 100 !== 0;
        return '₹' + n.toLocaleString('en-IN', {
            minimumFractionDigits: hasFraction ? 2 : 0,
            maximumFractionDigits: 2
        });
    }

    function escapeHtml(str) {
        return String(str == null ? '' : str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    let audioCtx = null;
    function beep(ok) {
        try {
            audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            const duration = ok ? 0.12 : 0.3;
            osc.type = ok ? 'sine' : 'square';
            osc.frequency.value = ok ? 1046 : 220;
            gain.gain.setValueAtTime(0.06, audioCtx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + duration);
            osc.connect(gain).connect(audioCtx.destination);
            osc.start();
            osc.stop(audioCtx.currentTime + duration);
        } catch (e) { /* audio not available — ignore */ }
    }

    const ICONS = {
        success: 'bi-check-circle-fill',
        danger: 'bi-x-octagon-fill',
        warning: 'bi-exclamation-triangle-fill',
        info: 'bi-info-circle-fill'
    };

    function notify(type, html, timeout) {
        const el = document.createElement('div');
        el.className = 'alert alert-' + type + ' pos-alert d-flex align-items-start gap-2 mb-2';
        el.setAttribute('role', type === 'danger' ? 'alert' : 'status');
        el.innerHTML = '<i class="bi ' + (ICONS[type] || ICONS.info) + ' mt-1"></i>' +
            '<div class="flex-grow-1">' + html + '</div>' +
            '<button type="button" class="btn-close btn-sm" aria-label="Close"></button>';
        el.querySelector('.btn-close').addEventListener('click', () => el.remove());
        feedback.prepend(el);
        // Keep at most 3 messages visible
        while (feedback.children.length > 3) feedback.lastElementChild.remove();
        setTimeout(() => {
            el.classList.add('pos-alert-hide');
            setTimeout(() => el.remove(), 300);
        }, timeout || (type === 'danger' || type === 'warning' ? 6000 : 3500));
    }

    // ------------------------------------------------------------------
    // Cart rows
    // ------------------------------------------------------------------
    function rows() {
        return Array.from(tbody.querySelectorAll('.item-row'));
    }

    function reindexRows() {
        rows().forEach((row, index) => {
            row.querySelector('.product-select').name = 'items[' + index + '].productId';
            row.querySelector('.qty-input').name = 'items[' + index + '].quantity';
            row.querySelector('.price-input').name = 'items[' + index + '].unitPrice';
        });
    }

    function createRow() {
        const fragment = rowTemplate.content.cloneNode(true);
        const row = fragment.querySelector('.item-row');
        tbody.appendChild(fragment);
        reindexRows();
        return row;
    }

    function resetRow(row) {
        row.querySelector('.product-select').value = '';
        row.querySelector('.qty-input').value = '1';
        row.querySelector('.qty-input').removeAttribute('max');
        row.querySelector('.price-input').value = '';
        row.querySelector('.stock-indicator').innerHTML = '';
        row.querySelector('.item-total').textContent = formatINR(0);
        row.classList.remove('has-product');
    }

    function findRowByProduct(productId, exceptRow) {
        return rows().find(r => r !== exceptRow && r.querySelector('.product-select').value === String(productId));
    }

    function findEmptyRow() {
        return rows().find(r => !r.querySelector('.product-select').value);
    }

    function flashRow(row) {
        row.classList.remove('pos-row-flash');
        void row.offsetWidth; // restart animation
        row.classList.add('pos-row-flash');
        row.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function getOption(select, productId) {
        return Array.from(select.options).find(o => o.value === String(productId));
    }

    function stockOf(row) {
        const select = row.querySelector('.product-select');
        const opt = select.options[select.selectedIndex];
        const stock = opt ? opt.getAttribute('data-stock') : null;
        return stock !== null && stock !== '' ? parseInt(stock, 10) : null;
    }

    function updateQtyButtons(row) {
        const qtyInput = row.querySelector('.qty-input');
        const minusBtn = row.querySelector('.pos-qty-minus');
        const plusBtn = row.querySelector('.pos-qty-plus');
        const qty = parseInt(qtyInput.value, 10) || 1;
        const stock = stockOf(row);
        if (minusBtn) minusBtn.disabled = qty <= 1;
        if (plusBtn) plusBtn.disabled = (stock !== null && row.querySelector('.product-select').value && qty >= stock);
    }

    function renderStockIndicator(row) {
        const select = row.querySelector('.product-select');
        const opt = select.options[select.selectedIndex];
        const indicator = row.querySelector('.stock-indicator');
        const qtyInput = row.querySelector('.qty-input');
        if (!select.value || !opt) {
            indicator.innerHTML = '';
            qtyInput.removeAttribute('max');
            updateQtyButtons(row);
            return;
        }
        const stock = parseInt(opt.getAttribute('data-stock'), 10);
        const name = opt.getAttribute('data-name') || '';
        const brand = opt.getAttribute('data-brand') || '';
        const model = opt.getAttribute('data-model') || '';
        const code = opt.getAttribute('data-code') || '';
        const barcode = opt.getAttribute('data-barcode') || '';

        const metaParts = [brand, model].filter(Boolean).map(escapeHtml).join(' · ');
        let html = '';
        if (name) {
            html += '<div class="pos-selected-title">' + escapeHtml(name) +
                (metaParts ? ' <span class="pos-selected-sub">(' + metaParts + ')</span>' : '') + '</div>';
        }
        html += '<div class="pos-chip-row">';
        if (code) {
            html += '<span class="pos-chip"><i class="bi bi-tag"></i> ' + escapeHtml(code) + '</span>';
        }
        if (barcode && barcode !== code) {
            html += '<span class="pos-chip"><i class="bi bi-upc"></i> ' + escapeHtml(barcode) + '</span>';
        }
        if (!isNaN(stock)) {
            if (stock <= 0) {
                html += '<span class="pos-chip pos-chip-danger"><i class="bi bi-x-circle"></i> Out of stock</span>';
                qtyInput.max = 0;
            } else {
                const low = stock <= 3 ? ' pos-chip-warning' : ' pos-chip-success';
                html += '<span class="pos-chip' + low + '"><i class="bi bi-box-seam"></i> Available: <strong>' + stock + '</strong></span>';
                qtyInput.max = stock;
            }
        }
        html += '</div>';
        indicator.innerHTML = html;
        updateQtyButtons(row);
    }

    /** Called when a product is chosen from a row dropdown (existing behaviour, now duplicate-safe). */
    window.handleProductSelect = function (selectElem) {
        const row = selectElem.closest('.item-row');
        const productId = selectElem.value;

        // Prevent duplicate cart lines: merge into the existing row instead
        if (productId) {
            const existing = findRowByProduct(productId, row);
            if (existing) {
                const name = selectElem.options[selectElem.selectedIndex].text.split(' - Stock:')[0];
                resetRow(row);
                if (rows().length > 1) {
                    row.remove();
                    reindexRows();
                }
                incrementRow(existing, name);
                updateGrandTotal();
                return;
            }
        }

        const opt = selectElem.options[selectElem.selectedIndex];
        const price = opt ? opt.getAttribute('data-price') : null;
        const priceInput = row.querySelector('.price-input');
        if (productId && price) {
            priceInput.value = price;
        }
        if (!productId) {
            priceInput.value = '';
        }
        row.classList.toggle('has-product', !!productId);
        renderStockIndicator(row);
        calculateRow(priceInput);
    };

    window.calculateRow = function (inputElem) {
        const row = inputElem.closest('.item-row');
        const qtyInput = row.querySelector('.qty-input');
        let qty = parseFloat(qtyInput.value);
        if (isNaN(qty) || qty < 0) qty = 0;
        const price = parseFloat(row.querySelector('.price-input').value) || 0;
        row.querySelector('.item-total').textContent = formatINR(qty * price);
        updateQtyButtons(row);
        updateGrandTotal();
    };

    window.clampQty = function (inputElem) {
        const row = inputElem.closest('.item-row');
        let qty = parseInt(inputElem.value, 10);
        const stock = stockOf(row);
        if (isNaN(qty) || qty < 1) qty = 1;
        if (stock !== null && row.querySelector('.product-select').value && qty > stock) {
            notify('warning', 'Only <strong>' + stock + '</strong> unit(s) available in stock for this product.');
            qty = Math.max(stock, 1);
        }
        inputElem.value = qty;
        calculateRow(inputElem);
    };

    window.stepQty = function (btn, delta) {
        const row = btn.closest('.item-row');
        const input = row.querySelector('.qty-input');
        const current = parseInt(input.value, 10) || 1;
        input.value = Math.max(1, current + delta);
        clampQty(input);
    };

    window.updateGrandTotal = function () {
        let subtotal = 0, items = 0, units = 0;
        rows().forEach(row => {
            const qty = parseFloat(row.querySelector('.qty-input').value) || 0;
            const price = parseFloat(row.querySelector('.price-input').value) || 0;
            if (row.querySelector('.product-select').value) {
                subtotal += qty * price;
                items += 1;
                units += qty;
            }
        });

        const discount = parseFloat(document.getElementById('discountInput').value) || 0;
        let net = subtotal - discount;
        if (net < 0) net = 0;

        document.getElementById('subtotalDisplay').textContent = formatINR(subtotal);
        document.getElementById('netAmountDisplay').textContent = formatINR(net);
        document.getElementById('itemsUnitsDisplay').textContent = items + ' / ' + units;
        document.getElementById('cartCountBadge').textContent = items + (items === 1 ? ' item' : ' items');

        const tableSubtotalEl = document.getElementById('tableSubtotalDisplay');
        if (tableSubtotalEl) tableSubtotalEl.textContent = formatINR(subtotal);

        const tableSummaryEl = document.getElementById('tableSummaryText');
        if (tableSummaryEl) {
            tableSummaryEl.innerHTML = items > 0
                ? '<strong>' + items + '</strong> product(s) · <strong>' + units + '</strong> total unit(s) in billing cart'
                : 'No products selected yet — scan barcode, enter barcode, search, or choose a product above.';
        }

        const clearCartBtn = document.getElementById('clearCartBtn');
        if (clearCartBtn) clearCartBtn.classList.toggle('d-none', items === 0);
    };

    window.removeRow = function (btn) {
        const row = btn.closest('.item-row');
        if (rows().length > 1) {
            row.remove();
            reindexRows();
        } else {
            resetRow(row); // keep one empty line available
        }
        updateGrandTotal();
    };

    function incrementRow(row, productName) {
        const input = row.querySelector('.qty-input');
        const stock = stockOf(row);
        const current = parseInt(input.value, 10) || 0;
        if (stock !== null && current + 1 > stock) {
            beep(false);
            notify('warning', '<strong>' + escapeHtml(productName) + '</strong> — only ' + stock +
                ' unit(s) in stock. Quantity cannot be increased further.');
            flashRow(row);
            return false;
        }
        input.value = current + 1;
        calculateRow(input);
        flashRow(row);
        beep(true);
        notify('success', '<strong>' + escapeHtml(productName) + '</strong> is already in the cart — quantity increased to <strong>' + (current + 1) + '</strong>.');
        return true;
    }

    /**
     * Single entry point used by ALL three selection methods.
     * @param product PosProductDTO from the server ({id, name, brand, sellingPrice, quantity, ...})
     * @param source  'camera' | 'manual' | 'search'
     */
    function addProductToCart(product, source) {
        const label = product.name + (product.brand ? ' (' + product.brand + ')' : '');
        const templateSelect = rowTemplate.content.querySelector('.product-select');
        if (!getOption(templateSelect, product.id)) {
            beep(false);
            notify('danger', '<strong>' + escapeHtml(label) + '</strong> is not available for billing (inactive product). Refresh the page if it was recently activated.');
            return false;
        }

        // Refresh stock on all dropdown options with the latest value from the server
        if (typeof product.quantity === 'number') {
            document.querySelectorAll('.product-select option[value="' + product.id + '"]').forEach(o => {
                o.setAttribute('data-stock', product.quantity);
            });
            const tplOpt = getOption(templateSelect, product.id);
            if (tplOpt) tplOpt.setAttribute('data-stock', product.quantity);
        }

        if (product.quantity <= 0) {
            beep(false);
            notify('danger', '<strong>' + escapeHtml(label) + '</strong> is <strong>out of stock</strong> and cannot be billed.');
            return false;
        }

        const existing = findRowByProduct(product.id);
        if (existing) {
            renderStockIndicator(existing);
            return incrementRow(existing, label);
        }

        const row = findEmptyRow() || createRow();
        const select = row.querySelector('.product-select');
        select.value = String(product.id);
        row.querySelector('.qty-input').value = '1';
        window.handleProductSelect(select);
        flashRow(row);
        beep(true);

        const via = source === 'camera' ? 'Scanned' : source === 'manual' ? 'Barcode matched' : 'Added';
        notify('success', '<strong>' + via + ':</strong> ' + escapeHtml(label) + ' — ' + formatINR(product.sellingPrice) + ' added to cart.');
        return true;
    }

    // ------------------------------------------------------------------
    // Server lookups (existing inventory)
    // ------------------------------------------------------------------
    async function fetchJson(url) {
        const res = await fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
        const type = res.headers.get('content-type') || '';
        if (!type.includes('application/json')) {
            const err = new Error('SESSION');
            err.status = res.status;
            throw err;
        }
        const body = await res.json();
        return { ok: res.ok, status: res.status, body: body };
    }

    function handleLookupError(err) {
        beep(false);
        if (err && err.message === 'SESSION') {
            notify('danger', 'Your session has expired. Please <a href="' + window.location.pathname + '" class="alert-link">reload the page</a> and log in again.');
        } else {
            notify('danger', 'Unable to reach the server. Please check your connection and try again.');
        }
    }

    let lookupInFlight = false;
    async function lookupBarcode(code, source) {
        const value = (code || '').trim();
        if (!value) {
            beep(false);
            notify('warning', 'Please enter a barcode number.');
            return false;
        }
        if (lookupInFlight) return false;
        lookupInFlight = true;
        try {
            const result = await fetchJson(API_BASE + '/barcode?code=' + encodeURIComponent(value));
            if (result.ok) {
                return addProductToCart(result.body, source);
            }
            beep(false);
            if (result.status === 404) {
                notify('danger', '<strong>Product not found.</strong> No product in the inventory matches barcode <code>' +
                    escapeHtml(value) + '</code>. Verify the number, or assign this barcode to the product in <em>Products Catalog</em>.');
            } else {
                notify('danger', escapeHtml((result.body && result.body.message) || 'Invalid barcode.'));
            }
            return false;
        } catch (err) {
            handleLookupError(err);
            return false;
        } finally {
            lookupInFlight = false;
        }
    }

    // ------------------------------------------------------------------
    // Method tabs
    // ------------------------------------------------------------------
    const tabs = Array.from(document.querySelectorAll('.pos-method-tab'));
    const panes = Array.from(document.querySelectorAll('.pos-method-pane'));

    function activateMethod(method) {
        tabs.forEach(t => {
            const active = t.dataset.method === method;
            t.classList.toggle('active', active);
            t.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        panes.forEach(p => p.classList.toggle('active', p.dataset.pane === method));
        if (method !== 'scan') stopScanner(true);
        try { localStorage.setItem('pos.method', method); } catch (e) { /* ignore */ }
        if (method === 'manual') setTimeout(() => manualInput.focus(), 50);
        if (method === 'search') setTimeout(() => searchInput.focus(), 50);
    }

    tabs.forEach(t => t.addEventListener('click', () => activateMethod(t.dataset.method)));

    // ------------------------------------------------------------------
    // Method 1: Camera barcode scanner
    // ------------------------------------------------------------------
    const startBtn = document.getElementById('startScanBtn');
    const stopBtn = document.getElementById('stopScanBtn');
    const cameraSelect = document.getElementById('cameraSelect');
    const scannerWrap = document.getElementById('scannerWrap');
    const scanStatus = document.getElementById('scanStatus');

    let scanner = null;
    let scanning = false;
    let starting = false;
    let scanLocked = false;
    let lastCode = null;
    let lastCodeAt = 0;

    function setScanStatus(type, html) {
        scanStatus.className = 'pos-scan-status pos-scan-' + type;
        scanStatus.innerHTML = html;
    }

    function supportedFormats() {
        const F = window.Html5QrcodeSupportedFormats;
        if (!F) return undefined;
        return [F.EAN_13, F.EAN_8, F.UPC_A, F.UPC_E, F.UPC_EAN_EXTENSION, F.CODE_128, F.CODE_39,
            F.CODE_93, F.ITF, F.CODABAR, F.QR_CODE].filter(v => v !== undefined);
    }

    function cameraErrorMessage(err) {
        const text = String((err && (err.name || err.message)) || err || '');
        if (/NotAllowed|Permission|denied/i.test(text)) {
            return '<strong>Camera permission denied.</strong> Allow camera access for this site in your browser settings (click the camera / lock icon in the address bar), then press <em>Start Camera</em> again.';
        }
        if (/NotFound|Requested device not found|no camera/i.test(text)) {
            return '<strong>No camera found.</strong> Connect a webcam, or use <em>Enter Barcode</em> / <em>Search Product</em> instead.';
        }
        if (/NotReadable|Could not start video|TrackStart|in use/i.test(text)) {
            return '<strong>Camera is busy.</strong> Close other apps or tabs using the camera and try again.';
        }
        if (/Overconstrained/i.test(text)) {
            return '<strong>Selected camera is not available.</strong> Choose another camera from the list.';
        }
        return '<strong>Unable to start the camera.</strong> ' + escapeHtml(text || 'Unknown error.');
    }

    async function populateCameras() {
        try {
            const cams = await window.Html5Qrcode.getCameras();
            if (!cams || cams.length < 2) {
                cameraSelect.classList.add('d-none');
                return;
            }
            const current = cameraSelect.value;
            cameraSelect.innerHTML = cams.map((c, i) =>
                '<option value="' + escapeHtml(c.id) + '">' + escapeHtml(c.label || ('Camera ' + (i + 1))) + '</option>').join('');
            if (current && cams.some(c => c.id === current)) {
                cameraSelect.value = current;
            } else {
                // Prefer the rear camera on phones
                const back = cams.find(c => /back|rear|environment/i.test(c.label || ''));
                if (back) cameraSelect.value = back.id;
            }
            cameraSelect.classList.remove('d-none');
        } catch (e) {
            cameraSelect.classList.add('d-none');
        }
    }

    async function startScanner(cameraId) {
        if (scanning || starting) return;

        if (!window.isSecureContext) {
            setScanStatus('error', '<i class="bi bi-shield-lock me-1"></i><strong>Camera requires a secure connection.</strong> ' +
                'Browsers only allow camera access on <code>https://</code> or <code>localhost</code>. Open this page over HTTPS on mobile devices, ' +
                'or use <em>Enter Barcode</em> / <em>Search Product</em>.');
            beep(false);
            return;
        }
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            setScanStatus('error', '<i class="bi bi-exclamation-triangle me-1"></i><strong>This browser does not support camera access.</strong> Please use a recent version of Chrome, Edge, Safari or Firefox.');
            return;
        }
        if (typeof window.Html5Qrcode === 'undefined') {
            setScanStatus('error', '<i class="bi bi-exclamation-triangle me-1"></i><strong>Scanner component failed to load.</strong> Reload the page and try again.');
            return;
        }

        starting = true;
        startBtn.disabled = true;
        scannerWrap.classList.remove('d-none');
        setScanStatus('info', '<span class="spinner-border spinner-border-sm me-2"></span>Requesting camera permission…');

        try {
            if (!scanner) {
                const config = { verbose: false, experimentalFeatures: { useBarCodeDetectorIfSupported: true } };
                const formats = supportedFormats();
                if (formats) config.formatsToSupport = formats;
                scanner = new window.Html5Qrcode('posReader', config);
            }
            const camera = cameraId ? cameraId : { facingMode: 'environment' };
            await scanner.start(camera, {
                fps: 12,
                qrbox: function (w, h) {
                    const width = Math.floor(Math.min(w * 0.85, 420));
                    const height = Math.floor(Math.min(h * 0.5, 200));
                    return { width: Math.max(width, 150), height: Math.max(height, 90) };
                }
            }, onScanSuccess, function () { /* per-frame decode misses are expected */ });

            scanning = true;
            startBtn.classList.add('d-none');
            stopBtn.classList.remove('d-none');
            setScanStatus('live', '<span class="pos-live-dot"></span>Camera active — align the barcode inside the frame. Each successful scan adds the product to the cart.');
            populateCameras();
        } catch (err) {
            scannerWrap.classList.add('d-none');
            setScanStatus('error', '<i class="bi bi-camera-video-off me-1"></i>' + cameraErrorMessage(err));
            beep(false);
        } finally {
            starting = false;
            startBtn.disabled = false;
        }
    }

    async function stopScanner(silent) {
        if (!scanner || !scanning) {
            if (!silent) scannerWrap.classList.add('d-none');
            return;
        }
        try {
            await scanner.stop();
            scanner.clear();
        } catch (e) { /* already stopped */ }
        scanning = false;
        scanLocked = false;
        scannerWrap.classList.add('d-none');
        stopBtn.classList.add('d-none');
        startBtn.classList.remove('d-none');
        setScanStatus('info', '<i class="bi bi-info-circle me-1"></i>Camera closed. Click <strong>Start Camera</strong> to scan again.');
    }

    async function onScanSuccess(decodedText) {
        const now = Date.now();
        // Ignore repeated reads of the same physical barcode while it stays in view
        if (scanLocked || (decodedText === lastCode && now - lastCodeAt < 2500)) return;
        scanLocked = true;
        lastCode = decodedText;
        lastCodeAt = now;

        try { scanner.pause(true); } catch (e) { /* ignore */ }
        setScanStatus('info', '<span class="spinner-border spinner-border-sm me-2"></span>Barcode detected: <code>' + escapeHtml(decodedText) + '</code> — identifying product…');

        const added = await lookupBarcode(decodedText, 'camera');
        setScanStatus(added ? 'success' : 'error', added
            ? '<i class="bi bi-check-circle me-1"></i>Last scan: <code>' + escapeHtml(decodedText) + '</code> added. Ready for the next item…'
            : '<i class="bi bi-x-circle me-1"></i>Last scan: <code>' + escapeHtml(decodedText) + '</code> could not be added. Ready to scan again…');

        setTimeout(() => {
            try { if (scanning) scanner.resume(); } catch (e) { /* ignore */ }
            scanLocked = false;
        }, 1200);
    }

    startBtn.addEventListener('click', () => startScanner(cameraSelect.value || null));
    stopBtn.addEventListener('click', () => stopScanner(false));
    cameraSelect.addEventListener('change', async () => {
        if (!scanning) return;
        const id = cameraSelect.value;
        await stopScanner(true);
        startScanner(id);
    });
    window.addEventListener('pagehide', () => stopScanner(true));
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) stopScanner(true);
    });

    // ------------------------------------------------------------------
    // Method 2: Manual barcode entry
    // ------------------------------------------------------------------
    const manualInput = document.getElementById('manualBarcode');
    const manualBtn = document.getElementById('manualBarcodeBtn');

    async function submitManualBarcode() {
        manualBtn.disabled = true;
        const added = await lookupBarcode(manualInput.value, 'manual');
        manualBtn.disabled = false;
        if (added) manualInput.value = '';
        else manualInput.select();
        manualInput.focus();
    }

    manualBtn.addEventListener('click', submitManualBarcode);
    manualInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();
            submitManualBarcode();
        }
    });

    // ------------------------------------------------------------------
    // Method 3: Product search
    // ------------------------------------------------------------------
    const searchInput = document.getElementById('productSearch');
    const searchResults = document.getElementById('searchResults');
    const clearSearchBtn = document.getElementById('clearSearchBtn');
    let searchTimer = null;
    let searchSeq = 0;
    let searchItems = [];
    let activeIndex = -1;

    function closeResults() {
        searchResults.classList.add('d-none');
        searchInput.setAttribute('aria-expanded', 'false');
        activeIndex = -1;
    }

    function renderResults(list, query) {
        searchItems = list;
        activeIndex = list.length ? 0 : -1;
        if (!list.length) {
            searchResults.innerHTML = '<div class="pos-search-empty"><i class="bi bi-emoji-neutral me-1"></i>No products match <strong>' +
                escapeHtml(query) + '</strong>.</div>';
        } else {
            searchResults.innerHTML = list.map((p, i) => {
                const out = p.quantity <= 0;
                const stockCls = out ? 'pos-chip-danger' : (p.quantity <= 3 ? 'pos-chip-warning' : 'pos-chip-success');
                const stockTxt = out ? 'Out of stock' : 'Stock: ' + p.quantity;
                const meta = [p.brand, p.model, p.productCode].filter(Boolean).map(escapeHtml).join(' · ');
                return '<button type="button" class="pos-search-item' + (out ? ' is-disabled' : '') + (i === 0 ? ' active' : '') +
                    '" data-index="' + i + '" role="option">' +
                    '<div class="pos-search-main"><div class="pos-search-name">' + escapeHtml(p.name) + '</div>' +
                    '<div class="pos-search-meta">' + meta + (p.barcode ? ' · <i class="bi bi-upc"></i> ' + escapeHtml(p.barcode) : '') + '</div></div>' +
                    '<div class="pos-search-side"><div class="pos-search-price">' + formatINR(p.sellingPrice) + '</div>' +
                    '<span class="pos-chip ' + stockCls + '">' + stockTxt + '</span></div>' +
                    '</button>';
            }).join('');
        }
        searchResults.classList.remove('d-none');
        searchInput.setAttribute('aria-expanded', 'true');
    }

    function highlight(index) {
        const items = searchResults.querySelectorAll('.pos-search-item');
        items.forEach((el, i) => el.classList.toggle('active', i === index));
        if (items[index]) items[index].scrollIntoView({ block: 'nearest' });
        activeIndex = index;
    }

    async function runSearch() {
        const query = searchInput.value.trim();
        clearSearchBtn.classList.toggle('d-none', !query);
        const seq = ++searchSeq;
        try {
            const result = await fetchJson(API_BASE + '/search?q=' + encodeURIComponent(query));
            if (seq !== searchSeq) return; // a newer search is in progress
            renderResults(result.ok ? result.body : [], query);
        } catch (err) {
            if (seq === searchSeq) {
                closeResults();
                handleLookupError(err);
            }
        }
    }

    function selectResult(index) {
        const product = searchItems[index];
        if (!product) return;
        const added = addProductToCart(product, 'search');
        if (added) {
            searchInput.value = '';
            clearSearchBtn.classList.add('d-none');
            closeResults();
        }
        searchInput.focus();
    }

    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(runSearch, 200);
    });
    searchInput.addEventListener('focus', () => {
        if (searchResults.classList.contains('d-none')) runSearch();
    });
    searchInput.addEventListener('keydown', e => {
        const open = !searchResults.classList.contains('d-none');
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (!open) { runSearch(); return; }
            if (searchItems.length) highlight(Math.min(activeIndex + 1, searchItems.length - 1));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (searchItems.length) highlight(Math.max(activeIndex - 1, 0));
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (open && activeIndex >= 0) selectResult(activeIndex);
        } else if (e.key === 'Escape') {
            closeResults();
        }
    });
    searchResults.addEventListener('mousedown', e => e.preventDefault()); // keep input focus
    searchResults.addEventListener('click', e => {
        const item = e.target.closest('.pos-search-item');
        if (item) selectResult(parseInt(item.dataset.index, 10));
    });
    clearSearchBtn.addEventListener('click', () => {
        searchInput.value = '';
        clearSearchBtn.classList.add('d-none');
        searchInput.focus();
        runSearch();
    });
    document.addEventListener('click', e => {
        if (!e.target.closest('.pos-search-box')) closeResults();
    });

    // ------------------------------------------------------------------
    // Add Another Item & Clear Cart
    // ------------------------------------------------------------------
    document.getElementById('addItemBtn').addEventListener('click', () => {
        const row = createRow();
        flashRow(row);
        row.querySelector('.product-select').focus();
        updateGrandTotal();
    });

    const clearCartBtn = document.getElementById('clearCartBtn');
    if (clearCartBtn) {
        clearCartBtn.addEventListener('click', () => {
            const allRows = rows();
            allRows.slice(1).forEach(r => r.remove());
            if (allRows[0]) resetRow(allRows[0]);
            reindexRows();
            updateGrandTotal();
            notify('info', 'Cart cleared.');
        });
    }

    // ------------------------------------------------------------------
    // Form submission (Complete Sale & Generate Bill)
    // ------------------------------------------------------------------
    // Prevent the Enter key (e.g. from keyboard-wedge barcode scanners) from submitting the bill accidentally
    form.addEventListener('keydown', e => {
        if (e.key === 'Enter' && e.target.tagName === 'INPUT') e.preventDefault();
    });

    const paymentSelect = document.getElementById('paymentMethod');
    paymentSelect.addEventListener('change', () => paymentSelect.classList.remove('is-invalid'));

    form.addEventListener('submit', e => {
        e.preventDefault();

        // Drop empty lines so they do not block the bill
        const filled = rows().filter(r => r.querySelector('.product-select').value);
        if (!filled.length) {
            beep(false);
            notify('danger', 'The cart is empty. Add at least one product using <em>Scan Barcode</em>, <em>Enter Barcode</em> or <em>Search Product</em>.');
            return;
        }
        rows().forEach(r => {
            if (!r.querySelector('.product-select').value) r.remove();
        });
        reindexRows();
        updateGrandTotal();

        if (!paymentSelect.value) {
            paymentSelect.classList.add('is-invalid');
            paymentSelect.focus();
            beep(false);
            notify('warning', 'Please select a <strong>Payment Mode</strong> before completing the sale.');
            return;
        }

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        stopScanner(true);
        const btn = document.getElementById('checkoutBtn');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Generating Bill…';
        form.submit();
    });

    // ------------------------------------------------------------------
    // Init
    // ------------------------------------------------------------------
    document.addEventListener('DOMContentLoaded', init);
    if (document.readyState !== 'loading') init();

    let initialised = false;
    function init() {
        if (initialised) return;
        initialised = true;

        if (!rows().length) createRow();
        reindexRows();
        rows().forEach(row => {
            const select = row.querySelector('.product-select');
            row.classList.toggle('has-product', !!select.value);
            renderStockIndicator(row);
            calculateRow(row.querySelector('.qty-input'));
        });
        updateGrandTotal();

        let method = 'scan';
        try { method = localStorage.getItem('pos.method') || 'scan'; } catch (e) { /* ignore */ }
        if (!['scan', 'manual', 'search'].includes(method)) method = 'scan';
        activateMethod(method);
    }
})();

