---
name: tdd-workflow
description: 테스트 주도 개발(TDD)의 전략적 수행 방식과 레이어별 테스트 원칙을 정의합니다. 수석 엔지니어 관점에서 테스트가 설계를 주도하도록 가이드합니다.
---

# 🧪 TDD (Test Driven Development) Strategy Guide

## 🎭 Role: TDD Strategy Architect (Staff Level)
이 가이드를 참조할 때, 에이전트는 사용자가 단순히 '테스트 코드를 짜는 것'을 넘어, **테스트를 통해 도메인 설계를 정교화**하도록 유도하는 아키텍트 역할을 수행합니다. **80:20 원칙**에 따라, 테스트 시나리오 설계와 핵심 로직 구현은 사용자가 고민하게 만들고, 반복적인 테스트 툴 설정은 에이전트가 완벽히 보조합니다.

---

## 💎 핵심 테스트 원칙 (Staff Standard)

1. **Red-Green-Refactor Cycle:** 프로덕션 코드 작성 전, 반드시 실패하는 테스트(Red)를 먼저 작성해야 합니다. 테스트를 통과하기 위한 최소한의 코드(Green)를 작성한 뒤, 기존 기능을 파괴하지 않고 구조를 개선(Refactor)하는 사이클을 엄격히 준수합니다.
2. **Given-When-Then Pattern:** 모든 테스트는 가독성과 의도 전달을 위해 `Given(준비)-When(실행)-Then(검증)` 구조를 유지해야 합니다.
3. **Domain-First Testing:** 통합 테스트보다 도메인 모델의 단위 테스트를 우선합니다. 도메인 로직이 엔티티와 VO 내부에 응집되어 있다면, 단위 테스트만으로도 핵심 비즈니스를 완벽히 보호할 수 있습니다.
4. **Isolation (격리):** 단위 테스트 시 외부 의존성(DB, 외부 API 등)은 Mockito를 통해 철저히 격리하여 테스트의 속도와 결정성을 보장합니다.

---

## 🛠️ 실무 TDD 프롬프트 패턴 (Staff Edition)

### 패턴 1: 요구사항 분석 및 테스트 케이스 설계 (Start)
새로운 기능을 시작할 때, 코드를 짜기 전 설계 단계를 강제합니다.

```text
[Task: TDD Scenario Design]
'{기능명}' 구현을 위한 TDD 사이클을 시작하려고 해. 

**요청 사항:**
1. **Scenario Discovery:** 정상 케이스뿐만 아니라 예외(Exception), 경계값(Boundary) 케이스를 나열해줘.
2. **Design Question:** "이 기능을 테스트하기 위해 도메인 모델에 어떤 인터페이스가 필요할까요?"와 같은 설계 관점의 질문을 던져줘.
3. **Template:** JUnit 5와 AssertJ를 기반으로 한 Given-When-Then 테스트 뼈대를 제공해줘.

## 패턴 2: 도메인 로직 리팩토링 및 안전망 확보
기존 코드를 개선할 때 테스트가 안전망(Safety Net) 역할을 하도록 합니다.

[Task: Safe Refactoring]
현재 `{클래스명}`의 로직을 리팩토링하고 싶어.

**가이드:**
1. **Safety Net:** 기존 동작을 보호하는 테스트가 충분한지 먼저 검토해줘. 부족하다면 테스트 보강부터 제안해줘.
2. **Refactor Goal:** 테스트를 통과 상태로 유지하면서, [코드 가독성/성능 최적화/DDD 순수성]을 개선하는 방안을 제시해줘.
3. **Regression Check:** 리팩토링 후에도 모든 기존 테스트가 통과하는지 확인하는 단계를 명시해줘.

🚀 레이어별 테스트 전략 (Staff Level)
1. Domain Layer (The Heart)
Focus: 비즈니스 규칙의 완전성.

Rule: @Nested를 활용하여 상태 전환이나 복잡한 비즈니스 정책을 계층적으로 테스트합니다.

Mentor Tip: "이 비즈니스 규칙이 엔티티 내부에 캡슐화되어 있나요? 테스트가 엔티티의 데이터를 꺼내서 검증하고 있지는 않나요?"

2. Service Layer (The Orchestrator)
Focus: 객체 간의 협력 및 트랜잭션 흐름.

Rule: Mockito를 활용하여 Repository와 외부 클라이언트를 모킹하고, **행위 검증(Verify)**과 상태 검증을 적절히 혼합합니다.

3. API/Presentation Layer
Focus: HTTP 규격 준수 및 데이터 매핑.

Rule: MockMvc를 활용하여 응답 상태 코드, JSON 구조, 필드 유효성 검증(@Valid)을 테스트합니다.

---

🚫 금지 사항 (Anti-Patterns)
No Logic in Tests: 테스트 코드 내부에 복잡한 if문이나 for문을 넣지 마세요. 테스트는 명확한 명세서여야 합니다.

Over-Mocking: 도메인 모델(Entity, VO)까지 모킹하지 마세요. 도메인 객체는 실제 객체를 사용하여 테스트의 신뢰도를 높입니다.

Testing Private Methods: 프라이빗 메서드를 직접 테스트하려고 하지 마세요. 퍼블릭 메서드의 행위를 통해 간접적으로 검증되어야 합니다.