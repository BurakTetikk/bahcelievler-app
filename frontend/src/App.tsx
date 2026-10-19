import { useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { api, errorMessage } from './api'
import type { ApiProblem, Book, BookInput, Borrower, BorrowerInput, Copy, CopyStatus, Dashboard, ImportResult, Loan, Page } from './api'

type Notify = (message: string, kind?: 'success' | 'error') => void
const navItems = [
  { to: '/', label: 'Genel Bakış', icon: '◫', end: true },
  { to: '/books', label: 'Kitaplar', icon: '▤' },
  { to: '/copies', label: 'Fiziksel Kopyalar', icon: '▧' },
  { to: '/loans', label: 'Ödünç İşlemleri', icon: '↗' },
  { to: '/borrowers', label: 'Kişiler', icon: '♙' },
  { to: '/overdue', label: 'Geciken Kitaplar', icon: '◷' },
  { to: '/import-export', label: 'Excel İşlemleri', icon: '▦' },
]
const pageTitles: Record<string, string> = {
  '/': 'Genel Bakış', '/books': 'Kitaplar', '/copies': 'Fiziksel Kopyalar', '/loans': 'Ödünç İşlemleri',
  '/borrowers': 'Kişiler', '/overdue': 'Geciken Kitaplar', '/import-export': 'Excel İşlemleri',
}

function localDate() {
  const date = new Date(); return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}
function dateText(value?: string | null) {
  if (!value) return '—'
  const [year, month, day] = value.slice(0, 10).split('-')
  return `${day}.${month}.${year}`
}
function errorFields(error: unknown) {
  return error && typeof error === 'object' && 'fieldErrors' in error ? (error as ApiProblem).fieldErrors || {} : {}
}
function fullName(person: Pick<Borrower, 'firstName' | 'lastName'> | Loan) {
  return 'borrowerFirstName' in person ? `${person.borrowerFirstName} ${person.borrowerLastName}` : `${person.firstName} ${person.lastName}`
}
function statusLabel(status: CopyStatus) {
  return ({ AVAILABLE: 'Müsait', ON_LOAN: 'Ödünçte', LOST: 'Kayıp', DAMAGED: 'Hasarlı' })[status]
}

function App() {
  const [toast, setToast] = useState<{ message: string; kind: 'success' | 'error' } | null>(null)
  const notify: Notify = (message, kind = 'success') => setToast({ message, kind })
  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), 4200)
    return () => window.clearTimeout(timer)
  }, [toast])
  const location = useLocation()
  const title = pageTitles[location.pathname] || 'Sayfa bulunamadı'
  return <div className="app-shell">
    <aside className="sidebar">
      <Link className="brand" to="/"><span className="brand-mark">k.</span><span>kitaplık<small>YÖNETİM PANELİ</small></span></Link>
      <div className="nav-caption">KÜTÜPHANE</div>
      <nav className="main-nav" aria-label="Ana menü">
        {navItems.map(item => <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}><span className="nav-icon">{item.icon}</span>{item.label}</NavLink>)}
      </nav>
      <div className="sidebar-bottom"><div className="sidebar-tip"><span className="tip-icon">✦</span><strong>Düzenli bir kütüphane</strong><p>Kitaplarını, kopyalarını ve ödünç işlemlerini tek yerden takip et.</p></div><span className="sidebar-version">Kitaplık · Yerel çalışma alanı</span></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><div className="breadcrumb">Kütüphane <span>/</span> <strong>{title}</strong></div><div className="topbar-right"><span className="status-dot"/> Yerel geliştirme</div></header>
      <div className="page-container"><Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/books" element={<BooksPage notify={notify} />} />
        <Route path="/copies" element={<CopiesPage notify={notify} />} />
        <Route path="/borrowers" element={<BorrowersPage notify={notify} />} />
        <Route path="/loans" element={<LoansPage notify={notify} />} />
        <Route path="/overdue" element={<LoansPage notify={notify} overdueOnly />} />
        <Route path="/import-export" element={<ExcelPage notify={notify} />} />
        <Route path="*" element={<section className="panel empty-state"><h2>Sayfa bulunamadı</h2><Link className="button primary" to="/">Genel bakışa dön</Link></section>} />
      </Routes></div>
      <footer className="footer">Kitaplık Yönetim Sistemi <span>•</span> Kayıtlarınız bu çalışma alanında saklanır.</footer>
    </main>
    {toast && <div className={`toast ${toast.kind}`} role="status"><span>{toast.kind === 'success' ? '✓' : '!'}</span>{toast.message}<button aria-label="Bildirimi kapat" onClick={() => setToast(null)}>×</button></div>}
  </div>
}

function PageHeading({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: ReactNode }) {
  return <div className="page-heading"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1><p>{description}</p></div>{action && <div className="heading-action">{action}</div>}</div>
}
function Panel({ children, className = '' }: { children: ReactNode; className?: string }) { return <section className={`panel ${className}`}>{children}</section> }
function Button({ children, onClick, variant = 'secondary', type = 'button', disabled = false }: { children: ReactNode; onClick?: () => void; variant?: 'primary' | 'secondary' | 'danger' | 'quiet'; type?: 'button' | 'submit'; disabled?: boolean }) {
  return <button className={`button ${variant}`} type={type} onClick={onClick} disabled={disabled}>{children}</button>
}
function Modal({ title, description, children, onClose, wide = false }: { title: string; description?: string; children: ReactNode; onClose: () => void; wide?: boolean }) {
  useEffect(() => { const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }; window.addEventListener('keydown', onKey); return () => window.removeEventListener('keydown', onKey) }, [onClose])
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}><section className={`modal${wide ? ' wide' : ''}`} role="dialog" aria-modal="true" aria-labelledby="modal-title"><div className="modal-heading"><div><h2 id="modal-title">{title}</h2>{description && <p>{description}</p>}</div><button className="icon-button" onClick={onClose} aria-label="Pencereyi kapat">×</button></div>{children}</section></div>
}
function Loading({ label = 'Kayıtlar yükleniyor…' }: { label?: string }) { return <div className="state-box"><span className="spinner"/><span>{label}</span></div> }
function ErrorState({ message, retry }: { message: string; retry?: () => void }) { return <div className="state-box error-state"><span className="state-symbol">!</span><strong>Veriler alınamadı</strong><span>{message}</span>{retry && <Button onClick={retry}>Tekrar dene</Button>}</div> }
function EmptyState({ title, text }: { title: string; text: string }) { return <div className="state-box empty"><span className="empty-symbol">⌕</span><strong>{title}</strong><span>{text}</span></div> }
function StatusPill({ status }: { status: CopyStatus }) { return <span className={`status-pill ${status.toLowerCase()}`}><i/>{statusLabel(status)}</span> }
function ActivePill({ active, overdue = false }: { active: boolean; overdue?: boolean }) { return <span className={`status-pill ${overdue ? 'late' : active ? 'on_loan' : 'available'}`}><i/>{overdue ? 'Gecikti' : active ? 'Aktif' : 'İade edildi'}</span> }
function Pagination({ data, onPage }: { data: Page<unknown>; onPage: (page: number) => void }) {
  if (data.totalPages < 2) return null
  return <div className="pagination"><span>{data.totalElements} kayıttan {data.page * data.size + 1}–{Math.min((data.page + 1) * data.size, data.totalElements)} arası</span><div><Button disabled={data.first} onClick={() => onPage(data.page - 1)}>← Önceki</Button><span className="page-count">{data.page + 1} / {data.totalPages}</span><Button disabled={data.last} onClick={() => onPage(data.page + 1)}>Sonraki →</Button></div></div>
}
function MetricCard({ label, value, note, icon, tone }: { label: string; value: number; note: string; icon: string; tone: string }) {
  return <div className="metric-card"><div className={`metric-icon ${tone}`}>{icon}</div><div className="metric-label">{label}</div><div className="metric-value">{value.toLocaleString('tr-TR')}</div><div className="metric-note">{note}</div></div>
}

function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState('')
  const load = () => { setLoading(true); api.dashboard().then(setData).catch(e => setError(errorMessage(e))).finally(() => setLoading(false)) }
  useEffect(load, [])
  return <>
    <PageHeading eyebrow="KÜTÜPHANE DURUMU" title="Genel bakış" description="Kütüphanenizdeki kitapları ve hareketleri bir arada takip edin." action={<Link className="button primary" to="/loans">＋ Ödünç işlemi başlat</Link>} />
    {loading ? <Panel><Loading label="Dashboard verileri alınıyor…"/></Panel> : error ? <Panel><ErrorState message={error} retry={load}/></Panel> : data && <>
      <div className="metrics-grid">
        <MetricCard label="Kitap kaydı" value={data.totalBookCount} note={`${data.activeBookCount} aktif bibliyografik kayıt`} icon="▤" tone="sage"/>
        <MetricCard label="Fiziksel kopya" value={data.totalCopyCount} note={`${data.availableCopyCount} kopya ödünç verilebilir`} icon="▧" tone="blue"/>
        <MetricCard label="Ödünçteki kopya" value={data.onLoanCopyCount} note="Şu anda okuyucularda" icon="↗" tone="gold"/>
        <MetricCard label="Aktif ödünç" value={data.activeLoanCount} note="Henüz iade edilmemiş kayıtlar" icon="◷" tone="violet"/>
        <MetricCard label="Gecikmiş" value={data.overdueLoanCount} note="İade tarihi geçmiş ödünçler" icon="!" tone="rose"/>
      </div>
      <div className="dashboard-grid">
        <Panel><div className="panel-heading"><div><h2>Son ödünç işlemleri</h2><p>En son oluşturulan hareketler</p></div><Link className="text-link" to="/loans">Tümünü gör →</Link></div>
          {data.recentLoans.length ? <LoanTable loans={data.recentLoans} compact/> : <EmptyState title="Henüz ödünç işlemi yok" text="Yeni bir ödünç işlemi başlatınca burada görünür."/>}
        </Panel>
        <Panel><div className="panel-heading"><div><h2>İade zamanı geçenler</h2><p>Öncelikli takip listesi</p></div><Link className="text-link" to="/overdue">Listeye git →</Link></div>
          {data.overdueLoans.length ? <div className="overdue-list">{data.overdueLoans.slice(0, 5).map(loan => <div className="overdue-item" key={loan.id}><span className="overdue-book-icon">▤</span><div className="overdue-info"><strong>{loan.bookTitle}</strong><span>{fullName(loan)} · {loan.inventoryCode}</span></div><div className="overdue-date"><small>Son tarih</small><strong>{dateText(loan.dueDate)}</strong></div></div>)}</div> : <EmptyState title="Gecikmiş kayıt yok" text="İade tarihi geçen ödünç bulunmuyor."/>}
        </Panel>
      </div>
      <div className="dashboard-footnote">Sayımlar canlı API verilerinden gelir. Gecikme, iade edilmemiş ve son tarihi bugünden önce olan ödünçler için hesaplanır.</div>
    </>}
  </>
}

const emptyBook: BookInput = { title: '', author: '', isbn: '', publisher: '', publicationYear: null, category: '', description: '' }
function BooksPage({ notify }: { notify: Notify }) {
  const [data, setData] = useState<Page<Book> | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState('')
  const [q, setQ] = useState(''); const [search, setSearch] = useState(''); const [page, setPage] = useState(0); const [includeArchived, setIncludeArchived] = useState(false)
  const [editing, setEditing] = useState<Book | null | false>(false); const [confirm, setConfirm] = useState<Book | null>(null)
  const load = () => { setLoading(true); setError(''); api.books({ q: search, page, includeArchived }).then(setData).catch(e => setError(errorMessage(e))).finally(() => setLoading(false)) }
  useEffect(load, [search, page, includeArchived])
  const save = async (value: BookInput) => {
    try { editing ? await api.updateBook(editing.id, value) : await api.createBook(value); notify(editing ? 'Kitap bilgileri güncellendi.' : 'Kitap kaydı oluşturuldu.'); setEditing(false); load() }
    catch (e) { throw e }
  }
  const archive = async () => { if (!confirm) return; try { await api.archiveBook(confirm.id); notify('Kitap arşivlendi.'); setConfirm(null); load() } catch (e) { notify(errorMessage(e), 'error') } }
  return <>
    <PageHeading eyebrow="KOLEKSİYON" title="Kitaplar" description="Bibliyografik kayıtları arayın, düzenleyin ve arşivleyin." action={<Button variant="primary" onClick={() => setEditing(null)}>＋ Kitap ekle</Button>}/>
    <Panel className="table-panel"><div className="toolbar"><form className="search-box" onSubmit={e => { e.preventDefault(); setPage(0); setSearch(q.trim()) }}><span>⌕</span><input value={q} onChange={e => setQ(e.target.value)} placeholder="Kitap adı, yazar veya ISBN ara"/><button aria-label="Ara">Ara</button></form><label className="check-control"><input type="checkbox" checked={includeArchived} onChange={e => { setIncludeArchived(e.target.checked); setPage(0) }}/> Arşivlenenleri göster</label></div>
      {loading ? <Loading/> : error ? <ErrorState message={error} retry={load}/> : data?.content.length ? <><div className="table-scroll"><table><thead><tr><th>KİTAP</th><th>KATEGORİ</th><th>YAYINEVİ / YIL</th><th>ISBN</th><th>DURUM</th><th className="actions-col">İŞLEMLER</th></tr></thead><tbody>{data.content.map(book => <tr key={book.id}><td><div className="book-cell"><span className="book-cover">▤</span><span><strong>{book.title}</strong><small>{book.author}</small></span></div></td><td>{book.category || '—'}</td><td>{[book.publisher, book.publicationYear].filter(Boolean).join(' · ') || '—'}</td><td className="muted-cell">{book.isbn || '—'}</td><td><span className={`status-pill ${book.active ? 'available' : 'archived'}`}><i/>{book.active ? 'Aktif' : 'Arşivli'}</span></td><td><div className="row-actions"><button className="text-action" onClick={() => setEditing(book)}>Düzenle</button>{book.active && <button className="text-action danger-text" onClick={() => setConfirm(book)}>Arşivle</button>}</div></td></tr>)}</tbody></table></div><Pagination data={data} onPage={setPage}/></> : <EmptyState title="Kitap bulunamadı" text={search ? 'Arama ölçütlerini değiştirip tekrar deneyin.' : 'İlk kitap kaydınızı oluşturarak koleksiyonunuzu başlatın.'}/>}</Panel>
    {editing !== false && <BookForm initial={editing || undefined} onClose={() => setEditing(false)} onSave={save}/>}
    {confirm && <Modal title="Kitap arşivlensin mi?" description={`“${confirm.title}” kaydı silinmeden arşivlenecek.`} onClose={() => setConfirm(null)}><div className="modal-actions"><Button onClick={() => setConfirm(null)}>Vazgeç</Button><Button variant="danger" onClick={archive}>Arşivle</Button></div></Modal>}
  </>
}
function BookForm({ initial, onClose, onSave }: { initial?: Book; onClose: () => void; onSave: (value: BookInput) => Promise<void> }) {
  const [form, setForm] = useState<BookInput>(initial ? { title: initial.title, author: initial.author, isbn: initial.isbn || '', publisher: initial.publisher || '', publicationYear: initial.publicationYear, category: initial.category || '', description: initial.description || '' } : emptyBook)
  const [errors, setErrors] = useState<Record<string, string>>({}); const [saving, setSaving] = useState(false); const [message, setMessage] = useState('')
  const set = (key: keyof BookInput, value: string | number | null) => setForm(current => ({ ...current, [key]: value }))
  const submit = async (e: FormEvent) => { e.preventDefault(); setSaving(true); setErrors({}); setMessage(''); try { await onSave({ ...form, isbn: form.isbn || null, publisher: form.publisher || null, category: form.category || null, description: form.description || null }); } catch (err) { setErrors(errorFields(err)); setMessage(errorMessage(err)) } finally { setSaving(false) } }
  return <Modal title={initial ? 'Kitabı düzenle' : 'Yeni kitap kaydı'} description="Kitabın bibliyografik bilgilerini girin." onClose={onClose} wide><form className="form-grid" onSubmit={submit}>
    <Field label="Kitap adı" error={errors.title} required><input autoFocus maxLength={200} value={form.title} onChange={e => set('title', e.target.value)} required/></Field>
    <Field label="Yazar" error={errors.author} required><input maxLength={150} value={form.author} onChange={e => set('author', e.target.value)} required/></Field>
    <Field label="ISBN" error={errors.isbn}><input maxLength={20} value={form.isbn || ''} onChange={e => set('isbn', e.target.value)} placeholder="İsteğe bağlı"/></Field>
    <Field label="Yayınevi" error={errors.publisher}><input maxLength={150} value={form.publisher || ''} onChange={e => set('publisher', e.target.value)}/></Field>
    <Field label="Yayın yılı" error={errors.publicationYear}><input type="number" min="0" max="3000" value={form.publicationYear ?? ''} onChange={e => set('publicationYear', e.target.value ? Number(e.target.value) : null)}/></Field>
    <Field label="Kategori" error={errors.category}><input maxLength={100} value={form.category || ''} onChange={e => set('category', e.target.value)}/></Field>
    <Field label="Açıklama" error={errors.description} className="full-field"><textarea rows={3} maxLength={10000} value={form.description || ''} onChange={e => set('description', e.target.value)}/></Field>
    {message && <p className="form-error full-field">{message}</p>}<div className="modal-actions full-field"><Button onClick={onClose}>Vazgeç</Button><Button type="submit" variant="primary" disabled={saving}>{saving ? 'Kaydediliyor…' : initial ? 'Değişiklikleri kaydet' : 'Kitabı kaydet'}</Button></div>
  </form></Modal>
}

function Field({ label, error, required, children, className = '' }: { label: string; error?: string; required?: boolean; children: ReactNode; className?: string }) {
  return <label className={`field ${className}`}><span>{label}{required && <b> *</b>}</span>{children}{error && <small className="field-error">{error}</small>}</label>
}

function CopiesPage({ notify }: { notify: Notify }) {
  const [books, setBooks] = useState<Book[]>([]); const [bookId, setBookId] = useState(''); const [data, setData] = useState<Page<Copy> | null>(null)
  const [page, setPage] = useState(0); const [status, setStatus] = useState(''); const [loading, setLoading] = useState(false); const [error, setError] = useState(''); const [inventoryCode, setInventoryCode] = useState(''); const [adding, setAdding] = useState(false)
  useEffect(() => { api.books({ size: 100, sort: 'title,asc' }).then(result => { setBooks(result.content); if (result.content.length) setBookId(String(result.content[0].id)) }).catch(e => setError(errorMessage(e))) }, [])
  const load = () => { if (!bookId) { setData(null); return }; setLoading(true); setError(''); api.copies(Number(bookId), { page, status: status || undefined }).then(setData).catch(e => setError(errorMessage(e))).finally(() => setLoading(false)) }
  useEffect(load, [bookId, page, status])
  const addCopy = async (e: FormEvent) => { e.preventDefault(); setAdding(true); try { await api.createCopy(Number(bookId), inventoryCode.trim()); setInventoryCode(''); notify('Fiziksel kopya eklendi.'); load() } catch (e) { notify(errorMessage(e), 'error') } finally { setAdding(false) } }
  const changeStatus = async (copy: Copy, next: CopyStatus) => { try { await api.updateCopyStatus(copy.id, next); notify('Kopya durumu güncellendi.'); load() } catch (e) { notify(errorMessage(e), 'error') } }
  return <><PageHeading eyebrow="ENVANTER" title="Fiziksel kopyalar" description="Envanter kodlarını ve kopyaların güncel durumunu yönetin."/>
    <Panel><div className="copies-controls"><Field label="Kitap seçin"> <select value={bookId} onChange={e => { setBookId(e.target.value); setPage(0) }}><option value="">Kitap seçin</option>{books.map(book => <option key={book.id} value={book.id}>{book.title} · {book.author}</option>)}</select></Field>
      <form className="add-copy-form" onSubmit={addCopy}><Field label="Yeni envanter kodu"><input maxLength={50} value={inventoryCode} onChange={e => setInventoryCode(e.target.value)} placeholder="Örn. KTP-001" required/></Field><Button type="submit" variant="primary" disabled={!bookId || adding}>{adding ? 'Ekleniyor…' : '＋ Kopya ekle'}</Button></form></div>
    </Panel>
    <Panel className="table-panel copies-table"><div className="toolbar"><div><h2>{books.find(book => String(book.id) === bookId)?.title || 'Kopyalar'}</h2><p>Bir fiziksel kopyayı ödünçteyken durum listesinden değiştiremezsiniz; iade sırasında durumu seçin.</p></div><select className="compact-select" value={status} onChange={e => { setStatus(e.target.value); setPage(0) }}><option value="">Tüm durumlar</option><option value="AVAILABLE">Müsait</option><option value="ON_LOAN">Ödünçte</option><option value="DAMAGED">Hasarlı</option><option value="LOST">Kayıp</option></select></div>
      {error ? <ErrorState message={error} retry={load}/> : loading ? <Loading/> : data?.content.length ? <><div className="table-scroll"><table><thead><tr><th>ENVANTER KODU</th><th>KİTAP</th><th>DURUM</th><th>EKLENME TARİHİ</th><th>GÜNCELLE</th></tr></thead><tbody>{data.content.map(copy => <tr key={copy.id}><td><strong className="code-text">{copy.inventoryCode}</strong></td><td>{copy.bookTitle}</td><td><StatusPill status={copy.status}/></td><td>{dateText(copy.createdAt)}</td><td>{copy.status === 'ON_LOAN' ? <span className="muted-cell">İade işlemiyle güncellenir</span> : <select className="status-select" value={copy.status} onChange={e => changeStatus(copy, e.target.value as CopyStatus)}><option value="AVAILABLE">Müsait</option><option value="DAMAGED">Hasarlı</option><option value="LOST">Kayıp</option></select>}</td></tr>)}</tbody></table></div><Pagination data={data} onPage={setPage}/></> : <EmptyState title={bookId ? 'Bu kitap için kopya yok' : 'Önce kitap ekleyin'} text={bookId ? 'Envanter kodu girerek ilk fiziksel kopyayı ekleyin.' : 'Kopya yönetimi için önce bir kitap kaydı oluşturun.'}/>}</Panel>
  </>
}

const emptyBorrower: BorrowerInput = { firstName: '', lastName: '', email: '', phone: '', notes: '' }
function BorrowersPage({ notify }: { notify: Notify }) {
  const [data, setData] = useState<Page<Borrower> | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [q, setQ] = useState(''); const [search, setSearch] = useState(''); const [active, setActive] = useState('true'); const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Borrower | null | false>(false); const [confirm, setConfirm] = useState<Borrower | null>(null)
  const load = () => { setLoading(true); setError(''); api.borrowers({ q: search, active: active === '' ? undefined : active === 'true', page }).then(setData).catch(e => setError(errorMessage(e))).finally(() => setLoading(false)) }
  useEffect(load, [search, active, page])
  const save = async (value: BorrowerInput) => { if (editing) await api.updateBorrower(editing.id, value); else await api.createBorrower(value); notify(editing ? 'Kişi bilgileri güncellendi.' : 'Kişi kaydı oluşturuldu.'); setEditing(false); load() }
  const toggle = async () => { if (!confirm) return; try { await api.borrowerStatus(confirm.id, !confirm.active); notify(confirm.active ? 'Kişi pasifleştirildi.' : 'Kişi yeniden etkinleştirildi.'); setConfirm(null); load() } catch (e) { notify(errorMessage(e), 'error') } }
  return <><PageHeading eyebrow="OKUYUCULAR" title="Kişiler" description="Ödünç alan kişilerin iletişim bilgilerini ve kayıt durumunu yönetin." action={<Button variant="primary" onClick={() => setEditing(null)}>＋ Kişi ekle</Button>}/>
    <Panel className="table-panel"><div className="toolbar"><form className="search-box" onSubmit={e => { e.preventDefault(); setPage(0); setSearch(q.trim()) }}><span>⌕</span><input value={q} onChange={e => setQ(e.target.value)} placeholder="Ad, e-posta veya telefon ara"/><button>Ara</button></form><select className="compact-select" value={active} onChange={e => { setActive(e.target.value); setPage(0) }}><option value="true">Aktif kişiler</option><option value="false">Pasif kişiler</option><option value="">Tüm kişiler</option></select></div>
      {loading ? <Loading/> : error ? <ErrorState message={error} retry={load}/> : data?.content.length ? <><div className="table-scroll"><table><thead><tr><th>KİŞİ</th><th>E-POSTA</th><th>TELEFON</th><th>DURUM</th><th className="actions-col">İŞLEMLER</th></tr></thead><tbody>{data.content.map(person => <tr key={person.id}><td><div className="person-cell"><span className="avatar">{person.firstName[0]}{person.lastName[0]}</span><span><strong>{person.firstName} {person.lastName}</strong><small>Kayıt #{person.id}</small></span></div></td><td>{person.email || '—'}</td><td>{person.phone || '—'}</td><td><span className={`status-pill ${person.active ? 'available' : 'archived'}`}><i/>{person.active ? 'Aktif' : 'Pasif'}</span></td><td><div className="row-actions"><button className="text-action" onClick={() => setEditing(person)}>Düzenle</button><button className="text-action" onClick={() => setConfirm(person)}>{person.active ? 'Pasifleştir' : 'Etkinleştir'}</button></div></td></tr>)}</tbody></table></div><Pagination data={data} onPage={setPage}/></> : <EmptyState title="Kişi bulunamadı" text="Aramayı değiştirin veya yeni kişi kaydı ekleyin."/>}</Panel>
    {editing !== false && <BorrowerForm initial={editing || undefined} onClose={() => setEditing(false)} onSave={save}/>}
    {confirm && <Modal title={confirm.active ? 'Kişi pasifleştirilsin mi?' : 'Kişi yeniden etkinleştirilsin mi?'} description={`${confirm.firstName} ${confirm.lastName} için bu işlem ödünç geçmişini silmez.`} onClose={() => setConfirm(null)}><div className="modal-actions"><Button onClick={() => setConfirm(null)}>Vazgeç</Button><Button variant="primary" onClick={toggle}>{confirm.active ? 'Pasifleştir' : 'Etkinleştir'}</Button></div></Modal>}
  </>
}
function BorrowerForm({ initial, onClose, onSave }: { initial?: Borrower; onClose: () => void; onSave: (value: BorrowerInput) => Promise<void> }) {
  const [form, setForm] = useState<BorrowerInput>(initial ? { firstName: initial.firstName, lastName: initial.lastName, email: initial.email || '', phone: initial.phone || '', notes: initial.notes || '' } : emptyBorrower)
  const [errors, setErrors] = useState<Record<string, string>>({}); const [message, setMessage] = useState(''); const [saving, setSaving] = useState(false)
  const update = (key: keyof BorrowerInput, value: string) => setForm(current => ({ ...current, [key]: value }))
  const submit = async (e: FormEvent) => { e.preventDefault(); setSaving(true); setErrors({}); setMessage(''); try { await onSave({ ...form, email: form.email || null, phone: form.phone || null, notes: form.notes || null }) } catch (error) { setErrors(errorFields(error)); setMessage(errorMessage(error)) } finally { setSaving(false) } }
  return <Modal title={initial ? 'Kişi bilgilerini düzenle' : 'Yeni kişi kaydı'} description="İletişim bilgileri isteğe bağlıdır." onClose={onClose}><form className="form-grid one-column" onSubmit={submit}>
    <Field label="Ad" required error={errors.firstName}><input autoFocus maxLength={80} required value={form.firstName} onChange={e => update('firstName', e.target.value)}/></Field><Field label="Soyad" required error={errors.lastName}><input maxLength={80} required value={form.lastName} onChange={e => update('lastName', e.target.value)}/></Field>
    <Field label="E-posta" error={errors.email}><input type="email" maxLength={254} value={form.email || ''} onChange={e => update('email', e.target.value)}/></Field><Field label="Telefon" error={errors.phone}><input type="tel" maxLength={30} value={form.phone || ''} onChange={e => update('phone', e.target.value)}/></Field><Field label="Notlar" error={errors.notes}><textarea rows={3} maxLength={10000} value={form.notes || ''} onChange={e => update('notes', e.target.value)}/></Field>
    {message && <p className="form-error">{message}</p>}<div className="modal-actions"><Button onClick={onClose}>Vazgeç</Button><Button type="submit" variant="primary" disabled={saving}>{saving ? 'Kaydediliyor…' : 'Kaydet'}</Button></div>
  </form></Modal>
}

function LoansPage({ notify, overdueOnly = false }: { notify: Notify; overdueOnly?: boolean }) {
  const [data, setData] = useState<Page<Loan> | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [page, setPage] = useState(0)
  const [q, setQ] = useState(''); const [search, setSearch] = useState(''); const [active, setActive] = useState('true'); const [returning, setReturning] = useState<Loan | null>(null); const [creating, setCreating] = useState(false)
  const load = () => { setLoading(true); setError(''); api.loans({ q: search, active: overdueOnly ? true : active === '' ? undefined : active === 'true', overdue: overdueOnly ? true : undefined, page }).then(setData).catch(e => setError(errorMessage(e))).finally(() => setLoading(false)) }
  useEffect(load, [search, active, page, overdueOnly])
  const returnLoan = async (date: string, status: CopyStatus) => { if (!returning) return; await api.returnLoan(returning.id, date, status); notify('Kitap iade alındı; kopya durumu güncellendi.'); setReturning(null); load() }
  return <><PageHeading eyebrow={overdueOnly ? 'TAKİP GEREKİYOR' : 'DOLAŞIM'} title={overdueOnly ? 'Geciken kitaplar' : 'Ödünç işlemleri'} description={overdueOnly ? 'İade tarihi geçmiş, henüz iade edilmemiş ödünç kayıtları.' : 'Aktif ve geçmiş ödünç hareketlerini takip edin.'} action={!overdueOnly && <Button variant="primary" onClick={() => setCreating(true)}>＋ Ödünç ver</Button>}/>
    <Panel className="table-panel"><div className="toolbar"><form className="search-box" onSubmit={e => { e.preventDefault(); setPage(0); setSearch(q.trim()) }}><span>⌕</span><input value={q} onChange={e => setQ(e.target.value)} placeholder="Kitap, envanter kodu veya kişi ara"/><button>Ara</button></form>{!overdueOnly && <select className="compact-select" value={active} onChange={e => { setActive(e.target.value); setPage(0) }}><option value="true">Aktif ödünçler</option><option value="false">İade edilenler</option><option value="">Tüm işlemler</option></select>}</div>
      {loading ? <Loading/> : error ? <ErrorState message={error} retry={load}/> : data?.content.length ? <><LoanTable loans={data.content} onReturn={setReturning}/><Pagination data={data} onPage={setPage}/></> : <EmptyState title={overdueOnly ? 'Gecikmiş kitap yok' : 'Ödünç kaydı bulunamadı'} text={overdueOnly ? 'Şu anda iade tarihi geçmiş aktif kayıt bulunmuyor.' : 'Filtreleri değiştirebilir veya yeni bir ödünç işlemi başlatabilirsiniz.'}/>}</Panel>
    {creating && <LoanForm onClose={() => setCreating(false)} onSave={async body => { await api.createLoan(body); notify('Ödünç işlemi oluşturuldu.'); setCreating(false); load() }}/>}
    {returning && <ReturnForm loan={returning} onClose={() => setReturning(null)} onSave={returnLoan}/>}
  </>
}
function LoanTable({ loans, onReturn, compact = false }: { loans: Loan[]; onReturn?: (loan: Loan) => void; compact?: boolean }) {
  return <div className="table-scroll"><table><thead><tr><th>KİTAP / KOPYA</th><th>ÖDÜNÇ ALAN</th><th>ÖDÜNÇ TARİHİ</th><th>SON TARİH</th><th>DURUM</th>{!compact && <th>İŞLEM</th>}</tr></thead><tbody>{loans.map(loan => <tr key={loan.id}><td><div className="loan-book"><strong>{loan.bookTitle}</strong><small>{loan.inventoryCode}</small></div></td><td>{fullName(loan)}</td><td>{dateText(loan.loanDate)}</td><td className={loan.overdue ? 'late-date' : ''}>{dateText(loan.dueDate)}</td><td><ActivePill active={loan.active} overdue={loan.overdue}/></td>{!compact && <td>{loan.active && onReturn ? <button className="text-action" onClick={() => onReturn(loan)}>İade al</button> : <span className="muted-cell">—</span>}</td>}</tr>)}</tbody></table></div>
}
function LoanForm({ onClose, onSave }: { onClose: () => void; onSave: (body: { copyId: number; borrowerId: number; loanDate: string; dueDate: string; notes: string | null }) => Promise<void> }) {
  const [books, setBooks] = useState<Book[]>([]); const [people, setPeople] = useState<Borrower[]>([]); const [copies, setCopies] = useState<Copy[]>([])
  const [bookId, setBookId] = useState(''); const [copyId, setCopyId] = useState(''); const [borrowerId, setBorrowerId] = useState(''); const [loanDate, setLoanDate] = useState(localDate()); const [dueDate, setDueDate] = useState(''); const [notes, setNotes] = useState('')
  const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [error, setError] = useState('')
  useEffect(() => { Promise.all([api.books({ size: 100, sort: 'title,asc' }), api.borrowers({ active: true, size: 100 })]).then(([b, p]) => { setBooks(b.content.filter(item => item.active)); setPeople(p.content.filter(item => item.active)); setLoading(false) }).catch(e => { setError(errorMessage(e)); setLoading(false) }) }, [])
  useEffect(() => { if (!bookId) { setCopies([]); return }; api.copies(Number(bookId), { status: 'AVAILABLE', size: 100 }).then(result => { setCopies(result.content); setCopyId(result.content[0] ? String(result.content[0].id) : '') }).catch(e => setError(errorMessage(e))) }, [bookId])
  const submit = async (e: FormEvent) => { e.preventDefault(); setSaving(true); setError(''); try { await onSave({ copyId: Number(copyId), borrowerId: Number(borrowerId), loanDate, dueDate, notes: notes || null }) } catch (e) { setError(errorMessage(e)) } finally { setSaving(false) } }
  return <Modal title="Yeni ödünç işlemi" description="Yalnızca aktif kişi ve müsait kopyalar seçilebilir." onClose={onClose} wide>{loading ? <Loading label="Kitap ve kişi listeleri alınıyor…"/> : <form className="form-grid" onSubmit={submit}>
    {error && <p className="form-error full-field">{error}</p>}
    <Field label="Kitap" required><select required value={bookId} onChange={e => setBookId(e.target.value)}><option value="">Kitap seçin</option>{books.map(book => <option key={book.id} value={book.id}>{book.title}</option>)}</select></Field>
    <Field label="Fiziksel kopya" required><select required value={copyId} onChange={e => setCopyId(e.target.value)} disabled={!bookId}><option value="">{copies.length ? 'Kopya seçin' : 'Müsait kopya yok'}</option>{copies.map(copy => <option key={copy.id} value={copy.id}>{copy.inventoryCode}</option>)}</select></Field>
    <Field label="Ödünç alan" required><select required value={borrowerId} onChange={e => setBorrowerId(e.target.value)}><option value="">Kişi seçin</option>{people.map(person => <option key={person.id} value={person.id}>{person.firstName} {person.lastName}</option>)}</select></Field>
    <Field label="Ödünç tarihi" required><input type="date" required value={loanDate} onChange={e => setLoanDate(e.target.value)}/></Field>
    <Field label="İade son tarihi" required><input type="date" required min={loanDate} value={dueDate} onChange={e => setDueDate(e.target.value)}/></Field>
    <Field label="Notlar"><input maxLength={10000} value={notes} onChange={e => setNotes(e.target.value)}/></Field>
    {!books.length && <div className="inline-hint full-field">Ödünç işlemi için aktif kitap kaydı bulunamadı.</div>}{!people.length && <div className="inline-hint full-field">Ödünç işlemi için aktif kişi kaydı bulunamadı.</div>}
    <div className="modal-actions full-field"><Button onClick={onClose}>Vazgeç</Button><Button type="submit" variant="primary" disabled={saving || !copies.length || !people.length}>{saving ? 'İşleniyor…' : 'Ödünç ver'}</Button></div>
  </form>}</Modal>
}
function ReturnForm({ loan, onClose, onSave }: { loan: Loan; onClose: () => void; onSave: (date: string, status: CopyStatus) => Promise<void> }) {
  const [date, setDate] = useState(localDate()); const [status, setStatus] = useState<CopyStatus>('AVAILABLE'); const [saving, setSaving] = useState(false); const [error, setError] = useState('')
  const submit = async (e: FormEvent) => { e.preventDefault(); setSaving(true); setError(''); try { await onSave(date, status) } catch (e) { setError(errorMessage(e)) } finally { setSaving(false) } }
  return <Modal title="Kitabı iade al" description={`${loan.bookTitle} · ${loan.inventoryCode} · ${fullName(loan)}`} onClose={onClose}><form className="form-grid one-column" onSubmit={submit}><Field label="İade tarihi" required><input type="date" min={loan.loanDate} value={date} required onChange={e => setDate(e.target.value)}/></Field><Field label="Kopyanın iade sonrası durumu" required><select value={status} onChange={e => setStatus(e.target.value as CopyStatus)}><option value="AVAILABLE">Müsait</option><option value="DAMAGED">Hasarlı</option><option value="LOST">Kayıp</option></select></Field>{error && <p className="form-error">{error}</p>}<div className="modal-actions"><Button onClick={onClose}>Vazgeç</Button><Button type="submit" variant="primary" disabled={saving}>{saving ? 'Kaydediliyor…' : 'İadeyi tamamla'}</Button></div></form></Modal>
}

function ExcelPage({ notify }: { notify: Notify }) {
  const [file, setFile] = useState<File | null>(null); const [importing, setImporting] = useState(false); const [result, setResult] = useState<ImportResult | null>(null); const [error, setError] = useState('')
  const [q, setQ] = useState(''); const [category, setCategory] = useState(''); const [copyStatus, setCopyStatus] = useState(''); const [includeArchived, setIncludeArchived] = useState(false)
  const importFile = async (e: FormEvent) => { e.preventDefault(); if (!file) return; setImporting(true); setError(''); setResult(null); try { const response = await api.importBooks(file); setResult(response); notify(`${response.importedRows} kitap içe aktarıldı${response.failedRows ? `, ${response.failedRows} satırda hata var` : ''}.`, response.failedRows ? 'error' : 'success') } catch (e) { setError(errorMessage(e)); notify(errorMessage(e), 'error') } finally { setImporting(false) } }
  const url = useMemo(() => api.exportUrl({ q: q.trim(), category: category.trim(), copyStatus: copyStatus || undefined, includeArchived }), [q, category, copyStatus, includeArchived])
  return <><PageHeading eyebrow="TOPLU İŞLEMLER" title="Excel işlemleri" description="Kitap metaverisini Excel ile içe aktarın veya filtreleyerek dışa aktarın."/>
    <div className="excel-grid"><Panel className="excel-card"><div className="excel-card-icon import-icon">↓</div><h2>Kitapları içe aktar</h2><p>Yalnızca <strong>.xlsx</strong> dosyası. En fazla 5 MB ve 1000 kitap satırı kabul edilir. Her satır ayrı değerlendirilir; fiziksel kopya oluşturulmaz.</p>
      <div className="template-note"><strong>Beklenen kolon sırası</strong><span>Başlık · Yazar · ISBN · Yayınevi · Yayın Yılı · Kategori · Açıklama</span></div>
      <form onSubmit={importFile} className="upload-form"><label className="file-picker"><input type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={e => { setFile(e.target.files?.[0] || null); setResult(null); setError('') }}/><span className="file-icon">▧</span><span><strong>{file?.name || 'Excel dosyası seçin'}</strong><small>{file ? `${(file.size / 1024 / 1024).toFixed(2)} MB` : 'veya dosyayı buraya bırakın'}</small></span><span className="browse-label">Gözat</span></label>
        {error && <div className="upload-error">{error}</div>}{file && file.size > 5 * 1024 * 1024 && <div className="upload-error">Dosya 5 MB sınırını aşıyor.</div>}
        <Button type="submit" variant="primary" disabled={!file || importing || file.size > 5 * 1024 * 1024}>{importing ? 'Dosya işleniyor…' : 'Dosyayı içe aktar'}</Button>
      </form>
      {result && <div className={`import-result ${result.failedRows ? 'has-errors' : ''}`}><div className="result-summary"><strong>{result.importedRows} başarılı</strong><span>{result.failedRows} hatalı · {result.totalRows} satır</span></div>{result.errors.length > 0 && <div className="import-errors">{result.errors.slice(0, 8).map(err => <div key={err.rowNumber}><b>Satır {err.rowNumber}</b><span>{err.message}</span></div>)}{result.errors.length > 8 && <small>ve {result.errors.length - 8} hata daha…</small>}</div>}</div>}
    </Panel>
    <Panel className="excel-card"><div className="excel-card-icon export-icon">↑</div><h2>Kitapları dışa aktar</h2><p>Arama ve filtrelere uyan kitap kayıtlarını Excel çalışma kitabı olarak indirin.</p><div className="export-filters"><Field label="Başlık, yazar veya ISBN"><input value={q} onChange={e => setQ(e.target.value)} placeholder="Tüm kitaplar"/></Field><Field label="Kategori"><input value={category} onChange={e => setCategory(e.target.value)} placeholder="Tüm kategoriler"/></Field><Field label="Kopya durumu"><select value={copyStatus} onChange={e => setCopyStatus(e.target.value)}><option value="">Tüm durumlar</option><option value="AVAILABLE">Müsait</option><option value="ON_LOAN">Ödünçte</option><option value="DAMAGED">Hasarlı</option><option value="LOST">Kayıp</option></select></Field><label className="check-control"><input type="checkbox" checked={includeArchived} onChange={e => setIncludeArchived(e.target.checked)}/> Arşivlenenleri dahil et</label></div><a className="button primary full-button" href={url} download><span>↓</span> Excel dosyasını indir</a><div className="inline-hint">Dışa aktarma fiziksel kopya listesini değil, kitap metaverisini içerir.</div></Panel></div>
  </>
}

export default App
