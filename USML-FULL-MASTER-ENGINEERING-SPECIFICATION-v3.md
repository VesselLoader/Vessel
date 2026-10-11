# UNIVERSAL SERVER-SIDE MOD LOADER

## Full Master Engineering Specification v3

## 0. ĐỊNH NGHĨA PROJECT

Tên tạm:

**USML — Universal Server-Side Mod Loader**

USML là một runtime platform mới cho Minecraft, mục tiêu:

```text
FORGE MOD
FABRIC MOD
NEOFORGE MOD
QUILT MOD
BUKKIT PLUGIN
SPIGOT PLUGIN
PAPER PLUGIN
FOLIA PLUGIN
CANVAS PLUGIN
PURPUR PLUGIN
JAVA RESOURCE PACK
JAVA DATA PACK
JAVA TEXTURE INPUT
BEDROCK RESOURCE PACK
BEDROCK BEHAVIOR PACK
BEDROCK ADD-ON
BEDROCK TEXTURE INPUT
BEDROCK SCRIPT
          ↓
      USML
          ↓
 UNIVERSAL SEMANTIC IR
          ↓
 SERVER RUNTIME
          ↓
 ┌───────────────────┬──────────────────┐
 │                   │                  │
JAVA PRESENTATION  BEDROCK            DATA
BACKEND            PRESENTATION       BACKEND
                    BACKEND
```

Mục tiêu không phải là tạo một “Forge-to-Paper translator” đơn giản.

Mục tiêu là:

> **tách semantic của mod/plugin khỏi loader/server implementation ban đầu, chuyển chúng vào một runtime trung gian, sau đó dùng adapter/compiler phù hợp với target.**

---

# 1. CORE PRINCIPLE

```text
SERVER = SOURCE OF TRUTH
CLIENT = PRESENTATION + INPUT
```

Server sở hữu:

```text
gameplay
logic
physics
AI
collision
inventory
combat
entities
blocks
world state
persistence
economy
commands
permissions
network authority
```

Client sở hữu:

```text
rendering
visualization
animation interpolation
textures
models
audio playback
particle rendering
UI presentation
input
```

Client không được là authority đối với:

```text
damage
item ownership
inventory validity
physics result
entity state
economy
permissions
```

---

# 2. CURRENT VERSION POLICY

Project phải hỗ trợ **toàn bộ phiên bản hiện hành được upstream phát hành/duy trì trong phạm vi support policy**, không hard-code vài version cố định.

Tại thời điểm snapshot nghiên cứu hiện tại:

* Fabric đã có Minecraft 26.2 và tài liệu Fabric 26.2; bài release của Fabric nêu Loader 0.19.3 tại thời điểm phát hành. Source tree hiện tại của Fabric Loader cũng đã tiến tới 0.19.5, cho thấy release registry và source-head phải được phân biệt.
* Forge có nhánh Minecraft 1.21.11 với Forge 61.2.1 và danh sách các build trước đó.
* NeoForge đã có nhánh 26.2 stable và tài liệu migration sang 26.3, vì vậy 26.3 phải được registry coi là một target/version candidate riêng chứ không assume 26.2 API còn nguyên.
* Spigot master đã cập nhật Minecraft 26.2; Javadocs hiện có API snapshot 26.3-R0.1-SNAPSHOT.
* Quilt changelog index hiện liệt kê loader 0.31.0 và các bản trước đó.

Những con số này chỉ là **initial snapshot**.

Không được nhúng trực tiếp:

```java
if (version.equals("26.2")) ...
```

vào core.

---

# 3. VERSION REGISTRY

Tạo:

```text
versions/
├── minecraft/
├── fabric/
├── forge/
├── neoforge/
├── quilt/
├── bukkit/
├── spigot/
├── paper/
├── folia/
├── canvas/
├── purpur/
├── geyser/
└── bedrock/
```

Mỗi version profile:

```yaml
id: "..."
minecraft-version: "..."
platform: "..."
platform-version: "..."
api-version: "..."

status:
  stable: true
  beta: false
  snapshot: false
  maintained: true

java-version: "..."
mapping:
  namespace: "..."
  version: "..."

protocol:
  java: ...
  bedrock: ...

pack:
  resource-pack-format: ...
  data-pack-format: ...

capabilities:
  display-entity: true
  block-display: true
  item-display: true
  item-components: true
  server-resource-pack: true
  datapack-discovery: true
  registry-mutation: true
  ...
```

---

# 4. VERSION STATUS MODEL

```text
CURRENT_STABLE
CURRENT_BETA
CURRENT_SNAPSHOT
MAINTAINED
LEGACY_SUPPORTED
EOL
UNSUPPORTED
UNKNOWN
```

“Current” phải được xác định dựa trên upstream registry/release channels, không dựa vào một danh sách tĩnh do developer viết vài tháng trước.

---

# 5. UPSTREAM RESEARCH REGISTRY

Tạo:

```text
docs/upstream/
```

Mỗi platform:

```text
docs/upstream/fabric/
docs/upstream/forge/
docs/upstream/neoforge/
docs/upstream/quilt/
docs/upstream/bukkit/
docs/upstream/spigot/
docs/upstream/paper/
docs/upstream/folia/
docs/upstream/canvas/
docs/upstream/purpur/
docs/upstream/geyser/
docs/upstream/bedrock/
```

Mỗi version:

```text
docs/upstream/<platform>/<version>/
├── docs.md
├── api-inventory.json
├── mappings.json
├── lifecycle.json
├── capabilities.json
├── threading.json
├── rendering.json
├── networking.json
├── filesystem.json
├── changes.md
└── sources.json
```

---

# 6. SOURCE AUTHORITY

Thứ tự authority:

```text
1. Official documentation
2. Official source repository
3. Official Javadocs
4. Official mappings
5. Official migration primers
6. Official changelogs
7. Official tests/examples
8. Maintainer issues/PRs
9. Community docs
10. Tutorials/blogs
```

Không dùng tutorial để suy ra behavior khi source hoặc Javadocs có thể kiểm tra.

---

# 7. RESEARCH COMPLETENESS

Không được đánh dấu platform/version là “researched” khi chưa có:

```text
metadata
loader lifecycle
dependency system
classloading
resources
filesystem
config
data
registries
events
commands
scheduler
networking
persistence
blocks
items
entities
AI
physics
rendering
client/server split
mixins
transformers
worldgen
resource packs
datapacks
threading
error handling
```

---

# 8. PUBLIC API INVENTORY

Mỗi version phải inventory:

```text
package
class
interface
enum
annotation
constructor
field
method
generic type
parameter
return type
exception
event
callback
registry
scheduler
resource loader
networking endpoint
```

Mỗi symbol:

```yaml
symbol: "..."
platform: "..."
version: "..."
side: SERVER/CLIENT/COMMON
thread: MAIN/REGION/ENTITY/ASYNC/UNKNOWN
semantic: "..."
replacement: "..."
convertibility: FULL/PARTIAL/UNSUPPORTED
```

---

# 9. ROOT DIRECTORY CONTRACT

USML server root:

```text
/
├── universal-loader.jar
│
├── mods/
├── plugins/
│
├── config/
├── defaultconfigs/
│
├── data/
│   ├── mods/
│   ├── plugins/
│   ├── shared/
│   ├── worlds/
│   ├── players/
│   └── server/
│
├── cache/
├── libraries/
├── runtime/
├── generated/
├── logs/
├── temp/
├── reports/
├── quarantine/
├── backups/
│
├── datapacks/
├── resourcepacks/
├── texturepacks/
├── addons/
│
├── bedrock/
│   ├── addons/
│   ├── resourcepacks/
│   ├── behaviorpacks/
│   ├── texturepacks/
│   └── scripts/
│
└── world/
    └── ...
```

---

# 10. CRITICAL DIRECTORY RULE

`mods/` và `plugins/` là **INSTALLATION/BINARY ROOTS**.

Không còn coi:

```text
plugins/<plugin>/
```

là data root mặc định của USML.

USML canonical data:

```text
data/plugins/<plugin-id>/
```

Tương tự:

```text
data/mods/<mod-id>/
```

---

# 11. WHY PLUGIN DATA MUST BE SEPARATED

Bukkit/Spigot/Paper expose `Plugin#getDataFolder()`, `JavaPlugin#getDataFolder()`, `saveConfig()`, `saveDefaultConfig()`, `saveResource()` và các API resource/data riêng. Plugin docs mô tả data folder là nơi chứa file dữ liệu của plugin; `saveResource()` cũng lưu embedded resource vào data folder và giữ nguyên hierarchy.

USML phải giữ **semantic này**, nhưng thay physical root:

```text
Source:
plugins/MyPlugin/

USML:
data/plugins/MyPlugin/
```

Plugin code vẫn thấy:

```java
getDataFolder()
```

nhưng trả về:

```text
<data-root>/plugins/MyPlugin/
```

---

# 12. PLUGIN BINARY VS PLUGIN DATA

Phải phân biệt hoàn toàn:

```text Module Binary:
plugins/MyPlugin.jar

Module Resource:
inside MyPlugin.jar

Module Config:
data/plugins/MyPlugin/config.yml

Module Persistent Data:
data/plugins/MyPlugin/

Module Cache:
cache/plugins/MyPlugin/

Module Generated:
generated/plugins/MyPlugin/

Module Logs:
logs/plugins/MyPlugin/

Module Temp:
temp/plugins/MyPlugin/
```

---

# 13. MOD BINARY VS MOD DATA

```text mods/Create.jar

config/create-common.toml
world/serverconfig/create-server.toml

data/mods/create/
cache/mods/create/
generated/mods/create/
logs/mods/create/
temp/mods/create/
```

Fabric không có một API chuẩn tương đương `Plugin#getDataFolder`; Loader cung cấp game directory và config directory. Fabric Loader cũng load mods từ classpath và `mods` directory, còn `fabric.modsFolder` có thể đổi vị trí mods folder.

USML do đó phải **tạo một per-module data root của riêng nó**.

---

# 14. UNIVERSAL MODULE FILESYSTEM

Tạo:

```text
ModuleFilesystem
```

Mỗi module có:

```text
ModuleFilesystemContext
```

fields:

```text moduleId
moduleType
sourcePlatform
sourceVersion
targetPlatform
targetVersion

binaryPath
resourceRoot
configRoot
dataRoot
worldDataRoot
cacheRoot
tempRoot
generatedRoot
logsRoot
libraryRoot
sharedRoot
```

---

# 15. STORAGE CLASSES

```text
INSTALLATION
RESOURCE
CONFIG
DEFAULT_CONFIG
SERVER_CONFIG
WORLD_DATA
PLAYER_DATA
GLOBAL_DATA
MODULE_DATA
SHARED_DATA
CACHE
TEMP
GENERATED
LOG
LIBRARY
BACKUP
MIGRATION
NATIVE
DATABASE
EXTERNAL
SECRET
```

Mọi filesystem access phải được classify vào một storage class.

---

# 16. CONFIG CLASSIFICATION

USML bắt buộc phân biệt:

```text STARTUP_CONFIG
CLIENT_CONFIG
COMMON_CONFIG
SERVER_CONFIG
WORLD_CONFIG
DEFAULT_CONFIG
PLUGIN_CONFIG
RUNTIME_CONFIG
CACHE_CONFIG
SECRET_CONFIG
MIGRATION_CONFIG
EXTERNAL_CONFIG
```

Không được coi mọi `.yml`, `.json`, `.toml` là cùng một loại config.

---

# 17. FORGE CONFIG

Forge có config types:

```text CLIENT
COMMON
SERVER
```

và Forge docs xác định:

```text CLIENT:
client-only
<game>/config

COMMON:
both sides
client config
server config

SERVER:
server-only
world/serverconfig
synced to client
```

Forge docs cũng mô tả server config trong `<server_folder>/world/serverconfig`.

USML phải preserve semantic này.

---

# 18. NEOFORGE CONFIG

NeoForge bổ sung:

```text STARTUP
CLIENT
COMMON
SERVER
```

với:

```text STARTUP
read immediately
not synced

CLIENT
physical client only

COMMON
physical client + physical server

SERVER
world-specific
synced
```

NeoForge còn hỗ trợ `worldRestart` và `gameRestart`, và configuration events cho Loading/Reloading/Unloading.

USML phải model:

```text restartRequirement:
NONE
WORLD
GAME
```

---

# 19. FABRIC CONFIG

Fabric Loader có:

```text getGameDir()
getConfigDir()
```

và config directory mặc định gắn với game directory.

USML phải:

```text map getGameDir() -> virtual server root
map getConfigDir() -> config/
```

---

# 20. QUILT CONFIG/DIRECTORIES

Quilt Loader cho phép:

```text loader.modsDir
loader.configDir
loader.cacheDir
loader.addMods
```

và các mod folders có semantics riêng cho nested folders, `.disabled`, hidden files, version folders, v.v.

USML phải parse và normalize toàn bộ path semantics này.

---

# 21. PAPER PLUGIN CONFIG

Paper docs xác định plugin configuration mặc định nằm trong plugin data folder; `config.yml` embedded sẽ được copy vào đó khi plugin lưu default config. Paper cũng cho phép custom configuration files trong chính plugin data folder.

USML phải map:

```text getConfig()
saveConfig()
reloadConfig()
saveDefaultConfig()
saveResource()
```

đến:

```text data/plugins/<id>/
```

---

# 22. PLUGIN DATA API SEMANTICS

Phải preserve:

```text getDataFolder()
getConfig()
saveConfig()
reloadConfig()
saveDefaultConfig()
saveResource()
getResource()
getTextResource()
```

`getResource()` và `getTextResource()` là **embedded JAR resource access**, không phải persistent disk data. `saveResource()` mới là bridge từ embedded resource → persistent data folder.

---

# 23. PLUGIN BINARY API SEMANTICS

Phải preserve:

```text getFile()
getClassLoader()
getPluginLoader()
getDescription()
getServer()
getLogger()
getSLF4JLogger()
getComponentLogger()
```

Đặc biệt:

```text getFile()
```

phải trỏ tới **actual binary**:

```text plugins/MyPlugin.jar
```

không được trả về:

```text data/plugins/MyPlugin/
```

---

# 24. PLUGIN LIFECYCLE

Universal lifecycle:

```text DISCOVER
LOAD_METADATA
CONSTRUCT
ON_LOAD
BOOTSTRAP
REGISTER
ENABLE
RUN
DISABLE
UNLOAD
```

Bukkit/Paper lifecycle phải map:

```text onLoad()
onEnable()
onDisable()
```

Paper bootstrapper/plugin loader phải map sang:

```text UniversalBootstrap
UniversalModuleLoader
```

Paper plugins có `bootstrap`, `loader` và dependency stages khác nhau; bootstrap dependencies và runtime server dependencies phải được model riêng.

---

# 25. PAPER DEPENDENCY GRAPH

Phải support:

```text depend
softdepend
loadbefore
provides
libraries
```

và Paper plugin dependencies:

```text bootstrap
server
load BEFORE
load AFTER
required
optional
join-classpath
```

Không được gộp chúng thành một boolean `dependency=true`.

---

# 26. DEPENDENCY PHASES

Dependency graph phải phân biệt:

```text BOOTSTRAP_DEPENDENCY
REGISTRATION_DEPENDENCY
RUNTIME_DEPENDENCY
OPTIONAL_DEPENDENCY
PROVIDED_DEPENDENCY
LIBRARY_DEPENDENCY
```

---

# 27. CYCLIC DEPENDENCIES

Graph solver phải detect:

```text A -> B -> A
A -> B -> C -> A
```

và report cycle.

Không được silently break cycle bằng thứ tự ngẫu nhiên.

Paper plugin documentation explicitly mô tả circular plugin loading là lỗi cần xử lý.

---

# 28. LIBRARIES

Plugin `libraries` declarations phải được chuyển sang:

```text libraries/plugins/<plugin-id>/
```

Không đặt tất cả dependencies trực tiếp vào root classpath.

Paper có thể tải libraries khai báo trong `plugin.yml`; đây phải là một `LibraryDependency` IR riêng.

---

# 29. MOD NESTED JARS

Fabric hỗ trợ nested JARs, và nested mods có thể tiếp tục chứa nested mods; chúng được load mà không nhất thiết extract ra filesystem.

USML phải có:

```text EmbeddedModule
NestedDependency
```

và không được assume dependency luôn là một file `.jar` riêng trong `mods/`.

---

# 30. MODULE RESOURCE ROOT

Mỗi source JAR có:

```text RESOURCE_ROOT
```

read-only.

Ví dụ Fabric:

```text src/main/resources/
```

chứa:

```text assets/
fabric.mod.json
mixins
```

và client resources riêng có thể tồn tại trong client resource set.

USML phải expose:

```text ModuleResources.get(...)
```

cho converted code.

---

# 31. EMBEDDED RESOURCE

Phải giữ nguyên semantic:

```text resource exists inside JAR
```

và:

```text persistent file copied from resource
```

là hai operation khác nhau.

---

# 32. VIRTUAL CURRENT WORKING DIRECTORY

Đây là phần CỐT LÕI.

Mỗi module có một virtual working directory:

```text MODULE_CWD
```

Ví dụ plugin:

```text relative:
./data.json
```

USML resolve thành:

```text data/plugins/myplugin/data.json
```

mod:

```text ./cache.bin
```

resolve thành:

```text data/mods/mymod/cache.bin
```

theo context của module.

---

# 33. WHY RELATIVE PATH MUST BE VIRTUALIZED

Không thể chỉ xử lý:

```text getDataFolder()
```

vì arbitrary mod/plugin có thể làm:

```java
new File("foo")
Paths.get("foo")
Files.write(...)
new FileOutputStream("foo")
```

mà không hề gọi loader API.

USML phải instrument các call site.

---

# 34. JAVA.IO INTERCEPTOR

Phân tích và rewrite call sites cho:

```text File
FileInputStream
FileOutputStream
FileReader
FileWriter
RandomAccessFile
FileChannel
PrintWriter
Scanner
```

---

# 35. JAVA.NIO INTERCEPTOR

Phải hỗ trợ:

```text Paths.get
Path.of
Files.newInputStream
Files.newOutputStream
Files.newBufferedReader
Files.newBufferedWriter
Files.readAllBytes
Files.write
Files.copy
Files.move
Files.delete
Files.exists
Files.isDirectory
Files.createDirectory
Files.createDirectories
Files.list
Files.walk
Files.find
Files.newDirectoryStream
Files.newByteChannel
Files.newFileChannel
Files.size
Files.getLastModifiedTime
Files.setLastModifiedTime
Files.readAttributes
Files.setAttribute
```

---

# 36. WATCHSERVICE

Không được quên:

```text WatchService
WatchKey
register()
ENTRY_CREATE
ENTRY_MODIFY
ENTRY_DELETE
OVERFLOW
```

Nếu plugin watch:

```text config.yml
```

USML phải map watch path tới canonical path và trả event dưới semantic path mà plugin mong đợi.

---

# 37. ASYNC FILE IO

Phải hỗ trợ:

```text AsynchronousFileChannel
AsynchronousFileChannel.open()
```

---

# 38. MEMORY-MAPPED FILES

Phải hỗ trợ:

```text FileChannel.map()
MappedByteBuffer
```

nếu data nằm trong virtual module storage.

---

# 39. FILE LOCKING

Phải preserve:

```text FileLock
tryLock()
lock()
release()
```

và report unsupported cross-filesystem locking nếu backend không thể emulate.

---

# 40. DIRECTORY LISTING

Rất quan trọng:

```java
new File(".").listFiles()
```

không được bị bỏ qua.

Instrument:

```text File.list
File.listFiles
Files.list
Files.walk
DirectoryStream
```

để trả virtual view khi module đang ở virtual filesystem mode.

---

# 41. VIRTUAL ROOT VIEW

Một module có thể nhìn thấy:

```text .
├── config/
├── data/
├── cache/
├── generated/
└── ...
```

theo compatibility profile.

Mục tiêu là tránh plugin viết:

```java
new File(".").listFiles()
```

rồi không thấy file mà nó vừa tạo.

---

# 42. MODULE FILETREE MODES

```text STRICT
VIRTUAL
LEGACY
HYBRID
```

`HYBRID` mặc định.

---

# 43. HYBRID FILESYSTEM

Ví dụ module truy cập:

```text ./foo
```

USML thử:

```text module-data/foo
```

Nếu legacy mapping tồn tại:

```text legacy/root/foo
```

thì resolver dùng mapping đã lưu.

---

# 44. ABSOLUTE PATH HANDLING

Nếu plugin gọi:

```text /absolute/path
```

USML phải classify:

```text INSIDE_SERVER_ROOT
OUTSIDE_SERVER_ROOT
OS_GLOBAL
TEMP
HOME
USER_APPDATA
UNC
NETWORK_SHARE
```

---

# 45. ABSOLUTE PATH POLICY

Default:

```text INSIDE_ROOT:
allow + classify

OUTSIDE_ROOT:
deny or isolated redirect

OS_GLOBAL:
deny

NETWORK_SHARE:
deny
```

Có override admin.

---

# 46. WINDOWS PATHS

Support:

```text C:\
C:relative
\\server\share
\\?\C:\
UNC
```

---

# 47. UNIX PATHS

Support:

```text /
/tmp
/home/user
../
```

với escape detection.

---

# 48. PATH NORMALIZATION

Mỗi access:

```text raw path
↓
absolute normalization
↓
symlink resolution policy
↓
module context
↓
storage classification
↓
ownership resolution
↓
physical path
```

---

# 49. PATH TRAVERSAL

Detect:

```text ../
..\ 
%2e%2e
symlink escape
junction escape
```

---

# 50. SYMLINK POLICY

Admin config:

```yaml
filesystem:
  symlinks:
    allow: false
    resolve: true
    permit-inside-root-only: true
```

---

# 51. FOREIGN ROOT DETECTION

Nếu mod/plugin tự tạo:

```text /SomeFolder/
```

USML không được crash.

Phải ghi:

```text FOREIGN_ROOT_DISCOVERED
```

với:

```text owner module
path
first access
operation
callsite
timestamp
```

---

# 52. FOREIGN ROOT OWNERSHIP

Phân loại:

```text MODULE_PRIVATE
SHARED
SERVER_GLOBAL
WORLD_SCOPED
PLAYER_SCOPED
PLUGIN_ECOSYSTEM
LIBRARY
CACHE
TEMP
UNKNOWN
```

---

# 53. FOREIGN ROOT REGISTRY

Tạo:

```text config/filesystem.yml
```

và:

```text data/server/filesystem-registry.json
```

Example:

```yaml
foreign-roots:
  "journeymap":
    owner: "journeymap"
    type: MODULE_PRIVATE

  "schematics":
    owner: "shared"
    type: SHARED

  "worldedit":
    owner: "worldedit"
    type: MODULE_PRIVATE
```

---

# 54. AUTO-DISCOVERY

Khi thấy:

```text new root folder
```

USML có thể:

```text AUTO_REGISTER
ASK
REPORT_ONLY
DENY
```

---

# 55. DEFAULT FOREIGN ROOT MAPPING

Nếu ownership chắc chắn:

```text root/MyFolder
```

→

```text data/mods/<id>/legacy-root/MyFolder
```

nhưng giữ virtual alias:

```text MyFolder -> canonical location
```

---

# 56. SHARED FOREIGN ROOT

Nếu mod A và mod B cùng access:

```text /schematics/
```

USML phải không tạo:

```text data/mods/A/schematics
data/mods/B/schematics
```

một cách mù quáng.

Phải recognize:

```text shared root
```

hoặc yêu cầu explicit mapping.

---

# 57. SHARED DIRECTORY RULE

```yaml
shared-roots:
  - path: schematics
    physical: data/shared/schematics
    modules:
      - worldedit
      - fawe
      - example
```

---

# 58. ROOT ALIAS

Hệ thống phải hỗ trợ:

```text virtual alias
```

Example:

```text root:
  schematics:
    -> data/shared/schematics
```

---

# 59. PHYSICAL ALIAS

Optional:

```text symlink
junction
hardlink where possible
```

Nhưng không được yêu cầu admin chạy OS-specific commands.

Nếu physical alias không tạo được:

```text fall back to virtual alias
```

---

# 60. LEGACY PRESERVATION

Nếu server cũ có:

```text plugins/MyPlugin/config.yml
```

và USML mới canonical path là:

```text data/plugins/MyPlugin/config.yml
```

USML phải có migration scanner.

---

# 61. MIGRATION POLICY

```text COPY
MOVE
LINK
KEEP_LEGACY
MERGE
ASK
```

Default:

```text COPY + LEGACY_BACKUP
```

---

# 62. LEGACY CONFLICT

Nếu hai nguồn:

```text old file
new file
```

khác nội dung:

```text report conflict
```

không overwrite âm thầm.

---

# 63. MIGRATION MANIFEST

Tạo:

```text data/server/migrations/<timestamp>.json
```

record:

```text source
destination
hash-before
hash-after
policy
owner
```

---

# 64. MODULE DATA DIRECTORY API

USML API:

```java
ModuleStorage.getDataDirectory()
ModuleStorage.getConfigDirectory()
ModuleStorage.getWorldDataDirectory(world)
ModuleStorage.getCacheDirectory()
ModuleStorage.getTempDirectory()
ModuleStorage.getGeneratedDirectory()
ModuleStorage.getLogDirectory()
ModuleStorage.getLibraryDirectory()
ModuleStorage.getSharedDirectory(namespace)
```

---

# 65. PLUGIN DATA DIRECTORY MAPPING

For converted Bukkit/Paper plugin:

```text Plugin#getDataFolder()
→ data/plugins/<plugin-id>/
```

---

# 66. MOD DATA DIRECTORY MAPPING

For converted Fabric:

```text FabricLoader#getGameDir()
→ server root virtual
FabricLoader#getConfigDir()
→ config/
```

and:

```text USML ModuleStorage.getDataDirectory()
→ data/mods/<mod-id>/
```

---

# 67. FORGE PATH MAPPING

Map concepts:

```text FMLPaths.GAMEDIR
→ server root

FMLPaths.MODSDIR
→ mods/

FMLPaths.CONFIGDIR
→ config/

serverconfig
→ current world/serverconfig

defaultconfigs
→ defaultconfigs/
```

Forge docs explicitly use `GAMEDIR`, `MODSDIR`, `CONFIGDIR` style filesystem concepts and `world/serverconfig` for SERVER config.

---

# 68. NEOFORGE PATH MAPPING

Same semantic mapping:

```text game root
config
defaultconfigs
world/serverconfig
```

plus NeoForge version-specific startup/common/client/server configuration behavior.

---

# 69. QUILT PATH MAPPING

Support:

```text loader.modsDir
loader.configDir
loader.cacheDir
loader.addMods
```

and preserve path resolution rules such as relative-to-game-dir behavior.

---

# 70. PLUGIN FILE RESOURCE ROOT

Plugin resources remain:

```text JAR://
```

`getResource()`:

```text JAR read
```

`saveResource()`:

```text JAR read
→ module data write
```

---

# 71. CLASSLOADER RESOURCE RESOLUTION

Support:

```text Class.getResource
ClassLoader.getResource
ClassLoader.getResourceAsStream
```

but classify whether the resource is:

```text EMBEDDED_RESOURCE
EXTERNAL_FILE
URL
```

---

# 72. RESOURCE VS DATA

This distinction must be explicit:

```text JAR assets/
JAR data/
JAR config templates/
```

are source resources.

```text saved config
database
world state
cache
```

are persistent runtime data.

---

# 73. DATA DIR DIRECTORY CONTRACT

For every module:

```text data/<type>/<id>/
├── config/
├── data/
├── databases/
├── storage/
├── generated/
├── backups/
└── legacy/
```

This is optional internal subdivision; source APIs may still see a flatter directory.

---

# 74. PHYSICAL DIRECTORY VS VIRTUAL DIRECTORY

The source module can think:

```text ./foo.json
```

while physical storage is:

```text data/mods/example/foo.json
```

Do not force source code to know USML paths.

---

# 75. PATH PROVENANCE

Every file access should be traceable:

```text moduleId
sourceClass
sourceMethod
sourceFile
operation
rawPath
resolvedPath
storageClass
ownership
```

---

# 76. FILE ACCESS TRACE

Example:

```text [USML-FS]

module: example
operation: WRITE
raw: ./config/data.json

resolved:
data/mods/example/config/data.json

class:
com.example.ConfigManager

method:
save()

storage:
MODULE_DATA
```

---

# 77. FILESYSTEM DEBUG

Command:

```text /usml fs inspect <module>
```

---

# 78. FILE ACCESS REPORT

Command:

```text /usml fs trace <module>
```

outputs:

```text all filesystem calls
```

with aggregation.

---

# 79. WRITE AUDITING

Admin can enable:

```yaml
filesystem:
  audit-writes: true
```

---

# 80. READ AUDITING

Optional:

```yaml
audit-reads: true
```

Disabled by default for performance.

---

# 81. FILESYSTEM PERFORMANCE

Do not instrument every JDK method with expensive reflection.

Use:

```text bytecode transform
+
resolved callsite cache
+
method handle cache
+
path mapping cache
```

---

# 82. PATH MAPPING CACHE

Cache key:

```text moduleId
+
raw path
+
context
```

Value:

```text physical path
storage class
ownership
```

---

# 83. ASYNC CONTEXT PROPAGATION

This is mandatory.

Module context must follow:

```text Thread
Runnable
Callable
Future
CompletableFuture
Executor
ExecutorService
ScheduledExecutorService
ForkJoinTask
```

---

# 84. THREAD CONTEXT

Use:

```text ModuleExecutionContext
```

with:

```text ThreadLocal
```

but propagate explicitly across async boundaries.

---

# 85. THREAD INSTRUMENTATION

If plugin spawns:

```java
new Thread(...)
```

USML should wrap the Runnable with:

```text ModuleContextPropagatingRunnable
```

when safe.

---

# 86. EXECUTOR INSTRUMENTATION

If code submits:

```text executor.submit(...)
```

USML captures current module context.

---

# 87. THREAD CONTEXT ESCAPE

If context is unknown:

```text filesystem access = SYSTEM_CONTEXT
```

not a random module.

---

# 88. UNKNOWN CONTEXT

If module A creates a thread and passes Runnable to module B, USML must preserve original ownership unless explicitly delegated.

---

# 89. MODULE DELEGATION

Support:

```java
ModuleContext.delegateTo(otherModule)
```

for legitimate shared libraries.

---

# 90. SHARED LIBRARY ACCESS

A library loaded by multiple modules must not accidentally inherit one module's filesystem root permanently.

Use:

```text calling module context
```

rather than:

```text library class origin
```

when resolving data paths.

---

# 91. CALLBACK OWNERSHIP

Events/callbacks should execute with:

```text registered module context
```

---

# 92. TASK OWNERSHIP

Universal scheduler tasks have:

```text owner module
```

and inherited filesystem context.

---

# 93. RESOURCE WATCHER OWNERSHIP

A WatchService registered by module A belongs to A even if watcher callback is invoked asynchronously.

---

# 94. JDBC PATH MAPPING

Detect:

```text jdbc:sqlite:foo.db
jdbc:h2:./foo
file:foo.db
```

and classify:

```text DATABASE
```

relative database paths should map to:

```text data/<type>/<id>/databases/
```

---

# 95. DATABASE DIRECTORY

Default:

```text data/mods/<id>/databases/
data/plugins/<id>/databases/
```

---

# 96. EXTERNAL DATABASE

For:

```text jdbc:mysql://...
postgresql://...
mongodb://...
```

do not rewrite host or network semantics.

Only credentials/security policy applies.

---

# 97. NATIVE LIBRARIES

Detect:

```text System.load()
System.loadLibrary()
JNI
native extraction
```

and map temporary native libraries to:

```text runtime/native/<module>/
```

---

# 98. NATIVE LIBRARY SECURITY

Default:

```text native libraries = REVIEW_REQUIRED
```

because a native library is executable code.

---

# 99. PROCESS SPAWNING

Detect:

```text Runtime.exec
ProcessBuilder
ProcessHandle
```

and classify:

```text EXTERNAL_PROCESS
```

Default policy:

```text DENY
```

unless explicitly allowed.

---

# 100. ENVIRONMENT VARIABLES

Detect:

```text System.getenv()
System.getProperties()
user.dir
java.io.tmpdir
user.home
user.name
```

and map known filesystem-related properties through a module environment layer where possible.

---

# 101. `user.dir`

Important compatibility target.

A module that uses:

```java
System.getProperty("user.dir")
```

must receive a virtual/module-compatible path when safe.

If true JVM-global semantics are required:

```text report GLOBAL_JVM_PROPERTY
```

---

# 102. `java.io.tmpdir`

Map module-created temp files to:

```text temp/<type>/<id>/
```

when the access can be attributed to a module.

---

# 103. USER HOME

Default policy:

```text OS user home = READ-DENY for arbitrary module code
```

unless allowlisted.

---

# 104. APPDATA

On Windows:

```text %APPDATA%
%LOCALAPPDATA%
```

must be treated as external data roots.

Do not silently read/write these.

---

# 105. CACHE

Any temp/cache access should map to:

```text cache/<type>/<id>/
```

when classification is reliable.

---

# 106. LOG FILES

If a module writes:

```text plugin.log
debug.log
```

through a module-controlled logger or relative path:

```text logs/plugins/<id>/
logs/mods/<id>/
```

---

# 107. CRASH FILES

Module-specific crash dumps:

```text reports/crashes/<type>/<id>/
```

but server-wide Minecraft crash reports remain native server paths.

---

# 108. GENERATED FILES

Datagen-generated content must go:

```text generated/<type>/<id>/
```

---

# 109. BACKUPS

Module-generated backups:

```text backups/<type>/<id>/
```

---

# 110. TEMPORARY EXTRACTION

Temporary extraction:

```text temp/<type>/<id>/
```

must be garbage-collected on shutdown or after TTL.

---

# 111. CACHE INVALIDATION

Cache key:

```text moduleHash
minecraftVersion
platformVersion
converterVersion
targetVersion
configHash
```

---

# 112. PACK ROOTS

Java:

```text datapacks/
resourcepacks/
texturepacks/
```

Bedrock:

```text bedrock/addons/
bedrock/resourcepacks/
bedrock/behaviorpacks/
bedrock/texturepacks/
bedrock/scripts/
```

Universal:

```text addons/
```

---

# 113. TEXTUREPACK SEMANTICS

Java không có runtime loại riêng tên “texture pack” như một platform pack type độc lập.

USML vẫn giữ:

```text texturepacks/
```

là **user-facing source folder**.

Pipeline:

```text texturepacks/
↓
Texture Input Adapter
↓
Java ResourcePack IR
```

---

# 114. BEDROCK TEXTUREPACK SEMANTICS

Bedrock texture input:

```text bedrock/texturepacks/
```

→

```text BedrockResourcePackIR
```

---

# 115. RESOURCE PACK INPUTS

Support:

```text directory
zip
jar embedded resource
generated
URL when enabled
```

---

# 116. DATA PACK INPUTS

Support:

```text folder
zip
embedded
generated
```

---

# 117. ADDON INPUTS

Support:

```text folder
zip
.mcaddon
.mcpack
universal addon
generated
```

Microsoft documents `.mcaddon` as an archive containing `.mcpack` or `.mcworld` and `.mcpack` as a zipped resource/behavior pack.

---

# 118. BEDROCK MANIFEST

Every Bedrock RP/BP must validate:

```text manifest.json
format_version
header
modules
dependencies
capabilities
metadata
settings
subpacks
```

Current Bedrock pack manifest documentation includes these fields and uses unique UUID/version/module declarations.

---

# 119. BEDROCK DEPENDENCIES

Support:

```text UUID + version
module_name
native Script API dependency
```

Dependencies must be resolved before activation. Minecraft documents these dependency forms directly.

---

# 120. BEDROCK SUBPACKS

Support:

```text subpacks/
folder_name
memory_performance_tier
name
```

Subpack contents override corresponding main pack contents.

---

# 121. BEDROCK RESOURCE CONTENT

Validate and compile:

```text animation_controllers
animations
attachables
block_culling
entity
fogs
items
materials
models
particles
render_controllers
sounds
texts
textures
ui
blocks.json
sounds.json
manifest.json
pack_icon.png
```

Microsoft's comprehensive pack reference enumerates these resource/behavior pack content areas.

---

# 122. BEDROCK BEHAVIOR CONTENT

Validate and compile:

```text animation_controllers
animations
blocks
cameras
entities
features
feature_rules
functions
items
loot_tables
recipes
spawn_rules
structures
texts
trading
scripts
manifest
```

---

# 123. BEDROCK SCRIPT

Support:

```text JavaScript
TypeScript build outputs
Script modules
@microsoft/minecraft native dependencies
```

But script API version phải version-aware.

---

# 124. BEDROCK ADDON EXTRACTION

`.mcaddon`:

```text archive
↓
extract safely
↓
discover mcpack
↓
classify RP/BP
↓
dependency graph
↓
compiler
```

---

# 125. BEDROCK PACK SECURITY

Reject:

```text path traversal
symlink escapes
oversized decompression
zip bomb
invalid manifest
duplicate UUID
invalid dependencies
unsafe scripts
```

---

# 126. GAMESOURCE PACK STACK

Final stack:

```text vanilla base
↓
library packs
↓
module packs
↓
server packs
↓
admin override packs
```

---

# 127. PACK PRIORITY

Config:

```yaml
pack-order:
  resourcepacks:
    - vanilla-overrides
    - libraries
    - modules
    - admin

  datapacks:
    - libraries
    - gameplay
    - admin
```

Dependency graph must override unsafe priority choices.

---

# 128. RESOURCE CONFLICT

For:

```text assets/minecraft/...
```

must detect:

```text source A
source B
winner
reason
```

---

# 129. SEMANTIC MERGE

Different resource types require different merge semantics:

```text sounds.json
font
models
blockstates
animations
lang
manifest
render controllers
```

Do not use generic `putAll`.

---

# 130. PACK OWNERSHIP

Every generated asset must record:

```text source module
source pack
source file
target pack
target file
compiler
version
```

---

# 131. PACK SOURCE MAP

Create:

```text reports/packs/source-map.json
```

Example:

```text generated:
assets/usml_create/textures/brass.png

source:
Create.jar!/assets/create/textures/brass.png
```

---

# 132. JAVA RESOURCE DELIVERY

USML must generate a final Java Resource Pack.

It must support:

```text embedded HTTP
external host
reverse proxy
CDN
```

---

# 133. JAVA RESOURCE HASH

For client verification:

```text SHA-1
```

For internal content identity:

```text SHA-256
```

---

# 134. JAVA RESOURCE PACK ENFORCEMENT

Server policy:

```yaml
java:
  resource-pack:
    required: true
    prompt: true
```

Paper's APIs expose resource-pack sending/required behavior and client caching is hash-sensitive.

---

# 135. BEDROCK DELIVERY

Through Geyser:

```text generated Bedrock RP
generated BP/mapping where supported
```

Geyser documents server-side Bedrock pack delivery and has its own pack events/pack location rules.

---

# 136. JAVA→BEDROCK RESOURCE COMPILER

Do not blindly copy:

```text Java RP -> Bedrock RP
```

Instead:

```text Java Asset
↓
Universal Asset IR
↓
Bedrock compiler
```

Geyser itself does not serve as the universal Java Resource Pack → Bedrock converter; its ecosystem includes separate tools such as Hydraulic/Thunder for additional conversion capabilities.

---

# 137. SERVER PACK STATE

Track:

```text GENERATED
PUBLISHED
SENT
DOWNLOADED
ACCEPTED
ACTIVE
FAILED
TIMED_OUT
```

---

# 138. JAVA PACK CACHE

Cache identity:

```text hash
```

not filename.

---

# 139. BEDROCK PACK CACHE

Cache identity:

```text manifest UUID
version
content hash
target engine version
```

---

# 140. PACK HOT RELOAD

Resource packs:

```text rebuild
rehash
republish
resend
```

Data packs:

```text rebuild
validate
reload if supported
```

Code:

```text restart by default
```

---

# 141. WORLD-SCOPED DATA

Canonical:

```text data/worlds/<world-id>/
```

under that:

```text mods/
plugins/
shared/
generated/
```

---

# 142. WORLD-SCOPED MOD DATA

```text data/worlds/<world-id>/mods/<mod-id>/
```

---

# 143. WORLD-SCOPED PLUGIN DATA

```text data/worlds/<world-id>/plugins/<plugin-id>/
```

---

# 144. GLOBAL MOD DATA

```text data/mods/<mod-id>/
```

---

# 145. GLOBAL PLUGIN DATA

```text data/plugins/<plugin-id>/
```

---

# 146. SHARED DATA

```text data/shared/<namespace>/
```

---

# 147. PLAYER DATA

```text data/players/<uuid>/
```

But gameplay-critical player state should prefer Minecraft's own persistent/world save systems when possible.

---

# 148. PAPER PDC

Where target supports it, use PDC/vanilla persistent storage rather than arbitrary files for per-object data.

Paper PDC supports items, chunks, worlds, entities, tile states, structures and other holders, giving plugins durable namespaced object data.

---

# 149. PDC → UNIVERSAL PERSISTENCE

Map:

```text Bukkit PDC
Forge capabilities
NeoForge attachments/capabilities
Fabric component/data systems
Quilt equivalents
```

to:

```text UniversalPersistentData
```

---

# 150. FILE DATA VS GAME DATA

Do not store everything in files.

Classify:

```text OBJECT_STATE
WORLD_STATE
CONFIG
CACHE
DATABASE
ASSET
```

and choose the proper backend.

---

# 151. SERVER CONFIG VS MODULE DATA

Example:

```text create-server.toml
```

is:

```text SERVER_CONFIG
```

while:

```text create-machine-db.sqlite
```

is:

```text MODULE_DATA / DATABASE
```

while:

```text create-render-cache.bin
```

is:

```text CACHE
```

---

# 152. CONFIG RELOADABILITY

Each config has:

```text LIVE
TICK
WORLD_RELOAD
SERVER_RELOAD
GAME_RESTART
UNSAFE
UNKNOWN
```

---

# 153. CONFIG SYNC

Each config declares:

```text server -> client
none
server only
client only
```

Forge/NeoForge SERVER configs may sync; CLIENT configs do not.

---

# 154. CLIENT CONFIG ON SERVER

If converted mod has:

```text CLIENT_CONFIG
```

do not blindly load it.

Classify:

```text EXCLUDED_CLIENT_CONFIG
```

unless some values are semantically useful on server.

---

# 155. CONFIG TRANSLATION

If a CLIENT config contains gameplay-relevant values:

```text analyzer
↓
extract field
↓
semantic classification
↓
move to common/server config
```

---

# 156. CONFIG FILE FORMAT SUPPORT

At minimum:

```text YAML
JSON
JSON5 where source supports it
TOML
HOCON
Properties
SNBT
NBT
XML
INI-like
custom text
binary
SQLite
```

Do not identify config format purely by extension.

---

# 157. CONFIG DEFAULTS

Support:

```text embedded default
built-in default
defaultconfig
defaultconfigs
server config template
user override
```

Forge `defaultconfigs` behavior around new worlds must be modeled; its server config system is world-specific and can be templated through `defaultconfigs`.

---

# 158. DEFAULT CONFIG COPY

When creating a world:

```text defaultconfigs/
↓
world/serverconfig/
```

when target/source semantics require it.

---

# 159. CONFIG MIGRATION

If config schema changes:

```text detect version
run migration
backup original
write new schema
```

---

# 160. CONFIG SCHEMA

Each normalized config:

```yaml
id: "..."
owner: "..."
type: "COMMON"
format: "TOML"
scope: "GLOBAL"
reload: "SERVER_RELOAD"
restart: "NONE"
sync: "NONE"
```

---

# 161. MODULE STATE MACHINE

```text DISCOVERED
ANALYZED
DEPENDENCIES_RESOLVED
CONVERTING
CONVERTED
REGISTERING
ENABLED
RUNNING
DISABLED
FAILED
QUARANTINED
IGNORED
```

---

# 162. CONVERSION MODES

```text NATIVE
CONVERT
PARTIAL
EMULATE
FALLBACK
IGNORE
DISABLE
FAILED
```

---

# 163. BYTECODE ANALYSIS

Must inspect:

```text classes
methods
fields
annotations
interfaces
inheritance
reflection
invokedynamic
method handles
threads
filesystem
network
JNI
OpenGL
rendering
Mixin
Transformers
```

---

# 164. SOURCE-SIDE CLASSIFICATION

```text SERVER_SAFE
COMMON
CLIENT_ONLY
CLIENT_RENDER
CLIENT_INPUT
CLIENT_HUD
CLIENT_OPTIMIZATION
SERVER_OPTIMIZATION
PHYSICS
AI
NETWORK
PERSISTENCE
UNKNOWN
```

---

# 165. METHOD-SIDE CLASSIFICATION

Same classification applies per method.

---

# 166. FIELD CLASSIFICATION

Fields:

```text config
runtime
persistent
networked
render-state
client-only
server-only
static
```

---

# 167. MIXIN

Parse:

```text config
target
injector
method
field
callback
redirect
overwrite
modifyconstant
modifyvariable
accessor
invoker
```

Fabric officially uses Mixin as its supported class transformation mechanism; Quilt also carries Fabric compatibility internals.

---

# 168. MIXIN SEMANTIC TRANSLATION

Do not:

```text copy Mixin into Paper
```

Instead:

```text Mixin intent
↓
semantic hook
↓
Universal Event/Transformer IR
↓
target adapter
```

---

# 169. ACCESS WIDENER

Parse and normalize:

```text field
method
class
access
extend
```

---

# 170. FORGE/NEOFORGE TRANSFORMERS

Detect:

```text coremods
ASM transformers
access transformers
runtime patches
ModLauncher transformations
```

---

# 171. TRANSFORMER SEMANTICS

Each transformation:

```text target class
target member
bytecode change
semantic effect
side
thread
```

---

# 172. RUNTIME CODE REWRITING

Use:

```text ASM
bytecode IR
```

not regex.

---

# 173. AST/DECOMPILER USE

Decompiler may assist analysis but must not be considered source of truth.

---

# 174. REFLECTION

Detect:

```text Class.forName
getDeclaredMethod
getDeclaredField
MethodHandles
Unsafe
ServiceLoader
dynamic proxies
```

---

# 175. UNKNOWN REFLECTION

If dynamic string cannot be resolved:

```text UNKNOWN_DYNAMIC_BEHAVIOR
```

not fake conversion.

---

# 176. EXTERNAL RESOURCE LOADING

Detect:

```text ClassLoader resource
URL
file URL
HTTP
filesystem
```

and classify separately.

---

# 177. NETWORK ACCESS

Module network access:

```text HTTP
HTTPS
WebSocket
raw TCP
UDP
```

must be distinct from Minecraft networking.

---

# 178. SECURITY

Default:

```text arbitrary network = allow for native trusted modules
converter worker = restricted
```

---

# 179. CONVERTER WORKER

Static analysis worker should be isolated:

```text read-only source
limited CPU
limited RAM
limited filesystem
restricted network
```

---

# 180. NEVER EXECUTE SOURCE JAR BY DEFAULT

Static analyzer must not invoke:

```text mod constructors
plugin onEnable
static initializers
```

just to “see what happens”.

---

# 181. OPTIONAL DYNAMIC ANALYSIS

If required:

```text isolated JVM/process
```

with sandbox.

---

# 182. CODE SIGNING

Optional:

```text signed module
trusted publisher
hash allowlist
```

---

# 183. LICENSE

Store:

```text license
authors
source repository
version
hash
```

Do not redistribute transformed third-party code when the license does not allow it.

---

# 184. UIR

Universal Intermediate Representation must be typed.

Core types:

```text UniversalModule
UniversalRegistryEntry
UniversalBlock
UniversalItem
UniversalEntity
UniversalBlockEntity
UniversalFluid
UniversalRecipe
UniversalLoot
UniversalWorldgen
UniversalCommand
UniversalPermission
UniversalEvent
UniversalSchedulerTask
UniversalPhysicsBody
UniversalModel
UniversalAnimation
UniversalCamera
UniversalHUD
UniversalPacket
UniversalPersistentData
UniversalConfig
UniversalResource
UniversalPack
```

---

# 185. UIR SIDE

Each object:

```text SERVER
COMMON
CLIENT
PRESENTATION
DATA
```

---

# 186. UIR SCOPE

Each object:

```text GLOBAL
WORLD
DIMENSION
CHUNK
ENTITY
PLAYER
SESSION
MODULE
PACK
```

---

# 187. UIR THREAD

Each callback:

```text GLOBAL
MAIN
REGION
ENTITY
ASYNC
RENDER
NETWORK
ANY
```

---

# 188. UIR PERSISTENCE

Each object:

```text NONE
SESSION
CHUNK
WORLD
PLAYER
GLOBAL
DATABASE
FILE
```

---

# 189. UIR NETWORK

Every networked object:

```text authoritative source
sync direction
delta fields
compression
version
client capability
```

---

# 190. REGISTRY

Universal registry:

```text ResourceKey
RegistryType
Entry
Lifecycle
Aliases
Dependencies
ClientRepresentation
```

---

# 191. BLOCK

Universal block:

```text logical ID
base block
state
collision
hardness
resistance
light
sound
interaction
redstone
tick
block entity
visual
```

---

# 192. BLOCK REPRESENTATION

Default:

```text logical block
+
vanilla placeholder
+
visual representation
```

---

# 193. BLOCK BASE SELECTION

Config:

```yaml
blocks:
  example:machine:
    base: minecraft:note_block
```

---

# 194. BLOCK STATE VS VISUAL STATE

Separate:

```text logical state
physical state
visual state
```

---

# 195. BLOCK ENTITY

Persistent:

```text inventory
energy
progress
machine state
custom data
```

---

# 196. CUSTOM ENTITY

Separate:

```text LogicalEntity
CarrierEntity
VisualModel
Collision
AI
Physics
Animation
Interaction
Persistence
NetworkState
```

---

# 197. ENTITY CARRIER BACKENDS

```text native vanilla entity
display entity
block display
item display
interaction entity
armor stand legacy carrier
```

---

# 198. DISPLAY ENTITY

Paper documents Display entities as suitable for block/item/text display and arbitrary affine transformations, with client interpolation for smooth movement.

USML should use this capability wherever available.

---

# 199. MODEL ENGINE-LIKE SYSTEM

Create independent abstraction:

```text UniversalModel
Bone
BoneTransform
Animation
AnimationController
AnimationLayer
Attachment
MountPoint
RenderPart
Hitbox
```

Model Engine can be treated as an architectural reference, not a mandatory dependency.

---

# 200. ENTITY MODEL FALLBACK

```text ModelRenderer
→ DisplayRenderer
→ CarrierRenderer
→ VanillaFallback
```

---

# 201. ANIMATION

Universal animation:

```text timeline
keyframes
bones
layers
loop
transition
interpolation
event markers
```

---

# 202. SERVER ANIMATION STATE

Server transmits:

```text animationId
startTime
speed
parameters
state
```

not frame-by-frame transforms.

---

# 203. CLIENT INTERPOLATION

Client does:

```text interpolation
```

server does:

```text authoritative state
```

---

# 204. DYNAMIC SCENE GRAPH

```text Scene
└── Root
    ├── BlockNode
    ├── ItemNode
    ├── EntityNode
    ├── ModelNode
    ├── ParticleNode
    ├── TextNode
    └── LightNode
```

---

# 205. MOVING BLOCKS

Use:

```text UniversalContraption
```

with:

```text block collection
local transforms
origin
rotation
translation
physics
collision
actors
inventories
block entities
```

---

# 206. CREATE-LIKE SYSTEM

Create-like moving structures become:

```text server simulation
+
scene graph
+
client presentation
```

rather than literal client renderer conversion.

---

# 207. CONTRAPTION SYNC

Send:

```text root transform
dirty child state
animation state
```

not one absolute transform per block every tick.

---

# 208. CONTRAPTION INTERPOLATION

Client interpolates:

```text translation
rotation
scale
```

where target primitives permit it.

---

# 209. PHYSICS

Universal:

```text PhysicsWorld
RigidBody
KinematicBody
Collider
CollisionShape
Constraint
Force
Torque
Impulse
```

---

# 210. PHYSICS SHAPES

```text AABB
BOX
OBB
SPHERE
CAPSULE
MESH
VOXEL
COMPOUND
```

---

# 211. SERVER-AUTHORITATIVE PHYSICS

Input:

```text player
vehicle
machine
entity
```

server computes:

```text collision
velocity
transform
```

client renders result.

---

# 212. CLIENT PREDICTION

Optional:

```text prediction
reconciliation
snap threshold
teleport threshold
```

---

# 213. PLAYER PHYSICS

If original mod changes:

```text gravity
jump
dash
slide
wall-run
custom collision
```

convert logic to server simulation.

---

# 214. CAMERA

Universal:

```text FOV
offset
follow
orbit
look-at
shake
zoom
cutscene
attach
```

---

# 215. CAMERA CAPABILITY

Per target:

```text NATIVE
EMULATED
LIMITED
UNSUPPORTED
```

---

# 216. HUD

Universal HUD:

```text Text
Font
Glyph
Bossbar
Scoreboard
Actionbar
Title
Container
Screen
Icon
```

---

# 217. HUD BACKENDS

Java:

```text resource pack
font
glyph
bossbar
scoreboard
actionbar
container
```

Bedrock:

```text UI JSON
font
textures
forms where appropriate
resource pack
```

---

# 218. INPUT

Universal:

```text keyboard
mouse
interaction
gesture
controller
```

If vanilla client lacks custom input primitive:

```text INPUT_UNAVAILABLE
```

---

# 219. PARTICLES

```text server event
particle state
client render
```

---

# 220. SOUNDS

Universal sound:

```text ID
volume
pitch
position
category
distance
loop
```

Java and Bedrock compilers generate respective definitions.

---

# 221. ITEMS

Logical item:

```text id
base item
components
custom data
model
use behavior
attack behavior
food
armor
durability
attributes
cooldown
```

---

# 222. ITEM IDENTIFICATION

Avoid lore-only identification.

Prefer:

```text universal ID
custom data
components
persistent data
```

where target permits.

---

# 223. COMMANDS

Universal:

```text command tree
literal
argument
suggestion
permission
execution
error
```

---

# 224. BRIGADIER

Brigadier should be the semantic command IR where available.

Paper currently exposes Brigadier-oriented APIs and lifecycle integration; Paper's modern plugin system also uses Brigadier instead of relying on `commands` metadata for Paper plugins.

---

# 225. PERMISSIONS

Universal:

```text permission node
default
children
wildcard semantics
```

---

# 226. EVENTS

Universal event bus:

```text ServerStart
ServerStop
WorldLoad
WorldUnload
PlayerJoin
PlayerQuit
PlayerMove
BlockPlace
BlockBreak
BlockInteract
EntitySpawn
EntityDamage
EntityDeath
ItemUse
ItemDrop
InventoryOpen
InventoryClose
InventoryClick
Command
Chat
Tick
PhysicsTick
ChunkLoad
ChunkUnload
```

---

# 227. EVENT SEMANTICS

Every event must preserve:

```text timing
side
thread
cancellable
mutation
ordering
result
```

---

# 228. EVENT ORDER

Do not assume source platform ordering equals target ordering.

Universal EventBus must support:

```text priority
before
after
stage
```

---

# 229. SERVICES

Universal ServiceRegistry:

```text economy
permissions
claims
placeholder
model
world-edit
storage
network
```

Paper/Bukkit `ServicesManager` concepts map here. The Bukkit plugin package explicitly contains services/plugin management abstractions.

---

# 230. SCHEDULER

Universal:

```text run
runDelayed
runRepeating
runAsync
runGlobal
runRegion
runEntity
```

---

# 231. FOLIA

Folia needs its own scheduler mapping:

```text GlobalScheduler
RegionScheduler
EntityScheduler
AsyncScheduler
```

Do not replace all Folia tasks with a single Bukkit main-thread scheduler.

---

# 232. PLATFORM ADAPTERS

Mandatory source adapters:

```text Fabric
Forge
NeoForge
Quilt
Bukkit
Spigot
Paper
Folia
Canvas
Purpur
```

---

# 233. TARGET ADAPTERS

Mandatory server target adapters:

```text Paper
Folia
Canvas
Purpur
Spigot
Bukkit
```

Additional future adapters:

```text Pufferfish
Leaves
other compatible forks
```

---

# 234. PAPER AS A BASE TARGET

Paper target should reuse:

```text Bukkit API semantics
Spigot API semantics
Paper API
```

then add:

```text PaperCapabilities
```

---

# 235. PURPUR

Purpur should reuse:

```text Paper backend
```

and add only Purpur-specific capabilities.

---

# 236. CANVAS

Canvas must have an independent adapter because server behavior/API/patching can diverge from Paper/Folia.

No:

```text Canvas == Paper
```

assumption.

---

# 237. SPIGOT

Spigot target reuses Bukkit semantic layer and adds Spigot-specific differences.

---

# 238. BUKKIT

Bukkit-only target is the lowest common server API layer.

---

# 239. PAPER PLUGIN METADATA

Parser:

```text plugin.yml
paper-plugin.yml
```

Paper currently supports both, with Paper-specific bootstrap/loader semantics and dependency handling.

---

# 240. PLUGIN RESOURCE FILES

Parse:

```text plugin.yml
paper-plugin.yml
config.yml
embedded datapacks
embedded resources
translations
libraries metadata
```

---

# 241. PAPER DATAPACK DISCOVERY

Paper has lifecycle datapack discovery so plugins can supply datapacks directly from plugin resources; USML must normalize this into:

```text UniversalDataPackSource
```

rather than assuming all data packs are loose files under `datapacks/`.

---

# 242. REGISTRY MUTATION

Paper registry modification occurs during bootstrap/lifecycle phases, and the registry API is currently experimental.

USML must expose:

```text BOOTSTRAP_REGISTRATION_STAGE
```

separate from:

```text RUNTIME_STAGE
```

Paper documents registry creation/modification during bootstrap and data-driven registry composition.

---

# 243. REGISTRY CAPABILITY

For each version:

```text mutable
read-only
bootstrap-only
data-driven
server->client synced
```

---

# 244. WORLDGEN

Universal worldgen:

```text biome
noise
density
feature
placement
structure
processor
dimension
surface rule
```

---

# 245. DATAPACK-FIRST WORLDGEN

If target supports required worldgen via datapack:

```text compile to datapack
```

otherwise:

```text native runtime
```

---

# 246. PERSISTENCE

Priority:

```text native Minecraft persistence
>
platform persistent-data APIs
>
UniversalStorage
>
module files
```

---

# 247. WORLD PERSISTENCE

Each custom world object has:

```text version
schema
migration
```

---

# 248. ENTITY PERSISTENCE

If carrier entity is destroyed/recreated, logical entity state must survive independently.

---

# 249. VISUAL ENTITY LIFECYCLE

```text logical create
↓
carrier spawn
↓
visual attach
↓
update
↓
unload
↓
respawn
```

---

# 250. CHUNK UNLOAD

Visual objects must not leak on chunk unload.

---

# 251. PLAYER TRACKING

Only send visual state to players who should see it.

Track:

```text chunk
distance
dimension
permission
visibility
```

Paper world configuration exposes tracking ranges, including display entity tracking, which reinforces the need to distinguish server simulation from tracking/visibility.

---

# 252. NETWORK DELTA

Use:

```text dirty bitsets
field deltas
batched packets
compression
```

---

# 253. PACKET CHANNEL

Universal:

```text handshake
capabilities
object create
object update
object destroy
animation state
camera state
HUD state
custom event
```

---

# 254. CLIENT CAPABILITY HANDSHAKE

Player join:

```text identify:
Java
Bedrock
Geyser
Universal Client
```

Then negotiate:

```text display entity
custom item representation
animation
camera
HUD
resource pack
Bedrock RP
Bedrock mapping
```

---

# 255. JAVA VANILLA MODE

Client:

```text no USML client mod
```

Server uses:

```text vanilla-compatible primitives
+
resource pack
+
packets
```

---

# 256. UNIVERSAL CLIENT OPTIONAL MODE

Future:

```text USML Client
```

may unlock:

```text advanced camera
advanced renderer
advanced HUD
custom input
client-side prediction
```

But server-only mode must remain functional.

---

# 257. BEDROCK MODE

Bedrock:

```text Geyser
+
Bedrock Resource Pack
+
Behavior/mapping backend
```

---

# 258. GEYSER INTEGRATION

USML should detect:

```text Geyser plugin
Geyser Fabric
Geyser NeoForge
Geyser standalone
```

and use a backend interface rather than hard-coding a single integration.

Geyser currently distributes builds for Spigot/Paper, Fabric and NeoForge among other platforms, reinforcing the need for platform-specific integration points.

---

# 259. JAVA→BEDROCK CUSTOM ITEM

Need:

```text logical item
Bedrock mapping
Bedrock RP
runtime identity
```

---

# 260. JAVA→BEDROCK CUSTOM BLOCK

Need:

```text logical block
physical representation
Bedrock mapping
RP
behavior
```

---

# 261. BEDROCK LIMITATION

If Geyser/backend cannot faithfully represent a feature:

```text convert logic
fallback presentation
report limitation
```

Never crash all Java clients because one Bedrock visual feature is unavailable.

---

# 262. RESOURCE COMPILERS

Interfaces:

```java
ContentCompiler<I, O>
ResourceCompiler
DataCompiler
ModelCompiler
AnimationCompiler
BedrockCompiler
JavaPackCompiler
```

---

# 263. MOD CONVERTER INTERFACE

```java
interface ModConverter {
    AnalysisResult analyze(ModuleSource source);
    UniversalModule convert(AnalysisResult result);
}
```

---

# 264. PACK LOADER INTERFACE

```java
interface PackLoader {
    boolean supports(Source source);
    UniversalPack load(Source source);
}
```

---

# 265. PLATFORM ADAPTER INTERFACE

```java
interface ServerPlatformAdapter {
    PlatformInfo detect();
    RuntimeCapabilities capabilities();
    SchedulerAdapter scheduler();
    RegistryAdapter registry();
    EntityAdapter entities();
    BlockAdapter blocks();
    ResourcePackAdapter resources();
}
```

---

# 266. FILESYSTEM ADAPTER INTERFACE

```java
interface ModuleFilesystemAdapter {
    Path resolve(ModuleContext context, Path source);
    InputStream openRead(...);
    OutputStream openWrite(...);
    DirectoryStream<Path> list(...);
    WatchService createWatchService(...);
}
```

---

# 267. FILE ACCESS INTERCEPTOR LAYERS

```text
API Adapter
↓
Bytecode Callsite Transformer
↓
Module Context Resolver
↓
Virtual Filesystem
↓
Storage Policy
↓
Physical Filesystem
```

---

# 268. DIRECT LOADER API REWRITE

Examples:

```text FabricLoader#getConfigDir
Forge FMLPaths
NeoForge FMLPaths
Plugin#getDataFolder
```

map directly.

---

# 269. RAW IO REWRITE

Examples:

```text new File(...)
Paths.get(...)
Files.*
FileInputStream
FileOutputStream
```

map through USML filesystem.

---

# 270. REFLECTION-AWARE IO

If reflection invokes:

```text Files.write(...)
```

directly, static analyzer tries to resolve it.

If impossible:

```text DYNAMIC_IO_UNKNOWN
```

---

# 271. LEGACY IO MODE

For known modules, provide compatibility rules:

```yaml
filesystem:
  legacy:
    examplemod:
      relative-root: data/mods/examplemod
      foreign:
        - oldfolder
```

---

# 272. MODULE-SPECIFIC FILE OVERRIDES

```text config/overrides/<module>/filesystem.yml
```

---

# 273. FILESOURCE RULE

Example:

```yaml
maps:
  - source: "./database"
    target: "MODULE_DATABASE"

  - source: "./cache"
    target: "CACHE"

  - source: "./exports"
    target: "GENERATED"

  - source: "./config"
    target: "CONFIG"
```

---

# 274. FILE EXTENSION HEURISTICS

Only heuristic.

Examples:

```text .db .sqlite .mv.db
→ DATABASE

.log
→ LOG

.cache
→ CACHE

.toml .yaml .yml .json .properties
→ potential CONFIG
```

Semantic use always outranks extension.

---

# 275. CONFIG DETECTOR

Detect if file is:

```text loaded at startup
parsed as key/value
contains schema/defaults
reloaded on config event
named config/settings/options
```

---

# 276. DATABASE DETECTOR

Detect:

```text JDBC
SQLite
H2
embedded database file patterns
```

---

# 277. ASSET DETECTOR

Detect:

```text texture
model
sound
font
shader
animation
```

and route to pack compiler instead of module data.

---

# 278. GENERATED ASSET DETECTOR

If module writes:

```text generated JSON
generated PNG
generated model
```

from datagen:

```text generated/
```

not persistent user data.

---

# 279. ADMIN ROOT POLICY

Config:

```yaml
filesystem:
  root-policy:
    unknown-folder: "AUTO_REGISTER"
    outside-root: "DENY"
    absolute-inside-root: "ALLOW"
    root-write: "AUDIT"
```

---

# 280. UNKNOWN ROOT BEHAVIOR

Unknown root folder:

```text first occurrence:
WARN

second occurrence:
persist registry

after admin decision:
redirect/allow/deny
```

---

# 281. ROOT FILES

If module creates:

```text server.properties
eula.txt
banned-players.json
```

USML must never silently redirect those.

Classify:

```text SERVER_PROTECTED
```

---

# 282. PROTECTED ROOT PATHS

Default protected:

```text server.jar
server.properties
eula.txt
ops.json
whitelist.json
banned-ips.json
banned-players.json
usercache.json
logs/
world/
mods/
plugins/
config/
libraries/
```

A module writing to protected root should be reported.

---

# 283. SERVER-SHARED PATHS

Known shared roots:

```text datapacks/
resourcepacks/
texturepacks/
addons/
```

A module may intentionally write there; classify as:

```text SERVER_CONTENT_MUTATION
```

---

# 284. MODULE CROSS-WRITE

Mod A writing into mod B's data directory:

```text SECURITY_WARNING
```

unless explicit shared capability exists.

---

# 285. SHARED DATA CAPABILITY

```java
SharedStorage.open(namespace)
```

required for deliberate collaboration.

---

# 286. FILE OWNERSHIP ACL

Each path:

```text owner module
readers
writers
delete permission
```

---

# 287. DEFAULT ACL

Module:

```text read/write own
read shared declared
read resources
read server APIs
```

No write access to another module.

---

# 288. ADMIN ACL OVERRIDE

```yaml
filesystem:
  grants:
    - module: modA
      read:
        - shared/common
      write:
        - shared/common
```

---

# 289. CLASSLOADER ISOLATION

Each converted module should have:

```text ModuleClassLoader
```

with explicit dependencies.

Paper plugin classpath isolation can be mirrored semantically: one plugin should not automatically see unrelated plugin libraries unless dependency/classpath access was declared.

---

# 290. PLUGIN CLASSLOADER COMPATIBILITY

For Bukkit APIs:

```text getClassLoader()
getPluginLoader()
```

must behave predictably.

---

# 291. MOD CLASSLOADER COMPATIBILITY

For Fabric/Forge/NeoForge/Quilt source code using classloader discovery:

```text resource lookup
class lookup
service loading
```

must resolve through ModuleClassLoader.

---

# 292. SERVICELOADER

Parse:

```text META-INF/services/*
```

and make provider ownership explicit.

---

# 293. EMBEDDED LIBRARY COLLISION

If two modules embed incompatible libraries:

```text isolate
```

unless semantic sharing is safe.

---

# 294. RUNTIME LIBRARY STORE

Physical:

```text libraries/
├── mods/
├── plugins/
└── shared/
```

---

# 295. DEPENDENCY DOWNLOAD CACHE

Use:

```text cache/libraries/
```

and verify hashes.

---

# 296. LIBRARY LICENSE

Track licenses of downloaded libraries where metadata permits.

---

# 297. MOD IGNORE

Config:

```yaml
ignore:
  ids:
    - create
    - modelengine

  jars:
    - Create-6.0.10.jar
    - ModelEngine-4.0.0.jar
```

Exact `jars` entries mean **exact filename**.

---

# 298. ID IGNORE

Matches metadata ID.

---

# 299. JAR IGNORE

Matches binary filename.

---

# 300. HASH IGNORE

Optional:

```yaml
hashes:
  - sha256:...
```

---

# 301. IGNORE BEHAVIOR

Ignored source:

```text not analyze
not convert
not generate resources
not load runtime
not modify
```

---

# 302. DISABLED MODULE

Difference:

```text IGNORE = loader does not own/process it
DISABLED = USML knows it but intentionally doesn't activate it
```

---

# 303. PARTIAL CONVERSION

Example:

```text gameplay ✅
physics ✅
blocks ✅
items ✅
worldgen ✅
resource ✅
renderer ❌
GPU optimizer ❌
```

Result:

```text PARTIAL
```

---

# 304. NO WHOLE-MOD FAILURE

One unsupported client feature must not fail unrelated converted server logic.

---

# 305. FEATURE ISOLATION

Every feature has its own state:

```text ENABLED
DISABLED
EMULATED
FALLBACK
UNSUPPORTED
```

---

# 306. CONVERSION GRAPH

```text module
├── metadata
├── dependencies
├── classes
├── methods
├── resources
├── data
├── features
└── generated outputs
```

---

# 307. FEATURE TRACEABILITY

Every generated feature must point to:

```text source JAR
class
method
resource
config
```

---

# 308. SOURCE MAP

```json
{
  "generated": "ul:example:machine",
  "source": {
    "module": "example",
    "class": "com.example.Machine",
    "method": "tick"
  }
}
```

---

# 309. CONVERSION REPORT

Status:

```text FULL
PARTIAL
EMULATED
FALLBACK
CLIENT_REQUIRED
UNSUPPORTED
FAILED
IGNORED
```

---

# 310. UNKNOWN REPORT

Unknown dynamic behavior must include:

```text file
class
method
line/bytecode offset
API call
reason
```

---

# 311. LOGGING PREFIXES

```text [USML]
[USML-CONVERTER]
[USML-FS]
[USML-PACK]
[USML-IR]
[USML-RENDER]
[USML-PHYSICS]
[USML-JAVA]
[USML-BEDROCK]
[USML-GEYSER]
[USML-VERSION]
[USML-SECURITY]
```

---

# 312. FS LOG

Example:

```text
[USML-FS]
module=create
WRITE ./config/foo.toml
→ config/create/foo.toml
storage=CONFIG
```

---

# 313. FOREIGN ROOT LOG

Example:

```text
[USML-FS][WARN]
Foreign root detected:
module=example
path=./MyGeneratedData
classification=MODULE_PRIVATE
action=REDIRECT
destination=data/mods/example/legacy-root/MyGeneratedData
```

---

# 314. SECURITY LOG

Example:

```text
[USML-SECURITY][WARN]
module=example
attempted path=C:\Users\Admin\.minecraft\...
policy=DENY
```

---

# 315. DIAGNOSTIC COMMANDS

```text /usml modules
/usml modules inspect <id>
/usml modules report <id>

/usml fs list <id>
/usml fs inspect <id>
/usml fs trace <id>
/usml fs roots
/usml fs conflicts

/usml packs
/usml packs inspect
/usml packs order

/usml versions
/usml capabilities
/usml dependencies

/usml cache
/usml reload packs
/usml reload data
```

---

# 316. FS ROOT COMMAND

```text /usml fs roots
```

shows:

```text canonical
legacy
foreign
shared
protected
```

---

# 317. FS MIGRATION COMMANDS

```text /usml fs migrate <module>
/usml fs import-legacy <module>
/usml fs rollback <migration-id>
```

---

# 318. PACK COMMANDS

```text /usml packs list
/usml packs inspect <id>
/usml packs rebuild
/usml packs conflicts
/usml packs order
```

---

# 319. CONFIG COMMANDS

```text /usml config list
/usml config inspect <id>
/usml config reload <id>
/usml config migrate <id>
```

---

# 320. WORLD COMMANDS

```text /usml world data list <world>
/usml world modules <world>
```

---

# 321. DEBUG FILE TREE

```text /usml fs tree <module>
```

show:

```text virtual tree
physical tree
aliases
```

---

# 322. PACK VALIDATOR

Validate:

```text path
manifest
schema
references
namespace
dependencies
hash
pack version
```

---

# 323. JAVA RESOURCE PACK VALIDATOR

Validate:

```text pack.mcmeta
assets
models
textures
sounds
fonts
lang
```

---

# 324. JAVA DATA PACK VALIDATOR

Validate:

```text pack.mcmeta
data/
namespaces
functions
recipes
loot
worldgen
advancements
tags
structures
```

---

# 325. BEDROCK VALIDATOR

Validate:

```text manifest
UUID
modules
dependencies
subpacks
resource references
behavior references
scripts
Molang
geometry
render controllers
```

---

# 326. FILE ARCHIVE VALIDATOR

All ZIP-like inputs:

```text no path traversal
no duplicate unsafe paths
no excessive decompression
no symlink escape
```

---

# 327. PACK ICON

Preserve:

```text pack_icon.png
```

when platform supports it.

---

# 328. LANGUAGE

Universal localization:

```text namespace
locale
translation key
source
```

Paper supports server-side translations via components; USML should prefer server-side translation where possible instead of requiring a pack for plain textual content.

---

# 329. MODEL COMPILER

Input:

```text source model systems
```

Output:

```text UniversalModel
```

then:

```text Java Model
Bedrock Geometry
Display Representation
```

---

# 330. SHADER COMPILER

Do not fake arbitrary shaders.

Classify:

```text VANILLA_COMPATIBLE
RESOURCE_SHADER
CLIENT_SHADER_REQUIRED
UNSUPPORTED
```

---

# 331. RENDER PIPELINE VERSIONING

Minecraft rendering changes must be treated as a version capability.

Fabric explicitly documented major render/backend changes for 26.2, including raw OpenGL assumptions and the backend transition.

Therefore no global:

```text OpenGLRenderer
```

assumption.

---

# 332. RENDER BACKEND

```text JavaVanillaPresentationBackend
JavaDisplayBackend
JavaCarrierBackend
BedrockPresentationBackend
```

---

# 333. DISPLAY BACKEND

If target supports Display entities:

```text use display primitives
```

Paper documentation confirms block/item/text displays and transform interpolation.

---

# 334. ANIMATION BACKEND

Java:

```text transforms
display interpolation
entity metadata
resource model animations
```

Bedrock:

```text animations
animation controllers
Molang
render controllers
```

---

# 335. CONTRAPTION RENDER BACKEND

```text scene graph
root transform
local nodes
interpolation
resource model
```

---

# 336. LIGHTING

Visual object must declare:

```text block light
sky light
emissive
```

If exact light sampling is impossible:

```text fallback
```

---

# 337. COLLISION

Logical collision independent of visual model.

---

# 338. HITBOX

Custom entity:

```text logical hitbox
carrier hitbox
visual bounds
```

three may differ.

---

# 339. INTERACTION

Custom interaction:

```text click
attack
use
pickup
mount
dismount
```

---

# 340. NETWORK TRACKING

Every logical visual object has:

```text tracked players
```

---

# 341. VIEW DISTANCE

Support per object:

```text default
custom
permission
world-specific
```

---

# 342. VISIBILITY

```text all players
specific player
permission
team
world
dimension
```

---

# 343. ENTITY OWNERSHIP

Carrier entities must record:

```text logicalEntityId
sourceModule
sceneId
```

---

# 344. CLEANUP

On module disable:

```text unregister events
cancel tasks
destroy temporary visuals
flush data
close DB
stop watchers
close executors
```

---

# 345. FILE WATCHER CLEANUP

All module watch services must be closed on disable.

---

# 346. DB CLEANUP

DB connections must be closed.

---

# 347. THREAD CLEANUP

Module-created executors must be tracked.

---

# 348. MODULE THREAD LEAK

At disable:

```text detect alive module-owned threads
report
attempt shutdown
```

---

# 349. ASYNC TASK LEAK

Track:

```text future
scheduled task
executor
```

---

# 350. MODULE METRICS

Per module:

```text tick time
CPU time
memory
filesystem reads
filesystem writes
network bytes
packet count
entity count
visual count
physics cost
```

---

# 351. SERVER PERFORMANCE

Do not claim to replace client optimizer mods.

USML can improve:

```text server tick
network
entity tracking
physics scheduling
data cache
resource compilation
```

but not magically implement:

```text Sodium GPU optimizations
Vulkan driver optimization
client mesh pipeline replacement
```

---

# 352. CLIENT OPTIMIZATION EXCLUSION

If mod includes:

```text render optimization
OpenGL optimization
GPU buffers
shader replacement
mesh optimizer
client memory optimizer
```

exclude those parts unless a server-side analogue exists.

---

# 353. SERVER OPTIMIZATION RETENTION

If same mod also contains:

```text server-side optimization
```

preserve it.

---

# 354. MULTI-SIDE MOD

A single JAR may contain:

```text common
server
client
resources
```

Analyzer must split feature graph accordingly.

---

# 355. CLIENT-ONLY ENTRYPOINT

Do not invoke on dedicated server.

---

# 356. COMMON ENTRYPOINT

Invoke in Universal Runtime.

---

# 357. SERVER ENTRYPOINT

Map to server lifecycle.

---

# 358. CLIENT ENTRYPOINT

Try:

```text emulate
presentation compiler
exclude
```

depending on feature.

---

# 359. FABRIC ENTRYPOINTS

Parse:

```text fabric.mod.json
entrypoints
```

and route by side/type.

Fabric Loader uses entrypoints as mod initialization/integration hooks.

---

# 360. QUILT ENTRYPOINTS

Parse:

```text quilt.mod.json
entrypoints
```

plus Fabric compatibility.

---

# 361. FORGE LIFECYCLE

Parse:

```text mod constructor
event bus
mod event bus
common setup
server lifecycle
```

and translate into lifecycle stages.

---

# 362. NEOFORGE LIFECYCLE

Same semantic model, but with NeoForge-specific API mappings.

Do not copy Forge symbol names blindly.

---

# 363. BUKKIT LIFECYCLE

```text plugin load
onLoad
onEnable
onDisable
```

---

# 364. PAPER LIFECYCLE

```text bootstrap
plugin loader
onLoad
onEnable
onDisable
```

---

# 365. REGISTRY BOOTSTRAP STAGE

Universal:

```text BOOTSTRAP
```

available before world runtime where target permits.

---

# 366. RESOURCE DISCOVERY STAGE

```text DISCOVER_RESOURCES
```

before actual runtime activation.

---

# 367. DATA DISCOVERY STAGE

```text DISCOVER_DATA
```

---

# 368. REGISTRY FREEZE

After required registrations:

```text REGISTRY_FREEZE
```

No arbitrary late registry mutation unless target supports it.

---

# 369. MODULE FREEZE

Equivalent to Fabric's loader freeze concept: once the appropriate loading phase completes, late registration should be constrained rather than silently injecting arbitrary modules. Fabric source explicitly exposes a freeze concept to prevent additional mods from being loaded after resolution.

---

# 370. CONVERSION CACHE

Key:

```text sourceHash
platform
platformVersion
minecraftVersion
target
targetVersion
converterVersion
configHash
```

---

# 371. DETERMINISTIC BUILD

Same input must generate same output.

---

# 372. PACK BUILD CACHE

Generated Java/Bedrock packs cached separately.

---

# 373. MODULE BUILD CACHE

Converted module runtime cached separately.

---

# 374. INVALIDATION

Invalidate when:

```text source JAR changes
pack changes
config changes
target changes
version profile changes
converter version changes
mapping changes
```

---

# 375. LOCKFILES

Create:

```text universal-mods.lock
universal-packs.lock
version-registry.lock
```

---

# 376. MODULE LOCK

Records:

```text source hash
resolved dependencies
target adapter
conversion status
generated artifact hash
```

---

# 377. PACK LOCK

Records:

```text source hashes
stack order
dependencies
generated hashes
```

---

# 378. VERSION LOCK

Records exact upstream versions used during build.

---

# 379. REPRODUCIBILITY

Given the same:

```text JARs
packs
config
version lock
converter
```

the resulting generated content must be reproducible.

---

# 380. QUARANTINE

Broken modules go to:

```text quarantine/
```

with:

```text source hash
error report
conversion report
```

Do not delete original.

---

# 381. FAILURE ISOLATION

A renderer failure should not kill gameplay module.

A pack conversion failure should not kill the server if the pack is optional.

---

# 382. CORE DEPENDENCY FAILURE

Only fail startup if:

```text required gameplay dependency
required bootstrap dependency
corrupted server state
```

cannot be recovered.

---

# 383. PACK OPTIONALITY

Every pack:

```text REQUIRED
OPTIONAL
COSMETIC
DEV_ONLY
ADMIN_ONLY
```

---

# 384. MODULE OPTIONALITY

Every module dependency:

```text REQUIRED
OPTIONAL
SOFT
PROVIDED
```

---

# 385. ADMIN OVERRIDES

Admin can override:

```text base block
entity carrier
resource path
filesystem mapping
thread policy
feature disable
pack order
ignore
```

---

# 386. ADMIN OVERRIDE EXAMPLE

```yaml
module-overrides:
  create:
    filesystem:
      legacy:
        "schematics": SHARED

    features:
      client-renderer: emulate
      gpu-optimizer: disable

    blocks:
      create:andesite_casing:
        base: minecraft:note_block
```

---

# 387. MOD-SPECIFIC CONVERTERS

Optional:

```text converters/create/
converters/modelengine/
converters/worldedit/
```

They output UIR.

---

# 388. GENERIC CONVERTER VS SPECIALIZED CONVERTER

Generic:

```text common APIs
```

Specialized:

```text semantic frameworks
```

---

# 389. NO HARD-CODE IN CORE

Core must not contain:

```java
if (modId.equals("create"))
```

---

# 390. SPECIAL CONVERTER REGISTRATION

```text ServiceLoader
```

or explicit converter registry.

---

# 391. API COMPATIBILITY SHIMS

Universal API must map:

```text Fabric Event
Forge Event
NeoForge Event
Bukkit Event
Paper Event
```

to:

```text Universal Event
```

---

# 392. SEMANTIC MAPPING

A source API call is not valid simply because:

```text names look similar
```

Must preserve:

```text cancellation
mutation
timing
thread
state
```

---

# 393. RETURN VALUE MAPPING

Return semantics must be preserved.

---

# 394. EXCEPTION MAPPING

Exceptions should map to semantic equivalents or be translated with source information preserved.

---

# 395. NULLABILITY

Source nullability must be tracked.

---

# 396. OPTIONAL API

Missing optional dependency:

```text feature disabled
```

not whole module failure.

---

# 397. REQUIRED API

Missing required dependency:

```text conversion failure
```

with exact symbol.

---

# 398. UNKNOWN API

Mark:

```text UNKNOWN_API
```

and continue where safe.

---

# 399. VERSION-SPECIFIC API

Mapping record:

```yaml
source:
  platform: paper
  version: 26.2
  symbol: "..."

target:
  ir: "..."

valid:
  from: ...
  to: ...
```

---

# 400. DEPRECATION

Track:

```text introduced
deprecated
removed
replacement
```

---

# 401. REMOVED SYMBOL

Cannot be treated as typo.

---

# 402. MAPPINGS

Support:

```text Mojmap
Yarn
official
Spigot mappings
source names
```

and normalize through mapping registry.

---

# 403. BYTECODE MAPPING

Map:

```text owner
name
descriptor
signature
```

not only human-readable method names.

---

# 404. DESCRIPTOR MAPPING

When method descriptor changes across versions, mapping must include descriptor transformation.

---

# 405. FIELD DESCRIPTOR

Same.

---

# 406. GENERICS

Generic signatures should be preserved when useful for source-level reconstruction, but runtime conversion must use JVM descriptors.

---

# 407. RESOURCE NAMESPACE

Every asset:

```text namespace
path
owner
target
```

---

# 408. NAMESPACE COLLISION

If two modules claim same logical namespace:

```text error
```

unless explicit alias.

---

# 409. ID ALIAS

```yaml
aliases:
  oldmod:item:
    newmod:item
```

---

# 410. SAVE MIGRATION

When IDs change:

```text save migration
```

must transform:

```text block IDs
item IDs
entity IDs
persistent data
```

---

# 411. PLAYER DATA MIGRATION

Preserve:

```text inventories
custom data
UUID
progress
```

---

# 412. WORLD DATA MIGRATION

Preserve:

```text block entities
chunk metadata
structures
world custom data
```

---

# 413. PACK MIGRATION

When generated asset path changes:

```text pack source-map
```

must map old→new.

---

# 414. SERVER STARTUP ORDER

```text 1 config
2 filesystem
3 version detection
4 platform detection
5 source scanning
6 pack scanning
7 dependency graph
8 module analysis
9 UIR construction
10 registry bootstrap
11 runtime generation
12 Java/Bedrock pack compilation
13 target registration
14 network init
15 server world startup
16 client content delivery
```

---

# 415. FILESYSTEM INIT

Filesystem must be initialized **before module code is constructed**.

Otherwise:

```text early static initializer
```

can write to wrong paths.

---

# 416. EARLY STATIC INITIALIZERS

Static initialization must be analyzed.

If class static initializer performs filesystem writes:

```text classify
```

and ensure ModuleFS context exists before load.

---

# 417. STATIC INIT FAILURE

If context cannot be established:

```text isolate
or
fail with precise diagnostic
```

never silent.

---

# 418. BOOTSTRAP FILESYSTEM

Bootstrap modules get:

```text bootstrap filesystem context
```

with:

```text config
libraries
resources
runtime
```

---

# 419. WORLD CONTEXT

When module operates against world:

```text WorldModuleContext
```

should resolve:

```text world-scoped config
world data
world generated
```

---

# 420. PLAYER CONTEXT

When module handles player-specific data:

```text PlayerModuleContext
```

should resolve:

```text player data
session
```

---

# 421. DIMENSION CONTEXT

For dimension-local data:

```text data/worlds/<world>/dimensions/<dimension>/<module>/
```

---

# 422. CHUNK CONTEXT

For chunk metadata:

```text native chunk persistence if possible
```

rather than arbitrary files.

---

# 423. SESSION DATA

Temporary:

```text temp/sessions/<session-id>/
```

---

# 424. CACHE POLICY

Caches can be deleted safely.

---

# 425. DATA POLICY

Persistent data must not be deleted merely because module disabled.

---

# 426. CONFIG POLICY

Config must survive module updates.

---

# 427. GENERATED POLICY

Generated output can be rebuilt.

---

# 428. LEGACY POLICY

Legacy data preserved during migration until confirmation.

---

# 429. BACKUP POLICY

Before destructive migration:

```text backup
```

---

# 430. RESTORE POLICY

Support:

```text restore file
restore module
restore pack
restore world module data
```

---

# 431. ADMIN DATA EXPORT

```text /usml export <module>
```

must package:

```text config
persistent data
database
logs
conversion report
filesystem mappings
```

not original JAR unless explicitly allowed.

---

# 432. DATA IMPORT

```text /usml import <module> <archive>
```

must validate hashes and schemas.

---

# 433. SERVER CLONE

A server can be cloned with:

```text mods
plugins
config
data
datapacks
resourcepacks
texturepacks
addons
bedrock packs
lockfiles
```

while excluding:

```text cache
temp
runtime transient
```

---

# 434. BACKUP CLASSIFICATION

Backup:

```text CONFIG
DATA
WORLD
PACK
MODULE
```

exclude:

```text CACHE
TEMP
```

by default.

---

# 435. WORLD BACKUP

USML must not treat world data as ordinary module data.

---

# 436. ADMIN PACK OVERRIDE

Admin pack:

```text root/resourcepacks/admin/
```

or through priority list.

---

# 437. GENERATED PACK ROOT

```text generated/packs/java/
generated/packs/bedrock/
```

---

# 438. GENERATED DATAPACK ROOT

```text generated/datapacks/<module>/
```

---

# 439. GENERATED ADDON ROOT

```text generated/addons/<module>/
```

---

# 440. GENERATED GEYSER ROOT

```text generated/geyser/
```

---

# 441. GEYSER PACK PUBLISHER

Implement:

```text GeyserPackPublisher
```

---

# 442. BEDROCK PACK MERGE

Merge:

```text modules
addons
RP
texture input
generated content
```

---

# 443. BEDROCK PACK UUID

Generated UUIDs must be deterministic or persisted, never random on every build.

---

# 444. BEDROCK DEPENDENCY UUID

When generating BP→RP dependencies, use matching generated UUID/version records. Bedrock manifest dependency semantics require the referenced UUID/version.

---

# 445. BEDROCK SCRIPT MODULE

Use:

```text module_name
version
```

as part of dependency graph where current schema allows it.

---

# 446. SUBPACK PERFORMANCE

Subpacks can be used for:

```text HD
SD
low-memory
```

and USML may auto-generate tiers when beneficial. Bedrock's current docs explicitly use subpacks for different memory/performance tiers.

---

# 447. DEVICE-AWARE CONTENT

Bedrock backend may choose pack variant based on:

```text device tier
memory tier
server policy
```

---

# 448. JAVA CLIENT CONTENT

Java server cannot force arbitrary executable client code.

Therefore:

```text resource/data/packet/emulation
```

are primary mechanisms.

---

# 449. OPTIONAL USML CLIENT

Advanced client features can be negotiated when available.

---

# 450. CLIENTLESS GUARANTEE

A feature may claim:

```text CLIENTLESS
```

only when:

```text vanilla-compatible
+
required pack/data/packet primitives exist
```

---

# 451. CLIENTLESS PARTIAL

Feature may claim:

```text CLIENTLESS_PARTIAL
```

when visual fidelity is lower but gameplay preserved.

---

# 452. CLIENT REQUIRED

Use only when:

```text no vanilla-compatible representation
```

---

# 453. RENDER FIDELITY

Reports should say:

```text FULL_FIDELITY
HIGH
EMULATED
APPROXIMATE
FALLBACK
```

not a fake percentage.

---

# 454. PHYSICS FIDELITY

Same:

```text EXACT
SERVER_AUTHORITATIVE_EMULATION
APPROXIMATE
FALLBACK
```

---

# 455. INPUT FIDELITY

```text EXACT
EMULATED
UNAVAILABLE
```

---

# 456. CAMERA FIDELITY

```text EXACT
EMULATED
LIMITED
UNAVAILABLE
```

---

# 457. HUD FIDELITY

```text EXACT
PACK_BASED
VANILLA_PRIMITIVE
LIMITED
UNAVAILABLE
```

---

# 458. NETWORK COMPATIBILITY

Versioned protocol:

```text universal protocol version
```

separate from:

```text Minecraft protocol
```

---

# 459. UNIVERSAL PROTOCOL NEGOTIATION

Handshake fields:

```text USML protocol version
server target
client capability
pack hash
module content hash
```

---

# 460. UNKNOWN CLIENT

Treat as:

```text vanilla-compatible
```

unless a feature specifically requires known capability.

---

# 461. BEDROCK CLIENT IDENTIFICATION

Through Geyser integration:

```text Bedrock
```

is a client backend identity, not a source loader identity.

---

# 462. SOURCE/TARGET MATRIX

Every conversion should expose:

```text source loader
source version
source MC version
target loader
target version
client backend
```

---

# 463. TEST MATRIX

Required:

```text Fabric → Paper
Fabric → Folia
Fabric → Canvas
Forge → Paper
Forge → Folia
Forge → Canvas
NeoForge → Paper
NeoForge → Folia
Quilt → Paper
Bukkit → Paper
Spigot → Paper
Paper → Paper
Paper → Folia
Purpur → Paper
Any → Java
Any → Geyser → Bedrock
```

---

# 464. FILESYSTEM TEST MATRIX

Test:

```text relative file
absolute root path
nested directory
directory listing
watcher
rename
delete
move
symlink
database
cache
temp
external path
shared folder
```

---

# 465. PLUGIN FILESYSTEM FIXTURE

Create plugin:

```text getDataFolder()
saveDefaultConfig()
saveResource()
new File("foo")
Paths.get("bar")
Files.write(...)
Files.list(...)
WatchService
SQLite
```

and verify all resolve correctly.

---

# 466. MOD FILESYSTEM FIXTURE

Create mod:

```text FabricLoader#getGameDir
FabricLoader#getConfigDir
new File("...")
```

and verify.

---

# 467. FORGE FILESYSTEM FIXTURE

Use:

```text FMLPaths
config
defaultconfigs
world/serverconfig
```

and raw IO.

---

# 468. NEOFORGE FILESYSTEM FIXTURE

Use:

```text STARTUP
COMMON
SERVER
raw IO
```

---

# 469. QUILT FILESYSTEM FIXTURE

Use:

```text configurable modsDir
configDir
cacheDir
additional mods
```

---

# 470. FOREIGN ROOT TEST

Module writes:

```text ./myfolder/file.dat
```

and then:

```text new File(".").listFiles()
```

must see its own logical file.

---

# 471. ABSOLUTE ROOT TEST

Module accesses:

```text ./../foo
```

must resolve according to policy, not escape sandbox.

---

# 472. SHARED ROOT TEST

A+B both access:

```text ./shared
```

must share one mapped root when configured.

---

# 473. ASYNC FILE TEST

Module writes from:

```text async executor
```

must retain module context.

---

# 474. WATCHER TEST

Config watcher must fire correctly after canonical relocation.

---

# 475. MIGRATION TEST

Old data:

```text plugins/Test/data.db
```

must become:

```text data/plugins/Test/data.db
```

with backup and source map.

---

# 476. PLUGIN API TEST

Verify:

```text getDataFolder()
getFile()
getResource()
saveResource()
getConfig()
saveConfig()
reloadConfig()
```

each returns correct semantic target.

---

# 477. MOD API TEST

Verify:

```text getGameDir()
getConfigDir()
resources
mod IDs
entrypoints
```

---

# 478. PACK TEST

Verify:

```text Java RP
Java DP
Java texture input
Bedrock RP
Bedrock BP
Bedrock addon
Bedrock texture input
```

---

# 479. VERSION REGRESSION TEST

Every upstream update triggers:

```text API inventory diff
capability diff
mapping diff
filesystem diff
render diff
pack diff
```

---

# 480. UPSTREAM CHANGE REPORT

Example:

```text Fabric 26.2 -> next:
3 rendering API changes
2 loader changes
1 config behavior change
0 filesystem changes
```

---

# 481. RENDER MIGRATION

When renderer changes:

```text capability diff
```

not core rewrite.

---

# 482. CONFIG MIGRATION

When config schema changes:

```text mapping migration
```

---

# 483. PACK FORMAT MIGRATION

When pack format changes:

```text compiler profile update
```

---

# 484. DATA PACK FORMAT MIGRATION

Same.

---

# 485. BEDROCK MANIFEST MIGRATION

Same.

---

# 486. DEPENDENCY SOLVER

Must support:

```text version ranges
intervals
optional
conflict
provides
replaces
```

---

# 487. MOD IDENTITY

```text source ID
universal ID
target alias
```

must remain separate.

---

# 488. PLUGIN IDENTITY

Plugin `name` is an identifier and is case-sensitive in Bukkit semantics; it is also used to determine the default plugin data folder. USML must preserve the source identity independently from its new physical data path.

---

# 489. JAR FILENAME VS ID

Never assume:

```text filename == mod ID
```

or:

```text filename == plugin ID
```

Use metadata first.

---

# 490. DUPLICATE ID

If two active modules claim same logical ID:

```text dependency solver conflict
```

---

# 491. DUPLICATE JAR NAME

Do not overwrite automatically.

---

# 492. DISABLED JAR

Support:

```text .jar.disabled
```

without treating it as active module.

Quilt already has its own semantics for `.disabled` and hidden files; USML should honor source-loader rules during source analysis.

---

# 493. NESTED MODS

Nested modules must be discoverable recursively.

---

# 494. EMBEDDED PACKS

If module JAR contains:

```text resourcepack
datapack
addon
```

extract semantically into PackSource without necessarily materializing every source file.

---

# 495. EMBEDDED DATAPACK

Paper's datapack discovery architecture demonstrates why embedded data packs should be first-class runtime resources rather than always extracted manually.

---

# 496. GENERATED DATAPACK

Compiler may create a datapack rather than writing direct server code when target semantics fit.

---

# 497. NATIVE DATAPACK

A user-provided pack under:

```text datapacks/
```

must be active even without a mod.

---

# 498. NATIVE RESOURCEPACK

A user-provided pack under:

```text resourcepacks/
```

must be active even without a mod.

---

# 499. NATIVE TEXTURE INPUT

A user-provided pack under:

```text texturepacks/
```

must be normalized.

---

# 500. NATIVE BEDROCK RP

Must be independently loadable.

---

# 501. NATIVE BEDROCK BP

Must be independently loadable or converted based on target backend.

---

# 502. NATIVE BEDROCK ADDON

Must unpack and route.

---

# 503. SERVER ROOT DISCOVERY

On first start USML scans:

```text known roots
```

and optionally:

```text unknown roots
```

to identify existing legacy module data.

---

# 504. LEGACY ROOT SCAN

Scan patterns:

```text known mod IDs
known plugin IDs
known package prefixes
known configured paths
known source maps
```

---

# 505. PATH OWNERSHIP HEURISTIC

Sources:

```text class origin
module ID
filename
callsite
resource namespace
existing data
```

---

# 506. MULTIPLE OWNER HEURISTIC

If ambiguous:

```text UNKNOWN_SHARED
```

do not auto-move.

---

# 507. ADMIN CONFIRMATION

For ambiguous path:

```text report
```

and allow:

```yaml
ownership:
  "folder":
    owner: shared
```

---

# 508. FILE LOCKING / LIVE MIGRATION

Do not migrate a file while module is active.

Migration occurs:

```text pre-start
disabled
or
server stop
```

---

# 509. HOT RELOAD FILE MIGRATION

Not allowed for arbitrary persistent data.

---

# 510. DATA BACKUP BEFORE UPDATE

Module update:

```text compare schema
backup
migrate
```

---

# 511. CONVERSION VERSIONING

Generated runtime schema:

```text UIR schema version
```

must be versioned.

---

# 512. UIR SERIALIZATION

Use:

```text deterministic binary/JSON
```

for cache/debug.

---

# 513. UIR JSON DEBUG

Optional human-readable:

```text reports/uir/<module>.json
```

---

# 514. UIR VALIDATOR

Validate:

```text required fields
references
IDs
dependency graph
capability compatibility
```

---

# 515. SEMANTIC NORMALIZER

Normalize:

```text blocks
items
entities
events
configs
filesystems
```

before target conversion.

---

# 516. TARGET COMPILER

Target compiler reads:

```text UIR
target capabilities
version profile
```

---

# 517. JAVA TARGET

Output:

```text runtime code
resource pack
data pack
network state
display entities
```

---

# 518. BEDROCK TARGET

Output:

```text Resource Pack
Behavior Pack
Addon
Geyser mapping
script
```

---

# 519. TARGET FALLBACK ORDER

```text native target capability
→ server emulation
→ resource representation
→ display/carrier
→ placeholder
```

---

# 520. UNSUPPORTED FEATURE

Must never silently disappear.

Output:

```text excluded
reason
fallback
impact
```

---

# 521. PERFORMANCE PROFILE

Per module:

```text ANALYSIS
CONVERSION
RUNTIME
FS
NETWORK
RENDER
PHYSICS
```

---

# 522. FS METRICS

Count:

```text reads
writes
opens
closes
lists
watchers
bytes
database operations
```

---

# 523. FS THROTTLING

Optional guard against modules doing enormous filesystem scans.

---

# 524. IO BLOCKING WARNING

Paper docs explicitly warn that plugin file load/save on the main thread can slow the server. USML should track blocking file operations by module/thread and report them.

---

# 525. IO THREAD POLICY

```text MAIN
REGION
ENTITY
ASYNC
```

with warnings for blocking operations where target platform considers them unsafe.

---

# 526. FOLIA FILESYSTEM + THREADING

Filesystem operations themselves may still be thread-safe, but surrounding world interaction is not necessarily so.

Do not assume:

```text file thread = world thread
```

---

# 527. WORLD + FILE COUPLING

A task that writes world state:

```text requires world context
```

even if its file access is async.

---

# 528. DATABASE TRANSACTION

Universal storage must support:

```text begin
commit
rollback
```

when source semantics require it.

---

# 529. DATA INTEGRITY

Atomic writes:

```text temp file
fsync where appropriate
atomic move
```

---

# 530. CONFIG SAFE WRITE

Config writers should prefer:

```text write temp
validate
atomic replace
```

when target FS allows.

---

# 531. CRASH RECOVERY

On startup:

```text detect *.tmp
*.bak
journal
```

and recover where policy permits.

---

# 532. FS JOURNAL

Optional:

```text data/server/filesystem-journal.log
```

for migrations/recovery.

---

# 533. FILE HASHING

For migrated/generated files:

```text SHA256
```

---

# 534. SOURCE INTEGRITY

Original JAR hash must be retained.

---

# 535. PACK INTEGRITY

Generated pack hashes must be retained.

---

# 536. DATA INTEGRITY

Persistent data migrations should record before/after hashes.

---

# 537. SAVE FORMAT

Universal storage schema must include:

```text schemaVersion
moduleVersion
minecraftVersion
converterVersion
```

---

# 538. MOD UPDATE

When source mod updates:

```text old UIR
new UIR
migration
```

---

# 539. CROSS-LOADER UPDATE

When mod moves:

```text Forge -> NeoForge
Fabric -> Quilt
```

preserve logical IDs and data.

---

# 540. CROSS-SERVER UPDATE

When server moves:

```text Paper -> Folia
Paper -> Canvas
```

preserve logical module data where possible.

---

# 541. CROSS-EDITION DATA

Java→Bedrock should preserve:

```text logical data
```

but not assume file-level compatibility.

---

# 542. PACK CONTENT SOURCE PRIORITY

When same resource exists:

```text explicit admin override
>
module-specific override
>
source pack priority
>
generated fallback
```

---

# 543. DATA CONTENT SOURCE PRIORITY

For datapack content:

```text explicit world override
>
server pack
>
module default
>
vanilla
```

---

# 544. CONFIG SOURCE PRIORITY

```text runtime override
>
user config
>
defaultconfig
>
embedded default
```

---

# 545. ADMIN CONFIG

Main:

```text config/universal-loader.yml
```

Pack:

```text config/packs.yml
```

Ignore:

```text config/ignore.yml
```

Filesystem:

```text config/filesystem.yml
```

Version overrides:

```text config/versions.yml
```

Module overrides:

```text config/overrides/<module>/
```

---

# 546. FILESYSTEM CONFIG EXAMPLE

```yaml
filesystem:
  mode: hybrid

  roots:
    module-data: ./data/mods
    plugin-data: ./data/plugins
    shared-data: ./data/shared
    world-data: ./data/worlds

  relative-path-policy: module-data

  foreign-root-policy: auto-register

  absolute-path-policy: deny-outside-root

  symlinks:
    allow: false

  watchers:
    enabled: true

  audit:
    reads: false
    writes: true

  migration:
    auto-detect: true
    backup: true
```

---

# 547. PACK CONFIG EXAMPLE

```yaml
packs:
  java:
    datapacks:
      enabled: true
    resourcepacks:
      enabled: true
    texturepacks:
      enabled: true

  bedrock:
    addons:
      enabled: true
    resourcepacks:
      enabled: true
    behaviorpacks:
      enabled: true
    texturepacks:
      enabled: true
    scripts:
      enabled: true
```

---

# 548. IGNORE CONFIG EXAMPLE

```yaml
ignore:
  ids:
    - create
    - modelengine

  jars:
    - Create-6.0.10.jar
    - ModelEngine-4.0.0.jar
```

---

# 549. FOREIGN ROOT OVERRIDE

```yaml
foreign-roots:
  schematics:
    owner: shared
    target: data/shared/schematics

  mycustomdata:
    owner: examplemod
    target: data/mods/examplemod/legacy/mycustomdata
```

---

# 550. DATA ROOT EXAMPLE

```text
data/
├── mods/
│   ├── create/
│   └── examplemod/
│
├── plugins/
│   ├── WorldEdit/
│   └── MyPlugin/
│
├── shared/
│   ├── schematics/
│   └── economy/
│
├── worlds/
│   ├── world/
│   │   ├── mods/
│   │   └── plugins/
│   └── minigame/
│
├── players/
└── server/
```

---

# 551. PLUGIN EXAMPLE

Source plugin:

```java
@Override
public void onEnable() {
    saveDefaultConfig();

    File db = new File(getDataFolder(), "database.db");

    File legacy = new File("OldPluginData/cache.bin");

    getServer().getScheduler().runTaskAsynchronously(
        this,
        () -> saveSomething()
    );
}
```

USML:

```text
getDataFolder()
→ data/plugins/MyPlugin/

database.db
→ data/plugins/MyPlugin/database.db

OldPluginData/cache.bin
→ detected legacy root
→ data/plugins/MyPlugin/legacy/OldPluginData/cache.bin

async task
→ module context propagated
```

---

# 552. PLUGIN RESOURCE EXAMPLE

```text
MyPlugin.jar
└── config.yml
```

`saveDefaultConfig()`:

```text
JAR config.yml
↓
data/plugins/MyPlugin/config.yml
```

---

# 553. MOD EXAMPLE

```java
Path config = FabricLoader.getInstance().getConfigDir();

Path file = Paths.get("machine-data");
```

USML:

```text
config
→ config/

machine-data
→ data/mods/<id>/machine-data
```

---

# 554. FORGE EXAMPLE

```text
COMMON:
config/example-common.toml

SERVER:
world/serverconfig/example-server.toml

DEFAULT:
defaultconfigs/example-server.toml
```

USML preserves all three semantics.

---

# 555. NEOFORGE EXAMPLE

```text
STARTUP
COMMON
CLIENT
SERVER
```

USML records each as independent config descriptors.

---

# 556. BEDROCK EXAMPLE

```text
bedrock/addons/example.mcaddon
```

USML:

```text unpack
→ RP
→ BP
→ dependencies
→ compile/publish
```

---

# 557. JAVA PACK EXAMPLE

```text
texturepacks/realistic.zip
```

USML:

```text JavaTextureInput
→ ResourcePackIR
→ final Java resource pack
```

---

# 558. PACK STACK EXAMPLE

```text
resourcepacks/
├── base.zip
├── module-assets.zip
└── admin.zip
```

final:

```text base
→ module
→ admin
```

---

# 559. BEDROCK STACK EXAMPLE

```text
bedrock/resourcepacks/
├── base.mcpack
├── module.mcpack
└── admin.mcpack
```

plus BP dependencies.

---

# 560. SERVER CONTENT BUILD

The loader should build once:

```text Universal Content Graph
```

then produce:

```text Java package
Bedrock package
```

from the same graph.

---

# 561. NO DUPLICATE LOGIC

Gameplay implementation must live once:

```text Universal Runtime
```

not once for Java and once for Bedrock.

---

# 562. PLATFORM PRESENTATION ONLY

Java/Bedrock differences belong mainly in:

```text presentation backend
network backend
asset compiler
input adapter
```

---

# 563. SOURCE LOADER DIFFERENCES

Source loader differences belong mainly in:

```text metadata adapter
API compatibility adapter
bytecode translator
lifecycle adapter
```

---

# 564. SERVER FORK DIFFERENCES

Paper/Folia/Canvas/Purpur differences belong mainly in:

```text target adapter
scheduler
registry
network
lifecycle
```

---

# 565. NO N-SQUARED ADAPTER EXPLOSION

Architecture:

```text Forge
Fabric
NeoForge
Quilt
Bukkit
Paper
Folia
...
   ↓
Universal IR
   ↓
Target
```

not:

```text every-source × every-target
```

---

# 566. SOURCE ADAPTER COUNT

Source adapters scale with:

```text number of source platforms
```

---

# 567. TARGET ADAPTER COUNT

Target adapters scale with:

```text number of target platforms
```

---

# 568. VERSION ADAPTER COUNT

Version-specific behavior lives inside:

```text capability profiles
version adapters
mapping tables
```

not core.

---

# 569. CAPABILITY PROFILE

```yaml
displayEntity:
  introduced: 1.19.4

itemComponent:
  introduced: 1.20.5

someFeature:
  unsupported: [legacy-range]
```

---

# 570. CAPABILITY QUERY

Core asks:

```text supports(Capability.DISPLAY_ENTITY)
```

not:

```text if version >= 1.19.4
```

---

# 571. CURRENT API DIFF

Every upstream update generates:

```text added
removed
deprecated
changed semantics
```

---

# 572. DOCUMENTATION DIFF

Also track documentation changes separately from API changes.

---

# 573. MIGRATION PRIMER INPUT

NeoForge/Fabric/etc. migration primers must be parsed into:

```text version delta
```

---

# 574. SOURCE REPOSITORY REVISION

Record commit/tag/release used for research.

---

# 575. SOURCE DATE

Record:

```text checkedAt
```

---

# 576. CURRENT SOURCE VS RELEASE

Keep separate:

```text latest stable release
latest source branch
latest beta
```

---

# 577. RELEASE CHANNELS

```text stable
beta
snapshot
development
```

---

# 578. DEVELOPMENT PROFILE

Can be supported optionally:

```text source branch
development API
```

but must never contaminate stable capability profile.

---

# 579. TEST MODE

```text /usml test <module>
```

runs:

```text analyzer
filesystem simulation
conversion
pack compiler
```

without activating module.

---

# 580. SIMULATED FILESYSTEM

Testing uses:

```text in-memory FS
```

where practical.

---

# 581. GOLDEN FILESYSTEM

Expected:

```text virtual path
physical path
storage class
```

---

# 582. GOLDEN CONVERSION

Expected:

```text generated UIR
resource
data
runtime
report
```

---

# 583. VISUAL REGRESSION

Screenshot-based where supported.

---

# 584. BEDROCK PACK REGRESSION

Validate:

```text manifest
references
UUID
dependency
schema
```

---

# 585. NETWORK REGRESSION

Test:

```text Java client
Bedrock client
client reload
reconnect
dimension change
respawn
```

---

# 586. PLAYER JOIN REGRESSION

Test:

```text first join
rejoin
resource cache
pack decline
pack timeout
pack success
```

---

# 587. RESOURCE FAILURE

If Java resource pack fails:

```text required:
disconnect
optional:
fallback
```

---

# 588. BEDROCK PACK FAILURE

If Bedrock presentation pack fails:

```text fallback
```

while Java remains operational.

---

# 589. RESOURCE PACK CONTENT VERSION

Target-specific:

```text Java pack format
Bedrock manifest/min_engine_version
```

---

# 590. DATA PACK CONTENT VERSION

Target-specific:

```text datapack version
```

---

# 591. GENERATED PACK ICONS

Optional auto-generate from module icon.

---

# 592. TRANSLATION MERGE

Conflict policy:

```text source priority
admin override
```

---

# 593. FONT MERGE

Font providers should merge semantically.

---

# 594. SOUND MERGE

`java sounds.json` and Bedrock sounds structures require platform-specific merge.

---

# 595. MODEL MERGE

No generic JSON merge.

---

# 596. ANIMATION MERGE

Version-aware.

---

# 597. BEDROCK GEOMETRY

Universal Model IR must be able to compile into Bedrock geometry when possible.

---

# 598. RENDER CONTROLLERS

Bedrock compiler should produce render controllers when needed.

---

# 599. MOLANG

Bedrock dynamic visuals may use Molang, but only from the Bedrock compiler/backend.

---

# 600. FINAL ARCHITECTURE

```text
                         SERVER ROOT
                              │
          ┌───────────────────┼────────────────────┐
          │                   │                    │
        mods/              plugins/             packs
          │                   │                    │
          └──────────────┬────┴─────┬──────────────┘
                         ↓          ↓
                    SOURCE SCANNERS
                         │
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
       METADATA      BYTECODE        RESOURCE
       ANALYZER       ANALYZER         ANALYZER
          │              │              │
          └──────────────┼──────────────┘
                         ↓
                 FEATURE CLASSIFIER
                         ↓
                 DEPENDENCY GRAPH
                         ↓
              UNIVERSAL INTERMEDIATE
                    REPRESENTATION
                         ↓
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
   SERVER RUNTIME    DATA COMPILER    ASSET COMPILER
        │                │                │
        │                │         ┌──────┴──────┐
        │                │         ↓             ↓
        │                │      JAVA          BEDROCK
        │                │      ASSETS        ASSETS
        │                │
        ↓                ↓
   TARGET ADAPTERS    PACK MANAGER
        │
  ┌─────┼─────┬────────┬────────┐
  ↓     ↓     ↓        ↓        ↓
Paper Folia Canvas  Purpur   Bukkit/Spigot
  │
  └───────────────┬──────────────────────┐
                  ↓                      ↓
            Java Player             Geyser Player
                  ↓                      ↓
             Java Backend          Bedrock Backend
```

---

# 601. CORE DIRECTORY ARCHITECTURE

```text
server/
│
├── mods/
├── plugins/
│
├── config/
├── defaultconfigs/
│
├── data/
│   ├── mods/
│   ├── plugins/
│   ├── shared/
│   ├── worlds/
│   ├── players/
│   └── server/
│
├── libraries/
├── cache/
├── runtime/
├── generated/
├── logs/
├── temp/
├── reports/
├── quarantine/
├── backups/
│
├── datapacks/
├── resourcepacks/
├── texturepacks/
├── addons/
│
├── bedrock/
│   ├── addons/
│   ├── resourcepacks/
│   ├── behaviorpacks/
│   ├── texturepacks/
│   └── scripts/
│
└── world/
```

---

# 602. GOLDEN RULE

`plugins/`:

```text INSTALL JAR
```

`mods/`:

```text INSTALL JAR
```

`data/plugins/`:

```text PLUGIN PERSISTENT DATA
```

`data/mods/`:

```text MOD PERSISTENT DATA
```

`config/`:

```text GLOBAL/NATIVE CONFIG
```

`defaultconfigs/`:

```text CONFIG TEMPLATES
```

`world/<world>/serverconfig/`:

```text WORLD-SCOPED SERVER CONFIG
```

`datapacks/`:

```text JAVA DATA
```

`resourcepacks/`:

```text JAVA PRESENTATION
```

`texturepacks/`:

```text JAVA TEXTURE INPUT
```

`addons/`:

```text UNIVERSAL ADDON INPUT
```

`bedrock/*`:

```text BEDROCK-NATIVE CONTENT
```

---

# 603. NON-NEGOTIABLE FILESYSTEM RULE

**Nếu plugin/mod tự tạo bất kỳ file hoặc folder nào mà code có thể quan sát được, USML phải cố duy trì đúng semantic path mà source code mong đợi, dù physical storage đã được chuyển sang canonical USML data root.**

Nói cách khác:

```text source thinks:
./foo/bar

physical:
data/plugins/example/foo/bar
```

hoặc:

```text source thinks:
/schematics

physical:
data/shared/schematics
```

nếu registry xác định đó là shared root.

---

# 604. NON-NEGOTIABLE FAILURE RULE

Không được:

```text UNKNOWN → ignore
```

Mà phải:

```text UNKNOWN
→ report
→ continue where safe
→ fallback where available
```

---

# 605. NON-NEGOTIABLE VERSION RULE

Không được:

```text current = hard-coded list
```

Mà phải:

```text current = upstream-discovered registry
```

---

# 606. NON-NEGOTIABLE DATA RULE

Không được:

```text plugin binary = plugin data
```

Mà:

```text binary
≠
resources
≠
config
≠
persistent data
≠
cache
≠
world data
≠
generated content
```

---

# 607. NON-NEGOTIABLE PACK RULE

Không được:

```text Java RP == Bedrock RP
```

Không được:

```text Java DP == Bedrock BP
```

Không được:

```text mcaddon == one flat pack
```

Phải giữ riêng semantic của từng hệ thống. Microsoft hiện tách rõ Resource Pack/Behavior Pack và manifest/dependency structures trong Bedrock, trong khi Java có Resource Pack/Data Pack riêng.

---

# 608. NON-NEGOTIABLE CONVERSION RULE

Không được:

```text one unsupported class
→ whole mod failed
```

Mà:

```text unsupported feature
→ isolate
→ report
→ continue remaining conversion
```

---

# 609. NON-NEGOTIABLE SECURITY RULE

Không được:

```text arbitrary mod code
→ unrestricted OS filesystem
```

---

# 610. NON-NEGOTIABLE ARCHITECTURE RULE

Core never directly depends on:

```text Bukkit
Paper
Folia
Fabric
Forge
NeoForge
Quilt
Geyser
```

Only adapters depend on them.

---

# 611. TARGET END STATE

Admin có thể bỏ:

```text mods/Foo.jar
plugins/Bar.jar

datapacks/Data.zip
resourcepacks/Resources.zip
texturepacks/Textures.zip
addons/Addon.zip

bedrock/addons/A.mcaddon
bedrock/resourcepacks/B.mcpack
bedrock/behaviorpacks/C.mcpack
bedrock/texturepacks/D.zip
bedrock/scripts/...
```

vào đúng folder.

USML:

```text detect
analyze
resolve
convert
normalize
compile
load
publish
track
persist
migrate
report
```

---

# 612. PLAYER END STATE

Java Vanilla:

```text connect
↓
receive resource pack
↓
server-authoritative gameplay
↓
render generated presentation
```

Bedrock Mobile:

```text connect through Geyser
↓
receive Bedrock resource content
↓
server-authoritative gameplay
↓
render generated Bedrock presentation
```

---

# 613. PROJECT DEFINITION OF DONE

Project chỉ được coi là đạt mục tiêu khi:

```text 1. Multiple source loaders supported
2. Multiple target server platforms supported
3. Dynamic version registry works
4. Universal IR works
5. Mod/plugin conversion works
6. Partial conversion works
7. Resource/data pack loading works
8. Bedrock content pipeline works
9. Java clientless mode works where capability permits
10. Virtual filesystem works
11. Plugin data is separated from plugins/
12. Mod data is separated from mods/
13. Foreign root directories are tracked
14. Relative raw IO is virtualized
15. Async IO preserves module ownership
16. Config classes are differentiated
17. World-scoped data is differentiated
18. Shared data is differentiated
19. Generated data is differentiated
20. Cache/temp/logs are differentiated
21. Dependency system works
22. Mixin/transform analysis works
23. Unsupported client features are isolated
24. Reports are deterministic
25. Migration is reversible
26. Version adapters are testable
27. Java and Bedrock pack stacks are independent
28. No silent filesystem/data loss occurs
```

---

# 614. FIRST DEVELOPMENT ORDER

Để project không chết vì scope quá lớn, implementation phải đi theo:

```text PHASE 1
Core + Version Registry

PHASE 2
Universal Module Metadata

PHASE 3
Virtual Filesystem

PHASE 4
Paper/Bukkit Plugin Adapter

PHASE 5
Plugin Data/Config Compatibility

PHASE 6
Fabric Adapter

PHASE 7
Forge Adapter

PHASE 8
NeoForge Adapter

PHASE 9
Quilt Adapter

PHASE 10
Folia Scheduler/Threading

PHASE 11
Canvas/Purpur

PHASE 12
UIR Core

PHASE 13
Block/Item/Entity abstraction

PHASE 14
Resource Pack Compiler

PHASE 15
Datapack Compiler

PHASE 16
Display/Model/Animation backend

PHASE 17
Physics

PHASE 18
Dynamic Scene/Contraptions

PHASE 19
Bytecode conversion

PHASE 20
Mixin/Transformer semantic conversion

PHASE 21
Geyser/Bedrock

PHASE 22
Advanced migration

PHASE 23
Full compatibility test matrix
```

---

# 615. MVP MINIMUM

MVP phải chứng minh 4 thứ:

```text A. Plugin filesystem virtualization
B. Mod filesystem virtualization
C. Custom entity/block rendering
D. Java clientless resource delivery
```

Sau đó mới mở rộng converter coverage.

---

# 616. FIRST DEMO

Một plugin:

```text getDataFolder()
saveDefaultConfig()
SQLite
relative path
foreign root
async write
```

phải chạy được với:

```text plugins/Test.jar

data/plugins/Test/
```

không cần sửa source plugin.

---

# 617. SECOND DEMO

Một Fabric mod:

```text FabricLoader.getConfigDir()
new File("data")
new File("legacy-folder")
```

phải chạy được với:

```text config/
data/mods/Test/
```

---

# 618. THIRD DEMO

Một custom entity:

```text server logic
+
model
+
animation
```

chạy trên:

```text Java vanilla
```

---

# 619. FOURTH DEMO

Một moving structure:

```text server physics
+
Display/scene backend
+
interpolation
```

---

# 620. FIFTH DEMO

Cùng logical object đó trên:

```text Bedrock mobile through Geyser
```

với backend resource/behavior tương ứng.

---

# 621. FINAL DEVELOPMENT INSTRUCTION TO THE CODING AI

Trước khi viết implementation của mỗi platform:

```text SEARCH OFFICIAL DOCS
SEARCH OFFICIAL SOURCE
READ VERSIONED DOCS
READ Javadocs
READ MIGRATION PRIMERS
READ CHANGELOGS
READ TESTS
READ RELEVANT SOURCE
BUILD API INVENTORY
BUILD CAPABILITY MATRIX
BUILD FILESYSTEM MAPPING
BUILD THREADING MAPPING
BUILD LIFECYCLE MAPPING
BUILD PACK MAPPING
ONLY THEN CODE
```

---

# 622. CODING AI MUST NOT

```text guess API
copy old API blindly
assume version parity
assume Forge == NeoForge
assume Paper == Folia
assume Quilt == Fabric
assume Java RP == Bedrock RP
assume plugin folder == data folder
ignore raw File/Path access
ignore arbitrary root folders
ignore WatchService
ignore async IO
ignore database files
ignore generated content
ignore migration
ignore nested JARs
ignore classloader behavior
ignore dependency phases
```

---

# 623. FINAL DESIGN PHILOSOPHY

USML không chỉ “convert mods”.

USML phải làm:

```text MOD LOADER COMPATIBILITY
+
SERVER PLATFORM COMPATIBILITY
+
SEMANTIC CONVERSION
+
VIRTUAL FILESYSTEM
+
DATA MANAGEMENT
+
RESOURCE PIPELINE
+
PACK COMPILER
+
RENDER BACKEND
+
PHYSICS BACKEND
+
NETWORK BACKEND
+
BEDROCK BACKEND
+
VERSION REGISTRY
+
MIGRATION SYSTEM
+
SECURITY LAYER
+
DIAGNOSTIC SYSTEM
```

---

# 624. THE MOST IMPORTANT ARCHITECTURAL STATEMENT

```text
A module has no single filesystem.

A module has:
    binary space
    resource space
    config space
    persistent data space
    world data space
    cache space
    temp space
    generated space
    shared space
    library space
    log space
    external access space
```

Source loader APIs, source code, target server APIs and USML must all map onto these semantic spaces.

---

# 625. FINAL MENTAL MODEL

```text
                  SOURCE MODULE
                        │
             ┌──────────┴──────────┐
             │                     │
          CODE/LOGIC            CONTENT
             │                     │
             ↓                     ↓
      SOURCE ANALYZER       PACK ANALYZER
             │                     │
             └──────────┬──────────┘
                        ↓
                 UNIVERSAL IR
                        │
       ┌────────────────┼─────────────────┐
       │                │                 │
       ↓                ↓                 ↓
 SERVER RUNTIME    VIRTUAL FS       CONTENT GRAPH
       │                │                 │
       └────────────────┼─────────────────┘
                        ↓
                TARGET PLATFORM
                        │
          ┌─────────────┴─────────────┐
          ↓                           ↓
      JAVA CLIENT                 BEDROCK
          │                           │
 Resource/Data Pack            RP/BP/Add-On
 Display/Packet               Geyser Mapping
          │                           │
          └─────────────┬─────────────┘
                        ↓
                     PLAYER
```

**Đây mới là architecture chính thức cần code.**
