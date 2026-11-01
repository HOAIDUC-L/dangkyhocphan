# HỆ THỐNG ĐĂNG KÝ HỌC PHẦN PHÂN TÁN (DISTRIBUTED DUAL-SERVER CLUSTER)

> 🎓 **Đồ án môn học**: Lập trình Mạng (Network Programming)  
> 🏛️ **Kiến trúc**: Cụm 2 Server Cân Bằng Tải (Dual-Server Cluster) / TCP Socket / Multi-threading / PostgreSQL JDBC / PostgreSQL LISTEN–NOTIFY / Java Swing FlatLaf / JSON Protocol / Apache POI  
> 🚫 **Ràng buộc kiến trúc**: **Tuyệt đối KHÔNG sử dụng Spring Boot, Hibernate, JPA hay RMI.** Toàn bộ tầng mạng, phân luồng và kết nối CSDL được hiện thực bằng Java Core thuần.

---

## 1. TỔNG QUAN HỆ THỐNG

Hệ thống mô phỏng cổng thông tin đăng ký tín chỉ học phần đại học theo kiến trúc **hệ phân tán chịu lỗi và cân bằng tải (High Availability & Load Balancing Cluster)** gồm 2 Server Node hoạt động độc lập song song:

* **Cụm 2 Registration Server (`SERVER_NODE_1` & `SERVER_NODE_2`)**:
  - Chạy trên 2 tiến trình / terminal độc lập (mặc định Node 1 cổng `9991`, Node 2 cổng `9992`).
  - Hỗ trợ nạp cấu hình cổng và tên Node linh hoạt qua tham số dòng lệnh CLI (`--port=... --node=...`) hoặc System Properties (`-Dserver.port`, `-Dserver.node`).
  - Cùng trỏ chung vào một CSDL PostgreSQL duy nhất (**Single Source of Truth**).
  - Quản lý hàng trăm phiên kết nối đồng thời thông qua Thread Pool (`ExecutorService`), cơ chế Heartbeat định kỳ (`PING`/`PONG`) và chính sách một phiên duy nhất (**Single-Session Policy**).
* **Đồng bộ thời gian thực liên Server (Cross-Server Sync qua PostgreSQL LISTEN / NOTIFY)**:
  - Mỗi Node sở hữu một tiến trình daemon `PostgresNotificationListener` kết nối độc lập tới kênh `course_channel`.
  - Khi sinh viên ở Node 1 đăng ký hoặc hủy môn thành công, CSDL PostgreSQL phát tín hiệu `pg_notify` ngay trong transaction.
  - Cả 2 Node lập tức nhận được sự kiện và tự động bắn gói tin TCP Socket `COURSE_UPDATED` xuống tất cả Client đang kết nối tới Node đó mà không cần qua Redis hay Message Broker ngoài.
* **Khóa tranh chấp phân tán (Distributed Concurrency Control)**:
  - Bảo vệ số lượng slot học phần bằng giao dịch ACID kết hợp khóa dòng cấp độ CSDL (`SELECT ... FOR UPDATE`).
  - Đảm bảo khi Client ở Node 1 và Client ở Node 2 cùng bấm tranh chấp slot cuối cùng (`remaining_slots = 1`) tại cùng một mili-giây, chỉ đúng 1 sinh viên đăng ký thành công, sinh viên còn lại nhận thông báo hết chỗ. Tuyệt đối không bao giờ xảy ra overbooking hay âm slot.
* **Client Sinh viên (Swing GUI) & Tự động chuyển vùng (Auto Failover)**:
  - Giao diện Swing hiện đại (FlatLaf), giao tiếp với Server hoàn toàn bằng JSON UTF-8 framing qua ký tự `\n`.
  - Có sẵn menu lựa chọn Server Node linh hoạt (Node 1, Node 2 hoặc chế độ **Auto Failover** tự động chuyển vùng sang Node 2 nếu Node 1 gặp sự cố).
  - Ma trận Thời khóa biểu tuần 10 Tiết $\times$ 7 Ngày trực quan, tính năng xuất Thời khóa biểu ra Excel `.xlsx` chuyên nghiệp bằng Apache POI.

---

## 2. KIẾN TRÚC & CÔNG NGHỆ

```
                                    +--------------------------------+
                                    |     POSTGRESQL DATABASE        |
                                    |    (Single Source of Truth)    |
                                    |   - courses (FOR UPDATE Lock)  |
                                    |   - users, enrollments, logs   |
                                    |   - LISTEN / NOTIFY Broker     |
                                    +---------------+----------------+
                                                    |
                    +-------------------------------+-------------------------------+
                    | (LISTEN / NOTIFY)                             | (LISTEN / NOTIFY)
                    v                                               v
     +------------------------------+                +------------------------------+
     |        SERVER NODE 1         |                |        SERVER NODE 2         |
     |  - Port: 9991                |                |  - Port: 9992                |
     |  - Node: SERVER_NODE_1       |                |  - Node: SERVER_NODE_2       |
     |  - PostgresNotificationListener |             |  - PostgresNotificationListener |
     |  - Admin Dashboard Frame     |                |  - Admin Dashboard Frame     |
     +--------------+---------------+                +--------------+---------------+
                    | (TCP Socket)                                  | (TCP Socket)
                    v                                               v
     +------------------------------+                +------------------------------+
     |         CLIENT A             |                |         CLIENT B             |
     |  - Đang nối vào Node 1       |                |  - Đang nối vào Node 2       |
     |  - Nhận Push Real-time       |                |  - Nhận Push Real-time       |
     +------------------------------+                +------------------------------+
```

### Chi tiết Stack Công nghệ:
* **Ngôn ngữ**: Java 17 LTS (Java Core).
* **Quản lý dự án**: Maven Multi-Module (`common`, `server`, `client`).
* **Truyền thông mạng (Networking)**:
  - TCP Socket thuần: `java.net.ServerSocket`, `java.net.Socket`.
  - Đa luồng: Thread Pool (`FixedThreadPool`), `ScheduledExecutorService` (Watchdog Heartbeat).
  - Client Dedicated Reader Thread: Luồng đọc chuyên trách xử lý Server Push không gây gián đoạn UI.
* **Cơ sở dữ liệu**:
  - PostgreSQL 14+, JDBC Driver thuần (`org.postgresql.Driver`).
  - Connection Pool: `HikariCP`.
  - Đồng bộ cụm phân tán: PostgreSQL `LISTEN course_channel` / `SELECT pg_notify(...)`.
* **Giao diện & Tiện ích**:
  - Swing GUI + FlatLaf Look and Feel.
  - `jackson-databind`: Xử lý phân tích JSON Request/Response.
  - `jbcrypt`: Băm mật khẩu một chiều BCrypt (Cost 12).
  - `apache-poi` (POI-OOXML): Xuất Thời khóa biểu dạng bảng ma trận và danh mục ra file Excel `.xlsx`.
  - `junit-jupiter`: Kiểm thử tự động (Unit Test, Concurrency Race Condition Test).

---

## 3. CẤU TRÚC THƯ MỤC DỰ ÁN

```text
course-registration-system/
├── pom.xml                                   # Root Maven POM
├── common/                                   # Module dùng chung (DTO, Model, Protocol)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/course/common/
│       │   ├── constants/AppConstants.java   # Hằng số hệ thống, ngày học, tiết học
│       │   ├── protocol/                     # Giao thức truyền tin (Action, ErrorCode, Request, Response)
│       │   ├── model/                        # Thực thể (User, Course, Enrollment, SessionLog, Role)
│       │   ├── dto/                          # Các đối tượng trao đổi dữ liệu JSON
│       │   └── util/JsonUtil.java            # Tiện ích chuyển đổi Jackson ObjectMapper
│       └── test/java/...
├── server/                                   # Module Máy chủ & Admin Dashboard
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/course/server/
│       │   │   ├── ServerApplication.java    # Điểm chạy chính (hỗ trợ CLI args và GUI)
│       │   │   ├── ServerContext.java        # Ngữ cảnh quản lý vòng đời ứng dụng
│       │   │   ├── config/ServerConfig.java  # Đọc cấu hình properties & phân tích tham số dòng lệnh
│       │   │   ├── network/
│       │   │   │   ├── TcpServer.java        # Core ServerSocket accept loop & heartbeat
│       │   │   │   ├── ClientHandler.java    # Luồng xử lý giao tiếp từng Client
│       │   │   │   ├── ProtocolHandler.java  # Điều phối xử lý nghiệp vụ Request -> Response
│       │   │   │   └── PostgresNotificationListener.java # [MỚI] Lắng nghe đồng bộ liên Server qua PG LISTEN
│       │   │   ├── session/                  # Quản lý phiên, Single-Session, Kick client
│       │   │   ├── database/DatabaseManager.java # Quản lý HikariCP Connection Pool
│       │   │   ├── repository/               # Tầng dữ liệu JDBC (UserRepository, CourseRepository, ...)
│       │   │   ├── service/                  # Nghiệp vụ (AuthService, EnrollmentService, BroadcastService)
│       │   │   └── ui/AdminDashboardFrame.java # Bảng điều khiển quản trị Server đồ họa
│       │   └── resources/application.properties
│       └── test/java/...                     # Kiểm thử tranh chấp đồng thời & ngắt kết nối
├── client/                                   # Module Máy trạm Sinh viên
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/course/client/
│       │   │   ├── ClientApplication.java    # Điểm chạy Client
│       │   │   ├── config/ClientConfig.java  # Cấu hình Client (Server Host/Port)
│       │   │   ├── network/
│       │   │   │   ├── ClientSocketManager.java # Quản lý kết nối Socket & Dedicated Reader Thread
│       │   │   │   └── ServerPushListener.java  # Callback sự kiện đẩy từ máy chủ
│       │   │   ├── service/                  # ClientService, ScheduleExcelExporter
│       │   │   └── ui/
│       │   │       ├── LoginFrame.java       # Màn hình đăng nhập (chọn Server Node & Auto Failover)
│       │   │       ├── MainFrame.java        # Giao diện chính sinh viên
│       │   │       ├── CoursePanel.java      # Bảng tìm kiếm & Đăng ký môn
│       │   │       ├── MyEnrollmentsPanel.java # Bảng môn đã đăng ký & Hủy môn
│       │   │       └── ScheduleMatrixPanel.java # Ma trận Thời khóa biểu tuần trực quan
│       │   └── resources/application.properties
│       └── test/java/...
├── database/                                 # Kịch bản cơ sở dữ liệu
│   ├── schema.sql                            # DDL tạo bảng, ràng buộc và chỉ mục
│   ├── seed.sql                              # Dữ liệu tài khoản & môn học mẫu
│   └── test-data.sql                         # Dữ liệu kịch bản kiểm thử nâng cao
├── docs/                                     # Tài liệu thiết kế chi tiết
│   ├── architecture.md
│   ├── protocol.md
│   ├── database.md
│   └── test-scenarios.md
└── README.md
```

---

## 4. TÀI KHOẢN MẪU DÙNG THỬ (TEST ACCOUNTS)

| Tên đăng nhập | Mật khẩu | MSSV | Họ và tên | Vai trò / Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **`sv01`** | `123456` | `SV001` | Nguyễn Văn A | Sinh viên (Lớp D21CQCN01-B) |
| **`sv02`** | `123456` | `SV002` | Trần Thị B | Sinh viên (Lớp D21CQCN02-B) |
| **`sv03`** | `123456` | `SV003` | Lê Văn C | Sinh viên (Lớp D21CQCN01-B) |
| **`sv_locked`** | `123456` | `SV_LOCKED` | Sinh Viên Bị Khóa | Tài khoản đã bị vô hiệu hóa (`is_active = FALSE`) |
| **`admin`** | `admin123` | `ADMIN01` | Quản Trị Viên Hệ Thống | Quản trị viên (`ADMIN`) |

*(Toàn bộ mật khẩu trong `seed.sql` đã được mã hóa bảo mật bằng thuật toán BCrypt Cost 12).*

---

## 5. HƯỚNG DẪN THIẾT LẬP & BIÊN DỊCH

### Yêu cầu môi trường:
* **JDK 17 trở lên** (`java -version`).
* **Apache Maven 3.8+** (`mvn -version`).
* **PostgreSQL 14+** chạy cổng 5432 trên máy chủ.

### Bước 1: Khởi tạo Cơ sở dữ liệu PostgreSQL
Mở Terminal trên máy cài đặt PostgreSQL:

```bash
# 1. Khởi động PostgreSQL
sudo systemctl start postgresql

# 2. Tạo database course_registration
sudo -u postgres psql -c "CREATE DATABASE course_registration;"

# 3. Tạo cấu trúc bảng, ràng buộc toàn vẹn và các index
sudo -u postgres psql -d course_registration -f database/schema.sql

# 4. Nạp dữ liệu mẫu
sudo -u postgres psql -d course_registration -f database/seed.sql

# 5. (Tùy chọn) Nạp tài khoản test khóa
sudo -u postgres psql -d course_registration -f database/test-data.sql
```

> **Cách kiểm tra CSDL trực tiếp:**
> - Qua terminal: `sudo -u postgres psql -d course_registration` (gõ `\dt` xem bảng, `SELECT * FROM courses;`).
> - Qua **pgAdmin 4**: Tạo Server kết nối tới `localhost:5432`, Database: `course_registration`, User: `postgres`, Pass: `123456`. Mở `Databases -> course_registration -> Schemas -> public -> Tables`.

### Bước 2: Cấu hình thông số kết nối
* **Server**: Kiểm tra file `server/src/main/resources/application.properties`:
  ```properties
  db.url=jdbc:postgresql://localhost:5432/course_registration
  db.username=postgres
  db.password=123456
  server.port=9999
  server.max-clients=100
  ```
* **Client**: File `client/src/main/resources/application.properties` (chỉ chứa cấu hình mạng, tuyệt đối không chứa mật khẩu CSDL):
  ```properties
  server.host=127.0.0.1
  server.port=9991
  ```

### Bước 3: Biên dịch toàn bộ hệ thống
Tại thư mục gốc dự án:
```bash
mvn clean package
```
*Maven sẽ biên dịch cả 3 module và chạy kiểm thử tự động, kết quả đạt `BUILD SUCCESS`.*

---

## 6. HƯỚNG DẪN CHẠY THỬ NGHIỆM

Bạn có thể chạy thử nghiệm theo 2 mô hình bên dưới:

---

### Mô Hình A: Chạy Cụm 2 Server Cân Bằng Tải (Dual-Server Cluster) ⭐ *(Khuyên dùng)*

Mở **4 cửa sổ Terminal riêng biệt** tại thư mục gốc:

#### 1. Terminal 1: Khởi động Server Node 1 (Cổng 9991)
```bash
mvn -pl server exec:java -Dexec.args="--port=9991 --node=SERVER_NODE_1"
```
*(Hoặc thêm cờ `--cli` nếu muốn chạy chế độ Terminal không bật GUI Dashboard).*

#### 2. Terminal 2: Khởi động Server Node 2 (Cổng 9992)
```bash
mvn -pl server exec:java -Dexec.args="--port=9992 --node=SERVER_NODE_2"
```

> 🔔 **Kiểm tra trạng thái:** Cả 2 Server khởi động sẽ in log thông báo:  
> `[SERVER_NODE_1] Successfully listening on PostgreSQL channel 'course_channel'. Real-time cluster sync ACTIVE.`  
> `[SERVER_NODE_2] Successfully listening on PostgreSQL channel 'course_channel'. Real-time cluster sync ACTIVE.`

#### 3. Terminal 3: Khởi động Client A (Kết nối vào Node 1)
```bash
mvn -pl client exec:java
```
- Tên đăng nhập: `sv01` / Mật khẩu: `123456`.
- Cụm Máy Chủ: Chọn **Server Node 1 (Cổng 9991)** $\rightarrow$ Bấm **ĐĂNG NHẬP**.

#### 4. Terminal 4: Khởi động Client B (Kết nối vào Node 2)
```bash
mvn -pl client exec:java
```
- Tên đăng nhập: `sv02` / Mật khẩu: `123456`.
- Cụm Máy Chủ: Chọn **Server Node 2 (Cổng 9992)** $\rightarrow$ Bấm **ĐĂNG NHẬP**.

---

### Mô Hình B: Chạy 1 Server Đơn Lẻ (Single Node Legacy)

- **Chạy Server:**
  ```bash
  mvn -pl server exec:java
  ```
  *(Server lắng nghe trên cổng mặc định `9999`).*
- **Chạy Client:**
  ```bash
  mvn -pl client exec:java
  ```
  *(Tại màn hình đăng nhập, chọn mục "Mặc định (Cổng 9999)").*

---

## 7. CÁC KỊCH BẢN DEMO ĐỒ ÁN (LIVE DEMO CHECKLIST)

### Kịch bản 1: Đồng bộ Real-Time giữa 2 Server (PostgreSQL LISTEN / NOTIFY)
1. Đặt 2 cửa sổ **Client A** (nối Node 1) và **Client B** (nối Node 2) cạnh nhau trên màn hình.
2. Tại Client A, tìm môn `INT104_01` (45 slot), bấm **[ĐĂNG KÝ HỌC PHẦN]**.
3. **Hiện tượng quan sát được**: Tại màn hình Client B (dù đang nối vào một Server khác hoàn toàn), số slot còn lại của môn `INT104_01` **tự động nhảy về 44 ngay trong tích tắc** mà không cần người dùng bấm làm mới.

### Kịch bản 2: Khóa Tranh Chấp Slot Cuối Cùng Phân Tán (Distributed Race Condition)
1. Chọn môn học `COURSE_RACE_01` (trong database thiết lập chỉ còn đúng **1 slot duy nhất**).
2. Cho cả Client A (Node 1) và Client B (Node 2) cùng bấm nút Đăng ký tại cùng thời điểm.
3. **Kết quả**:
   - Nhờ câu lệnh `SELECT ... FOR UPDATE` trong transaction PostgreSQL, chỉ duy nhất 1 Client thành công.
   - Client còn lại lập tức nhận thông báo từ chối: `Lớp học phần đã hết chỗ đăng ký`.
   - Số slot còn lại tại Database và 2 Server chốt chính xác ở mức `0` (không bao giờ xảy ra tình trạng âm slot).

### Kịch bản 3: Tự Động Chuyển Vùng Chịu Lỗi (Auto Failover)
1. Nhấn `Ctrl + C` tại Terminal của **Server Node 1** để giả lập sự cố sập máy chủ 1.
2. Mở một Client mới, tại ô Cụm Máy Chủ chọn: **`Tự động (Auto Failover: 9991 -> 9992)`**.
3. Nhập tài khoản và bấm **ĐĂNG NHẬP**.
4. **Hiện tượng quan sát được**: Client nhận thấy Node 1 không phản hồi, tự động chuyển vùng sang Node 2 (9992) và đăng nhập thành công mượt mà.

### Kịch bản 4: Nghiệp Vụ Xung Đột & Ràng Buộc Đăng Ký
1. **Trùng lịch học (`SCHEDULE_CONFLICT`)**: Đã đăng ký môn `INT101_01` (Thứ 2, tiết 1-3). Tiếp tục đăng ký `INT102_01` (Thứ 2, tiết 2-4) $\rightarrow$ Server từ chối và báo chi tiết trùng tiết 2-3.
2. **Trùng môn học (`DUPLICATE_SUBJECT`)**: Đã đăng ký `INT101_01` (Lập trình mạng ca 1). Thử đăng ký lớp `INT101_02` (Lập trình mạng ca 2) $\rightarrow$ Server từ chối vì đã đăng ký môn này trước đó.
3. **Đăng nhập một phiên (`Single-Session Policy`)**: Đang đăng nhập `sv01` trên Client A, mở Client khác cố tình nhập `sv01` $\rightarrow$ Server từ chối cho phép đăng nhập trùng lặp.

### Kịch bản 5: Quản Trị Viên Giám Sát & Điều Khiển
1. Mở Admin Dashboard trên Server Node 1: Quan sát thấy bảng Online Clients cập nhật số lượng kết nối trực tiếp.
2. Bấm chọn một Client $\rightarrow$ nhấn **[KICK CLIENT ĐÃ CHỌN]**: Client tương ứng lập tức bị đóng phiên và đẩy về màn hình đăng nhập.
3. Nhập mã tài khoản $\rightarrow$ nhấn **[KHÓA TÀI KHOẢN (BLOCK)]**: Sinh viên bị ngắt kết nối ngay lập tức và tài khoản chuyển sang trạng thái bị vô hiệu hóa.

### Kịch bản 6: Xuất Thời Khóa Biểu Ra Excel (.xlsx)
1. Tại Client sinh viên, chuyển sang tab **"Học Phần Đã Đăng Ký"**.
2. Nhấn nút **[Xuất Thời Khóa Biểu Ra Excel]** và chọn nơi lưu file.
3. Mở file `.xlsx` kiểm tra kết quả: Gồm 2 sheet được định dạng chuyên nghiệp (Sheet 1: Ma trận Thời khóa biểu 10 tiết $\times$ 7 ngày có kẻ ô, tô màu; Sheet 2: Danh sách chi tiết các môn học và số tín chỉ).

---

## 8. LƯU Ý KHI CHẠY TRÊN 2 MÁY THỰC TẾ (MÁY UBUNTU SERVER & MÁY WINDOWS CLIENT)

Nếu bạn chạy Server trên 1 máy (Ubuntu) và Client trên 1 máy khác (Windows) qua mạng Wi-Fi / LAN:

1. **Lấy địa chỉ IP máy Server (Ubuntu):**
   ```bash
   hostname -I
   ```
   *(Ví dụ IP nhận được: `192.168.1.15`).*
2. **Mở cổng Firewall trên máy Ubuntu Server:**
   ```bash
   sudo ufw allow 9991/tcp
   sudo ufw allow 9992/tcp
   ```
3. **Cấu hình trên máy Client (Windows):**
   - Mở file `client/src/main/resources/application.properties` trên máy Windows, đổi `server.host=127.0.0.1` thành IP máy Server của bạn:
     ```properties
     server.host=192.168.1.15
     ```
4. **Lưu ý về Wi-Fi trường học / công cộng (AP Isolation):**
   - Một số router Wi-Fi trường học bật tính năng cách ly thiết bị (*Client Isolation*), khiến 2 laptop không ping thấy nhau.
   - **Khắc phục:** Hãy bật **Điểm phát sóng di động (Mobile Hotspot từ điện thoại)**, cho cả máy Ubuntu và máy Windows cùng kết nối vào mạng Wi-Fi đó để kiểm thử thông suốt 100%.

---

## 9. TÀI LIỆU KÈM THEO TRONG DỰ ÁN

* [Đặc tả Kiến trúc Hệ thống Phân tán](docs/architecture.md)
* [Đặc tả Giao thức JSON TCP Socket](docs/protocol.md)
* [Từ điển Dữ liệu CSDL PostgreSQL](docs/database.md)
* [Quy trình Kiểm thử Chi tiết](docs/test-scenarios.md)
