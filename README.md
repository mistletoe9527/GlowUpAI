# GlowUpAI

AI 个人形象顾问 App。当前主前端是 `ios/GlowUpAI` 下的 SwiftUI 原生 iOS App，基于 `idea.md` 的产品需求实现。

## iOS App

用 Xcode 打开：

```text
ios/GlowUpAI/GlowUpAI.xcodeproj
```

然后选择 iPhone Simulator 运行 `GlowUpAI` target。

本机需要安装完整 Xcode；只有 Command Line Tools 时无法编译 iOS Simulator target。

默认后端 API 地址：

```text
http://127.0.0.1:8080/api
```

Simulator 使用默认地址即可。如果在真机运行，需要把 API 地址改成电脑局域网 IP，例如：

```text
http://192.168.1.10:8080/api
```

该地址在 `ios/GlowUpAI/GlowUpAI/Info.plist` 的 `GLOWUP_API_BASE_URL` 中配置。
iOS 已配置 `NSLocalNetworkUsageDescription`，用于真机访问本地开发后端。

### Google Login

Google 登录使用 `ASWebAuthenticationSession + PKCE`，不依赖 Google Sign-In SDK。启用前需要在 Google Cloud Console 创建 iOS OAuth Client，然后在 Xcode target build settings 中配置：

```text
GLOWUP_GOOGLE_IOS_CLIENT_ID=你的 iOS OAuth Client ID
GLOWUP_GOOGLE_REDIRECT_SCHEME=你的 reversed client ID，例如 com.googleusercontent.apps.xxxxxx
```

App 使用的 redirect URI 为：

```text
$(GLOWUP_GOOGLE_REDIRECT_SCHEME):/oauth2redirect/google
```

## 后端

后端：

```bash
cd backend
mvn spring-boot:run
```

默认 API 地址：

```text
http://127.0.0.1:8080/api
```

默认使用 H2 文件数据库，数据目录为 `backend/data/`。使用 MySQL 时启用 `mysql` profile 并提供 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`。

iOS App 会优先调用本地后端；后端未启动时会回退到 App 内置演示数据，便于预览完整流程。

## Auth Boundary

本地开发默认不强制鉴权，便于 Simulator 和 mock 流程启动。共享测试环境可以先启用 MVP 身份头边界：

```bash
GLOWUP_AUTH_REQUIRED=true mvn spring-boot:run
```

iOS App 会在用户维度请求中携带 `X-GlowUp-User-Id`。启用后，后端会拒绝缺失身份头、身份头与请求 `userId` 不一致、以及非本人删除照片等越权请求。该身份头只适合本地开发或受控测试环境。

生产环境应启用 Firebase ID token 校验：

```bash
GLOWUP_AUTH_REQUIRED=true \
GLOWUP_FIREBASE_AUTH_ENABLED=true \
FIREBASE_PROJECT_ID=your-firebase-project-id \
mvn spring-boot:run
```

启用 Firebase 模式后，后端会用 Google SecureToken 公钥校验 `Authorization: Bearer <Firebase ID token>`，并使用 Firebase `uid` 作为可信用户 ID，不再接受 `X-GlowUp-User-Id` 替代 ID token。iOS `APIClient` 已预留 ID token provider 插口；接入 Firebase iOS SDK 后，把当前用户的 ID token 传给 `APIClient` 即可。

## AI Provider

默认后端使用本地 mock 分析和规则聊天，方便无密钥启动。要接入 OpenAI Vision/Text 生成风格报告和 AI Stylist 聊天建议：

```bash
cd backend
GLOWUP_AI_PROVIDER=openai \
OPENAI_API_KEY=你的_key \
mvn spring-boot:run
```

可选配置：

```text
OPENAI_MODEL=gpt-4.1-mini
OPENAI_BASE_URL=https://api.openai.com/v1/responses
OPENAI_TIMEOUT_SECONDS=45
```

OpenAI 调用失败、未配置密钥或未配置可用照片时，后端会自动回退到 mock 报告或本地聊天规则，保证 App MVP 流程不中断。

## Photo Privacy

后端照片会先用 AES-GCM 加密，再写入对象存储；数据库只保存照片元数据、存储模式和加密对象路径。默认使用本地加密文件存储 `backend/uploads/`。开发环境未配置密钥时会使用本地开发密钥；生产环境必须设置独立密钥：

```bash
export GLOWUP_PHOTO_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

如需把加密照片对象存到 AWS S3：

```bash
cd backend
GLOWUP_STORAGE_PROVIDER=s3 \
GLOWUP_STORAGE_S3_BUCKET=your-bucket \
AWS_REGION=us-east-1 \
GLOWUP_STORAGE_S3_PREFIX=photos \
mvn spring-boot:run
```

S3 模式使用 AWS SDK 默认凭证链读取凭证，例如环境变量、AWS profile 或部署环境 IAM role。S3 中保存的仍是加密后的字节，不是明文照片。

删除风格评估照片会删除对应的本地加密文件或 S3 加密对象，并把数据库照片记录标记为 deleted，不会删除衣橱单品照片。Profile 页还提供账户数据删除入口，会清理用户资料、全部照片对象、照片元数据、风格报告、衣橱、订阅记录和埋点记录；App Store 订阅取消仍需由 Apple 管理。

## Affiliate Feed

购物推荐默认使用后端内置 mock 商品。配置 `AFFILIATE_FEED_PATH` 后，会优先从 CSV/TSV affiliate feed 读取商品，按 `occasion` 匹配并返回前三个；没有匹配结果时自动回退 mock。

```bash
export AFFILIATE_FEED_PATH=backend/affiliate-feed.sample.csv
```

支持列：

```text
occasion,brand,name,tag,price,reason,buyUrl,image
```

`occasion` 可填写 `Daily`、`Work`、`Date` 等场景，也可填写 `All` 或 `*` 作为通用商品。

## App Store Subscription

iOS Paywall 已接入 StoreKit 2。需要在 App Store Connect 或 Xcode StoreKit Configuration 中创建以下自动续订订阅商品：

```text
com.glowupai.plus.weekly
com.glowupai.plus.monthly
com.glowupai.plus.yearly
```

App 会通过 StoreKit 加载商品、发起购买、监听交易更新、恢复购买，并在购买成功后调用后端 `/api/subscriptions/start` 记录订阅。后端同时提供 `/api/subscriptions/status`，App 进入主界面后会同步用户 Plus 状态。当前后端 MVP 会按套餐写入到期时间：Weekly 7 天、Monthly 30 天、Yearly 365 天；到期后状态查询会返回非 active。

当前 MVP 已把 AI Stylist Chat、AI Closet、Shopping Recommendations 接入 Plus 权益控制：未订阅用户在 App 内会看到统一的 GlowUp Plus 解锁卡，后端也会拒绝未订阅用户直接调用对应 API。若商品未配置，Paywall 会展示不可用状态并禁用购买按钮。

## 已覆盖模块

- SwiftUI iOS App：Welcome、原生 Apple 登录、Email MVP 登录、可配置 Google OAuth 登录
- Onboarding：gender、birthday、height、location、style goal，完成后进入照片上传页
- Home Dashboard：今日穿搭、Style Score、Improve Your Style 提升建议、快捷入口、购物推荐
- AI Style Assessment：PhotosPicker 照片选择、Face photo / Full body photo 后端必填校验、10MB 限制、HEIC/HEIF 客户端转 JPEG 上传、AI Loading 阶段文案、后端加密上传、OpenAI provider 可选接入、mock fallback 报告生成
- Style Score + Style Report：风格类型、脸型/发型、妆容建议、身形建议、色板、优势/改进、系统分享
- AI Outfit Generator：Daily / Work / Date / Party / Travel / Gym / Wedding / Interview
- AI Stylist Chat：首页独立入口和完整聊天页，基于当前 Style Profile 的正向穿搭建议，支持 OpenAI Text/Image 输入、上传当前穿搭照片后直接询问是否适合穿、本地规则 fallback，并接入前后端 Plus 权益校验
- Shopping Recommendation：支持 CSV/TSV affiliate feed、mock fallback、价格/推荐理由/购买入口和点击埋点的商品卡，并接入前后端 Plus 权益校验
- Virtual Wardrobe：衣服照片上传、单品品类/颜色/季节/风格识别、衣橱列表分组展示、基于衣橱/场景/天气生成今日穿搭，并接入前后端 Plus 权益校验
- Subscription Paywall：Weekly / Monthly / Yearly 套餐
- StoreKit 2 订阅：商品加载、购买、交易监听、恢复购买、后端订阅记录、订阅状态查询、前端 Plus 权益同步
- Privacy/Data：风格评估照片删除入口、账户数据删除入口
- Java Backend MVP：用户资料、真实照片上传/删除、账户数据删除、可配置本地/S3 加密对象存储、可配置身份头边界、衣橱单品识别入库、衣橱穿搭生成、风格分析、穿搭生成、购物推荐、AI Chat、订阅开始、分享/购物点击等埋点接收 API

## 当前边界

当前版本仍是 MVP：Apple 登录已接入系统按钮和 entitlement，Email 登录是本地 MVP 流程，Google 登录已用 `ASWebAuthenticationSession + PKCE` 接入可配置 OAuth 流程。使用 Google 登录前，需要在 iOS target build settings 配置 `GLOWUP_GOOGLE_IOS_CLIENT_ID` 和 `GLOWUP_GOOGLE_REDIRECT_SCHEME`，并确保 redirect scheme 与 Google iOS OAuth client 的 reversed client ID 一致。AI Vision 已有可配置 OpenAI provider，衣橱识别当前是可替换的本地规则引擎，订阅已接入 StoreKit 2 购买链路、后端订阅状态记录和前后端 Plus 权益校验，后端到期时间仍是 MVP 估算，生产环境应以 RevenueCat 或 App Store Server 通知的真实有效期为准。购物推荐已支持本地 affiliate feed 但还未直连 Amazon/Shopify 官方 API。照片存储已支持 AES-GCM 加密后的本地/S3 双模式，后端已支持 Firebase ID token 校验但 iOS 还未接入 Firebase SDK 获取真实 ID token；生产版本仍需要继续接入 Firebase Auth、RevenueCat 或完整 App Store Server 通知、MySQL、真实 AWS bucket/IAM/生命周期策略和正式商品 API。
