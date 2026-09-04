# 开局测试配置

配置文件：`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/TestStart.java`。

将 `ENABLED` 改成 `true` 并重新编译，然后创建新游戏。默认值为 `false`，发布前保持关闭。
`START_DEPTH`、`START_LEVEL`、`START_STRENGTH` 分别控制开局楼层、等级和力量。
默认测试配置是 16 楼、25 级、18 力量，装备巨剑和鳞甲，携带天狗面具、15 张升级卷轴、5 张嬗变卷轴、20 张蜕变卷轴。

## 指定怪物和地面陷阱

在 `TestStart.java` 中加入所需类型的导入，例如：

```java
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
```

在 `applyToLevel(Level level)` 的开关检查之后添加：

```java
spawnMobs(level, START_DEPTH, Rat::new, 3);
spawnTraps(level, START_DEPTH, WornDartTrap::new, 2, false);
// 也可以指定其他楼层；最后一个参数 true 表示隐藏陷阱。
spawnTraps(level, 17, WornDartTrap::new, 4, true);
```

两个接口依次接收当前生成的楼层、目标楼层深度、创建实例的工厂和数量；陷阱接口另接收是否隐藏。
可以添加多行配置，工厂每次必须返回一个新实例，也可使用 lambda 配置实例属性。
默认没有额外生成配置，以上示例需手动添加。

接口只作用于主地牢新生成的楼层，包括开局时预生成的 1–15 楼，不作用于支线和读档加载。
生成是在原有怪物和陷阱的基础上追加，优先选择入口附近的普通空地，避开过渡区域、物品、植物、怪物和陷阱；大型怪物还需足够空间。
距离按格子距离计算，不保证与入口之间无墙遮挡。没有合适空地时停止，返回实际生成数量。
不支持隐藏的陷阱始终可见。已有存档中的楼层不会因修改配置而自动补充生成；重新生成的楼层会重新应用配置。
