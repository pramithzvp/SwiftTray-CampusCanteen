import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { flushSync } from 'react-dom';
import { Utensils, LayoutGrid, ReceiptText, Users, Settings2, LogOut, ShoppingBag, Plus, Minus, X, Clock3, Check, ChefHat, Coffee, Cookie, RefreshCw, Pencil, Trash2, Menu as MenuIcon, CircleAlert, Search, CheckCircle2 } from 'lucide-react';
import { api, clearCsrf } from './api';
import './styles.css';

const money = n => new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', maximumFractionDigits: 2 }).format(Number(n));
const statusLabels = { QUEUED: 'In the queue', PREPARING: 'Preparing', READY: 'Ready to collect', COLLECTED: 'Collected', CANCELLED: 'Cancelled' };
const nextStatus = { QUEUED: 'PREPARING', PREPARING: 'READY', READY: 'COLLECTED' };
const actionLabels = { QUEUED: 'Start preparing', PREPARING: 'Mark ready', READY: 'Confirm collection' };
const categories = ['All items', 'Meals', 'Snacks', 'Drinks'];
const itemIcon = category => category === 'Drinks' ? Coffee : category === 'Snacks' ? Cookie : Utensils;

function App() {
  const [user, setUser] = useState(null), [menu, setMenu] = useState([]), [orders, setOrders] = useState([]), [queue, setQueue] = useState([]);
  const [view, setView] = useState('menu'), [category, setCategory] = useState('All items'), [search, setSearch] = useState('');
  const [cart, setCart] = useState({}), [auth, setAuth] = useState(null), [editor, setEditor] = useState(null), [staffForm, setStaffForm] = useState(false);
  const [loading, setLoading] = useState(true), [serviceError, setServiceError] = useState(''), [error, setError] = useState(''), [toast, setToast] = useState('');
  const [busy, setBusy] = useState(false), [mobileNav, setMobileNav] = useState(false), [mobileCart, setMobileCart] = useState(false);
  const mounted = useRef(true);
  const currentUserId = useRef(user?.id);
  currentUserId.current = user?.id;
  const toolState = useRef();
  toolState.current = { menu, cart, serviceError };
  const staff = user && ['STAFF', 'ADMIN'].includes(user.role), admin = user?.role === 'ADMIN';

  async function loadMenu() {
    setLoading(true);
    try { const data = await api('/menu-items'); if (mounted.current) { setMenu(data); setServiceError(''); } }
    catch (e) { if (mounted.current) setServiceError(e.message); }
    finally { if (mounted.current) setLoading(false); }
  }
  async function refreshOrders() {
    if (!user) return;
    try {
      const data = await api(staff ? '/staff/orders' : '/orders/mine');
      if (mounted.current && currentUserId.current === user.id) { staff ? setQueue(data) : setOrders(data); setError(''); }
    } catch (e) {
      if (!mounted.current || currentUserId.current !== user.id) return;
      if (e.status === 401) { setUser(null); setView('menu'); setOrders([]); setQueue([]); clearCsrf(); }
      setError(e.message);
    }
  }
  useEffect(() => {
    mounted.current = true; loadMenu();
    api('/auth/me').then(u => { if (mounted.current) { setUser(u); if (u.role !== 'STUDENT') setView('queue'); } }).catch(() => {});
    return () => { mounted.current = false; };
  }, []);
  useEffect(() => { refreshOrders(); const id = setInterval(refreshOrders, 5000); return () => clearInterval(id); }, [user]);
  useEffect(() => { if (view === 'menu' || view === 'manage') loadMenu(); }, [view]);
  useEffect(() => { if (!toast) return; const id = setTimeout(() => setToast(''), 5000); return () => clearTimeout(id); }, [toast]);
  useEffect(() => {
    const context = document.modelContext;
    if (!context?.registerTool) return;
    const lifecycle = new AbortController();
    const register = tool => {
      try { Promise.resolve(context.registerTool(tool, { signal: lifecycle.signal })).catch(() => {}); } catch { /* Unsupported registrations do not affect the app. */ }
    };
    register({ name: 'read_canteen_menu', title: 'Read canteen menu', description: 'Read the menu currently displayed, including prices and availability. Does not place an order.', inputSchema: { type: 'object', properties: {}, additionalProperties: false }, annotations: { readOnlyHint: true, untrustedContentHint: true }, execute(input) {
      if (!input || typeof input !== 'object' || Array.isArray(input) || Object.keys(input).length) throw new Error('Expected an empty object.');
      if (toolState.current.serviceError) throw new Error(toolState.current.serviceError);
      return { items: toolState.current.menu };
    } });
    register({ name: 'configure_meal_tray', title: 'Configure meal tray', description: 'Replace the visible tray with selected menu items and quantities. Stages an order for the student to review; does not submit or pay for it.', inputSchema: { type: 'object', properties: { items: { type: 'array', maxItems: 20, items: { type: 'object', properties: { menuItemId: { type: 'integer' }, quantity: { type: 'integer', minimum: 1, maximum: 20 } }, required: ['menuItemId', 'quantity'], additionalProperties: false } } }, required: ['items'], additionalProperties: false }, annotations: { readOnlyHint: false, untrustedContentHint: false }, execute(input) {
      if (!input || Object.keys(input).some(k => k !== 'items') || !Array.isArray(input.items) || input.items.length > 20) throw new Error('Provide at most 20 menu items.');
      if (toolState.current.serviceError) throw new Error(toolState.current.serviceError);
      const next = {};
      for (const line of input.items) {
        if (!line || Object.keys(line).some(k => !['menuItemId', 'quantity'].includes(k)) || !Number.isSafeInteger(line.menuItemId) || !Number.isInteger(line.quantity) || line.quantity < 1 || line.quantity > 20 || next[line.menuItemId]) throw new Error('Use unique menu item IDs and quantities from 1 to 20.');
        if (!toolState.current.menu.find(m => m.id === line.menuItemId && m.available)) throw new Error('A selected menu item is unavailable.');
        next[line.menuItemId] = line.quantity;
      }
      flushSync(() => { setCart(next); setView('menu'); setMobileCart(true); });
      return { staged: true, submitted: false, items: input.items };
    } });
    return () => lifecycle.abort();
  }, []);
  useEffect(() => {
    const id = setInterval(() => { if (view === 'menu') loadMenuQuietly(); }, 15000);
    return () => clearInterval(id);
  }, [view]);
  async function loadMenuQuietly() { try { const data = await api('/menu-items'); if (mounted.current) { setMenu(data); setServiceError(''); } } catch (e) { if (mounted.current) setServiceError(e.message); } }
  function navigate(v) { setView(v); setError(''); setMobileNav(false); }
  function changeQuantity(id, delta) {
    setCart(c => { const q = Math.max(0, Math.min(20, (c[id] || 0) + delta)); const next = { ...c }; if (q) next[id] = q; else delete next[id]; return next; });
  }
  const lines = Object.entries(cart).map(([id, quantity]) => ({ item: menu.find(m => m.id === Number(id)), id: Number(id), quantity }));
  const count = lines.reduce((n, l) => n + l.quantity, 0);
  const total = lines.reduce((n, l) => n + (Number(l.item?.price) || 0) * l.quantity, 0);
  const invalidCart = lines.some(l => !l.item || !l.item.available);

  async function placeOrder() {
    if (!user) { setAuth('login'); return; }
    if (user.role !== 'STUDENT') { setError('Sign in with a student account to place an order.'); return; }
    setBusy(true); setError('');
    try {
      const order = await api('/orders', { method: 'POST', body: { items: lines.map(l => ({ menuItemId: l.id, quantity: l.quantity })) } });
      setOrders(o => [order, ...o]); setCart({}); setMobileCart(false); setView('orders'); setToast(`Order placed. Your token is ${order.token}.`);
    } catch (e) { setError(e.message); loadMenuQuietly(); }
    finally { setBusy(false); }
  }
  async function logout() {
    setBusy(true);
    try { await api('/auth/logout', { method: 'POST' }); setUser(null); setOrders([]); setQueue([]); setView('menu'); setToast('You have signed out.'); clearCsrf(); }
    catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  async function quickLogin(email, password) {
    setBusy(true); setError('');
    try {
      const u = await api('/auth/login', { method: 'POST', body: { email, password } });
      setUser(u);
      if (u.role === 'STUDENT') {
        setView('menu');
      } else {
        setView('queue');
      }
      setToast(`Switched to ${u.name} (${u.role.toLowerCase()}).`);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function updateOrder(order) {
    setBusy(true);
    try { await api(`/staff/orders/${order.id}/status`, { method: 'PATCH', body: { status: nextStatus[order.status] } }); await refreshOrders(); }
    catch (e) { setError(e.message); await refreshOrders(); setError(e.message); } finally { setBusy(false); }
  }
  async function cancelOrder(order) {
    if (!window.confirm(`Cancel order ${order.token}?`)) return;
    setBusy(true);
    try { await api(`/orders/${order.id}/cancel`, { method: 'POST' }); await refreshOrders(); setToast('Your order was cancelled.'); }
    catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  async function removeItem(item) {
    if (!window.confirm(`Remove ${item.name} from the menu? Previous orders will be kept.`)) return;
    setBusy(true);
    try { await api(`/menu-items/${item.id}`, { method: 'DELETE' }); await loadMenu(); setToast('Menu item removed.'); }
    catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  const visibleMenu = menu.filter(m => (category === 'All items' || m.category === category) && `${m.name} ${m.description}`.toLowerCase().includes(search.toLowerCase()));
  const nav = [{ id: 'menu', label: 'Canteen menu', icon: LayoutGrid }, ...(user?.role === 'STUDENT' ? [{ id: 'orders', label: 'My orders', icon: ReceiptText }] : []), ...(staff ? [{ id: 'queue', label: 'Order queue', icon: ChefHat }] : []), ...(admin ? [{ id: 'manage', label: 'Manage menu', icon: Settings2 }, { id: 'staff', label: 'Staff accounts', icon: Users }] : [])];
  const activeOrders = orders.filter(o => ['QUEUED', 'PREPARING', 'READY'].includes(o.status));
  const title = { menu: 'Your next meal, sorted.', orders: 'Your orders', queue: 'The order queue', manage: 'Manage the menu', staff: 'Staff accounts' }[view];

  return <div className="app-shell">
    {mobileNav && <button className="nav-scrim" aria-label="Close navigation" onClick={() => setMobileNav(false)} />}
    <aside className={`sidebar ${mobileNav ? 'is-open' : ''}`}>
      <a href="#" className="brand" onClick={e => { e.preventDefault(); navigate('menu'); }}><span className="brand-symbol"><Utensils size={23} /></span><span>SwiftTray<span className="brand-caption">CAMPUS CANTEEN</span></span></a>
      <div className="nav-heading">YOUR CANTEEN</div>
      <nav aria-label="Main navigation">{nav.map(n => <button key={n.id} className={`nav-link ${view === n.id ? 'active' : ''}`} aria-current={view === n.id ? 'page' : undefined} onClick={() => navigate(n.id)}><n.icon size={20} />{n.label}{n.id === 'orders' && activeOrders.length > 0 && <span className="nav-count">{activeOrders.length}</span>}</button>)}</nav>
      <div className="sidebar-bottom"><div className="collection-note"><ShoppingBag size={24} /><strong>Order. Collect. Enjoy.</strong><p>Pay at the counter when you collect your meal.</p></div>
        {user ? (
          <>
            <div className="account"><div className="avatar">{user.name.slice(0, 1).toUpperCase()}</div><div><strong>{user.name}</strong><span>{user.role.toLowerCase()}</span></div><button className="icon-button" aria-label="Sign out" title="Sign out" disabled={busy} onClick={logout}><LogOut size={19} /></button></div>
            <div className="role-switcher-row">
              <span className="switch-label">Switch demo persona:</span>
              <div className="demo-buttons-mini">
                {user.role !== 'STUDENT' && <button type="button" className="demo-btn mini" disabled={busy} title="Switch to Student" onClick={() => quickLogin('student@swifttray.lk', 'SwiftTrayStudent2026!')}>🎓 Student</button>}
                {user.role !== 'STAFF' && <button type="button" className="demo-btn mini" disabled={busy} title="Switch to Staff" onClick={() => quickLogin('staff@swifttray.lk', 'SwiftTrayStaff2026!')}>👨‍🍳 Staff</button>}
                {user.role !== 'ADMIN' && <button type="button" className="demo-btn mini" disabled={busy} title="Switch to Admin" onClick={() => quickLogin('admin@swifttray.lk', 'SwiftTrayAdmin2026!')}>⚙️ Admin</button>}
              </div>
            </div>
          </>
        ) : <button className="button sidebar-login" onClick={() => setAuth('login')}>Sign in to order</button>}
      </div>
    </aside>
    <main className="workspace">
      <header className="topbar">
        <div className="breadcrumb"><button className="icon-button mobile-only" aria-label="Open navigation" onClick={() => setMobileNav(true)}><MenuIcon /></button><span>Campus canteen</span><span className="breadcrumb-divider">/</span><strong>{nav.find(n => n.id === view)?.label}</strong></div>
        <div className="topbar-actions">
          {user ? (
            <div className="user-top-info">
              <span className="greeting">Hi, {user.name.split(' ')[0]}</span>
              <span className={`role-badge role-${user.role.toLowerCase()}`}>{user.role}</span>
            </div>
          ) : <button className="text-button" onClick={() => setAuth('login')}>Sign in</button>}
          <button className="cart-shortcut" onClick={() => { navigate('menu'); setMobileCart(true); }} aria-label={`Open tray, ${count} items`}><ShoppingBag size={19} /><span>My tray</span><b>{count}</b></button>
        </div>
      </header>
      <div className="page-content"><div className="page-heading"><div><p className="eyebrow">{view === 'menu' ? 'LESS QUEUING. MORE LUNCH.' : staff ? 'CANTEEN WORKSPACE' : 'YOUR COLLECTION TOKENS'}</p><h1>{title}</h1><p className="page-description">{view === 'menu' ? 'Pick your favourites. We’ll let you know when they’re ready.' : view === 'queue' ? 'Prepare orders in token order and keep students updated.' : view === 'orders' ? 'Follow your meal from the queue to the collection counter.' : view === 'manage' ? 'Keep food, prices, and availability up to date.' : 'Create accounts for the people running your canteen.'}</p></div>{view === 'manage' && <button className="button" onClick={() => setEditor({})}><Plus size={18} />Add menu item</button>}</div>
        {error && <div className="alert" role="alert"><CircleAlert size={19} /><span>{error}</span><button className="icon-button" aria-label="Dismiss error" onClick={() => setError('')}><X size={18} /></button></div>}
        {view === 'menu' && <div className="menu-layout"><section className="menu-main" aria-label="Canteen menu"><div className="meal-banner"><div className="banner-copy"><span className="banner-label">FRESH FROM THE CANTEEN</span><h2>A good meal.<br />A shorter queue.</h2><p>Your campus favourites, ready for collection.</p><span className="banner-pill"><ShoppingBag size={15} /> Pay when you collect</span></div><img src="/rice-curry.jpg" alt="A freshly prepared Sri Lankan rice and curry meal" /></div><div className="menu-toolbar"><div className="category-tabs" role="group" aria-label="Filter menu category">{categories.map(c => <button className={category === c ? 'selected' : ''} key={c} aria-pressed={category === c} onClick={() => setCategory(c)}>{c}</button>)}</div><label className="search-box"><Search size={18} /><input aria-label="Search menu" placeholder="Find something tasty" value={search} onChange={e => setSearch(e.target.value)} /></label></div><div className="section-heading"><h2>{category === 'All items' ? 'On the menu' : category}</h2><span>{visibleMenu.length} {visibleMenu.length === 1 ? 'item' : 'items'}</span></div>
          {loading && menu.length === 0 ? <div className="empty-state"><RefreshCw className="spin" /><h3>Loading the menu…</h3></div> : serviceError ? <div className="empty-state connection-state"><CircleAlert /><h3>We couldn’t load the menu</h3><p>{serviceError}</p><button className="button button-secondary" onClick={loadMenu}><RefreshCw size={17} />Try again</button></div> : visibleMenu.length === 0 ? <div className="empty-state"><Utensils /><h3>{search || category !== 'All items' ? 'No matching items' : 'The menu is being prepared'}</h3><p>{search || category !== 'All items' ? 'Try another search or category.' : 'Check back once the canteen has added today’s meals.'}</p>{admin && <button className="button" onClick={() => navigate('manage')}>Manage menu</button>}</div> : <div className="menu-grid">{visibleMenu.map(item => { const Icon = itemIcon(item.category); return <article className={`food-card ${!item.available ? 'unavailable' : ''}`} key={item.id}><div className="food-card-top"><span className={`food-icon ${item.category.toLowerCase()}`}><Icon size={27} /></span><span className="category-label">{item.category}</span></div><h3>{item.name}</h3><p>{item.description}</p><div className="food-card-footer"><strong>{money(item.price)}</strong>{item.available ? <button className="add-button" disabled={cart[item.id] >= 20} onClick={() => changeQuantity(item.id, 1)} aria-label={`Add ${item.name} to tray`}><Plus size={18} />Add</button> : <span className="sold-out">Unavailable</span>}</div></article>; })}</div>}
          <p className="menu-footnote">Meals are prepared in order. Collection time depends on the canteen queue.</p></section>
          <aside className={`tray-panel ${mobileCart ? 'tray-open' : ''}`} aria-label="Your tray"><div className="tray-heading"><div><ShoppingBag size={22} /><h2>Your tray</h2></div><span className="tray-count">{count}</span><button className="icon-button mobile-only" aria-label="Close tray" onClick={() => setMobileCart(false)}><X /></button></div>{!count ? <div className="empty-tray"><span><ShoppingBag size={36} /></span><h3>Something delicious<br />belongs here.</h3><p>Add items from the menu<br />to start your order.</p></div> : <div className="tray-lines">{lines.map(l => <div className="tray-line" key={l.id}><div className="tray-line-title"><strong>{l.item?.name || 'Removed menu item'}</strong><button className="icon-button" aria-label={`Remove ${l.item?.name || 'item'} from tray`} onClick={() => setCart(c => { const next = { ...c }; delete next[l.id]; return next; })}><X size={15} /></button></div>{(!l.item || !l.item.available) && <span className="line-error">No longer available. Please remove.</span>}<div className="tray-line-controls"><div className="quantity-control"><button onClick={() => changeQuantity(l.id, -1)} aria-label={`Decrease ${l.item?.name} quantity`}><Minus size={13} /></button><span>{l.quantity}</span><button disabled={l.quantity >= 20} onClick={() => changeQuantity(l.id, 1)} aria-label={`Increase ${l.item?.name} quantity`}><Plus size={13} /></button></div><span>{money((l.item?.price || 0) * l.quantity)}</span></div></div>)}</div>}<div className="tray-bottom"><div className="tray-total"><span>Total</span><strong>{money(total)}</strong></div><p><CheckCircle2 size={15} /> Payment at collection</p><button className="button order-button" disabled={!count || busy || invalidCart || !!serviceError || !!staff} onClick={placeOrder}>{busy ? 'Placing your order…' : user ? 'Place order' : 'Sign in & place order'}</button><small>{staff ? 'Use a student account to place orders.' : 'You’ll receive a token after placing your order.'}</small>{staff && <button type="button" className="button button-secondary" style={{width:'100%', marginTop:'10px', fontSize:'12px', padding:'8px'}} disabled={busy} onClick={() => quickLogin('student@swifttray.lk', 'SwiftTrayStudent2026!')}>Switch to Student Account to Order</button>}</div></aside></div>}

        {view === 'orders' && <section className="orders-list" aria-label="Your orders">{orders.length === 0 ? <Empty icon={ReceiptText} title="No orders yet" description="Your collection tokens will appear here after you place an order." action={<button className="button" onClick={() => navigate('menu')}>Browse menu</button>} /> : orders.map(o => <OrderCard key={o.id} order={o} busy={busy} onCancel={cancelOrder} />)}</section>}
        {view === 'queue' && <><div className="queue-summary">{['QUEUED', 'PREPARING', 'READY'].map(s => <div key={s}><span>{statusLabels[s]}</span><strong>{queue.filter(o => o.status === s).length}</strong></div>)}</div><div className="section-heading"><h2>Active orders</h2><button className="text-button" onClick={refreshOrders}><RefreshCw size={16} />Refresh</button></div><section className="orders-list">{queue.length === 0 ? <Empty icon={ChefHat} title="The queue is clear" description="New student orders will appear here automatically." /> : queue.map(o => <OrderCard key={o.id} order={o} busy={busy} staff onUpdate={updateOrder} />)}</section></>}
        {view === 'manage' && <section className="management-panel">{serviceError ? <Empty icon={CircleAlert} title="Menu unavailable" description={serviceError} action={<button className="button" onClick={loadMenu}>Try again</button>} /> : menu.length === 0 ? <Empty icon={Utensils} title="Add your first menu item" description="Enter the actual meals, snacks, and drinks available at your canteen." action={<button className="button" onClick={() => setEditor({})}><Plus size={18} />Add menu item</button>} /> : <div className="table-scroll"><table><thead><tr><th>Menu item</th><th>Category</th><th>Price</th><th>Availability</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{menu.map(m => <tr key={m.id}><td><strong>{m.name}</strong><span>{m.description}</span></td><td>{m.category}</td><td>{money(m.price)}</td><td><span className={`status-badge ${m.available ? 'READY' : 'CANCELLED'}`}>{m.available ? 'Available' : 'Unavailable'}</span></td><td><div className="table-actions"><button className="icon-button" aria-label={`Edit ${m.name}`} onClick={() => setEditor(m)}><Pencil size={18} /></button><button className="icon-button danger" disabled={busy} aria-label={`Remove ${m.name}`} onClick={() => removeItem(m)}><Trash2 size={18} /></button></div></td></tr>)}</tbody></table></div>}</section>}
        {view === 'staff' && <section className="staff-panel"><span className="large-icon"><Users size={32} /></span><h2>Give your team access</h2><p>Staff accounts can view the order queue and update preparation and collection status.</p><button className="button" onClick={() => setStaffForm(true)}><Plus size={18} />Create staff account</button></section>}
      </div><footer className="page-footer"><span>SwiftTray · Campus canteen</span><a href="https://unsplash.com/photos/cooked-rice-on-brown-wooden-bowl-EXHIHvJu4VU" target="_blank" rel="noreferrer">Meal photo: Nilantha Sanjeewa / Unsplash</a></footer>
    </main>
    {toast && <div className="toast" role="status"><CheckCircle2 size={20} />{toast}<button className="icon-button" aria-label="Dismiss notification" onClick={() => setToast('')}><X size={17} /></button></div>}
    {auth && <AccountModal mode={auth} onMode={setAuth} onClose={() => setAuth(null)} onSignedIn={u => { setUser(u); setAuth(null); if (u.role !== 'STUDENT') setView('queue'); setToast(`Welcome, ${u.name.split(' ')[0]}.`); }} />}
    {editor && <MenuModal item={editor} onClose={() => setEditor(null)} onSave={() => { setEditor(null); loadMenu(); setToast('Menu saved.'); }} />}
    {staffForm && <StaffModal onClose={() => setStaffForm(false)} onSave={() => { setStaffForm(false); setToast('Staff account created. Share the credentials privately.'); }} />}
  </div>;
}

function Empty({ icon: Icon, title, description, action }) { return <div className="empty-state"><Icon size={32} /><h3>{title}</h3><p>{description}</p>{action}</div>; }
function OrderCard({ order: o, staff, busy, onUpdate, onCancel }) {
  const steps = ['QUEUED', 'PREPARING', 'READY', 'COLLECTED'], current = steps.indexOf(o.status);
  return <article className="order-card"><div className="order-card-heading"><div><span className="eyebrow">COLLECTION TOKEN</span><h2>{o.token}</h2>{staff && <p>{o.studentName}</p>}</div><div className="order-status"><span className={`status-badge ${o.status}`}>{statusLabels[o.status]}</span><time dateTime={o.createdAt}>{new Date(o.createdAt).toLocaleString('en-LK', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</time></div></div>{!staff && o.status !== 'CANCELLED' && <ol className="progress-steps" aria-label="Order progress">{steps.map((s, i) => <li key={s} className={i <= current ? 'done' : ''} aria-current={i === current ? 'step' : undefined}><span>{i < current ? <Check size={14} /> : i + 1}</span>{statusLabels[s]}</li>)}</ol>}<div className="order-detail-lines">{o.items.map((i, n) => <div key={n}><span><b>{i.quantity}×</b> {i.name}</span><span>{money(Number(i.unitPrice) * i.quantity)}</span></div>)}</div><div className="order-card-bottom"><span>Total <strong>{money(o.total)}</strong></span>{staff ? <button className="button" disabled={busy} onClick={() => onUpdate(o)}>{actionLabels[o.status]}</button> : o.status === 'QUEUED' ? <button className="text-button danger" disabled={busy} onClick={() => onCancel(o)}>Cancel order</button> : o.status === 'READY' ? <span className="ready-note"><ShoppingBag size={17} />Show this token at the counter</span> : o.status === 'PREPARING' ? <span className="muted"><Clock3 size={16} />Your meal is being prepared</span> : null}</div></article>;
}
function Modal({ title, onClose, children, busy }) {
  const ref = useRef();
  useEffect(() => {
    const previous = document.activeElement;
    const el = ref.current; el.querySelector('input, button')?.focus();
    const handler = e => {
      if (e.key === 'Escape' && !busy) onClose();
      if (e.key === 'Tab') {
        const controls = [...el.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled)')];
        if (e.shiftKey && document.activeElement === controls[0]) { e.preventDefault(); controls.at(-1)?.focus(); }
        else if (!e.shiftKey && document.activeElement === controls.at(-1)) { e.preventDefault(); controls[0]?.focus(); }
      }
    };
    const original = document.body.style.overflow; document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handler);
    return () => { document.body.style.overflow = original; document.removeEventListener('keydown', handler); previous?.focus(); };
  }, [busy]);
  return <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget && !busy) onClose(); }}><section className="modal" ref={ref} role="dialog" aria-modal="true" aria-label={title}><div className="modal-heading"><h2>{title}</h2><button className="icon-button" aria-label="Close dialog" disabled={busy} onClick={onClose}><X /></button></div>{children}</section></div>;
}
function AccountModal({ mode, onMode, onClose, onSignedIn }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  const register = mode === 'register';
  async function submit(e) {
    e.preventDefault(); const fields = Object.fromEntries(new FormData(e.currentTarget)); setBusy(true); setError('');
    try { if (register) await api('/auth/register', { method: 'POST', body: fields }); const u = await api('/auth/login', { method: 'POST', body: { email: fields.email, password: fields.password } }); onSignedIn(u); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  async function demoLogin(email, password) {
    setBusy(true); setError('');
    try { const u = await api('/auth/login', { method: 'POST', body: { email, password } }); onSignedIn(u); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  return <Modal title={register ? 'Create your student account' : 'Welcome back'} busy={busy} onClose={onClose}>
    <p className="modal-description">{register ? 'Order your meals and keep track of your collection tokens.' : 'Sign in to your SwiftTray account.'}</p>
    {!register && (
      <div className="demo-accounts-box">
        <span className="demo-accounts-title">Quick Demo Sign-In:</span>
        <div className="demo-buttons">
          <button type="button" className="demo-btn" disabled={busy} onClick={() => demoLogin('student@swifttray.lk', 'SwiftTrayStudent2026!')}>🎓 Student</button>
          <button type="button" className="demo-btn" disabled={busy} onClick={() => demoLogin('staff@swifttray.lk', 'SwiftTrayStaff2026!')}>👨‍🍳 Staff</button>
          <button type="button" className="demo-btn" disabled={busy} onClick={() => demoLogin('admin@swifttray.lk', 'SwiftTrayAdmin2026!')}>⚙️ Admin</button>
        </div>
      </div>
    )}
    <form onSubmit={submit} key={mode}>
      {register && <label>Full name<input name="name" autoComplete="name" required maxLength={80} defaultValue="Kamal Perera" /></label>}
      <label>Email address<input name="email" type="email" autoComplete="email" required maxLength={180} defaultValue={register ? '' : 'student@swifttray.lk'} /></label>
      <label>Password<input name="password" type="password" autoComplete={register ? 'new-password' : 'current-password'} required minLength={register ? 12 : undefined} maxLength={72} defaultValue={register ? '' : 'SwiftTrayStudent2026!'} /></label>
      {register && <p className="field-help">Use at least 12 characters.</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="button form-submit" disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}</button>
    </form>
    <p className="auth-switch">{register ? 'Already have an account?' : 'New to SwiftTray?'} <button className="text-button" disabled={busy} onClick={() => { setError(''); onMode(register ? 'login' : 'register'); }}>{register ? 'Sign in' : 'Create student account'}</button></p>
  </Modal>;
}
function MenuModal({ item, onClose, onSave }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function submit(e) {
    e.preventDefault(); const data = new FormData(e.currentTarget); const body = { name: data.get('name'), description: data.get('description'), category: data.get('category'), price: Number(data.get('price')), available: data.get('available') === 'on' }; setBusy(true); setError('');
    try { await api(`/menu-items${item.id ? `/${item.id}` : ''}`, { method: item.id ? 'PUT' : 'POST', body }); onSave(); }
    catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  return <Modal title={item.id ? 'Edit menu item' : 'Add menu item'} busy={busy} onClose={onClose}><form onSubmit={submit}><label>Item name<input name="name" defaultValue={item.name} required maxLength={100} /></label><label>Description<textarea name="description" defaultValue={item.description} required maxLength={500} rows={3} /></label><div className="form-row"><label>Category<select name="category" defaultValue={item.category || 'Meals'}>{categories.slice(1).map(c => <option key={c}>{c}</option>)}</select></label><label>Price (LKR)<input name="price" type="number" min="1" max="999999.99" step="0.01" defaultValue={item.price} required /></label></div><label className="checkbox-label"><input type="checkbox" name="available" defaultChecked={item.available ?? true} />Available to order</label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button form-submit" disabled={busy}>{busy ? 'Saving…' : 'Save menu item'}</button></form></Modal>;
}
function StaffModal({ onClose, onSave }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function submit(e) { e.preventDefault(); const body = Object.fromEntries(new FormData(e.currentTarget)); setBusy(true); setError(''); try { await api('/admin/staff', { method: 'POST', body }); onSave(); } catch (e) { setError(e.message); } finally { setBusy(false); } }
  return <Modal title="Create staff account" busy={busy} onClose={onClose}><form onSubmit={submit}><label>Full name<input name="name" required maxLength={80} autoComplete="name" /></label><label>Email address<input name="email" type="email" required maxLength={180} autoComplete="email" /></label><label>Initial password<input name="password" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></label><p className="field-help">Use at least 12 characters and share it privately with your staff member.</p>{error && <p className="form-error" role="alert">{error}</p>}<button className="button form-submit" disabled={busy}>{busy ? 'Creating…' : 'Create staff account'}</button></form></Modal>;
}

createRoot(document.getElementById('root')).render(<App />);
