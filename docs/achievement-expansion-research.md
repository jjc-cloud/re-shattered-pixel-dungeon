# 成就扩展调研与实现方案

日期：2026-10-02

分支：`codex/achievement-expansion`

状态：用户已同意方案，成就实现完成；共享核心编译及 151 项成就回归检查通过。

## 已确认的现有机制

- `Badges.java` 的 `Badge` 定义成就；`local` 记录本局成就，`global` 记录账号成就。`displayBadge` 负责通知与全局解锁，但为私有方法；外部事件通过对应的 `validate...` 方法进入。
- `tierBadgeReplacements` 已实现“已解锁仅展示最高等级、未解锁仅展示下一等级”，`addReplacedBadges` 为高阶成就补齐低阶。不需要新写成就分级系统。
- `Amulet.showAmuletScene` 调用 `Badges.validateVictory`，首次取得护符和后来再次打开护符界面都会进入。`Dungeon.win` 则是玩家最终选择结束或返回地表时提交胜利记录。两者不是同一个时点。
- `Statistics.duration` 只记录 `Actor.fixTime` 已归并的时间；完整当前回合数是 `Statistics.duration + Actor.now()`，不能直接拿排行榜显示的整数或仅拿 `duration` 判定速通。
- `Statistics.foodEaten` 包含普通食物及丰饶之角的进食；恢复饱食度的药剂不增加该计数。`Statistics.itemsCrafted` 包含炼金场景合成与饰物催化剂的初次制作。
- `Hero.talentPointsSpent(tier)` 可以统计每一阶实际投入的天赋点，所有阶合计即可判断 37 点门槛；不是计算已点亮的天赋种数或剩余可用点数。
- `Hero.STR` 是基础力量，`Hero.STR()` 包含力量之戒、激素涌动及相关天赋的加成；既有力量成就使用前者。
- `Statistics.qualifiedForRandomVictoryBadge` 由随机开局初始化。手选天赋升级、专精、护甲技能及选择鼠王技能会使它失效。现有随机获胜允许不投入天赋点。
- `Challenges.MASKS` 当前包含 10 项挑战，`activeChallenges()` 可计算开启数，不能按旧版的 9 挑战实现。
- 石化雕像为 `PetrifiedStatue`，不是普通 `Statue` / `ArmoredStatue`。其独立 `die` 不进入普通怪物死亡统计，也包含由场景物件生成的可击碎雕像。
- `YogFist.BrightFist.damage` 和 `DarkFist.damage` 在受伤后仍存活且从半血以上降至半血以下时触发闪现；直接致死则不会闪现。现有代码没有保存是否曾经闪现的字段。
- 近战伤害及哨卫攻击会将施害单位实例传入伤害结算。可通过 `Char.alignment == ALLY` 判断友方，必须排除英雄自身，不能将既有“友方法术致死”等同于“友方单位致死”。
- `Hero.reallyDie` 位于复活处理之后，适合记录真正结束本局的死亡；祝福十字架救回或使用普通十字架复活不应提前获得“倒戈”。

MCP 已用于查询成就、统计、英雄与拳头机制。其索引 HEAD 落后于工作区；上述结论均已根据当前源码复核。

## 已实施的判定口径

以下方案已获得用户同意并实施。

| 成就 | 建议判定 | 接入位置 |
| --- | --- | --- |
| 混沌终局！ | 满足现有随机获胜资格，已获得非 NONE 专精及护甲技能，同时开启全部挑战 | `Badges.validateVictory` |
| 不堪一击 | 击碎一尊 `PetrifiedStatue`，包括生物及物件来源的石化雕像 | `PetrifiedStatue.die` |
| 夺命一击 | 辉耀之拳或暗影之拳死亡，且该只拳头从未触发自身闪现技能 | `YogFist.die` |
| 倒戈 | 真正死亡时直接伤害来源是英雄以外的 ALLY 单位 | `Hero.reallyDie` |
| 速通玩家 | 完整回合数 ≤ 8000 | `Badges.validateVictory` |
| 借过一下 | 完整回合数 ≤ 6000 | 同上 |
| ？？？ | 完整回合数 ≤ 4000；保留用户指定名称 | 同上 |
| 绝食 | 本局 `foodEaten == 0`，包括不能使用丰饶之角进食 | 同上 |
| 孱弱之躯 | 基础力量 `STR == 10`，沿用现有力量成就口径 | 同上 |
| 炼金麻瓜 | 本局 `itemsCrafted == 0` | 同上 |
| 一无所长 | 本局从未投入天赋点，且结算时实际投入点数为零 | 同上 |
| 天启之人 | 实际投入的天赋点总数 ≥ 37，达到后立即获得，无需通关 | `Hero.upgradeTalent` → `Badges.validateLevelReached` |

“通关”沿用现有胜利成就的取得护符口径，不要求上行返回地表。绝食按进食统计判断，食物作为炼金原料等非进食用途不属于进食；炼金麻瓜则禁止任何计入现有制作统计的操作。

友方单位造成后续燃烧、毒伤等间接伤害，目前没有统一、完整的施害者追踪。本方案使用直接致死来源，不新建跨伤害类型的归因系统。

## 成就分级与重复结算

1. 将 `{VICTORY_RANDOM, VICTORY_CHAOS}` 放入 `tierBadgeReplacements`，混沌终局成为随机获胜的第二级。
2. 将 `{SPEEDRUN_1, SPEEDRUN_2, SPEEDRUN_3}` 放入同一分级链，分别对应 8000、6000、4000 回合。
3. 一次达成多级时补齐低级解锁，只展示本次最高级通知；在本局和全局成就列表均使用已有分级过滤。
4. 获得护符后能再次打开结算界面。在加入本局 `VICTORY` 前检查它是否已经存在，只在第一次进入时判定新增胜利条件，避免后来改变角色状态补发胜利成就，或重复弹出新增成就。天启之人已改为投入至少 37 点天赋后立即获得，不参与胜利结算；在本局成就集合中记录，防止继续投入时重复发放。
5. 继续经过已有种子限制、解锁及存档流程，不让固定种子局绕过成就限制。

## 已获同意并实施的新增方法

扩展现有 `validateVictory`、`die`、`damage`、`upgradeTalent`、`reset`、`storeInBundle`、`restoreFromBundle` 等方法。三个新事件缺少适合复用的外部成就入口，在 `Badges` 新增：

- `validatePetrifiedStatueShattered()`：登记本局“击碎雕像”，调用已有 `displayBadge`，同一局只触发一次。
- `validateFistSlain(YogFist fist)`：检查拳头类型及其闪现记录，登记并展示“夺命一击”。
- `validateDeathFromAlly(Object cause)`：检查致死来源为其他友方单位，登记并展示“倒戈”。

这三个方法与项目既有 `validate...` 成就入口保持一致，复用已有解锁、提示、种子限制及持久化机制。

另新增少量字段，并扩展已有保存/读取方法：

- 每只拳头独立的 `hasTeleported`：只在自身半血闪现技能触发时置为 true；读档后保留，其他来源的传送不使成就失效。
- `Statistics` 的本局是否曾激活天赋标记：在英雄真实加点成功时记录，避免通过天赋重置重新取得“一无所长”资格。

不为这几个新状态额外设计旧存档迁移。成就图标最初复用现有徽章纹理，已于 2026-10-04 改为图集第 18 行起的独立编号 136–147，名称、行列和坐标对照见 `docs/badge-display-and-art-guide.md`；本次只维护简体中文文案，位置为 `core/src/main/assets/messages/misc/misc_zh.properties`。

## 实施后验证方案

- 编译共享 core 模块，确认桌面、Android、iOS 共用代码没有引入平台专用 API。
- 按项目现有 Java 回归程序方式增加成就回归及必要测试方法，使用内存状态，避免修改玩家真实成就文件。
- 验证 8000 / 6000 / 4000 边界、刚超过边界、当前楼层时间被计入、三级折叠及低阶补齐。
- 验证十挑战且随机资格有效、少一个挑战、手选失效、专精未选等情形。
- 验证 0 / 1 次食物和制作，10 / 11 力量，0 / 36 / 37 天赋点及曾激活后清空天赋。
- 验证首次护符判定、之后更改角色状态及重新打开结算不会补发，新增统计字段可保存与恢复。
- 验证石化雕像击碎、两种拳头未闪现与已闪现的死亡、闪现状态存读档、其他拳头类型不误触发。
- 验证友方 / 敌方 / 中立 / 自身 / 非单位伤害来源，复活路径不误发成就。
- 最后运行 diff 检查，并按项目约定将本次改动与用户原有未提交改动一起 git 暂存，不提交、不上传、不合并。

## 实施结果

- 新增 12 个徽章定义；速通三个等级属于同一条成就链，混沌终局接在随机获胜之后。
- 中文名称及描述集中在 `core/src/main/assets/messages/misc/misc_zh.properties` 第 215–238 行；新增徽章当前使用独立编号 136–147，均为非隐藏成就，等级链在前置解锁后展示下一枚未解锁徽章。
- 按用户后续要求再次核对原结算数据：`WndRanking.StatsTab` 展示力量、回合数、深度/上行层数、击杀数、金币、进食次数与炼金次数；`Rankings.saveGameData` 保存英雄、统计及本局成就。此次直接使用既有进食、炼金、时间及英雄天赋数据，未重复计数。
- `Statistics` 仅新增 `talentsActivated`，用于保留“曾经激活”这一历史事实；`YogFist` 仅新增每只拳头的 `hasTeleported`，记录自身技能触发。首次胜利判定复用本局 `VICTORY` 徽章。
- `:core:compileJava` 通过。
- `:core:achievementRegression` 通过，共 151 项检查，包含两类拳头真实伤害/闪现/死亡入口、闪现后读档、外部传送、石化雕像击碎、普通十字架复活、统计边界、分级、重复结算、固定种子限制及全部新增中文文案。
- 测试为无图形回归；尚未进行人工界面验收或完整实机通关。祝福十字架路径通过源码检查确认在最终死亡判定前返回，自动回归实际执行的是普通十字架路径。
- 运行命令：`./gradlew.bat :core:achievementRegression --offline --configure-on-demand --console=plain`。构建使用项目缓存中的 JDK 21 与 Gradle 9.5；按需配置只加载 core 依赖，避开 Android 发布签名配置。

## 日志闪退修复与跨端检查

用户提供的桌面报错堆栈为 `JournalScene.create → TerrainFeaturesTilemap.<init> → DungeonTilemap.<init>`。主菜单尚未载入楼层，`Dungeon.level` 为 null；塌方墙染色初始化直接访问其 `caveCollapse` 字段，导致空指针。异常发生在日志成就页创建之前。

在 `DungeonTilemap` 原有构造方法中增加 `Dungeon.level != null` 条件。无楼层的日志图标正常使用原贴图；已载入楼层的塌方墙继续走原染色流程。生产代码未新增方法。

已核对三端均通过 `ShatteredPixelDungeon` 使用共享界面：主菜单入口为 `TitleScene → JournalScene`，游戏内入口为 `MenuPane → WndJournal`，无需平台分支修复。安卓资产来源和 iOS `robovm.xml` 均包含 core 资源目录，包含新增中文成就文案和既有徽章贴图。

验证结果：

| 范围 | 结果与限制 |
| --- | --- |
| 桌面横屏 960×640 | 隐藏 OpenGL 窗口实际创建并绘制主菜单四页及游戏内日志窗口，通过 |
| 桌面竖屏 480×800 | 同上五条路径，通过 |
| 安卓 | `:android:compileDebugJavaWithJavac` 通过；未进行安卓设备或模拟器实测 |
| iOS | `:ios:compileJava` 通过；当前 Windows 环境未执行 Xcode/RoboVM 原生链接、IPA 打包或实机测试 |
| 塌方回归 | `:core:caveCollapseRegression` 通过，14,105 项检查 |
| 成就回归 | `:core:achievementRegression` 通过，151 项检查 |

新增 `desktop/src/test/java/com/shatteredpixel/shatteredpixeldungeon/desktop/JournalRegression.java`，使用 `build/journal-regression` 隔离档案，不读取或写入玩家真实配置与存档。

桌面回归命令：

```text
./gradlew.bat :desktop:journalRegression --offline --configure-on-demand --console=plain
./gradlew.bat :desktop:journalRegression --offline --configure-on-demand --console=plain -PjournalOrientation=portrait
```

安卓/iOS 编译使用项目声明的依赖；缺失缓存已通过 Gradle 下载。安卓发布签名属性仅传入占位值以满足构建配置读取，未执行签名或发布。
