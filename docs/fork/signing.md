# 个性化版：签名和发布

GitHub Actions Secrets 供工作流签名用，不能从 GitHub 设置页或 API 读取原值。不要把正式私钥、密码或含私钥的 Base64 放入 Git、Issue、聊天、日志或 Actions 产物。

## 首次配置（已有正式密钥请复用，不要重新生成）

1. 在你信任的私有云终端中操作，例如自己控制的 GitHub Codespace；不需要在本机生成或存放密钥。首次生成可以使用以下命令，交互式输入密码（下例为 JKS，密钥密码可选与库密码相同）：

   ```bash
   umask 077
   ks=$(mktemp -d)/cyime-release.jks
   keytool -genkeypair -storetype JKS -keystore "$ks" \
     -alias cyime-personal -keyalg RSA -keysize 3072 -validity 10000 \
     -dname 'CN=GoGo35 CyIME'
   ```

2. 把 JKS 和所需密码独立备份到你掌控的加密保险库，确认备份能打开。只保存公开的证书指纹不能恢复私钥。云终端删除后仍应能恢复；仓库 Secret **不是**备份。
3. 在云终端用有权管理 `GoGo35/CyIME` 仓库 Secrets 的 GitHub CLI 登录身份执行以下命令。如果是自有密钥，把 `$ks` 换成相应 JKS 路径，`cyime-personal` 换成实际别名。两个密码由 CLI 提示输入，不放进命令行。

   ```bash
   base64 -w0 "$ks" | gh secret set SIGNING_KEY --repo GoGo35/CyIME
   gh secret set ALIAS --repo GoGo35/CyIME --body cyime-personal
   gh secret set KEY_STORE_PASSWORD --repo GoGo35/CyIME
   gh secret set KEY_PASSWORD --repo GoGo35/CyIME
   ```

   可以在 [Actions Secrets 设置](https://github.com/GoGo35/CyIME/settings/secrets/actions) 核对四个名字是否齐全。选 *Repository secrets*，不是 Variables。后续发版沿用原密钥。

## 发布 1.15.108

工作流入口：[Build and Test APK](https://github.com/GoGo35/CyIME/actions/workflows/build.yml)。选择分支 `personal-1.15.108`，勾选签名发布选项再运行；也可执行：

```bash
gh workflow run build.yml --repo GoGo35/CyIME --ref personal-1.15.108 -f publish_personal=true
```

推分支只做测试和未签名候选构建，不发布。手动发布必须先通过相同的测试和 APK 检查；然后在独立的临时 runner 上签名、校验，并创建 `personal-v1.15.108` 正式 Release。只发布 APK、SHA-256 校验和及公开的签名证书指纹。
