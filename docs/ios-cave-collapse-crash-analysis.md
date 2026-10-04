# iOS 松动岩壁触发塌方闪退分析

分析日期：2026-10-01。首次分析基于本地 `master`，HEAD 为 `1475ca75d531e04f7a0edd300e898b89dd634386`，当时未修改游戏代码。随后按用户要求实施修复，见末尾“修复结果”。本文的原问题行号均对应首次分析时的源码；未进行 iOS 真机复现。

## 结论

`CaveCollapse.collapse()` 第 190 行使用了：

```java
victims.removeIf(ch -> !ch.isAlive() || !contains(ch.pos) || regionAt[ch.pos] != index);
```

这是一个已确认的 iOS 运行时不兼容调用，与“攻击最后一格松动岩壁、触发塌方时闪退，重新启动回到上一次存档”的现象高度吻合。

项目 `build.gradle:31` 配置 `robovmVersion = '2.3.24'`，`ios/build.gradle:47` 使用该版本的 `robovm-rt`。本地 Gradle 缓存中的实际运行时 JAR 和源码 JAR 均已检查：

- 没有 `java/util/function/`，因此没有 lambda 所需的 `java.util.function.Predicate`。
- `Collection`、`AbstractCollection`、`HashSet`、`LinkedHashSet` 中均没有 `removeIf`。
- `LinkedHashSet` 继承 `HashSet`；`HashSet` 继承 `AbstractSet`，再继承 `AbstractCollection`，无法从集合继承链取得这个方法。

因此，编译时桌面 JDK 提供的 API，在 iOS 的 RoboVM 类库中无法解析。实际抛出的错误类型和完整栈仍需设备崩溃日志确认；应重点查找涉及 `java/util/function/Predicate`、`removeIf` 的类或方法链接错误，例如 `NoClassDefFoundError` 或 `NoSuchMethodError`。当前没有设备日志，不能把其中某一种错误写成已观测结果。

## 近战运行链路

1. `GameScene.java:1912`：选中格子后调用 `Dungeon.hero.handle(cell)`。
2. `Hero.java:2192`：点击规则允许交互时，创建 `HeroAction.InteractTerrain`。
3. `Hero.java:1024`：英雄回合分派到 `actInteractTerrain()`。
4. `Hero.java:1440`：检查楼层、地形、武器、距离及占位，随后在第 1458 行播放攻击动画并注册回调。
5. `MovieClip.java:74`、`CharSprite.java:880`：动画完成后执行回调。正常近战入口的实际破墙结算发生在场景更新的渲染线程。
6. `Hero.java:1472`：回调调用 `TerrainInteractions.interact(dst, Source.CLICK)`。
7. `TerrainInteractions.java:138`：`REPLACE` 响应调用 `Level.set()`，将岩壁改成 `Terrain.EMPTY`。
8. `Level.java:1163`：更新地图、陷阱与地形标记后，第 1175 行通知 `CaveCollapse.terrainChanged()`。
9. `CaveCollapse.java:169`：扣减该区域的支撑墙数量；第 180 行只有在剩余数量为 0 时进入 `collapse()`。
10. `CaveCollapse.java:183`：收集单位后，第 190 行执行上述不兼容调用。

异常会沿同步调用链向外传播。`collapse()` 的 `finally` 只负责把 `collapsing` 重置为 `false`，不会捕获错误。动画回调、地形交互、`Level.set()` 也没有在这条链上捕获该错误。

`IOSLauncher.java:58` 明确注册了 `NSException.registerDefaultJavaUncaughtExceptionHandler()`，其注释说明目的是让未捕获的运行时错误导致应用崩溃。

## 为什么恰好在塌方时触发

区域最初有 1 至 2 格支撑墙。还有墙时，`terrainChanged()` 只更新数量，不进入 `collapse()`；最后一格被打掉时才会首次执行 `removeIf`。

同一塌方方法也会被允许破墙的投掷、法杖和冲击波入口调用，因此风险不限于近战。英雄站在区域外、区域没有怪物，也不能解决 API 链接问题。

第 190 行之后才执行区域单位死亡（第 193 行）、整片填墙（第 197 行）、震屏提示（第 210 行）及英雄死亡（第 214 行）。因此这处问题不需要英雄死亡或十字架复活作为前提。

## 为什么重新启动回到上一次存档

这次交互的破墙和墙数量变化先发生在内存中；攻击回调及 `collapse()` 没有调用 `Dungeon.saveAll()`。发生错误后，后续 `Dungeon.observe()`、`spendAndNext()` 也无法正常完成。

常规存档入口包括换层时的 `Dungeon.java:607` 和暂停时的 `GameScene.java:854`。`Dungeon.saveAll()` 在第 817 至 818 行分别保存游戏和楼层。异常退出不能保证经过正常暂停存档流程，因此重启读取最后一次已落盘状态，与用户描述一致。这条链上没有主动回滚或自动恢复上一存档的代码。

## 项目已有先例与验证边界

提交 `95c130c08645dba42b81dd4a1829c92cbc41dba9` 已修复过同类问题：普通十字架复活时，`Dungeon.clearTemporaryMapKnowledgeOnDeath()` 的 lambda 和 `removeIf` 在 iOS 上无法链接，应用退出后回到死亡前存档。当前 `Dungeon.java:346` 仍保留相关注释，并使用显式 `Iterator` 循环。

本次塌方中的不兼容调用由提交 `4ada9cb721a475ecefe15e1d5b4a760c6d18b342`（“新增塌方地形”）引入。当前生产源码 `core` 和 `SPD-classes` 中检索到的 `removeIf` 调用只有这一处；其他 lambda 若目标为项目已有接口或 `Runnable`，不能仅凭 lambda 语法判定它们也有同类问题。

现有 `CaveCollapseRegression` 在桌面 JVM 上运行，并显式模拟 `ApplicationType.Desktop`；它可验证塌方逻辑，但不能验证 RoboVM 类库是否提供所调用的 API。Java 源码兼容级别为 11，同样不意味着 iOS 运行时拥有完整 Java 11 类库。

已完成的验证是当前源码链路、引入提交、历史同类修复、实际 RoboVM 运行时 JAR 和源码 JAR 的交叉检查。未声称真机复现，也未运行桌面测试来替代 iOS 验证。

## 最小修复路线

复用 `Dungeon.clearTemporaryMapKnowledgeOnDeath()` 的显式迭代器过滤方式，在现有 `collapse()` 内通过 `victims.iterator()`、`hasNext()`、`next()` 和 `Iterator.remove()` 删除不符合条件的单位，保持原筛选条件、去重及英雄最后结算的顺序。

该路线只需增加 `java.util.Iterator` 导入并替换现有方法内的一行，不需要新增方法，也不改变存档格式。修复后应运行现有塌方回归，并在 iOS 实测两格支撑墙的第一击、最后一击、英雄位于区域内外，以及投掷或法杖触发塌方。

## 修复结果

- `collapse()` 已改用显式 `Iterator` 过滤，保留筛选条件和英雄最后结算的顺序。
- 危险区按现有地形标记选择普通地面、水面和陷阱，仍排除实体障碍、悬崖、水井、门格，以及原有的出入口邻格等生成限制。
- 初始支撑墙与后续落石覆盖都会记录下方地形：高草及枯萎高草压成普通草地，水面填成等概率的普通空地或装饰空地，其他地面保持各自类型。随机结果在覆盖时确定，挖开或读档不再重抽。
- 掩埋的陷阱暂时移出楼层的活动陷阱表，挖开后复用 `setTrap()` 放回，保存其可见性、是否解除及老旧设计的待触发状态。再次塌方记录最新状态，不把已经解除的陷阱复原为活动陷阱。
- 下方地形与掩埋陷阱通过现有 `storeInBundle()` / `restoreFromBundle()` 保存和读取；本次增加了相应存档字段，不重建旧存档中已经丢失的原地形。
- 查看危险区保留基础地形或自定义图层的名称与描述，再追加落灰和塌方提示。危险区本身没有单独的新地面类型；原先误导观感的统一“散落灰尘的地面”名称覆盖已移除。
- 所有实现都在已有方法内完成，未新增方法。游戏文案只维护 `levels_zh.properties` 的 `levels.cavecollapse.ground_desc`。

修复后验证：`:core:caveCollapseRegression` 通过 13,921 项检查，`:core:terrainInteractionRegression` 通过 18,118 项检查，`:core:superSecretRoomRegression` 通过 150,375 项检查，Gradle 报告 `BUILD SUCCESSFUL`。公共地形回归在无图形环境的商店贴图初始化中输出了一条 `Gdx.files == null` 的异常日志，但其断言和任务仍通过；这不等于图形界面或 iOS 真机验证。

检查本次编译生成的 `CaveCollapse.class`：已没有 `java/util/function` 和 `removeIf` 引用，改为 `java/util/Iterator`；本地 RoboVM 2.3.24 运行时包含该接口。源码差异空白检查通过。
