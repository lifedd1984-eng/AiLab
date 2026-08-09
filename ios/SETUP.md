# 빠른 체크리스트 (iOS) — 빌드 & 검증 가이드

v2 명세 구현: **카테고리 칩 위젯 · 체크 후 줄긋기 유지 · 완료 히스토리** + App Group 공유 저장소.
위젯의 칩·체크박스 모두 앱 실행 없이 동작합니다(AppIntent).

## 필요한 것

- Mac + Xcode 15 이상
- iOS 17 이상 시뮬레이터 또는 실기기 (위젯 검증은 **실기기 권장**)
- [XcodeGen](https://github.com/yonaskolb/XcodeGen): `brew install xcodegen`

## 1. 식별자 바꾸기 (2곳)

`com.example`은 자리표시자입니다. 본인 팀 식별자로 바꿉니다.

| 파일 | 바꿀 것 |
|------|---------|
| `project.yml` | `bundleIdPrefix: com.example` → 본인 프리픽스, `group.com.example.quickchecklist` (2곳) → `group.<본인 프리픽스>.quickchecklist` |
| `Shared/ChecklistStore.swift` | `appGroupID` 상수를 위와 동일하게 |

> App Group ID는 `project.yml`(엔타이틀먼트 2곳)과 `ChecklistStore.swift`(코드 1곳) **총 3곳이 반드시 일치**해야 합니다. 불일치하면 위젯이 항상 빈 목록으로 보입니다 — 1단계 최대 리스크(§10).

## 2. 프로젝트 생성 & 실행

```bash
cd ios
xcodegen generate
open QuickChecklist.xcodeproj
```

Xcode에서:
1. `QuickChecklist`·`ChecklistWidget` 두 타깃 모두 **Signing & Capabilities → Team** 선택 (Automatically manage signing 상태면 App Group도 자동 등록됩니다).
2. `QuickChecklist` 스킴으로 실행.

## 3. 검증 체크리스트 (v2 AC)

- [ ] 첫 실행 시 기본 카테고리 "🛒 살 것" / "📝 할 것"이 보인다
- [ ] 카테고리 칩 전환, ＋ 추가로 새 카테고리(이름·아이콘) 생성
- [ ] 항목 입력 (return으로 연속 입력, 키보드 유지) → 체크 → **사라지지 않고 취소선 유지**, 재탭 해제
- [ ] "체크된 N개 정리 → 히스토리" → 우상단 🕘에서 날짜별 확인, "되돌리기" 동작
- [ ] 홈 화면에 중형(4×2) 위젯 추가 → 카테고리 칩 + 항목 3개 표시
- [ ] **앱을 스와이프로 완전 종료** 후: 위젯 칩 탭 → 카테고리 전환, ◯ 탭 → 취소선 토글 ← **핵심 AC**
- [ ] 위젯 텍스트 탭 → 앱이 해당 카테고리로 열림, 빈 영역 탭 → 입력창 포커스
- [ ] 대형(4×4) 위젯 → 8행 표시
- [ ] 다크 모드 전환 → 앱·위젯 모두 추종

## 문제 해결

| 증상 | 원인/조치 |
|------|-----------|
| 위젯이 항상 빈 목록 | App Group ID 3곳 불일치, 또는 타깃에 App Groups capability 미등록 |
| 위젯 체크가 반응 없음 | iOS 17 미만이거나, `CompleteItemIntent`가 위젯 타깃에 포함 안 됨 (XcodeGen 사용 시 자동 포함) |
| 위젯이 갱신 안 됨 | 위젯을 삭제 후 다시 추가. 시뮬레이터는 갱신이 느릴 수 있음 — 실기기로 확인 |

## 구조 (기획안 §9 대비 변경점)

```
ios/
├── project.yml            # XcodeGen 정의 (타깃 2개 + App Group)
├── ChecklistApp/          # 앱 타깃: 카테고리 칩·입력·목록·히스토리
├── ChecklistWidget/       # 위젯 타깃: 칩 전환 + 체크 토글 (모두 AppIntent), 딥링크
└── Shared/                # 두 타깃에 모두 포함 (SPM 패키지 대신 소스 공유 — 공수 절감)
    ├── Models.swift           # Category, ChecklistItem, StoreData
    ├── ChecklistStore.swift   # App Group 내 JSON 파일 (SwiftData 대체 — 공수 절감)
    └── Intents.swift          # ToggleItemIntent, SelectWidgetCategoryIntent
```

- **저장소: SwiftData → JSON 파일.** 200개 이하 목록엔 성능 동일, 위젯 공유 설정·스키마 관리 공수 제거.
- **ChecklistKit SPM 패키지 → Shared 폴더 소스 공유.** 타깃 2개 규모에서 패키지는 과함.
- Android 버전은 `../android/` — 같은 스키마·같은 명세의 네이티브 구현.

## Mac 없이 아이폰에 설치하기 (무서명 IPA + 사이드로딩)

CI(macOS 러너)가 무서명 IPA를 자동 빌드해 릴리스에 올립니다:
https://github.com/lifedd1984-eng/test1/releases/tag/checklist-ipa

Apple 정책상 IPA는 **서명 없이는 아이폰에 설치할 수 없습니다.** 서명 방법 두 가지:

1. **Sideloadly (Windows/Mac PC 필요)** — 가장 현실적
   1. PC에 [Sideloadly](https://sideloadly.io)와 iTunes(Windows) 설치
   2. 아이폰을 USB로 연결하고 `QuickChecklist-unsigned.ipa`를 Sideloadly에 드래그
   3. 본인 Apple ID 입력 → Advanced에서 **"Signing Mode: Apple ID"** 그대로, 번들 ID는 자동 변경 허용
   4. 설치 후 아이폰의 설정 → 일반 → VPN 및 기기 관리에서 본인 Apple ID 신뢰
   - 무료 Apple ID는 **7일마다 재서명** 필요, 앱 3개 제한
   - ⚠️ 위젯↔앱 데이터 공유(App Group)는 사이드로딩 도구가 그룹 ID를 재작성해야 동작합니다. Sideloadly 옵션에서 app group 처리를 켜세요. 도구 처리에 따라 위젯이 빈 목록일 수 있음 — 이 경우 Mac+Xcode 방식이 확실합니다.

2. **정식 배포 (TestFlight)** — 아래 "TestFlight 자동 업로드" 참고. 아이폰에서 가장 편한 최종 형태.

## TestFlight 자동 업로드 (CI 준비 완료 — 계정만 있으면 됨)

`.github/workflows/ios-testflight.yml`이 서명·업로드까지 자동으로 합니다. 한 번만 하면 되는 준비:

1. **Apple Developer Program 가입** — https://developer.apple.com (연 $99, 승인까지 최대 1~2일)
2. **Team ID 확인** — developer.apple.com → Membership 페이지의 10자리 Team ID
3. **API 키 생성** — App Store Connect → 사용자 및 액세스 → 통합 → App Store Connect API → 키 생성 (권한: **App Manager**). Key ID·Issuer ID를 적어두고 `.p8` 파일 다운로드 (1회만 다운로드 가능)
4. **App Store Connect에서 앱 생성** — 나의 앱 → ＋ → 신규 앱: 이름 "빠른 체크리스트", 번들 ID `com.lifedd.quickchecklist` (식별자 목록에 없으면 developer.apple.com → Identifiers에서 먼저 등록, App Groups capability 체크)
5. **GitHub Secrets 등록** — 저장소 → Settings → Secrets and variables → Actions → New repository secret:
   - `ASC_KEY_ID`: API 키의 Key ID
   - `ASC_ISSUER_ID`: Issuer ID
   - `ASC_KEY_P8`: `.p8` 파일을 텍스트 편집기로 열어 내용 전체 붙여넣기
   - `APPLE_TEAM_ID`: Team ID
6. **실행** — 저장소 → Actions → "iOS TestFlight" → Run workflow
7. 10~15분 후 App Store Connect → TestFlight에 빌드가 나타남 → 내부 테스트 그룹에 본인 추가 → 아이폰의 TestFlight 앱에서 설치. 이후는 워크플로만 다시 돌리면 새 버전이 TestFlight로 갑니다.

> 번들 ID 기본값은 `com.lifedd.quickchecklist` (App Group: `group.com.lifedd.quickchecklist`)로 설정돼 있습니다. 다른 ID를 쓰려면 `project.yml`(4곳)과 `Shared/ChecklistStore.swift`(1곳)를 함께 바꾸세요.

## 남은 작업 (다음 단계)

순서 변경(길게 눌러 드래그), 항목 딥링크 시 해당 행으로 스크롤·강조, 카테고리 이름 변경/삭제, 앱 아이콘.
