# 수면 타이머

Android 9(API 28) 이상에서 매일 지정한 시각에 다음 동작을 수행하는 간단한 수면 타이머입니다.

- Android 미디어 세션에 일시정지 요청
- 미디어 음량을 0으로 변경
- 접근성 서비스로 화면 잠금
- 재부팅·시간대 변경·앱 업데이트 후 예약 복구

## 사용 순서

1. Android Studio에서 프로젝트를 엽니다.
2. 앱을 설치하고 `정확한 알람`, `미디어 제어`, `화면 잠금` 권한을 각각 허용합니다.
3. 실행 시각을 선택하고 `매일 자동 실행`을 켭니다.
4. 처음에는 `지금 테스트`로 실제 기기에서 확인합니다.

Android 12 이상에서는 정확한 실행을 위해 시스템의 `알람 및 리마인더` 접근 권한이 필요합니다. 권한을 허용하지 않으면 시스템이 예약을 늦게 전달할 수 있습니다.

## 미디어 호환성

미디어 일시정지는 Android가 활성 미디어 세션으로 노출한 재생에 대해 동작합니다. YouTube나 CHZZK 공식 앱, 또는 브라우저 재생이 기기/앱 버전에 따라 세션을 노출하지 않으면 일시정지는 건너뛸 수 있습니다. 이 경우에도 음량 0과 화면 잠금은 별도로 시도합니다.

## 이 작업 디렉터리에서 빌드

현재 작업 디렉터리에서는 빌드에 필요한 JDK 17, Gradle 8.9, Android SDK 35와 Build Tools를 `.tools/` 아래에 설치해 두었습니다. `.tools/`와 `local.properties`는 개인 환경용이라 GitHub에는 올리지 않습니다.

저장소를 새 환경에서 빌드하려면 Android Studio의 SDK Manager로 Android SDK 35를 설치하거나, JDK 17과 Android SDK 경로를 준비한 뒤 `local.properties`를 생성하세요.

```bash
./build-debug.sh assembleDebug
```

APK 위치:

```text
app/build/outputs/apk/debug/app-debug.apk
```
