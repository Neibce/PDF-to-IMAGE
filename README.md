# PDF to IMAGE

[![Android CI](https://github.com/Neibce/PDF-to-IMAGE/actions/workflows/android.yml/badge.svg)](https://github.com/Neibce/PDF-to-IMAGE/actions/workflows/android.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/neibce/pdf-to-image/badge)](https://www.codefactor.io/repository/github/neibce/pdf-to-image)
[![Google Play](https://img.shields.io/badge/Google%20Play-Download-green?logo=google-play)](https://play.google.com/store/apps/details?id=dev.jun0.pdftoimage)

PDF 파일을 PNG/JPEG 이미지로 변환하는 안드로이드 앱

[Google Play 다운로드](https://play.google.com/store/apps/details?id=dev.jun0.pdftoimage)

2022년 1분기 개발 / Google Play 배포 완료

## 기능

- PNG, JPEG 형식으로 저장
- 원하는 페이지만 선택해서 변환
- 백그라운드에서 변환 (앱을 내려도 계속 진행)
- 이미지 크기 설정 (100px~9000px)
- 원본 비율 유지 가능
- 비밀번호 걸린 PDF도 변환 가능
- 변환 끝나면 알림

## 스크린샷

| | | | |
|--|--|--|--|
| ![1](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/f9e6809b-4253-435e-8a5e-7524eaecc724) | ![2](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/2380f78b-4e14-48ba-ab13-c7710cf1abb3) | ![3](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/0652db3d-4788-44b0-9b7e-3a47fb8cda1d) | ![4](https://github.com/Neibce/PDF-to-IMAGE/assets/18096595/e5a38bb7-74ae-4986-aa56-606ec5d847b1) |

## 사용 기술

- Android (Java)
- [PDFBox Android](https://github.com/TomRoush/PdfBox-Android)
- AndroidX WorkManager
- Firebase Analytics, Crashlytics

## 실행 환경

- Android 6.0 (API 23) 이상

## 빌드 방법

```bash
git clone https://github.com/Neibce/PDF-to-IMAGE.git
cd PDF-to-IMAGE
./gradlew assembleDebug
```
