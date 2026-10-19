# Kitap Takip ve Kütüphane Yönetim Sistemi

Küçük kütüphane ve ofis ortamlarında bibliyografik kitap kayıtlarını, fiziksel kopyaları, ödünç alan kişileri ve ödünç/iade işlemlerini yönetmek için aşamalı geliştirilen bir uygulama.

> Projede altyapı, kütüphane API'leri, dashboard ve kitap Excel akışlarının yanında bu API'leri kullanan responsive yönetim arayüzü tamamlandı. Uçtan uca kalite ve entegrasyon kontrolleri Aşama 7 kapsamındadır.

## Teknoloji yığını

- Backend: Java 21, Spring Boot 3.5.16, Maven, Spring Web, Validation, Data JPA, Actuator, Flyway
- Veritabanı: PostgreSQL 17
- Frontend: React 19, TypeScript, Vite 7, React Router 7
- Excel: Apache POI 5.5.1 (`.xlsx`)
- Yerel geliştirme: Docker Compose
- Backend testleri: JUnit 5, Mockito ve PostgreSQL Testcontainers

## Gereksinimler

- Docker Desktop ve Docker Compose eklentisi
- İsteğe bağlı, Docker dışında çalıştırmak için: Java 21, Maven 3.6.3+, Node.js 22.12+ ve npm

## Klasör yapısı

```text
backend/       Spring Boot API
frontend/      React + TypeScript arayüzü
compose.yaml   Yerel geliştirme servisleri
.env.example   Yerel ortam değişkenleri örneği
```

## Docker Compose ile başlatma

PowerShell'de proje kökünde:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend health: http://localhost:8080/actuator/health
- PostgreSQL yalnızca yerel makineye `localhost:55432` üzerinden açılır.

Frontend API isteklerini `VITE_API_BASE_URL` ile yapılandırılan `/api/v1` adresine gönderir. Vite geliştirme proxy'si `/api` isteklerini backend servisine iletir. Compose, önce PostgreSQL healthcheck'ini bekler; backend sağlık kontrolü de frontend başlamadan önce beklenir. Kaynak kodu frontend container'ına volume ile bağlanır.

## Ortam değişkenleri

`.env.example` dosyasını `.env` olarak kopyalayıp yerel geliştirme değerlerini düzenleyin. `.env` Git'e eklenmez. Örnek parola yalnızca yerel geliştirme içindir; paylaşılmış veya üretim ortamında kullanmayın. Üretimde gizli bilgileri bir secret manager üzerinden yönetin.

## Veritabanı ve migration

Flyway migration'ları `backend/src/main/resources/db/migration` altında sürümlenir. `V1` Flyway kurulumunu doğrulayan başlangıç migration'ıdır; `V2` ise şu tabloları oluşturur:

- `books`: bibliyografik kitap kaydı; ISBN boş olabilir, dolu ISBN normalize edilir ve benzersizdir.
- `book_copies`: fiziksel kopya ve benzersiz envanter kodu; durum CHECK kısıtıyla sınırlandırılır.
- `borrowers`: kişi bilgileri ve geçmişi koruyan aktif/pasif durumu.
- `loans`: kopya ve kişi foreign key'leri, ödünç/son tarih ve opsiyonel iade tarihi.

`loans` üzerinde, iade edilmemiş kayıtların aynı fiziksel kopya için çoğalmasını engelleyen PostgreSQL kısmi benzersiz indeksi vardır. Kitaplar veya kişiler silinince tarihçenin bozulmaması için foreign key'ler silmeyi kısıtlar. Hibernate şema üretmez ve `ddl-auto=validate` kullanır.

Ödünç verme sırasında fiziksel kopya satırı transaction içinde `PESSIMISTIC_WRITE` kilidiyle alınır. Kopya `AVAILABLE` değilse istek reddedilir; kayıt ve `ON_LOAN` durumuna geçiş aynı transaction'da yapılır. PostgreSQL kısmi benzersiz indeksi eşzamanlılık için son veri bütünlüğü korumasıdır. İade de ödünç satırını kilitler ve ödünç kaydı ile kopya durumunu birlikte günceller. İade sonrası durum belirtilmezse `AVAILABLE` olur; `DAMAGED` veya `LOST` seçilebilir. `ON_LOAN` iade sonucu olarak kabul edilmez.

Gecikme sabit bir veritabanı alanı değildir: yalnızca aktif ve `dueDate` bugünden önce olan kayıt gecikmiş sayılır. Bugünün tarihi JVM'in yerel saat dilimine göre hesaplanır.

İş tarih dilimi `APP_BUSINESS_TIME_ZONE` ile ayarlanabilir; varsayılan `Europe/Istanbul`'dur. Dashboard toplam kitap sayısını tüm bibliyografik kayıtlardan, aktif kitap sayısını arşivlenmemiş kayıtlardan; toplam fiziksel kopyayı tüm durumlardan; müsait/ödünçte kopyaları ilgili durum sayımlarından; aktif ve gecikmiş ödünçleri iade edilmemiş Loan kayıtlarından hesaplar. Son 5 ödünç ve son tarihi geçmiş ilk 10 kayıt yanıtta yer alır.

## API özeti

### Kitaplar

- `GET /api/v1/books`: `q` (başlık/yazar/ISBN), `category`, `copyStatus`, `includeArchived`, `page`, `size` ve `sort` ile arama, filtreleme ve sayfalama. Sayfa boyutu 1–100; sıralama alanları `title`, `author`, `publicationYear`, `category`, `createdAt`, `updatedAt`.
- `POST /api/v1/books`: bibliyografik kitap oluşturur.
- `GET /api/v1/books/{id}`: kitap ayrıntısını döndürür.
- `PUT /api/v1/books/{id}`: kitap metaverisini günceller.
- `DELETE /api/v1/books/{id}`: kaydı fiziksel olarak silmeden arşivler. Varsayılan liste arşivlenmiş kitapları göstermez.

### Fiziksel kopyalar

- `GET /api/v1/books/{bookId}/copies`: kitabın kopyalarını `status`, `page`, `size` ve `sort` ile listeler.
- `POST /api/v1/books/{bookId}/copies`: yeni kopya ve envanter kodu oluşturur.
- `GET /api/v1/copies/{copyId}`: fiziksel kopya ayrıntısını döndürür.
- `PATCH /api/v1/copies/{copyId}/status`: kopyayı `AVAILABLE`, `DAMAGED` veya `LOST` durumuna alır. `ON_LOAN` durumu yalnızca ödünç/iade akışıyla yönetilir.

### Ödünç alanlar

- `GET /api/v1/borrowers`: `q` (ad, soyad, e-posta, telefon), `active`, `page`, `size` ve `sort` ile arar ve sayfalar.
- `POST /api/v1/borrowers`: kişi kaydı oluşturur.
- `GET /api/v1/borrowers/{id}` ve `PUT /api/v1/borrowers/{id}`: kişi ayrıntısını gösterir ve bilgileri günceller.
- `PATCH /api/v1/borrowers/{id}/status`: `{"active": false}` ile pasifleştirir veya `true` ile yeniden etkinleştirir. Kişi silme endpoint'i yoktur; ödünç geçmişi korunur.
- `GET /api/v1/borrowers/{id}/loans`: kişinin aktif veya geçmiş ödünçlerini sayfalar; `active=true|false` ile filtreler.

### Ödünç ve iade

- `POST /api/v1/loans`: `copyId`, `borrowerId`, `loanDate`, `dueDate` ve isteğe bağlı `notes` ile ödünç verir. Kitap ve kişi aktif, kopya `AVAILABLE` olmalıdır.
- `GET /api/v1/loans`: `q` (kitap adı, envanter kodu, kişi adı), `borrowerId`, `active`, `overdue`, `loanDateFrom`, `loanDateTo`, `dueDateFrom`, `dueDateTo`, `page`, `size` ve `sort` filtrelerini destekler.
- `GET /api/v1/loans/active` ve `GET /api/v1/loans/overdue`: aktif ve gecikmiş kayıt listeleri.
- `GET /api/v1/loans/{id}`: ödünç kaydı ayrıntısı.
- `PATCH /api/v1/loans/{id}/return`: `returnDate` ve isteğe bağlı `copyStatus` (`AVAILABLE`, `DAMAGED` veya `LOST`) ile iade eder. Yalnızca aktif ödünç kaydı iade edilebilir; iade tarihi gelecekte veya ödünç tarihinden önce olamaz.

### Dashboard ve Excel

- `GET /api/v1/dashboard`: kitap, fiziksel kopya, aktif/gecikmiş ödünç sayımları; son 5 ödünç kaydı ve gecikmiş kayıtların ilk 10'unu döndürür.
- `POST /api/v1/books/import`: `file` multipart alanında `.xlsx` dosyası kabul eder. Dosya en fazla 5 MB ve başlık satırı hariç 1000 satır olabilir. İlk sayfada kolonlar tam olarak `Başlık`, `Yazar`, `ISBN`, `Yayınevi`, `Yayın Yılı`, `Kategori`, `Açıklama` sırasıyla bulunmalıdır. Her veri satırı ayrı kaydedilir; yanıt başarılı/başarısız satırları ve satır numaralı hata listesini verir. Bu akış yalnızca kitap metaverisi ekler, fiziksel kopya oluşturmaz.
- `GET /api/v1/books/export`: kitapları Excel `.xlsx` olarak indirir. `q`, `category`, `copyStatus` ve `includeArchived` filtrelerini alır; büyük sonuçları sayfalar ve streaming workbook ile yazar. Dışa aktarma başlıkları içe aktarma şablonuyla aynıdır.

## Kullanıcı arayüzü

- Dashboard gerçek API sayımlarını, son ödünçleri ve gecikmiş kayıtları gösterir.
- Kitaplar sayfası arama, sayfalama, kitap ekleme/düzenleme ve arşivleme işlemlerini destekler.
- Fiziksel kopyalar sayfasında kitap seçilerek o kitaba kopya eklenir ve müsait/hasarlı/kayıp durumları yönetilir. Ödünçteki kopyanın durumu iade akışı sırasında belirlenir.
- Kişiler sayfasında kayıt ekleme, düzenleme, arama ve pasifleştirme/yeniden etkinleştirme yapılır.
- Ödünç ekranı müsait kopya ve aktif kişi seçimiyle ödünç verir; iade sırasında tarih ve iade sonrası kopya durumu alınır. Gecikenler ayrı ekranda listelenir.
- Excel ekranı satır bazlı import sonucunu ve satır hata raporunu gösterir; dışa aktarma arama, kategori, kopya durumu ve arşiv filtrelerini kullanır.
- Form hataları, yükleniyor/boş/hata durumları ve işlem bildirimleri arayüzde gösterilir. Düzen mobil ve masaüstü ekranlarına uyarlanır.
- Arayüz kimlik doğrulaması eklemez. Uygulamayı yalnızca yerel veya erişimi ağ düzeyinde sınırlandırılmış güvenilir ortamda çalıştırın.

Geçersiz istekler alan hata detaylarıyla 400, bulunamayan kayıtlar 404, benzersiz ISBN/envanter kodu çakışmaları 409 döndürür. Sağlık endpoint'i `GET /actuator/health` olarak kalır.

## Geliştirme komutları

```powershell
# Backend testleri
cd backend
mvn test

# Backend paketi
mvn verify

# Frontend bağımlılıkları ve derleme
cd ..\frontend
npm install
npm run typecheck
npm run build
```

Backend PostgreSQL entegrasyon testleri Testcontainers ile geçici PostgreSQL 17 container'ı başlatır; test çalıştırırken Docker Desktop açık olmalıdır.

Compose günlükleri ve durdurma:

```powershell
docker compose logs -f
docker compose down
```

`docker compose down -v` PostgreSQL verisini içeren volume'u siler; veri kaybına yol açar. Yedekleme/geri yükleme prosedürü veritabanı şeması tamamlandığında ayrıca belgelenecektir. Migration'lar uygulama başlangıcında Flyway tarafından yürütülür.

## Bilinen sınırlamalar ve güvenlik

- Kopya listeleme API'si kitap altında sunulduğu için kopyalar ekranında önce bir kitap seçilir.
- İlk sürüm kimlik doğrulaması içermez. Bu durum uygulamayı internete açık kullanıma uygun yapmaz. Yerel makine veya erişimi ağ düzeyinde sınırlandırılmış güvenilir ortamla sınırlayın; CORS kimlik doğrulama değildir.
- İlk sürüm kimlik doğrulaması içermeyecek şekilde planlanmıştır. Bu durum uygulamayı internete açık kullanıma uygun yapmaz. Yerel makine veya erişimi ağ düzeyinde sınırlandırılmış güvenilir ortamla sınırlayın; CORS kimlik doğrulama değildir.
- Üretimden önce kimlik doğrulama ve yetkilendirme, TLS/reverse proxy, güvenli secret yönetimi, veritabanı erişim sınırları, yedekleme/geri yükleme ve güvenlik güncellemeleri değerlendirilmelidir.
- Docker Compose yapılandırması geliştirme içindir; üretim dağıtım dosyası değildir.
