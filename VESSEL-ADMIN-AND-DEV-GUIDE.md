# CẨM NANG TOÀN DIỆN CHO ADMIN & DEVELOPER — VESSEL SOVEREIGN SERVER MICROKERNEL (v2.3)

> **Phiên bản tài liệu:** `1.0.0`  
> **Mã phiên bản VESSEL hiện hành:** `VESSEL-2.3.0-rev1046`  
> **Kho lưu trữ GitHub:** `https://github.com/VesselLoader/Vessel`  
> **Tệp thực thi duy nhất:** `build/libs/server.jar`

---

## 1. Giới Thiệu Tổng Quan Về VESSEL

**VESSEL** là một **Universal Minecraft Server Microkernel & Loader** thế hệ mới, cho phép chạy đồng thời cả **6 hệ sinh thái Mod & Plugin** trong **1 tệp `server.jar` duy nhất**:
- **Hệ sinh thái Mod:** `Fabric`, `Quilt`, `Minecraft Forge`, `NeoForge`.
- **Hệ sinh thái Plugin:** `Bukkit`, `Spigot`, `Paper`, `Folia`.
- **Tích hợp sẵn từ trong nhân (Out-of-the-Box):**
  1. **GeyserMC (`Geyser` & `Floodgate` bản release mới nhất)** — Hỗ trợ người chơi Minecraft Bedrock Edition tham gia trực tiếp vào server Java Edition.
  2. **Spark Profiler (`lucko/spark` bản release mới nhất)** — Bộ đo đạc hiệu năng CPU, MSPT, GC và RAM chuẩn công nghiệp.
  3. **Mojang Auto-Latest Bundler Resolver** — Mặc định luôn tự động kiểm tra và tải phiên bản **Minecraft chính thức mới nhất (`latest.release`)** từ Mojang Piston Meta API.

---

## 2. Khởi Động Chỉ 1 Lần Duy Nhất (Single-Pass First-Run Boot)

Khác với các server truyền thống yêu cầu chạy lần đầu để tạo thư mục/file cấu hình rồi bắt buộc phải khởi động lại, **VESSEL hoàn tất toàn bộ 16 bước khởi tạo và mở server ngay trong 1 lần chạy đầu tiên** (`--vessel-single-pass-boot` được bật mặc định):

```bash
# Khởi chạy chuẩn (Tự động tải Minecraft release mới nhất, tích hợp Geyser + Spark, nạp mọi mod/plugin):
java -jar server.jar --nogui

# Khởi chạy kèm bộ Flag Tối Ưu Tối Đa (Khuyên dùng cho Production / Modpack lớn):
java -jar server.jar --vessel-ultra-optimize --nogui

# Khởi chạy kèm Flag Debug Sâu (Khuyên dùng cho Developer hoặc khi cần chẩn đoán lỗi sâu):
java -jar server.jar --vessel-deep-debug --nogui
```

---

## 3. Hướng Dẫn Chi Tiết Toàn Bộ Custom CLI Flags Của VESSEL

### 3.1. Nhóm Flag Tối Ưu Hiệu Năng Tối Đa (`--vessel-ultra-optimize` / `--vessel-max-perf`)

| CLI Flag | Mặc định | Tác dụng chi tiết |
| :--- | :---: | :--- |
| `--vessel-ultra-optimize` / `--vessel-max-perf` | `false` | **Flag tổng hợp:** Bật toàn bộ 6 flag tối ưu bên dưới để đạt hiệu năng 20.0 TPS tối đa. |
| `--vessel-simd-physics` | `false` | Sử dụng **Java 21 Vector API (`FloatVector` SIMD)** để tính toán va chạm AABB và vật lý thực thể theo lô song song. |
| `--vessel-zero-copy-netty` | `false` | Chuyển đường ống mạng Netty sang chế độ **Zero-Copy Direct ByteBuf** và nén gói tin tự thích ứng, giảm tối đa áp lực GC. |
| `--vessel-aggressive-ai-lod` | `false` | Kích hoạt **Dynamic Entity AI Level-of-Detail (LOD)**: giảm tần suất suy nghĩ AI của mob ở xa người chơi mà không làm hỏng farm. |
| `--vessel-zstd-ram-cache` | `false` | Nén các chunk nhàn rỗi và bytecode trong RAM bằng thuật toán từ điển Zstd tốc độ cao, tiết kiệm 35%–50% RAM. |
| `--vessel-lockless-strands` | `false` | Kích hoạt bộ lập lịch **Lockless Regionized Strand** (tương thích Folia) giúp xử lý đa luồng không khóa (lock-free). |
| `--vessel-lazy-dfu-noisium` | `false` | Trì hoãn biên dịch schema DataFixerUpper (LazyDFU) khi khởi động và ghi trực tiếp bảng màu khối (Noisium Direct Palette) khi tạo địa hình. |

### 3.2. Nhóm Flag Debug Sâu Dành Cho Admin & Developer (`--vessel-deep-debug` / `--vessel-debug-all`)

| CLI Flag | Mặc định | Tác dụng chi tiết |
| :--- | :---: | :--- |
| `--vessel-deep-debug` / `--vessel-debug-all` | `false` | **Flag tổng hợp:** Bật toàn bộ 5 cờ trace sâu bên dưới. |
| `--vessel-trace-bytecode` | `false` | Ghi log chi tiết từng lệnh ASM bytecode được dịch tự động (chuyển đổi `ResourceLocation`, `Entity.level`, `InventoryView`...). |
| `--vessel-trace-classloader` | `false` | Theo dõi định tuyến nạp class giữa 3 tầng ClassLoader (Tier-1 Vanilla, Tier-2 Kernel, Tier-3 Ecosystem Spokes). |
| `--vessel-trace-events` | `false` | Theo dõi thứ tự bắn sự kiện xuyên hệ sinh thái (`Fabric` $\leftrightarrow$ `Forge/NeoForge IEventBus` $\leftrightarrow$ `Bukkit/Paper PluginManager`). |
| `--vessel-trace-registry` | `false` | Kiểm toán chi tiết quá trình đóng băng/mở khóa `MappedRegistry`, `DeferredRegister` và đồng bộ Tag/DataPack. |
| `--vessel-profile-boot` | `false` | Đo thời gian thực thi chi tiết (ms) của từng bước trong 16 bước khởi động server. |

### 3.3. Nhóm Flag Điều Khiển Phiên Bản & Tương Thích

| CLI Flag | Mặc định | Tác dụng chi tiết |
| :--- | :---: | :--- |
| `--vessel-single-pass-boot` | `true` | Khởi động hoàn chỉnh trong 1 lần chạy duy nhất kể cả lần đầu tiên (không cần khởi động lại). |
| `--vessel-universal-compat` | `true` | Bật động cơ tương thích ngược đa thế hệ cho các mod/plugin ở phiên bản thấp hơn. |
| `--vessel-auto-latest` | `true` | Luôn tự động phân giải và chạy phiên bản Minecraft chính thức mới nhất (`latest.release`). |
| `--vessel-version=<ver>` | *(Trống)* | Ghim cứng phiên bản Minecraft cụ thể (ví dụ: `--vessel-version=1.21.4` hoặc `--vessel-version=1.20.1`). |
| `--vessel-accept-eula` | `true` | Tự động xác nhận `eula=true` trong `eula.txt`. |
| `--port=<port>` | `25565` | Thiết lập cổng TCP/UDP mặc định trong `server.properties`. |

---

## 4. Quy Tắc Tương Thích Mod/Plugin Đa Thế Hệ & Chặn Mod Bản Cao Hơn Server

1. **Chiều Tương Thích Ngược (`Phiên bản Mod/Plugin <= Phiên bản Server` — CHO PHÉP & TỰ ĐỘNG NÂNG CẤP):**
   - Nếu server đang chạy bản Minecraft mới nhất (hoặc bản cao hơn/bằng bản mà mod/plugin yêu cầu), VESSEL **tự động cho phép nạp** và chạy `MultiEraBytecodeTransformationPipeline` ngay trên RAM (`ResourceLocation.<init>` $\rightarrow$ `fromNamespaceAndPath`/`parse`, `Entity.level` $\rightarrow$ `level()`, `InventoryView` `INVOKEVIRTUAL` $\rightarrow$ `INVOKEINTERFACE`, DataPack `loot_tables/` $\rightarrow$ `loot_table/`).
2. **Chiều Chặn Phiên Bản Tương Lai (`Phiên bản Mod/Plugin > Phiên bản Server` — TỪ CHỐI LOAD THẲNG):**
   - Nếu server đang chạy phiên bản Minecraft **thấp hơn** phiên bản tối thiểu mà mod hoặc plugin yêu cầu, `ForwardVersionModRejectionGuard` sẽ **từ chối load thẳng mod/plugin đó ngay lập tức** và yêu cầu cài bản thấp hơn của mod/plugin hỗ trợ bản server hiện tại hoặc nâng server lên phiên bản Minecraft cao hơn (`--vessel-version=<ver>` hoặc mặc định `latest.release`).

---

## 5. Cơ Chế Auto-Update Từ GitHub Mỗi 1 Tiếng (Không Lag, Không Mất Data, Restart Để Apply Patches)

- **Tự động quét mỗi 1 tiếng (`3600` giây) không gây lag:** `HourlyGitHubAutoUpdateDaemon` chạy trên một Daemon Thread độc lập (`vessel-hourly-auto-updater`), hoàn toàn không gây ảnh hưởng tới TPS của server.
- **Chuỗi mã định danh `VesselBuildVersionCode`:** `VESSEL-<major>.<minor>.<patch>-rev<revision>+mc<mcVer>-geyser<geyserVer>-spark<sparkVer>` giúp VESSEL nhận diện chính xác bản hiện tại đang thấp hơn (`<`), bằng (`==`), hay cao hơn (`>`) bản trên GitHub.
- **Tải Staged Patches khi đang chạy & Apply khi Restart:** Ngay cả khi server đang chạy, VESSEL vẫn tải bản cập nhật cùng phiên bản mới nhất của **Geyser**, **Spark** và **Minecraft `latest.release`** vào `cache/updates/staged/`, đánh dấu `isRestartRequiredToApplyPatches() == true`, và tự động áp dụng các bản vá nhân ngay khi khởi động lại server.
- **Bảo vệ 100% dữ liệu riêng của user (`UserDataPreservationSnapshotGuard`):** Toàn bộ `mods/`, `plugins/`, `config/`, `vessel.yml`, `server.properties`, `eula.txt`, `world/` được bảo vệ tuyệt đối, không bao giờ bị mất hay ghi đè.

---

## 6. Ghi Nhận Bản Quyền & Tác Giả Bên Thứ Ba (Credits & Attributions)

1. **GeyserMC (`Geyser` & `Floodgate`)** — Copyright (c) 2019–2026 GeyserMC Team & Contributors (MIT License) — [https://geysermc.org](https://geysermc.org)
2. **spark (`lucko/spark` Performance Profiler)** — Copyright (c) 2018–2026 Lucko (`lucko`) & Contributors (GPL-3.0 Standalone Profiler Integration) — [https://spark.lucko.me](https://spark.lucko.me)
3. **Minecraft Dedicated Server** — Copyright (c) Mojang AB / Microsoft Corporation.
