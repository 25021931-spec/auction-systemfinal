
 🔨 Hệ Thống Đấu Giá Trực Tuyến
**Bài tập lớn – Lập trình Nâng cao**

---
- [Yêu cầu hệ thống]
- [Cách chạy nhanh]
- [Cấu trúc project]
- [Kiến trúc hệ thống]
- [Design Patterns đã áp dụng]
- [Tính năng]
- [Chạy Unit Tests]
- [CI/CD]
- [Tài khoản mặc định]

---
## Yêu cầu hệ thống
| Công cụ | Phiên bản tối thiểu |
|---------|-------------------|
| Java JDK | **17** hoặc cao hơn |
| Maven | **3.8+** |
| JavaFX | Tự động tải qua Maven |
| SQLite |
---
## Cách chạy nhanh
### Bước 1 – Clone và build

```bash
git clone <repo-url>
cd auction-system

# Build toàn bộ project
mvn install -DskipTests
```
### Bước 2 – Chạy Server
```bash
# Cách 1: Script có sẵn
chmod +x run-server.sh && ./run-server.sh
# Cách 2: Maven
cd server && mvn exec:java -Dexec.mainClass=com.auction.ServerMain
# Cách 3: JAR
java -jar server/target/auction-server-jar-with-dependencies.jar
```
Server chạy tại **port 9090** (mặc định). Có thể đổi port:
```bash
./run-server.sh 8080
```
### Bước 3 – Chạy Client (cửa sổ mới)
```bash
# Cách 1: Script (khuyến nghị)
./run-client.sh

# Cách 2: Maven JavaFX plugin
cd client && mvn javafx:run
```

Mỗi lần mở thêm một cửa sổ mới = thêm một client. Có thể chạy nhiều client cùng lúc để test realtime.

---

# Cấu trúc project

```
auction-system/
├── pom.xml                          ← Root POM (multi-module)
│
├── shared/                          ← Dùng chung client & server
│   └── src/main/java/com/auction/
│       ├── dto/                     ← Data Transfer Objects
│       │   ├── AuctionDto.java
│       │   ├── BidDto.java
│       │   ├── SocketMessage.java
│       │   └── UserDto.java
│       └── enums/
│           ├── AuctionStatus.java   ← OPEN→RUNNING→FINISHED→PAID/CANCELED
│           ├── ItemCategory.java
│           ├── MessageType.java
│           └── UserRole.java
│
├── server/
│   └── src/
│       ├── main/java/com/auction/
│       │   ├── model/
│       │   │   ├── entity/
│       │   │   │   ├── Entity.java          ← Abstract base (OOP)
│       │   │   │   ├── Auction.java
│       │   │   │   ├── BidTransaction.java
│       │   │   │   └── AutoBidConfig.java
│       │   │   ├── item/
│       │   │   │   ├── Item.java            ← Abstract (kế thừa Entity)
│       │   │   │   ├── Electronics.java
│       │   │   │   ├── Art.java
│       │   │   │   ├── Vehicle.java
│       │   │   │   ├── OtherItem.java
│       │   │   │   └── ItemFactory.java     ← Factory Method Pattern
│       │   │   └── user/
│       │   │       ├── User.java            ← Abstract (kế thừa Entity)
│       │   │       ├── Bidder.java
│       │   │       ├── Seller.java
│       │   │       └── Admin.java
│       │   ├── dao/
│       │   │   ├── DatabaseManager.java     ← Singleton Pattern + SQLite
│       │   │   ├── UserDao.java
│       │   │   └── AuctionDao.java
│       │   ├── service/
│       │   │   ├── AuctionService.java      ← Core logic + ReentrantLock
│       │   │   ├── UserService.java
│       │   │   ├── NotificationService.java ← Observer Pattern
│       │   │   └── AuctionException.java
│       │   ├── network/
│       │   │   ├── AuctionServer.java       ← TCP Socket Server
│       │   │   └── ClientHandler.java       ← Per-client thread
│       │   └── ServerMain.java
│       └── test/java/com/auction/
│           ├── service/
│           │   ├── AuctionServiceTest.java  ← 10 test cases
│           │   └── UserServiceTest.java     ← 12 test cases
│           └── model/
│               └── ModelTest.java          ← OOP hierarchy tests
│
├── client/
│   └── src/main/
│       ├── java/com/auction/
│       │   ├── controller/                  ← MVC Controllers (JavaFX)
│       │   │   ├── LoginController.java
│       │   │   ├── AuctionListController.java
│       │   │   ├── BiddingController.java   ← Realtime bidding + Chart
│       │   │   ├── SellerController.java
│       │   │   └── AdminController.java
│       │   ├── network/
│       │   │   └── ServerConnection.java    ← Socket client + push listener
│       │   ├── util/
│       │   │   └── AppContext.java          ← Session singleton
│       │   └── ClientMain.java              ← JavaFX Application
│       └── resources/
│           ├── fxml/                        ← FXML views (MVC View)
│           │   ├── Login.fxml
│           │   ├── AuctionList.fxml
│           │   ├── Bidding.fxml
│           │   ├── Seller.fxml
│           │   └── Admin.fxml
│           └── css/
│               └── style.css
│
├── .github/workflows/ci.yml         ← GitHub Actions CI/CD
├── run-server.sh
├── run-client.sh
└── run-tests.sh
```

---

## Kiến trúc hệ thống

```
┌─────────────────────┐     TCP Socket      ┌──────────────────────────┐
│     CLIENT (JavaFX) │  ←── JSON ──────→   │       SERVER             │
│                     │                     │                          │
│  View  (FXML)       │                     │  ClientHandler (Thread)  │
│    ↕                │                     │       ↕                  │
│  Controller         │                     │  AuctionService          │
│    ↕                │                     │  UserService             │
│  ServerConnection   │                     │  NotificationService     │
│  AppContext         │  ←── PUSH (async)── │       ↕                  │
└─────────────────────┘   BID_UPDATE        │  DAO Layer               │
                          AUCTION_ENDED     │       ↕                  │
                          AUCTION_EXTENDED  │  SQLite Database         │
                                            └──────────────────────────┘
```

**Giao tiếp:** TCP Socket + JSON (Jackson)
**Database:** SQLite nhúng trong server (`auction.db`)
**Concurrency:**
- Server dùng `CachedThreadPool` – mỗi client một thread
- Bidding dùng `ReentrantLock` per-auction để tránh race condition
- Push notification dùng `synchronized list` cho thread safety

---

# Design Patterns đã áp dụng

| Pattern | Vị trí | Mô tả |
|---------|--------|-------|
| **Singleton** | `DatabaseManager`, `AppContext` | Đảm bảo một instance duy nhất |
| **Factory Method** | `ItemFactory` | Tạo đúng subclass Item theo category |
| **Observer** | `NotificationService` | Push realtime tới tất cả client đang xem |
| **MVC** | Client (FXML + Controller) | Tách View – Controller – Model |
| **Strategy (ngầm)** | `AuctionService.processAutoBids()` | Xử lý logic auto-bid linh hoạt |

---

# Tính năng

### Bắt buộc ✅
- Đăng ký / Đăng nhập (3 vai trò: Bidder, Seller, Admin)
- Quản lý sản phẩm đấu giá (Electronics, Art, Vehicle, Other)
- Đặt giá thủ công với kiểm tra hợp lệ
- Chuyển trạng thái: `OPEN → RUNNING → FINISHED`
- Tự động đóng phiên khi hết thời gian
- Xử lý lỗi: giá thấp hơn, phiên đã đóng, trùng leader
- GUI JavaFX + FXML + CSS dark theme

### Nâng cao ✅
- **Auto-Bidding:** Đặt maxBid + increment, hệ thống tự bid khi có đối thủ
- **Realtime Update:** Server push qua Socket (Observer Pattern) – không polling
- **Bid History Chart:** Line chart hiển thị biểu đồ giá theo thời gian thực

---

## Chạy Unit Tests

```bash
./run-tests.sh

# Hoặc thủ công:
mvn -pl shared install -q
mvn -pl server test
```

**Test coverage:**
- `AuctionServiceTest` – 10 test cases: tạo đấu giá, đặt giá, concurrent bidding, auto-bid, timeout
- `UserServiceTest` – 12 test cases: đăng ký, đăng nhập, token, ban user
- `ModelTest` – OOP hierarchy, Factory, Polymorphism, Anti-sniping
---
## CI/CD

File `.github/workflows/ci.yml` tự động:
1. Build toàn bộ project
2. Chạy tất cả unit tests
3. Publish test results
4. Build JAR artifacts
5. Upload server JAR
---
# Tài khoản mặc định

| Vai trò | Username | Password |
|---------|----------|----------|
| Admin | `admin` | `admin123` |

Tạo thêm tài khoản Bidder/Seller qua màn hình Đăng Ký.
---
docs: cập nhật README
refactor: tách NotificationService ra module riêng
chore: cấu hình CI/CD GitHub Actions
```
