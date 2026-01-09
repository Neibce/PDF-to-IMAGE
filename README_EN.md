# PDF to IMAGE

[한국어](./README.md)

[![Android CI](https://github.com/Neibce/PDF-to-IMAGE/actions/workflows/android.yml/badge.svg)](https://github.com/Neibce/PDF-to-IMAGE/actions/workflows/android.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/neibce/pdf-to-image/badge)](https://www.codefactor.io/repository/github/neibce/pdf-to-image)
[![Google Play](https://img.shields.io/badge/Google%20Play-Download-green?logo=google-play)](https://play.google.com/store/apps/details?id=dev.jun0.pdftoimage)

Android app that converts PDF files to PNG/JPEG images.

Developed in Q1 2022 / Available on [Google Play](https://play.google.com/store/apps/details?id=dev.jun0.pdftoimage)

## Features

- Export as PNG or JPEG
- Select specific pages to convert
- Background conversion (keeps running when minimized)
- Custom image size (100px~9000px)
- Preserve original aspect ratio
- Supports password-protected PDFs
- Notification on completion

## Screenshots

| | | | |
|--|--|--|--|
| ![1](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/f9e6809b-4253-435e-8a5e-7524eaecc724) | ![2](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/2380f78b-4e14-48ba-ab13-c7710cf1abb3) | ![3](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/0652db3d-4788-44b0-9b7e-3a47fb8cda1d) | ![4](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/e5a38bb7-74ae-4986-aa56-606ec5d847b1) |

## Tech Stack

- Android (Java)
- [PDFBox Android](https://github.com/TomRoush/PdfBox-Android)
- AndroidX WorkManager
- Firebase Analytics, Crashlytics

## Requirements

- Android 6.0 (API 23) or higher

## Build

```bash
git clone https://github.com/Neibce/PDF-to-IMAGE.git
cd PDF-to-IMAGE
./gradlew assembleDebug
```
