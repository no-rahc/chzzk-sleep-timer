# 수면 타이머

Android 9(API 28) 이상에서 매일 지정한 시각에 다음 동작을 수행하는 간단한 수면 타이머입니다.

- Android 미디어 세션에 일시정지 요청
- 미디어 음량을 0으로 변경
- 접근성 서비스로 화면 잠금
- 재부팅·시간대 변경·앱 업데이트 후 예약 복구
- 실제 다음 예약 시각과 권한 준비 상태 표시

## 사용 순서

1. Android Studio에서 프로젝트를 엽니다.
2. 앱을 설치하고 `정확한 알람`, `미디어 제어`, `화면 잠금` 권한을 각각 허용합니다.
3. 실행 시각을 선택하고 `매일 자동 실행`을 켭니다.
4. 처음에는 `지금 테스트`로 실제 기기에서 확인합니다.

Android 12 이상에서는 정확한 실행을 위해 시스템의 `알람 및 리마인더` 접근 권한이 필요합니다. 권한을 허용하지 않아도 예약은 유지되지만 시스템이 예약을 늦게 전달할 수 있습니다.

앱은 다음 예약 시각과 정확/근사 예약 상태를 저장합니다. 앱 또는 시스템 이벤트에서 예약 상태가 오래되었거나 정확한 알람 권한 상태가 바뀐 것을 감지하면 예약을 다시 구성합니다.

## 미디어 호환성

미디어 일시정지는 Android가 활성 미디어 세션으로 노출한 재생에 대해 동작합니다. YouTube나 CHZZK 공식 앱, 또는 브라우저 재생이 기기/앱 버전에 따라 세션을 노출하지 않으면 일시정지는 건너뛸 수 있습니다. 이 경우에도 음량 0과 화면 잠금은 별도로 시도합니다.

## 빌드

JDK 17과 Android SDK 35가 필요합니다.

```bash
./gradlew --no-daemon assembleDebug
```

기존 `build-debug.sh`를 사용할 경우 프로젝트의 `.tools/jdk`, `.tools/android-sdk` 경로를 사용합니다.

```bash
./build-debug.sh assembleDebug
```

APK 위치:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions의 `Android CI` 워크플로도 `assembleDebug`와 `lintDebug`를 실행하고 debug APK를 artifact로 업로드합니다.
