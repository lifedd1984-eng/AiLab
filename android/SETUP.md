# 빠른 체크리스트 (Android) — 빌드 & 검증 가이드

iOS와 동일한 v2 명세(카테고리 칩 위젯 · 줄긋기 유지 · 히스토리)의 Android 네이티브 구현입니다.
Kotlin + Jetpack Compose(앱) + Glance(위젯).

## 필요한 것

- Android Studio (Koala 이상 권장)
- Android 8.0(API 26) 이상 기기/에뮬레이터 — 위젯 검증은 실기기 권장

## 실행

1. Android Studio에서 `android/` 폴더를 엽니다 (Gradle 동기화 자동).
2. 출시 전이라면 `app/build.gradle.kts`의 `applicationId`(`com.example.quickchecklist`)를 본인 것으로 변경 — 개발 중 테스트는 그대로도 됩니다.
3. Run ▶ 으로 설치.

> iOS와 달리 App Group 설정이 없습니다 — Android 위젯은 앱과 같은 프로세스에서 실행되어 `filesDir/store.json`을 그대로 공유합니다.

## 검증 체크리스트 (iOS 1단계 AC와 동일 + Android 고유)

- [ ] 앱에서 카테고리 칩 전환, ＋ 추가로 새 카테고리(이름·아이콘) 생성
- [ ] 항목 입력 → 체크 → **사라지지 않고 취소선 유지**, 재탭으로 해제
- [ ] "체크된 N개 정리 → 히스토리" → 우상단 버튼으로 히스토리 확인, 되돌리기 동작
- [ ] 홈 화면에 위젯 추가 → 카테고리 칩 + 목록 표시
- [ ] **앱을 최근 앱에서 제거한 뒤** 위젯 칩 탭 → 카테고리 전환, ◯ 탭 → 취소선 토글
- [ ] 위젯 리사이즈: 1행(긴 막대) = 1개 표시, 2행 = 3개, 4행 = 8개 — **iOS에선 불가능했던 1×4 "긴 막대"가 Android에선 됩니다**
- [ ] 다크 모드 전환 → 앱·위젯 추종 (Material You 동적 색상)

## 구조

```
android/app/src/main/
├── AndroidManifest.xml                  # 액티비티 + 위젯 리시버
├── res/xml/checklist_widget_info.xml    # 위젯 크기·리사이즈 정의
└── java/com/example/quickchecklist/
    ├── MainActivity.kt                  # Compose 앱 화면 (칩·목록·히스토리)
    ├── data/Models.kt                   # iOS와 동일 스키마 (Category, ChecklistItem)
    ├── data/ChecklistStore.kt           # store.json 저장소 (iOS ChecklistStore와 대응)
    └── widget/ChecklistWidget.kt        # Glance 위젯 + Toggle/SelectCategory 액션
```

iOS(`ios/`)와 데이터 구조·기능 명세가 1:1로 대응하므로, 이후 기능 추가 시 두 폴더를 같이 수정합니다.

## 알려진 한계 (MVP)

- 앱 아이콘은 시스템 기본 체크 아이콘 — 출시 전 전용 아이콘 필요
- 위젯 → 앱 항목별 딥링크 없음 (위젯 배경 탭 = 앱 열기)
- 순서 변경(드래그) 미구현 — iOS와 함께 다음 단계
