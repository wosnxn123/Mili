# Minecraft 26.3 迁移记录

状态：**GitHub Actions 已完成补丁应用、三平台原生库校验与 JAR 打包，ALPHA 预发布标签已修正到实际构建提交；测试执行和服务端启动验证尚未完成，不可视为生产可用版本**。

## 分支与上游基线

- 发布分支：`ver/26.3`，基于仓库原来的 26.2 `master`；目标仓库 `wosnxn123/Mili`。本地迁移最初在 `ver/26.3.x` 进行。
- Minecraft / API：`26.3`；Folia 上游发布通道：`ALPHA`。
- Folia：`73865e0e1c11752a03f30320e0521d4611221e08`（`ver/26.3.x`）。
- Paper：`804dd0c3f08b666dc51a0250257915c002c59755`，由上述 Folia 提交指定。
- JDK：25；Gradle：9.8.1；Paperweight：2.0.0-beta.24；Mache：26.3+build.1。

Folia/Paper 补丁随锁定的上游提交更新，不直接用独立补丁覆盖 Mili 补丁。以后跟进上游时，必须更新 `foliaRef`、重建构建脚本补丁、重放所有下游补丁并编译验证，再确认兼容。此处记录的是本次同步，不是定时自动更新任务。

## 已修改内容

- 将无法解析的 Hyacinthusweight 插件替换为 Folia 使用的官方 Paperweight。
- 更新上游依赖，包括拆分后的 Leafpile 1.3.2 模块、JLine、Velocity native 和 Brigadier。
- 接入上游 Checkstyle 模块，使用 `patchRepo` 导入已生成的独立仓库，避免重复复制 `.git`。
- 重建构建脚本补丁，输出改为真正的 Mili 构建脚本，而不再仅生成未使用的 `.base` 文件；Paperweight 实际应用后的三个脚本与预期 SHA-256 全部一致。
- 26.3 删除旧珊瑚生成类，将可施肥珊瑚的 26.2 三种生长算法迁至自有 `CoralGrowth`，保留颜色、概率、失败恢复和海绵逻辑；已编译通过，尚待运行验证。
- 适配新的 Moonrise 区域文件恢复接口；独立核对第 0018 个补丁保留了 Anvil 的恢复实现、计数与缓存保护，并将格式扩展识别迁入 `RegionFileRecovery`。Linear 不再声明其原本未支持的旧 oversized sidecar 接口；实际存档读写和恢复仍待验证。
- 第 0027 个噪声补丁的主要旧优化已由 26.3 上游取代，保留的 floor 变换在 4,000,057 个边界及随机输入上数值核对通过；保持上游 float 计算与 seed 消耗，没有做性能或完整地形对比测试。
- 普通源码已适配 Jade 的 Holder 战利品模型与假玩家的 `syncVelocity` / final 移动模拟接口；服务端 Java 编译已通过。Jade 支持解析后的 predicate 引用，未绑定 tag 条目保守忽略，继续拒绝多条件合取。假玩家沿用上游服务端模拟类型，不额外启用双端预测。
- 第 0077 个补丁的旧分配优化已由新版编译架构取代，剩余数组循环不宣称新的性能收益；第 0078 个 biome 算法与第 0091 个安全种子 V2 的调用映射已独立核对，尚无实际世界生成测试。V2 禁用时 aquifer/ore 仍使用原补丁的独立同 seed RNG，不在迁移中改变已有世界的继承行为。
- 全局实体计数器的 spawn cost 已改为当前环境属性，保持与 26.3 上游一致；跨区 despawn 的已提交 hunk 保留所有持久实体保护，仅在无 local player 且非持久实体时回退。属性读取位于当前区域同步计数阶段，使用不加载区块的 biome 查询路径；尚待运行验证。
- 为兼容新增的 `PrepareHopperTransferEvent`，在监听器存在且 `disableMoveEvent` 关闭时，全体漏斗停用 Lithium 快路径与睡眠，避免动态槽策略改变后漏斗无法重试。三个判断共用同一谓词；这是有意且可回退的兼容性/性能取舍，尚未游戏内测试。
- 迁移并重建全部 12 个 API 功能补丁，保留 KioCG 区块热度接口及 26.3 新增的 `postEffects()`。
- 保留原自定义启动器的暂存更新与可选 Mixin 引导，避免直接切换标准 Paperclip 导致功能丢失。
- CI 改用已发布的官方构建插件与校验后下载的启动器，不再现场构建失效的备用插件。
- 迁移期间曾将 `release` 设为 `0`。按用户的 Actions 发布要求恢复为 `1`：成功构建后发布 ALPHA 预发布，`make_latest=false`，不将其标为最新稳定版；Qodana 触发配置保留。这些构建、CI 与发布配置调整都可回退。

## 启动器兼容性

打包前下载锁定的启动器：

```sh
python scripts/download_launcher.py
```

发布资产名称为 Hyacinthusclip 3.0.18，但实际入口是 `moe.luminolmc.riceear.Main`。其 SHA-256 锁定于 `gradle.properties`，打包任务会再次校验；不会因同名发布资产被替换而静默使用不同内容。

Bundler 与 Paperclip 打包任务写入一致的 UTF-8 `META-INF/riceear-version`，不带换行。最终 Paperclip 还补充 `META-INF/download-context-cn`，内容取自标准下载上下文。这修复发布启动器中两项资源缺失；CN 资源**不代表新增国内镜像**。

从没有版本标记的旧启动器迁移时，应直接替换实际启动的完整 JAR。仅更新 `core.path` 目标，不能通过旧启动器的版本检查。后续更新目标也必须包含兼容的版本标记。原有更新器仍指向 Luminol 发布源，这不是本次新增的 Mili 发布更新器。

26.3 实际启动、暂存更新端到端流程与 Mixin 运行效果尚未验证。

## 验证结果

已通过：

- Folia 26.3 `applyForDownstream`。
- Mili Gradle 配置检查（`help`）。
- 启动器下载与校验，包括 `:mili-server:verifyLauncher`。
- 全部 12 个 API 补丁重放、冲突修复与重建。
- Java 25 下的 `:mili-api:compileJava` 在最终含 Kotlin 配置的脚本下重验通过（4 分 25 秒）。存在 Java 弃用、unchecked、Gradle DSL 弃用与 Kotlin 插件多子项目加载警告，不能视为零警告构建。
- 24 个 Paper-server 功能补丁在独立的 26.3 工作树中迁移完成，普通 `git am` 重放验证通过，并核对了原始补丁的文件集合与新增代码保留；已回写并在正式构建工作树重放通过。
- 启动器下载器的 4 个离线测试：有效下载与缓存、校验失败不落盘、已有文件不覆盖、非法版本拒绝。
- 124 个 Minecraft 补丁在独立 26.3 工作树中完成迁移，两个 fixup 折入后仍为原 124 个补丁。主线 range-diff 仅 0069/0093 改变，其余提交 diff 不变；全部 23 处文件/API/代码差异逐项审阅。
- 主线无三方回退的普通 `git am` 重放 124/124，通过并得到相同源码 tree `9efb9816f415182943cc5044907c5927af1607db`。这是源码重放验证，不是编译或运行通过。
- 正式 `applyAllPatches` 通过（19 分 1 秒）：124 个 Minecraft 补丁、24 个 Paper-server 补丁全部应用，工作树 clean。正式源码 tree 分别匹配独立结果 `9efb9816f415182943cc5044907c5927af1607db` 与 `68ec112da7a45ca85ffcb93b1872c0fa77267c19`；构建脚本 SHA-256 再次 3/3 匹配。首次 0008 失败已由完整迁移解决。

- Paperweight canonical feature-patch rebuild 通过（3 分 3 秒），保留 124 个 Minecraft、24 个 Paper-server 和 12 个 API 补丁；正式源码树保持不变，构建脚本 SHA-256 仍 3/3 匹配。重建后的 23 项差异与独立审阅报告完全一致。
- 官方工具将第 0124 个文件名规范为 `0124-Guard-Connection.flush-against-null-channel.patch`（保留 subject 中的点）。旧连字符文件名已移除，无重复补丁；完整 diff 和原邮件元数据一致。`git diff --check` 仅报告 0119 补丁文件末尾空行，保留工具生成格式，不将其说成零空白告警。

## 服务端编译迭代

- 首次 `:mili-server:compileJava` 失败（3 分 54 秒，32 个错误）；完整日志已保留。错误集中于 swing/drop/bonemeal 新参数、旧 Bukkit ItemStack 桥、配方与命中位置 codec、RegistryLayer、旧 podium helper 和 PushReaction 枚举。
- 普通源码已按实际 26.3 定义适配：假玩家摆手区分 attack/interact 并保留手持物品动画，交互前保存动画；drop 保留原 randomly/death、非 thrownFromHand、事件与地图持有者清理，沿用上游新 drop 动画行为；invulnerability 由已覆盖两种 tick 模式的 `Entity.commonTick` 递减，删除重复递减。珊瑚接口补入 BonemealSource，不改变生长概率或随机顺序。
- Jade 的合法 BlockHitResult 字段顺序已源码核对，使用上游 STREAM_CODEC；非法方向输入解码现在沿用上游 wrap，而非旧越界异常。REI 同时支持 named 和 direct HolderSet，物品转换保持 mirror/copy 区别；Servux 配方仍以 DIRECT_CODEC 输出 inline NBT；回放注册表同步沿用上游 WORLD 层。尚无客户端或实际 round-trip 验证。
- 三个 Minecraft-side 编译修复已保存为生成工作树的追加提交 `e02f23b`（移动后的 podium helper、物品修补事件镜像桥、可移动紫水晶枚举），并规范重建为第 0125 个补丁；原 124 个移植补丁不丢弃。该补丁使用临时构建身份 `Mili Patch Build <noreply@example.invalid>`，没有猜测维护者身份；如需正式发布，可由维护者决定是否重署作者。
- 第二轮 `:mili-server:compileJava` **通过**（4 分 12 秒），125 个补丁已保存；Java 弃用、待删除 API、unchecked 及 Gradle 弃用提示仍存在，不是零警告构建。最终 Minecraft tree 为 `bc3ad4fe2ff2f8445b3d38811610629510ea0847`；其父提交 tree 仍为已审阅的原 124 个补丁结果。
- 最终 125 个 canonical 补丁从干净基线以普通 `git am`（无三方回退）重放通过，得到上述同一 tree。12 API / 24 Paper-server 补丁数量不变，构建脚本 SHA-256 仍为 3/3 匹配。

- 严格的 125 补丁 canonical 核对通过；`:mili-server:compileTestJava` 通过（2 分 57 秒）。测试执行尚未完成，不能将测试源码编译说成测试通过。
- 本地测试与打包 dry-run 需要 Rust 构建任务，启用 configuration cache 时发现 7 项不兼容，包括原 Rust 任务以及新增启动器闭包。Actions 明确使用 `--no-configuration-cache`，不声称这些任务支持 configuration cache；本机未安装 Rust，未执行完整打包。

## GitHub Actions 发布

- 用户授权将升级提交推送到 `wosnxn123/Mili` 的 `ver/26.3`，保留 Qodana 和 Release 触发，`master` 不变。
- Actions 使用 Windows x86_64 GNU、Linux x86_64 GNU、Linux ARM64 GNU 原生目标；ARM64 文件名为 `libmili_optimizer_aarch64.so`，与实际加载器一致。移除会覆盖架构文件的 fallback，要求三个编译目标与 staged 文件逐一校验，任一缺失会阻止发布。原先 musl 两种架构共用文件名的配置不再使用。
- `folia-server` 是生成输出而非构建输入。其 Git gitlink 随 `foliaRef` 更新为可从 PaperMC/Folia 获取的上游提交；绝不提交本地生成的 `a3acaa6` 指针。Actions checkout 不初始化该旧式 submodule，生成前清理该输出，由 Paperweight 按 `foliaRef` 获取上游。新 runner 上的完整补丁应用与 JAR 打包已通过。
- `.gitattributes` 对构建脚本与补丁采用 LF，保留工具生成补丁空白格式；两份 `.base` 是更新后的历史镜像，实际构建补丁输出为 `.kts`。这些额外调整可回退。
- 目标仓库当前没有 Qodana token；工作流不屏蔽此扫描，但能否成功连接 Qodana Cloud 取决于仓库配置，不能凭空提供密钥。

### 首次远端结果（构建提交 `9f255c1`）

- [Mili CI](https://github.com/wosnxn123/Mili/actions/runs/43543530064) 成功（11 分 49 秒）：三个 Rust 目标构建与 staged 文件校验、完整补丁应用、Java 编译与 Paperclip 打包、CI 源码检查全部完成。此 CI 未执行完整单元测试或启动服务端；不把源码/产物 smoke check 说成运行验证。
- [Qodana](https://github.com/wosnxn123/Mili/actions/runs/43543530044) 工作流成功，但分析报告有 303 项问题（High 270 / Moderate 33）及 40 项 Sanity 提示。未使用基线，不能认定全是本次升级新增；没有上传报告 artifact，日志保存了汇总。
- 已发布 [ALPHA 预发布 `26.3-9f255c1`](https://github.com/wosnxn123/Mili/releases/tag/26.3-9f255c1)，资产 `mili-26.3-paperclip.jar`（87,360,493 bytes），SHA-256 `1afb18dd7f9ad933469e24d73248f4c6c4d8ba30b3b97e7db9b859879d5be577`。资产来自提交 `9f255c1aa9f62406835d62799445f5050f52af97` 的构建，GitHub 显示 `prerelease=true`。
- 最终核对发现该 Release 的 tag 原先指向旧 `master`（`853b00bb9a84cafd1023d48129d358fb7edf61a8`），因为两条 release-action 路径均未显式传 `commit`。经用户确认，公开标签与 Release metadata 均已修正为实际构建提交 `9f255c1aa9f62406835d62799445f5050f52af97`，并在发布说明中记录标签移动；资产 ID、大小和 SHA-256 不变，`master` 未修改。
- 两条发布路径均补入 `commit: ${{ github.sha }}`，防止未来标签回退默认分支。YAML、shell 语法和两个发布路径的提交参数检查已通过；后续 Actions 仍需验证这项修复。

仍需完成：
- 核对工作流修复提交的后续 Actions 与预发布标签。
- 测试执行及启动 26.3 服务端，验证假玩家、协议、红石、世界生成、Mixin 与暂存更新；未代用户接受 Minecraft EULA。
