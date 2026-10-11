# Prompt đầu vào cho đoạn chat mới — VESSEL, giai đoạn 2

> Dán toàn bộ file này làm tin nhắn đầu tiên của đoạn chat mới và đính kèm `VESSEL-docs.zip` (bản giao cùng prompt này). Hai spec nháp (kiến trúc phân phối/loader và website) đã nằm sẵn trong zip ở `docs/drafts/`; nếu có thể, đính kèm thêm bản gốc của hai file đó.
> Phiên bản prompt: 1.0 · viết ngày 2026-10-09 (đồng hồ container) · khớp với MASTER-SPEC-STATUS v1.0.45.

## 0. Việc đầu tiên phải làm

1. Giải nén `VESSEL-docs.zip`. Đọc `MASTER-SPEC-STATUS.md` (đặc biệt mục 2 quyết định, mục 4 bảng 37 hàng, mục 5 các cập nhật gần nhất, mục 8 và 9), rồi `LOG.md` (entry mới nhất nằm trên cùng), rồi `docs/drafts/README.md` và hai spec nháp.
2. **Các file docs là nguồn đúng.** Nếu bộ nhớ của bạn có dòng tiến độ cũ hơn thì bỏ qua.
3. Trước khi làm bất cứ việc gì, hỏi tôi thật kỹ các chi tiết còn thiếu (mục 6 liệt kê những câu bắt buộc phải hỏi). Hỏi tới khi nào bạn chắc chắn nắm đủ thì mới bắt đầu, dù tôi thấy phiền.
4. Trả lời tôi bằng **tiếng Việt**; mọi file docs viết bằng **tiếng Anh**.

## 1. Dự án và trạng thái bàn giao

- **VESSEL** = *Virtualized Ecosystem Shared Server & Engine Loader*: một loader phía server chạy mod/plugin của nhiều hệ sinh thái (Forge, NeoForge, Fabric, Quilt, plugin Bukkit/Paper) bằng cách đưa chúng về một biểu diễn chung (IR) và các adapter theo từng loader. (Tên đầy đủ lấy từ spec nháp của tôi; tên cũ là USML.)
- **Nghiên cứu Forge đã đạt 100%** của bảng 37 hàng (mục 4.1 của status): 37 hàng DONE, trong đó **một hàng mang nhãn "documented limit"** (hàng 21: trang docs Forge "Model Extensions: Introduction" chưa từng được viết, đã chứng minh bằng lịch sử repo) và **hai hàng (2 và 16) chỉ DONE nhờ quyết định 35**, một quyết định phạm vi tạm thời do trợ lý trước đưa ra mà tôi có thể đảo lại. Tổng thể dự án khoảng 6,3% (145/23).
- Phiên bản các file: MASTER-SPEC-STATUS v1.0.45, `docs/upstream/forge/research.md` v0.4.55 (40 mục, nguồn S1 đến S161), `source-map.json` v0.1.10, `identity-and-collision.md` v0.1.12. Có **35 quyết định** (mục 2 của status). Những quyết định mới nhất cần nhớ: 27 (nguồn sơ cấp thắng nguồn thứ cấp; mâu thuẫn phải ghi lại), 28 (hàng gameplay API cần đủ docs, mã Forge 26.3 và nguồn loại B cho phần vanilla), 29 (id có namespace không bao giờ bị gộp bí danh), 30 (cách làm các hàng còn lại, nhãn "documented limit"), 31 (token selector và lớp argument type là khóa registry), 32 (mod dùng mapping official của Mojang cũng phải nạp được), 33 (nguồn dữ liệu dịch tên official: tải từ Mojang lúc chạy, lưu cache, **không đóng kèm trong gói**), 34 (ranh giới E1a/E1b ở Minecraft 1.2.5, kèm đính chính: Forge cho 1.2.5 vẫn dựa trên ModLoader còn FML 2.x là loader riêng), 35 (hai điều chỉnh phạm vi tạm thời).
- Có một **yêu cầu xem lại không chặn công việc** đang chờ tôi: quyết định 35 và đính chính quyết định 34. Hãy nhắc tôi ở lần báo cáo đầu tiên.

## 2. Quy tắc làm việc (giữ nguyên từ giai đoạn Forge)

- Làm **từng pass một**. Sau mỗi pass báo cáo tiến độ theo %, rồi giao lại `VESSEL-docs.zip` đầy đủ **cùng báo cáo trong cùng một tin nhắn**. Mỗi giai đoạn dùng một bảng hàng riêng (như bảng 37 hàng của Forge) để tính %; con số phải tính lại bằng script từ chính bảng, không tính nhẩm.
- **Mọi thay đổi dù nhỏ nhất** đều tăng version của file đó (version nằm trong file, mỗi thay đổi là một bước tăng) và ghi vào `LOG.md`. Ngày tháng trong log lấy từ **đồng hồ container** (`date -u`), không ghi theo trí nhớ.
- Không bịa. Mỗi mệnh đề phải có nguồn; ghi loại nguồn (A = chính thức hoặc mã nguồn, B = nguồn thứ cấp đáng tin, C = cộng đồng/trích đoạn). Điều chưa đọc phải ghi rõ là chưa đọc. Suy luận phải gắn nhãn suy luận.
- Diễn đạt lại bằng lời của bạn; không chép nguyên văn mã hay tài liệu của người khác vào gói (giấy phép: xem phụ lục A).
- Lỗi của chính bạn phải được ghi vào entry LOG của pass đó (giống các entry pass 22 đến 26).
- Tôi hay thấy các chuỗi dài im lặng; nếu một bước sắp kéo dài, nói trước bằng vài từ bạn đang làm gì.

## 3. Thứ tự công việc (đã chốt với tôi)

Làm lần lượt, **không nhảy cóc**:

**Giai đoạn A — Đào sâu kiến trúc của Sinytra Connector** (tài liệu tham khảo kiến trúc, **không** bắt đầu adapter Fabric nào). Phạm vi tôi đã chọn: **toàn bộ**: nạp mod, remap, mixin, cầu nối registry/event, Forgified Fabric API, data/resources. Sản phẩm: một thư mục `docs/upstream/sinytra/` (file nghiên cứu theo mẫu của `research.md`, có bảng nguồn riêng, và một file "bài học thiết kế cho VESSEL"), cộng bản nâng cấp các phần liên quan của spec loader nháp. Bắt đầu bằng bảng hàng khởi điểm ở phụ lục A.

**Giai đoạn B — Website của dự án.** Tôi **chưa có repo** cho dự án. Sản phẩm: bản đặc tả website đầy đủ (nâng cấp từ spec nháp), kèm kế hoạch tạo repo và bộ file khởi tạo đủ để tôi commit. Hỏi tôi trước các câu ở mục 6 (tên, tổ chức, tên miền…).

**Giai đoạn C — Tất cả các loader còn lại** (NeoForge, Fabric, Quilt, plugin…), mỗi loader được nghiên cứu theo đúng phương pháp đã dùng cho Forge: bảng hàng riêng, các pass có kế hoạch, nguồn loại A/B/C, mâu thuẫn ghi lại theo quyết định 27, nhãn "documented limit" chỉ khi đã chứng minh phần đó không thể lấy. Thứ tự các loader do tôi quyết ở đầu giai đoạn này.

Giai đoạn B có thể chạy xen với A nếu tôi bảo; mặc định là A trước, B sau, C cuối.

## 4. Xung đột cần xử lý ngay trong chat mới: "Forge gate"

Spec loader nháp có một "Forge gate" cứng: không bắt đầu loader mới trước khi Forge đạt 100%, và định nghĩa "Forge = 100%" gồm **cả adapter, runtime và kiểm thử đầu cuối** (mục 2.1 đến 2.3 và mục 49 của spec nháp). Còn tôi vừa chốt: nghiên cứu Forge 100% **trước**, rồi mới sang Sinytra, website, và nghiên cứu các loader khác. Hai điều này mâu thuẫn nhau nếu hiểu "gate" theo nghĩa của bản nháp.

Việc cần làm: nêu mâu thuẫn này **ngay câu hỏi đầu tiên**, đề xuất tách gate thành hai cổng (cổng nghiên cứu: Forge Research 100% đã PASS; cổng triển khai: adapter Forge, runtime, E2E chưa bắt đầu) và các điều kiện cho phép từng loại việc (giai đoạn A và B không phụ thuộc cổng; nghiên cứu loader khác ở giai đoạn C chỉ sau khi tôi duyệt định nghĩa cổng mới; **việc viết adapter của loader khác vẫn bị khóa cho tới khi cổng triển khai của Forge PASS**). Ghi kết quả thành quyết định mới (số 36) trong status và bản spec nâng cấp. Không tự ý bỏ gate.

## 5. Hai spec nháp: cách đối xử

Hai file `VESSEL-DISTRIBUTION-AND-LOADER-ARCHITECTURE-SPEC-v1_0.md` (khoảng 2.180 dòng, 52 mục và 2 phụ lục: bootstrap, installer, cập nhật, manifest, GitHub Pages/Releases/Actions, giao dịch cập nhật, bảo mật, ranh giới adapter…) và `VESSEL-PROJECT-WEBSITE-SPEC-v1_0.md` (khoảng 1.930 dòng, 72 mục; đầu file ghi "Design contract for implementation" nhưng tôi coi đây là bản nháp) **chỉ là bản sơ khai, không phải bản sẽ được áp dụng**. Hãy tham khảo những chỗ hữu ích rồi **nâng cấp**, không chép lại:

- đưa 35 quyết định và các phát hiện của nghiên cứu Forge vào spec (era model theo quyết định 34, các khóa registry theo quyết định 8, 17, 20, 29, 31, mô hình mapping theo 32 và 33…);
- **chính sách mapping**: spec phân phối phải khớp quyết định 33: gói VESSEL không chứa file mapping của Mojang; tải lúc cần, cache, kiểm tra theo phiên bản game; chủ server có thể tự cung cấp file; thiếu dữ liệu thì mod cần dịch **không được nạp, kèm thông báo rõ ràng**;
- đưa bài học từ Sinytra vào kiến trúc loader (giai đoạn A);
- sửa định nghĩa Forge gate (mục 4 ở trên);
- kiểm tra lại các con số, ràng buộc kỹ thuật (giới hạn GitHub Pages, quota, định dạng manifest…) bằng nguồn hiện hành, vì chúng được viết từ trí nhớ.

## 6. Câu hỏi bắt buộc phải hỏi tôi trước khi bắt đầu

1. **Forge gate**: tôi có đồng ý với đề xuất tách hai cổng ở mục 4 không (và nếu không, định nghĩa nào)?
2. **Giai đoạn A**: độ sâu mong muốn trên từng phần (nạp mod, remap, mixin, registry/event, Forgified Fabric API, data/resources, server-only); chỉ đọc mã và docs hay cần cả thử dựng (môi trường hiện chỉ cho clone và đọc GitHub, không dựng được Minecraft); bài học thiết kế nên nằm trong một file riêng hay trong spec nâng cấp.
3. **Quyết định 35 và đính chính quyết định 34**: tôi duyệt hay đảo lại?
4. **Giai đoạn B**: tên dự án trên web, tổ chức hoặc tài khoản GitHub, tên repo, tên miền (hoặc github.io), ngôn ngữ của website, nội dung bắt buộc (tải về, docs, trang trạng thái…), ai duyệt nội dung.
5. **Giai đoạn C**: thứ tự các loader và số hàng/pass mong muốn.
6. Kích thước mỗi pass và việc có muốn nhắc lại `date -u` trong mỗi báo cáo hay không.

## Phụ lục A — Ghi chú khởi điểm về Sinytra Connector (nhìn sơ, **chưa kiểm chứng**)

Đây là những gì trợ lý trước thấy trong một lần xem ngắn (2026-10-07 đến 2026-10-08), dừng lại theo chỉ dẫn của tôi; **chưa có gì được ghi vào gói docs**. Mọi điều dưới đây phải được đọc lại và kiểm chứng trước khi trở thành docs.

**Repo, nhánh, đầu mốc, giấy phép** (clone nông):

| Repo | Nhánh đã xem | Đầu mốc | Giấy phép (file LICENSE) |
|---|---|---|---|
| `Sinytra/Connector` | `26.1.x` · `1.21.x` · `1.20.1` (còn `dev`, `master`) | `67b3cc58278696ddc8c6ae6515a0de52266258ab` (2026-09-01) · `5a2f668ad492c1586c279a197423f5a1d1a16509` (2026-09-26) · `7f68ac02291fde986119a3f5cab85436bed5c350` (2026-09-13) | MIT (Su5eD) |
| `Sinytra/Adapter` | `26.1.x` (còn `1.21.x`, `1.20.1`) | `60fdfcdfc5dc04c5911c2b674a55f6f5bd3beeab` (2026-08-14) | MIT (Sinytra) |
| `Sinytra/ForgifiedFabricAPI` | `26.2.x` (còn `26.1.2`, `1.21.1`, `1.21`, `1.20.4`, `1.20.1`, `1.19.4`, `neo/1.20.6`, `old/1.20.6`, `master`) | `1683ac22b45abda068a9bea3d8581c9161c1b326` (2026-10-07) | Apache-2.0 |
| `Sinytra/ConnectorExtras` | `26.1.x` | `0f36aae25d17ef9d07f010e9fbfe24a543c04e60` (2026-09-13) | MIT (Su5eD) |
| `Sinytra/Launchpad` | `master` | `804f327545626f17441521c70a45d0b64b483c8f` (2026-08-15) | GNU GPL (README nói v3 kèm ngoại lệ classpath) |
| `Sinytra/ForgifiedFabricLoader` | (chưa clone) có các nhánh `1.19.4`, `1.20.1`, `1.20.4`, `1.20.6`, `1.21.x`, `26.1.x` | — | chưa xem |

**Tóm tắt những gì đã thấy (cần kiểm chứng):**
- Connector là lớp tương thích chạy **mod Fabric trên NeoForge** (không phải Forge). Docs trong repo (`docs/docs/*.mdx`): giới thiệu, cấu hình (`config/connector.json`: `hiddenMods`, `globalModAliases`, `enableMixinSafeguard`…), API plugin (`TransformerPlugin` nạp qua ServiceLoader, đăng ký transformer cho jar và patch cho Mixin), hướng dẫn cho nhà phát triển (mod placeholder `connector:placeholder`), FAQ (hybrid server như Mohist/Magma/Arclight không được hỗ trợ; không backport cho bản trước 1.20.1; không tương thích OptiFine). Yêu cầu dùng **Forgified Fabric API** thay cho Fabric API gốc.
- Luồng nạp (nhánh 26.1.x): `ConnectorLocator` là một `IDependencyLocator` ưu tiên thấp nhất; `ConnectorModFileReader` (`IModFileReader`) đánh dấu jar Fabric là "stub" mang metadata Fabric; jar được biến đổi rồi tạo lại thành mod file cho FML; có cache kết quả biến đổi, biến đổi song song theo jar, hợp nhất split package cho JPMS, và một "safeguard" phát hiện lỗi Mixin. Các dịch vụ `ClassProcessor` (`ConnectorClassProcessor`, `ConnectorMethodProcessor`, `FabricASMClassGenerator`) biến đổi lớp lúc chạy; `ModuleLayerMigrator` xử lý lớp module.
- Pipeline biến đổi jar (module `transformer`, dựa trên công cụ đổi tên tự động của NeoForged): bỏ chữ ký jar; **viết lại `fabric.mod.json`** (chuẩn hóa id: `-` thành `_`, tránh từ khóa JPMS bằng hậu tố, ghi id gốc vào `provides`, bỏ entrypoint MixinExtras vì NeoForge có sẵn, thêm bí danh phụ thuộc, bỏ đoạn patch của phiên bản Fabric API); chuyển truy cập kiểu class tweaker/access widener; phân tích lớp; **áp patch Mixin** bằng thư viện Adapter; chuyển hướng accessor; gỡ phần riêng của môi trường client/server.
- **Remap**: các nhánh `1.20.1` và `1.21.x` có `OptimizedRenamingTransformer` đổi tên **Intermediary → tên chạy của NeoForge** (kể cả chuỗi trong annotation Mixin, hằng LDC, tham số invokedynamic, refmap), dựa trên một bản fork công cụ đổi tên; dữ liệu mapping lấy từ artifact Minecraft đã đổi tên mà loader cung cấp (cần kiểm chứng). Nhánh `26.1.x` **không còn bước remap** vì game không còn obfuscate — khớp với quyết định 32 và 33.
- Launchpad: công cụ đọc `fabric.mod.json`, chạy entrypoint, chuyển class tweaker, hỗ trợ jar lồng nhau; **Forgified Fabric Loader** cài API của Fabric Loader trên NeoForge. Forgified Fabric API: hàng chục module theo tên `fabric-*-v1/v2` (ví dụ `fabric-command-api-v2`, `fabric-biome-api-v1`…).
- Chưa đọc: Forgified Fabric Loader; bên trong Forgified Fabric API (cầu nối event, registry, networking, data/resources); các mixin của chính Connector (registry, network, entity data serializer, POI); `ConnectorExtras`; mô hình nhánh/phiên bản và CI của họ.

**Bảng hàng khởi điểm cho giai đoạn A (đề xuất, hãy chỉnh cùng tôi):**
1 bản đồ repo/nhánh/giấy phép · 2 phát hiện mod (locator, reader, stub) · 3 viết lại metadata Fabric · 4 pipeline biến đổi jar và cache · 5 remap (1.20.1, 1.21.x) và việc bỏ remap ở 26.1 · 6 Mixin (refmap, safeguard, patch) · 7 thư viện Adapter · 8 `ClassProcessor` lúc chạy · 9 split package/JPMS · 10 gỡ môi trường client/server · 11 entrypoint và giả lập Fabric Loader (Launchpad, Forgified Fabric Loader) · 12 cấu trúc và cách làm của Forgified Fabric API · 13 cầu nối registry · 14 cầu nối event · 15 cầu nối networking/payload · 16 data/resources và điều kiện nạp · 17 cấu hình và API plugin · 18 phụ thuộc, bí danh, phiên bản · 19 jar lồng nhau · 20 class tweaker/access widener → AT · 21 enum extension, fluid type · 22 các giới hạn đã công bố (hybrid server…) · 23 chẩn đoán lỗi · 24 kiểm thử/CI và mô hình phiên bản · 25 hệ quả giấy phép cho VESSEL (MIT, Apache-2.0, GPL kèm ngoại lệ classpath) · 26 so sánh với hướng IR của VESSEL: chỗ nào nên học, chỗ nào VESSEL khác (VESSEL chạy trên server phần mềm riêng và đưa mod về IR; Sinytra biến đổi tại chỗ để mod Fabric chạy trên NeoForge).

## Phụ lục B — Bản đồ gói docs

- `MASTER-SPEC-STATUS.md`: trạng thái, quyết định, bảng 37 hàng, hướng dẫn bàn giao (mục 9 có mục ghi chú công cụ).
- `LOG.md`: nhật ký mọi thay đổi (entry mới nhất trên cùng).
- `docs/upstream/forge/research.md` (40 mục) · `source-map.json` · `version-index.json` · `metadata-inventory.json` · `vanilla-metadata-table.json`.
- `docs/architecture/identity-and-collision.md`: luật định danh và va chạm khóa.
- `docs/drafts/`: hai spec nháp của tôi (**bản sơ khai**, giữ nguyên, kèm README).
- `handoff/NEW-CHAT-PROMPT-STAGE-2.md`: bản sao của prompt này.

Công cụ tôi đã dùng và hạn chế: bash chỉ truy cập `github.com`, `raw.githubusercontent.com`, các registry gói; docs của Forge/NeoForged/Fabric nên đọc từ repo markdown; lịch sử lớn nên clone `--filter=blob:none`; kho Mojang không truy cập được từ bash.
