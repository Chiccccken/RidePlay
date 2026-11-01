# 安全上游同步设计

状态：设计完成，自动任务尚未部署或启用。2026-11-01 核查。

## 仓库现状与默认分支陷阱

- `main` 与上游同 SHA；`custom` 已建立。两者核查时均未保护。
- Fork Actions API：`enabled=true`、`allowed_actions=all`；默认 token 权限 `read`、`can_approve_pull_request_reviews=false`。
- 已注册工作流返回 `total_count=0`，尽管源码存在 `.github/workflows/android.yml` 等文件。不能将此状态当成 Android CI 已执行或定时同步已启用。
- 现有 Android CI 的 push 只匹配 `main`，另有 pull_request、workflow_dispatch；未来需要补上 `custom` push 验证。
- 上游还包含定时 Pages 部署。启用工作流前需避免在个人 Fork 上意外部署官网或运行无关定时任务。

GitHub schedule 只从默认分支运行。因此默认分支保持 `main`、同步工作流只放在 `custom` 的组合不会定时执行。若为 main 添加自定义工作流提交，又不能把它当作上游的纯快进镜像。

## 推荐拓扑

将个人 Fork 默认分支设为 `custom`，把同步控制工作流放在 `custom`，使 `main` 始终保持纯上游历史。该设置变更尚未执行；先完成并评审工作流，再切换默认分支和启用定时。其他可选方案为独立控制仓库或外部调度器，但不在本项目默认引入额外凭据或服务。

```text
官方 upstream/main
       │ 仅允许 fast-forward（不改写历史）
Fork main
       │ 自动建立同步 PR + 对合并结果运行只读 CI
Fork custom
       ↑ 人工评审并 merge commit
```

初期不自动合入 `custom`。上游定期检查采用每日一次，例如 UTC 02:17（上海 10:17），同时提供 workflow_dispatch。无变化不重复创建 PR；失败产生明确的 Actions failure/summary，并保留原分支。GitHub 自身的通知是否到达取决于账号通知设置。

## 工作流实现合同

1. 入口限定仓库 `Chiccccken/DiPlay`、默认分支 `custom`；固定官方上游 URL 和 `main`，不接受任意网络地址或 shell 输入。
2. 按仓库设置 concurrency，`cancel-in-progress: false`，限制同时写入；全局权限默认只读。
3. **同步 job** 只处理 Git 对象和 API，不执行拉取的上游脚本或构建。仅此 job 配置 `contents: write`、`pull-requests: write`。使用经过核实的 action 完整 SHA 固定依赖；checkout 不持久化凭据到可被构建读取的配置。
4. 完整 fetch 官方 main 与 Fork main/custom，记录全部 SHA。Fork main 不是官方 SHA 的祖先时停止。上游改写历史、main 出现本地提交、保护规则或权限拒绝时均不 force/reset。官方 SHA 已被 custom 包含时只跳过 PR，仍检查 main 是否需要快进。
5. 将 Fork main 以非强制 ref 更新/普通快进 push 更新到已读取的官方 SHA。并发更新造成失败则重新读取后退出重试，不覆盖对方提交。
6. 若 custom 尚未包含 main，检查是否已有 head=`main`、base=`custom` 的同步 PR：有则复用，无则创建。PR 记录上游 SHA、custom 基线 SHA、改动摘要与本次检查 URL；不重复刷评论。
7. 在临时 checkout 使用 `git merge-tree` 或 GitHub mergeability 检测 main→custom；冲突只标记 PR 并让预检失败，custom ref 不变。mergeability 的 unknown/null 不视为 clean，做有限次数重试后停止。
8. **校验 job** 与写权限 job 使用不同的临时 runner，权限 `contents: read`，无签名/认证 secret；只 checkout 固定的 PR 合并结果，并记录对应 main/custom SHA。
9. 执行 `python3 scripts/check_public_tree.py` 与 AGENTS.md 的完整 Android 检查，使用 JDK 25、平台 37、Build Tools 36、NDK 28.2.13676358。失败仍上传报告；公开产物仅 source-only 测试结果，不提供真实认证或签名。
10. 处理机器人 token 的触发规则：不要假设 bot 创建 PR 后普通 pull_request CI 已自动成功。显式触发只读 `workflow_dispatch` 校验，或在本轮工作流依赖 job 中完成校验；若采用后者，需要用独立 PR 校验工作流建立可用于保护规则的检查。
11. 人工确认 PR 的最新 SHA、合并结果检查及工作流文件改动后，使用 **merge commit** 合入 custom，保留上游祖先关系；不 squash/rebase 上游同步 PR。新提交或基线改变应使旧检查失效，重新检查最新合并结果。

分支保护建议：custom 要求 PR、最新合并结果 Android CI、禁止 force push/删除，避免无人能审批的自我 review 要求；main 限制日常人工修改，允许指定同步身份普通快进。工作流管理和规则仍可能由管理员绕过，应保持最小权限。

## 激活与回退顺序

1. 在 custom 增加工作流，并补充 custom push/PR 的 Android 检查；先手动运行。将上游 Pages workflow 在个人 custom 分支上禁用/限制为官方仓库后，再启用相关 Actions。
2. 在临时仓库/分支演练：无变化、正常快进、custom 无冲突、custom 冲突、main 分叉、上游历史改写、并发更新、token 无权限、CI 失败；确认所有异常都不写 custom。
3. 配置默认分支 custom、必要 PR 创建权限和分支保护；核对 Actions API 与实际一次运行结果。不要笼统提高所有工作流默认写权限。
4. 完成人工验证后开启 schedule。当前仍为 main 默认分支，故此处是待执行步骤。
5. 停用时 disable 同步 workflow / 去掉 schedule；关闭待处理 PR。已更新的 main 保留上游历史，custom 若需回退使用 revert；不强推。

长期应检查定时任务是否仍启用；公开仓库 60 天无活动时 GitHub 可能停用 schedule，调度也可能延迟。不要以每天产生空提交的方式制造活跃度。

官方依据：[schedule、workflow_dispatch 与 bot 事件规则](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows)、[仓库 Actions 权限](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository)。
