# 安卓原神自动对话点击器

目前只有对话功能~手机原神的拾取比电脑方便，没什么做的动力呢……

## 安装和使用

- 最低支持安卓13。

- 需要使用Shizuku来实现安卓的模拟点击，所以你需要安装Shizuku并且开启Shizuku服务才能使用模拟点击……

## 原理

将屏幕分享给这个APP然后使用OpenCV服务识别对话窗口，满足条件则使用ADB Shell command发送模拟点击命令。

# Genshin Impact Auto-Conversation Clicker on Android

Currently, it only has the conversation function. Picking up items in Genshin Impact on mobile is more convenient than on a desktop, so I don't have much motivation to do it...

## Installation and Usage

- Minimum support for Android 13.

- Requires Shizuku to implement simulated clicks on Android, so you need to install Shizuku and enable the Shizuku service to use simulated clicks...

## The way it’s implemented

Share your screen to this app and use the OpenCV service to identify the conversation window. If the conditions are met, use the ADB shell command to send a simulated click command.