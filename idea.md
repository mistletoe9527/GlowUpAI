PRD：AI Glow Up Coach —— AI Personal Stylist App
版本： V1.0 MVP
目标市场：
美国
平台：
iOS App（优先）
产品类型：
AI Consumer Subscription App

1. 产品概述
产品名称（暂定）
GlowUp AI

一句话介绍
Your personal AI stylist that helps you discover your style, improve your appearance, and build confidence.
中文：
你的 AI 私人形象顾问，帮助你找到适合自己的穿搭、风格和个人形象。

2. 产品目标
用户目标
帮助用户：
1. 找到自己的个人风格
2. 改善日常穿搭
3. 提升外貌吸引力
4. 获得购物建议
5. 建立个人形象

商业目标
通过：
* Subscription
* Affiliate Shopping
盈利。
目标：
30天：
10000用户
90天：
50000用户

3. 用户画像
Primary User
女性
年龄：
18-35岁
地区：
美国
职业：
学生
白领
年轻职场女性
特点：
喜欢：
* Instagram
* TikTok
* Pinterest
* Fashion
痛点：
"I have clothes but don't know what suits me."
"I want to look more attractive."
"I want a better style."

4. 产品核心价值
用户上传：
自拍 + 全身照
AI输出：
Personal Style Report
包含：
* Style Type
* Color Palette
* Outfit Recommendation
* Hair Suggestion
* Makeup Suggestion

5. MVP 功能范围
MVP只实现以下功能
Module 1：用户注册
支持：
* Apple Login
* Google Login
* Email
用户信息：


gender
age
height(optional)
location
style_goal


Module 2：AI Style Assessment（核心）
用户输入
上传：
必须：
1. Face photo
2. Full body photo
可选：
1. Current outfit photo

图片上传要求
支持：
jpg/png/heic
限制：
最大10MB

AI分析流程
用户上传图片
↓
Image Vision Model
↓
Feature Extraction
↓
Style Recommendation Engine
↓
Generate Report

AI输出结构
JSON:


{
"user_style":

{
"type":"Modern Minimalist",

"description":
"You prefer clean and elegant styles"
},


"face":

{
"face_shape":"Oval",
"hair_recommendation":
[
"Shoulder length layers",
"Soft bangs"
]
},


"body":

{
"body_ratio":
"Long legs",
"recommendations":
[
"High waist pants",
"Short jackets"
]
},


"color":

{
"best_colors":
[
"Black",
"White",
"Beige"
]
},


"outfit":

[
{
"occasion":"Daily",
"items":
[
"White shirt",
"Straight jeans"
]
}
]

}


Module 3：Style Score
生成：


Your Style Score

82/100


Strength:

✓ Good color matching

✓ Clean silhouette


Improve:

× Add accessories

× More layering


Module 4：AI Outfit Generator
用户选择：
Occasion
选项：


Daily

Work

Date

Party

Travel

Gym

Wedding

Interview


输入：
用户个人Style Profile
输出：
3套穿搭。
例如：
Look 1:
Modern Minimalist
Top:
White blouse
Bottom:
Black trousers
Shoes:
Loafers
Why:
Creates professional elegant appearance

Module 5：AI Closet（第二阶段）
用户上传衣服照片。
AI识别：


category

color

brand

season

style

数据库保存。
用户：
"What should I wear today?"
AI根据：
天气
场景
衣柜
推荐。

Module 6：Shopping Recommendation
用户：
"I need a date outfit"
AI生成：
商品列表。
商品来源：
* Amazon API
* Shopify API
* Affiliate Feed
展示：
Product Card
包含：
图片
价格
购买按钮

Module 7：Virtual Try On（未来）
用户上传：
照片
选择：
衣服
AI生成：
穿着效果。
技术：
Stable Diffusion + VTON
MVP不实现。

6. App 页面设计
页面1：Welcome
内容：
Logo
标题：
"Discover Your Personal Style"
按钮：
Start

页面2：Onboarding
步骤：
Step1:
Gender
Step2:
Age
Step3:
Style Goal
选择：


Look more attractive

Professional

Find my style

Dating confidence

Everyday outfit


页面3：Upload Photos
标题：
"Upload your photos"
显示：
Face photo
Full body photo

页面4：AI Loading
动画：
分析：


Analyzing your face shape...

Understanding your style...

Creating your profile...

时间：
10-30秒

页面5：Style Report
展示：
Style Score
Style Type
Color Palette
Recommendations

页面6：Home Dashboard
结构：
顶部：
Avatar
Your Style:
Modern Minimalist
卡片：
Today's Outfit
Improve Your Style
AI Closet
Shopping

页面7：AI Chat
类似聊天。
用户：
"Can I wear this?"
上传照片。
AI回答。

页面8：Subscription
Paywall。
标题：
"Unlock Your AI Stylist"
功能：
Unlimited Style Analysis
Daily Outfit Suggestions
AI Closet
Shopping Recommendations
价格：
Weekly:
$4.99
Monthly:
$14.99
Yearly:
$79.99

7. AI Prompt设计
System Prompt


You are an expert personal stylist in the United States.

Your job is to analyze user's appearance,
body proportions,
fashion preferences,
and recommend outfits.

Rules:

1. Never judge user's attractiveness.
2. Give positive constructive advice.
3. Focus on confidence and style improvement.
4. Consider American fashion trends.
5. Provide practical recommendations.

Return JSON only.


8. 后端架构
Client
swift
Backend
Java 

9. AI服务
Vision
推荐：
GPT Vision
Gemini Vision
负责：
图片理解。

Text Generation
GPT API
负责：
生成建议。

Image Generation
未来：
FLUX
Stable Diffusion

10. 安全要求
必须：
禁止：
* 外貌羞辱
* 身材评分
* 种族判断
* 年龄歧视
AI语言：
positive
例如：
不要：
"You look bad"
改：
"This style may better highlight your features."

11. 数据隐私
必须：
用户：
删除照片
功能。
照片：
加密存储。
符合：
美国：
CCPA

12. 埋点需求
记录：
注册
event:
signup_complete

上传照片
photo_uploaded

AI生成
style_report_generated

付费
subscription_started

分享
share_clicked

13. MVP开发任务拆分
Sprint 1
基础App
完成：
* 登录
* 页面
* 图片上传

Sprint 2
AI分析
完成：
Vision API
Prompt
Report生成

Sprint 3
推荐系统
完成：
Outfit Generator

Sprint 4
支付
完成：
RevenueCat
App Store Subscription

14. 技术选型建议
App
Swift

Backend
Java jdk21 

Storage
AWS S3

Database
Mysql

Authentication
Firebase Auth

Payment
RevenueCat

AI
GPT-5 Vision API

