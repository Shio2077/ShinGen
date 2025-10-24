# 安卓原神自动对话点击器

目前只有点击对话功能，当出现对话窗口的眼睛图案时自动点击屏幕中心，当有对话气泡时点击气泡~手机原神的拾取比电脑方便，没什么做的动力呢……

## 安装和使用

- 系统最低支持安卓12。

- 需要使用 [Shizuku](https://shizuku.rikka.app/download/) 来实现安卓的模拟点击，所以你需要下载安装 [Shizuku](https://shizuku.rikka.app/download/)并且开启Shizuku服务才能使用模拟点击。[Shizuku教程](https://shizuku.rikka.app/guide/setup/)请自行搜索

- 需要留意手机温度防止过热……

## 原理

将屏幕分享给这个APP然后使用OpenCV服务识别对话窗口，满足条件则使用ADB Shell command发送模拟点击命令。

项目使用了OpenCV官方提供的安卓[SDK-4.12.0](https://opencv.org/releases/)。

# Genshin Impact Auto-Conversation Clicker on Android

Currently, it only has the conversation clicking function, when the "eye" pattern appearing the APP should click center of the game interface; when conversation "bubble" pattern apperaing the bubble will be clicked. Picking up items in Genshin Impact on mobile is more convenient than on a desktop, so I don't have much motivation to do it...

## Installation and Usage

- OS: Android 12+

- Requires  [Shizuku](https://shizuku.rikka.app/download/) to implement simulated clicks on Android, so you need to install  [Shizuku](https://shizuku.rikka.app/download/) and enable the Shizuku service to use simulated clicks. [Shizuku tutorial](https://shizuku.rikka.app/guide/setup/)

- Pay attention to your phone temperature...

## The way it’s implemented

Share your screen to this app and use the OpenCV service to identify the conversation window. If the conditions are met, use the ADB shell command to send a simulated click command.

Project uses Android OpenCV SDK. Click [here](https://opencv.org/releases/) to get OpenCV SDK 4.12.0