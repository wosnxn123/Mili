# Carpet 插件规则接口

`Bukkit.getServer().getCarpet()` 返回 Mili 的规则接口。`rules()` / `rule(name)` 在任意线程可读，返回已公布的不可变快照；快照不是对实体或世界的写入许可。规则名区分大小写。

## 由受信任的本地插件写入

在 `carpet_config/carpet_global_config.toml` 的 `[carpet.api]` 中将指定插件的实际名称加入 `allowed_rule_writers`，默认值为 `[]`；不接受通配授权。这是插件接口的限制，不是对服务器进程内已安装 Java 代码的安全沙箱。

```java
// 在获准插件自己的类内发起，不要在任何 tick 线程上 join/get 等待。
CarpetApi.get().setRule(this, "flippinCactus", "true", false)
        .thenAccept(result -> getLogger().info(result.status() + ": " + result.rule().value()));
```

- 使用 `fun.bm.mili.api.carpet.CarpetApi`。实际改动在 Folia 全局区域排队处理，执行时再次检查插件实例仍已启用且被允许。不会从客户端数据包执行写入、命令或脚本。
- `persist=false` 仅更新运行值；`true` 先通过已有 TOML 配置管理器保存再更新。`largeBarrel` 等只在重启时生效的规则只能保存，返回 `SAVED_FOR_RESTART`，本次运行值不变。
- 支持带原生配置字段并可直接热更新的布尔规则；雪层融化光照接受 `0–16`，仙人掌音效接受 `0–5`；`thickFungusGrowth` 和 `reusableSmithingTemplate` 只接受它们已支持的模式。不接受任意路径、其他模块配置、命令权限或没有直接写入路径的兼容别名。
- 实时值变化后派发 `CarpetRuleChangeEvent`，已经握手的 Carpet 客户端从各自实体线程收到最新快照。启动时的首次快照、相同值和仅保存的重启设置不产生实时变更事件。插件在事件中可以排入后续请求，但不能阻塞当前区域线程。取消 future 不会撤销已经开始的写入。

该功能沿用 Lophine `7a51f49a` 的非脚本规则变更约定，保留 Mili 已有读取方法。它不安装远程应用，也不是 Scarpet 表达式引擎。
