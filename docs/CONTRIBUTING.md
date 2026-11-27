为 Mili 贡献代码
==================

[English](./CONTRIBUTING_EN.md) | **中文**

感谢您愿意为 Mili 做出贡献！本指南给出一个清晰的入门流程、补丁工作流与常见问题解答。

## 快速入门

1. 使用个人账号 Fork 仓库并克隆到本地：

```bash
git clone --branch ver/26.3 https://github.com/wosnxn123/Mili.git
cd Mili

# Windows 需启用长路径
git config --global core.longpaths true
```

2. 应用补丁工作树（首次构建必须执行）：

```bash
python -I scripts/download_launcher.py
./gradlew applyAllPatches --no-configuration-cache --no-build-cache
```

3. 在生成的 `mili-server/src/minecraft/` 目录中修改代码并按补丁流程提交。

## 开发环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 25 | 26.3 构建与运行时 |
| Rust | stable (edition 2024) | 完整打包与测试需要；只编译 Java 可不安装 |
| Git | 2.x | Windows 需启用长路径支持 |

## 补丁模型概览

26.3 使用官方 **Paperweight**。`mili-api/`、`mili-server/` 中的普通自有源码直接维护；`mili-server/src/minecraft/java/` 与 `paper-server/` 是生成的 Git 工作树，不直接提交到根仓库。`folia-server/` 是生成输出，不能把本地生成提交误记为 `foliaRef`。

上游源码变更通过 `.patch` 文件管理：
- Minecraft 功能补丁位于 `mili-server/minecraft-patches/features/`
- 每次修改源码后需要重建补丁文件

## 如何添加新补丁

1. 在 `mili-server/src/minecraft/` 下修改代码
2. 暂存更改：`git add <files>`
3. 提交修改：`git commit -m "描述"`
4. 重建补丁：`./gradlew :mili-server:rebuildAllServerPatches`
5. 提交补丁文件并推送

## 修改已存在补丁

1. 在 `mili-server/src/minecraft/` 下修改相关代码
2. 使用修正提交：`git commit -a --fixup <hash>`
3. 自动变基：`git rebase -i --autosquash base`
4. 运行 `./gradlew :mili-server:rebuildAllServerPatches`
5. 推送并更新 PR

## Rust 模块开发

Rust 源码位于 `mili-rust/src/rust/src/`，共 4 个模块文件：

```bash
cd mili-rust/src/rust

# 编译
cargo build --release

# 代码检查（必须 0 warning）
cargo clippy --release

# 单元测试（28 个）
cargo test --release

# 打包进 JAR
./gradlew :mili-rust:stageRustBinary
```

**edition 2024 注意事项**：
- `#[no_mangle]` 必须写 `#[unsafe(no_mangle)]`
- unsafe fn 内部必须显式 unsafe 块
- JNI 的 `catch_unwind` 需用 `AssertUnwindSafe` 包装 `JNIEnv`

## 常见问题

**我应当使用组织账号 Fork 吗？**
不建议。组织 Fork 的 PR 无法由本项目直接编辑，合并过程会更复杂。

**构建失败怎么办？**
先运行 `./gradlew assemble --stacktrace` 并检查错误输出与依赖问题。确保使用 JDK 25。

**修改 `mili-server/src/minecraft/` 下的文件后被覆盖？**
这些是 `applyAllPatches` 生成的文件。必须通过 `minecraft-patches/features/` 下的补丁文件修改，修改后执行 `rebuildAllServerPatches`。

**如何运行本地测试？**
- Java：`./gradlew :mili-api:test :mili-server:test :mili-server:verifyLauncherPatch --no-configuration-cache`。然后运行 `python -I scripts/verify_test_results.py` 检查确实执行了测试。
- Rust：`cd mili-rust/src/rust && cargo test --release`

## 更多帮助

查阅仓库根目录的 `README.md` / `README_EN.md` 获取构建、依赖和社区链接。如需进一步协助，请在 Issue 中提供构建日志、JDK 版本和复现步骤。
