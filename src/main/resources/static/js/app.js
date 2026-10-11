/* Akshara Mart - frontend (plain HTML + JS) */
const PH = 'data:image/svg+xml;utf8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="500" height="400"><rect width="500" height="400" fill="#f3ecff"/>' +
  '<text x="250" y="210" font-family="Arial" font-size="28" fill="#6d28d9" text-anchor="middle">Akshara Mart</text></svg>');

let token = localStorage.getItem('am_token');
let user = JSON.parse(localStorage.getItem('am_user') || 'null');
let cartCount = 0;
let loginRole = 'BUYER';
let adminTab = 'users';

const $ = (s) => document.querySelector(s);
const app = () => $('#app');
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const money = (n) => '₹' + Number(n).toLocaleString('en-IN');
const fdate = (d) => new Date(d).toLocaleString('en-IN', {dateStyle: 'medium', timeStyle: 'short'});
const img = (u, cls = '') => `<img class="${cls}" src="${esc(u || PH)}" alt="" loading="lazy" onerror="this.onerror=null;this.src=PH">`;

function toast(msg, err) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'show' + (err ? ' err' : '');
  clearTimeout(toast.h);
  toast.h = setTimeout(() => t.className = '', 2800);
}

async function api(path, method = 'GET', body) {
  const opt = {method, headers: {}};
  if (token) opt.headers['X-Auth-Token'] = token;
  if (body instanceof FormData) opt.body = body;
  else if (body) { opt.headers['Content-Type'] = 'application/json'; opt.body = JSON.stringify(body); }
  let res;
  try { res = await fetch('/api' + path, opt); }
  catch (e) { throw new Error('Cannot reach server. Is the Spring Boot app running?'); }
  let data = null;
  try { data = await res.json(); } catch (e) {}
  if (!res.ok) {
    if (res.status === 401 && token) { logout(true); }
    throw new Error((data && data.message) || 'Something went wrong');
  }
  return data;
}

function saveSession(d) {
  token = d.token; user = d.user;
  localStorage.setItem('am_token', token);
  localStorage.setItem('am_user', JSON.stringify(user));
}
function logout(silent) {
  token = null; user = null; cartCount = 0;
  localStorage.removeItem('am_token'); localStorage.removeItem('am_user');
  renderNav();
  if (!silent) toast('Logged out');
  location.hash = '#/login';
}

async function refreshCount() {
  cartCount = 0;
  if (user && user.role === 'BUYER') {
    try { cartCount = (await api('/cart')).reduce((s, i) => s + i.quantity, 0); } catch (e) {}
  }
  renderNav();
}

function renderNav() {
  let h = '<a href="#/">Home</a>';
  if (!user) h += '<a href="#/login" class="pill">Login</a>';
  else {
    if (user.role === 'BUYER') h += `<a href="#/cart">Cart<span class="badge">${cartCount}</span></a><a href="#/orders">My Orders</a>`;
    if (user.role === 'SELLER') h += '<a href="#/seller">Seller Dashboard</a>';
    if (user.role === 'ADMIN') h += '<a href="#/admin">Admin Panel</a>';
    h += `<a class="pill" href="javascript:void(0)" onclick="logout()">Logout (${esc(user.name.split(' ')[0])})</a>`;
  }
  $('#nav').innerHTML = h;
}

function doSearch(e) {
  e.preventDefault();
  const v = $('#q').value.trim();
  const nh = '#/' + (v ? '?q=' + encodeURIComponent(v) : '');
  if (location.hash === nh) route(); else location.hash = nh;
  return false;
}

/* ---------------- ROUTER ---------------- */
async function route() {
  const h = location.hash.slice(1) || '/';
  const [path, qs] = h.split('?');
  const params = new URLSearchParams(qs || '');
  window.scrollTo(0, 0);
  try {
    if (path === '/') await pageHome(params);
    else if (path === '/login') pageLogin();
    else if (path === '/register') pageRegister();
    else if (path === '/cart') await pageCart();
    else if (path === '/checkout') await pageCheckout();
    else if (path.startsWith('/success/')) pageSuccess(path.split('/')[2]);
    else if (path === '/orders') await pageOrders();
    else if (path === '/seller') await pageSeller();
    else if (path === '/admin') await pageAdmin();
    else app().innerHTML = '<div class="empty"><h3>Page not found</h3><a class="btn" href="#/">Go to Home</a></div>';
  } catch (e) {
    app().innerHTML = `<div class="empty"><h3>Oops</h3><p>${esc(e.message)}</p><a class="btn" href="#/">Go to Home</a></div>`;
  }
}
window.addEventListener('hashchange', route);

function need(role) {
  if (!user) { toast('Please login first', true); location.hash = '#/login'; return false; }
  if (user.role !== role) { toast('This page is for ' + role.toLowerCase() + 's only', true); location.hash = '#/'; return false; }
  return true;
}

/* ---------------- HOME ---------------- */
async function pageHome(params) {
  const q = params.get('q') || '', cat = params.get('cat') || 'All';
  $('#q').value = q;
  const [cats, prods] = await Promise.all([
    api('/categories'),
    api('/products?search=' + encodeURIComponent(q) + '&category=' + encodeURIComponent(cat))
  ]);
  const chip = (c) => `<a class="chip ${c === cat ? 'on' : ''}" href="#/?${q ? 'q=' + encodeURIComponent(q) + '&' : ''}cat=${encodeURIComponent(c)}">${esc(c)}</a>`;
  let h = '';
  if (!q && cat === 'All') h += `<div class="hero"><h1>Smart appliances for your home</h1><p>Washing machines, TVs, refrigerators, air fryers and more at great prices.</p></div>`;
  h += `<div class="chips">${['All', ...cats].map(chip).join('')}</div>`;
  if (q) h += `<h2 class="t">Results for "${esc(q)}" (${prods.length})</h2>`;
  else h += `<h2 class="t">${cat === 'All' ? 'All Products' : esc(cat)} (${prods.length})</h2>`;
  if (!prods.length) {
    h += `<div class="empty"><h3>No results found</h3><p>We could not find any product for "${esc(q || cat)}". Try another word like TV or fridge.</p><a class="btn" href="#/">View all products</a></div>`;
  } else {
    h += '<div class="grid">' + prods.map(p => {
      const off = p.mrp > p.price ? Math.round((p.mrp - p.price) * 100 / p.mrp) : 0;
      return `<div class="card"><div class="im">${img(p.imageUrl)}</div><div class="bd">
        <div class="cat">${esc(p.category)}</div><div class="nm">${esc(p.name)}</div>
        <div><span class="price">${money(p.price)}</span>${off ? `<span class="mrp">${money(p.mrp)}</span><span class="off">${off}% off</span>` : ''}</div>
        <div class="cat">${p.stock > 0 ? 'In stock' : 'Out of stock'} &middot; Sold by ${esc(p.sellerName)}</div>
        <button class="btn" ${p.stock > 0 ? '' : 'disabled'} onclick="addCart(${p.id})">Add to Cart</button></div></div>`;
    }).join('') + '</div>';
  }
  app().innerHTML = h;
}

async function addCart(id) {
  if (!user) { toast('Please login as buyer to add items', true); location.hash = '#/login'; return; }
  if (user.role !== 'BUYER') { toast('Only buyers can add to cart', true); return; }
  try {
    const r = await api('/cart', 'POST', {productId: id, quantity: 1});
    cartCount = r.count; renderNav(); toast('Added to cart');
  } catch (e) { toast(e.message, true); }
}

/* ---------------- LOGIN / REGISTER ---------------- */
function pageLogin() {
  const tab = (r, t) => `<button class="${loginRole === r ? 'on' : ''}" onclick="setRole('${r}')">${t}</button>`;
  const demo = {BUYER: 'buyer@akshara.com / buyer123', SELLER: 'seller@akshara.com / seller123', ADMIN: 'admin@akshara.com / admin123'}[loginRole];
  app().innerHTML = `<div class="box auth"><h2 class="t">Login to Akshara Mart</h2>
    <div class="tabs">${tab('BUYER', 'Buyer')}${tab('SELLER', 'Seller')}${tab('ADMIN', 'Admin')}</div>
    <label>Email</label><input id="em" type="email" placeholder="you@example.com">
    <label>Password</label><input id="pw" type="password" placeholder="Password" onkeydown="if(event.key==='Enter')doLogin()">
    <button class="btn" style="width:100%;margin-top:18px" onclick="doLogin()">Login as ${loginRole.toLowerCase()}</button>
    ${loginRole !== 'ADMIN' ? '<p style="text-align:center;margin-top:14px;font-size:.9rem">New here? <a href="#/register" style="color:var(--p);font-weight:600">Create an account</a></p>' : ''}
    <div class="hint">Demo ${loginRole.toLowerCase()} login: ${demo}</div></div>`;
}
function setRole(r) { loginRole = r; pageLogin(); }

async function doLogin() {
  try {
    const d = await api('/auth/login', 'POST', {email: $('#em').value, password: $('#pw').value, role: loginRole});
    saveSession(d); await refreshCount(); toast('Welcome, ' + user.name);
    location.hash = user.role === 'SELLER' ? '#/seller' : user.role === 'ADMIN' ? '#/admin' : '#/';
  } catch (e) { toast(e.message, true); }
}

function pageRegister() {
  app().innerHTML = `<div class="box auth"><h2 class="t">Create your account</h2>
    <label>Full name</label><input id="rn" placeholder="Your name">
    <label>Email</label><input id="re" type="email" placeholder="you@example.com">
    <label>Password (min 4 characters)</label><input id="rp" type="password">
    <label>I want to</label><select id="rr"><option value="BUYER">Buy products (Buyer)</option><option value="SELLER">Sell products (Seller)</option></select>
    <button class="btn" style="width:100%;margin-top:18px" onclick="doRegister()">Register</button>
    <p style="text-align:center;margin-top:14px;font-size:.9rem">Already have an account? <a href="#/login" style="color:var(--p);font-weight:600">Login</a></p></div>`;
}
async function doRegister() {
  try {
    const d = await api('/auth/register', 'POST', {name: $('#rn').value, email: $('#re').value, password: $('#rp').value, role: $('#rr').value});
    saveSession(d); await refreshCount(); toast('Account created');
    location.hash = user.role === 'SELLER' ? '#/seller' : '#/';
  } catch (e) { toast(e.message, true); }
}

/* ---------------- CART & CHECKOUT ---------------- */
async function pageCart() {
  if (!need('BUYER')) return;
  const items = await api('/cart');
  if (!items.length) {
    app().innerHTML = '<div class="empty"><h3>Your cart is empty</h3><p>Add some appliances to get started.</p><a class="btn" href="#/">Start shopping</a></div>';
    return;
  }
  const total = items.reduce((s, i) => s + i.price * i.quantity, 0);
  app().innerHTML = `<h2 class="t">My Cart (${items.length} items)</h2><div class="box">` + items.map(i => `
    <div class="ci">${img(i.imageUrl)}<div class="info"><div style="font-weight:500">${esc(i.name)}</div><div class="price">${money(i.price)}</div></div>
    <div class="qty"><button onclick="setQty(${i.id},${i.quantity - 1})">-</button><span>${i.quantity}</span><button onclick="setQty(${i.id},${i.quantity + 1})">+</button></div>
    <button class="btn ghost sm" onclick="removeItem(${i.id})">Remove</button></div>`).join('') +
    `<div class="sum"><span>Total</span><span>${money(total)}</span></div>
    <div style="text-align:right;margin-top:14px"><a class="btn" href="#/checkout">Proceed to Checkout</a></div></div>`;
}
async function setQty(id, q) { try { await api('/cart/' + id, 'PUT', {quantity: q}); await refreshCount(); route(); } catch (e) { toast(e.message, true); } }
async function removeItem(id) { try { await api('/cart/' + id, 'DELETE'); await refreshCount(); route(); } catch (e) { toast(e.message, true); } }

async function pageCheckout() {
  if (!need('BUYER')) return;
  const items = await api('/cart');
  if (!items.length) { location.hash = '#/cart'; return; }
  const total = items.reduce((s, i) => s + i.price * i.quantity, 0);
  app().innerHTML = `<h2 class="t">Checkout</h2><div class="box">
    <label>Delivery address</label><textarea id="addr" placeholder="House no, street, city, pincode, phone"></textarea>
    <label>Payment method</label><select id="pay"><option>Cash on Delivery</option><option>UPI (demo)</option><option>Credit / Debit Card (demo)</option></select>
    <div style="margin-top:16px">${items.map(i => `<div style="display:flex;justify-content:space-between;font-size:.9rem;padding:3px 0"><span>${esc(i.name)} x ${i.quantity}</span><span>${money(i.price * i.quantity)}</span></div>`).join('')}</div>
    <div class="sum"><span>Amount to pay</span><span>${money(total)}</span></div>
    <button id="pb" class="btn" style="width:100%;margin-top:16px" onclick="placeOrder()">Place Order</button></div>`;
}
async function placeOrder() {
  const btn = $('#pb'); btn.disabled = true;
  try {
    const o = await api('/orders', 'POST', {address: $('#addr').value, paymentMethod: $('#pay').value});
    await refreshCount();
    location.hash = '#/success/' + o.id;
  } catch (e) { toast(e.message, true); btn.disabled = false; }
}
function pageSuccess(id) {
  app().innerHTML = `<div class="box ok-box"><div class="tick">&#10003;</div><h2 class="t">Order placed successfully!</h2>
    <p style="color:var(--mut);margin-bottom:18px">Thank you for shopping with Akshara Mart. Your order number is <b>#${esc(id)}</b>.</p>
    <a class="btn" href="#/orders">View My Orders</a> <a class="btn ghost" href="#/">Continue shopping</a></div>`;
}

/* ---------------- MY ORDERS ---------------- */
function orderBlock(o, showUser) {
  return `<div class="box"><div class="oh"><div><b>Order #${o.id}</b><br><small>${fdate(o.createdAt)}${showUser ? ' &middot; ' + esc(o.userName) : ''}</small></div>
    <div><span class="status ${o.status}">${o.status}</span></div><div><small>${esc(o.paymentMethod)}</small><br><b>${money(o.total)}</b></div></div>
    ${o.items.map(i => `<div class="ci">${img(i.imageUrl)}<div class="info">${esc(i.productName)}<br><small>Qty ${i.quantity} &times; ${money(i.price)}</small></div><b>${money(i.price * i.quantity)}</b></div>`).join('')}
    <small style="color:var(--mut)">Deliver to: ${esc(o.address)}</small></div>`;
}
async function pageOrders() {
  if (!need('BUYER')) return;
  const list = await api('/orders');
  app().innerHTML = '<h2 class="t">My Orders</h2>' + (list.length ? list.map(o => orderBlock(o)).join('') :
    '<div class="empty"><h3>No orders yet</h3><p>Your placed orders will show up here.</p><a class="btn" href="#/">Shop now</a></div>');
}

/* ---------------- SELLER ---------------- */
async function pageSeller() {
  if (!need('SELLER')) return;
  const [prods, ords] = await Promise.all([api('/seller/products'), api('/seller/orders')]);
  const revenue = ords.reduce((s, o) => s + o.items.reduce((a, i) => a + i.price * i.quantity, 0), 0);
  app().innerHTML = `<h2 class="t">Seller Dashboard</h2>
  <div class="stats"><div class="stat"><b>${prods.length}</b><span>My products</span></div><div class="stat"><b>${ords.length}</b><span>Orders received</span></div><div class="stat"><b>${money(revenue)}</b><span>Total sales</span></div></div>
  <div class="box"><h3 style="color:var(--pd);margin-bottom:6px">Add a new product</h3>
    <div class="row"><div><label>Product name</label><input id="pn"></div><div><label>Category</label><input id="pc" list="cl" placeholder="e.g. Televisions"><datalist id="cl"><option>Washing Machines</option><option>Televisions</option><option>Refrigerators</option><option>Air Fryers</option><option>Air Conditioners</option><option>Microwave Ovens</option><option>Mixer Grinders</option><option>Vacuum Cleaners</option><option>Ceiling Fans</option><option>Water Heaters</option></datalist></div></div>
    <div class="row"><div><label>Selling price (₹)</label><input id="pp" type="number" min="1"></div><div><label>MRP (₹)</label><input id="pm" type="number" min="1"></div><div><label>Stock</label><input id="ps" type="number" min="0" value="10"></div></div>
    <label>Description</label><textarea id="pd"></textarea>
    <div class="row"><div><label>Upload product photo</label><input id="pf" type="file" accept="image/*"></div><div><label>or paste image URL</label><input id="pu" placeholder="https://..."></div></div>
    <button class="btn" style="margin-top:16px" onclick="addProduct()">Add Product</button></div>
  <div class="box"><h3 style="color:var(--pd);margin-bottom:10px">My products</h3><div class="tw"><table><tr><th></th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th></th></tr>` +
    prods.map(p => `<tr><td>${img(p.imageUrl)}</td><td>${esc(p.name)}</td><td>${esc(p.category)}</td><td>${money(p.price)}</td><td>${p.stock}</td>
      <td><button class="btn ghost sm" onclick="editProduct(${p.id},${p.price},${p.stock})">Edit</button> <button class="btn danger sm" onclick="delProduct(${p.id})">Delete</button></td></tr>`).join('') +
    `</table></div></div>
  <div class="box"><h3 style="color:var(--pd);margin-bottom:10px">Orders received</h3>` + (ords.length ? ords.map(o => `
    <div class="oh"><div><b>Order #${o.id}</b><br><small>${fdate(o.createdAt)} &middot; ${esc(o.customer)}</small></div>
    <div>${o.items.map(i => esc(i.productName) + ' x ' + i.quantity).join('<br>')}</div>
    <div><select onchange="setStatus(${o.id},this.value)">${['PLACED', 'SHIPPED', 'DELIVERED'].map(s => `<option ${s === o.status ? 'selected' : ''}>${s}</option>`).join('')}</select></div></div>`).join('') : '<p style="color:var(--mut)">No orders yet.</p>') + '</div>';
}
async function addProduct() {
  try {
    let url = $('#pu').value.trim();
    const f = $('#pf').files[0];
    if (f) { const fd = new FormData(); fd.append('file', f); url = (await api('/upload', 'POST', fd)).url; }
    await api('/products', 'POST', {name: $('#pn').value, category: $('#pc').value, price: $('#pp').value, mrp: $('#pm').value, stock: $('#ps').value, description: $('#pd').value, imageUrl: url});
    toast('Product added'); route();
  } catch (e) { toast(e.message, true); }
}
async function editProduct(id, price, stock) {
  const np = prompt('New price (₹):', price); if (np === null) return;
  const ns = prompt('New stock:', stock); if (ns === null) return;
  try {
    const all = await api('/seller/products');
    const p = all.find(x => x.id === id);
    await api('/products/' + id, 'PUT', {...p, price: np, stock: ns});
    toast('Product updated'); route();
  } catch (e) { toast(e.message, true); }
}
async function delProduct(id) {
  if (!confirm('Delete this product?')) return;
  try { await api('/products/' + id, 'DELETE'); toast('Product deleted'); route(); } catch (e) { toast(e.message, true); }
}
async function setStatus(id, s) { try { await api('/orders/' + id + '/status', 'PUT', {status: s}); toast('Status updated'); } catch (e) { toast(e.message, true); } }

/* ---------------- ADMIN ---------------- */
async function pageAdmin() {
  if (!need('ADMIN')) return;
  const [us, ps, os] = await Promise.all([api('/admin/users'), api('/products'), api('/admin/orders')]);
  const tab = (k, t) => `<button class="${adminTab === k ? 'on' : ''}" onclick="adminTab='${k}';pageAdmin()">${t}</button>`;
  let body = '';
  if (adminTab === 'users') body = `<div class="tw"><table><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th></th></tr>` +
    us.map(u => `<tr><td>${u.id}</td><td>${esc(u.name)}</td><td>${esc(u.email)}</td><td>${u.role}</td><td>${u.role === 'ADMIN' ? '' : `<button class="btn danger sm" onclick="delUser(${u.id})">Delete</button>`}</td></tr>`).join('') + '</table></div>';
  else if (adminTab === 'products') body = `<div class="tw"><table><tr><th></th><th>Name</th><th>Seller</th><th>Price</th><th>Stock</th><th></th></tr>` +
    ps.map(p => `<tr><td>${img(p.imageUrl)}</td><td>${esc(p.name)}</td><td>${esc(p.sellerName)}</td><td>${money(p.price)}</td><td>${p.stock}</td><td><button class="btn danger sm" onclick="adminDelProduct(${p.id})">Delete</button></td></tr>`).join('') + '</table></div>';
  else body = os.length ? os.map(o => orderBlock(o, true)).join('') : '<p style="color:var(--mut)">No orders yet.</p>';
  app().innerHTML = `<h2 class="t">Admin Panel</h2>
    <div class="stats"><div class="stat"><b>${us.length}</b><span>Users</span></div><div class="stat"><b>${ps.length}</b><span>Products</span></div><div class="stat"><b>${os.length}</b><span>Orders</span></div><div class="stat"><b>${money(os.reduce((s, o) => s + o.total, 0))}</b><span>Revenue</span></div></div>
    <div class="tabs">${tab('users', 'Users')}${tab('products', 'Products')}${tab('orders', 'Orders')}</div><div class="box">${body}</div>`;
}
async function delUser(id) { if (!confirm('Delete this user?')) return; try { await api('/admin/users/' + id, 'DELETE'); toast('User deleted'); pageAdmin(); } catch (e) { toast(e.message, true); } }
async function adminDelProduct(id) { if (!confirm('Delete this product?')) return; try { await api('/products/' + id, 'DELETE'); toast('Product deleted'); pageAdmin(); } catch (e) { toast(e.message, true); } }

/* ---------------- START ---------------- */
renderNav();
refreshCount();
route();
