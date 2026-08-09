# 빠른 체크리스트 — 빌드 & 1단계 검증 가이드

구현 1단계 결과물: App Group 공유 저장소 + **위젯에서 앱 실행 없이 체크되는 프로토타입** (기획안 §9 구현 순서 1).

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

## 3. 1단계 수용 기준(AC) 검증 체크리스트

- [ ] 앱에서 항목 3~4개 입력 (return으로 연속 입력, 키보드 유지)
- [ ] 홈 화면 → 위젯 갤러리에서 "빠른 체크리스트" 중형(4×2) 추가 → 방금 입력한 항목이 보인다
- [ ] **앱을 스와이프로 완전 종료**
- [ ] 위젯의 ◯ 탭 → 1초 이내에 항목이 사라지고 다음 항목이 올라온다 ← **핵심 AC (F8)**
- [ ] 앱을 다시 열면 체크한 항목이 "완료됨" 섹션에 있고, 체크 해제로 복원된다
- [ ] 위젯 텍스트/헤더 탭 → 앱이 열린다 (딥링크)
- [ ] 대형(4×4) 위젯 추가 → 8행 표시
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
├── ChecklistApp/          # 앱 타깃: 입력·목록·완료됨 (F1, F2, F4 최소 구현)
├── ChecklistWidget/       # 위젯 타깃: Medium/Large, AppIntent 체크, 딥링크 (F7~F9)
└── Shared/                # 두 타깃에 모두 포함 (SPM 패키지 대신 소스 공유 — 공수 절감)
    ├── ChecklistItem.swift
    ├── ChecklistStore.swift   # App Group 내 JSON 파일 (SwiftData 대체 — 공수 절감)
    └── CompleteItemIntent.swift
```

- **저장소: SwiftData → JSON 파일.** 200개 이하 목록엔 성능 동일, 위젯 공유 설정·스키마 관리 공수 제거. 스키마(필드)는 기획안 §8과 동일해서 되돌리기도 쉽습니다.
- **ChecklistKit SPM 패키지 → Shared 폴더 소스 공유.** 타깃 2개 규모에서 패키지는 과함.

## 다음 단계 (2단계~)

1단계 AC 통과 확인 후: 실행 취소 스낵바(F3), 인라인 편집(F5), 순서 변경(F6), 체크 0.4초 지연 폴리시(F2), 항목 딥링크 스크롤(F9 잔여).
