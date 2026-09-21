# 崩溃追踪补丁

补丁文件：`docs/bug-trace.patch`，对应分支 `bug-trace`（提交“测试闪退因素”“bug测试”“定位bug”）。

读档、换层时抛出的异常原本只显示一句“读取错误”之类的提示，排查时看不到来源。
打上补丁后，异常窗口会直接显示异常类型、Seed、层数、完整调用栈（最多 5 层 cause，每层 30 帧栈），
并同时写入系统剪贴板，可以直接粘贴到别处排查。

## 使用

```bash
git apply docs/bug-trace.patch
```

重新编译后复现问题，异常窗口里的内容就是崩溃来源。排查完撤销：

```bash
git apply -R docs/bug-trace.patch
```

补丁基于 5a60b7bee 的 `InterlevelScene.java` 生成。以后该文件改动较多导致 `git apply` 冲突时，
直接取整份文件即可：

```bash
git checkout bug-trace -- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/InterlevelScene.java
```

也随时可以用 `git diff master bug-trace` 查看这个补丁的全部内容。

## 说明

- `catch (Exception e)` 改为 `catch (Throwable e)`，覆盖加载线程里抛出的所有异常。
- 非 IO、非旧存档的异常不再直接抛 `RuntimeException`，而是把 Seed、层数、调用栈一并显示出来。
- 弹窗前调用 `Gdx.app.getClipboard().setContents(...)`，内容进入剪贴板。
- 这是排查用补丁，发布前不要带上。
