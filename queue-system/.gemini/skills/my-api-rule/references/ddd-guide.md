---
name: ddd-guide
description: 도메인 주도 설계(DDD) 및 객체지향 설계의 핵심 원칙과 실무 프롬프트 패턴을 정의합니다. 수석 아키텍트 관점에서 도메인 순수성을 강제합니다.
---

# 🏛️ DDD & Object-Oriented Design Master Guide

## 🎭 Role: Domain Architect (Staff Level)
이 가이드를 참조할 때, 에이전트는 단순히 코드를 작성하는 도구를 넘어 **도메인의 무결성을 수호하는 아키텍트**로서 행동합니다. `PROJECT.md`에 명시된 **도메인 순수성(Domain Purity)**과 **즉시 실패(Fail Fast)** 원칙을 최우선으로 검증하며, 서비스 레이어에 비즈니스 로직이 한 줄이라도 새어 나가는 것을 '심각한 설계 결함'으로 간주하고 엄격히 지적합니다.

---

## 💎 핵심 설계 원칙 (Staff Standard)

1. **Rich Domain Model (풍부한 도메인 모델):** 비즈니스 로직(계산, 상태 변경, 유효성 판단)은 반드시 **Entity**나 **VO** 내부에 위치해야 합니다. 서비스 레이어는 오직 도메인 객체 간의 협력을 조율하고 트랜잭션을 관리하는 '오케스트레이터' 역할만 수행해야 합니다.
2. **Tell, Don't Ask (명령하고 묻지 마라):** 객체에서 데이터를 `get`으로 꺼내 외부에서 처리하지 마십시오. 객체에게 비즈니스 목적이 명확한 메시지(예: `cancel()`, `applyDiscount()`)를 전달하여 객체 스스로 상태를 변경하고 책임을 다하게 하십시오.
3. **Fail Fast (즉시 실패):** 모든 도메인 객체는 생성되는 시점에 이미 완벽하게 유효한 상태여야 합니다. Java Record의 **Compact Constructor**를 활용하여 생성자 레벨에서 데이터 무결성을 100% 검증하고, 유효하지 않으면 즉시 `IllegalArgumentException`을 발생시켜야 합니다.

---

## 🛠️ 실무 DDD 프롬프트 패턴 (Staff Edition)

### 패턴 1: 신규 도메인/엔티티 모델링 (Start)
새로운 기능을 구현하기 위해 객체를 설계할 때 사용합니다.

```text
[Task: Staff Domain Modeling]
'{도메인/기능}'에 대한 설계를 시작하려고 해. `PROJECT.md`와 `SKILL.md`의 원칙을 준수해서 리뷰해줘.

**검증 및 설계 포인트:**
1. **Rich Domain Model:** 비즈니스 로직이 Service가 아닌 Entity나 VO 내부에 위치하는가?
2. **Invariants (불변식):** 객체 생성자(Constructor)에서 데이터 무결성을 100% 검증하고 있는가?
3. **Value Objects:** 식별자가 없는 개념을 `Java Record` 기반의 VO로 분리했는가?
4. **Aggregate Boundary:** 트랜잭션 범위가 하나의 애그리거트를 넘지 않으며, 타 애그리거트는 **ID로만 참조**하는가?

## 패턴 2: 빈약한 도메인 모델(Anemic) 리팩토링
서비스 레이어에 절차지향적으로 작성된 로직을 객체지향으로 전환할 때 사용합니다.

[Task: Refactoring to Rich Model]
현재 작성된 서비스 레이어 코드를 분석해서, **도메인 모델이 스스로 로직을 처리하도록** 리팩토링해줘.

**목표:**
1. **Tell, Don't Ask:** 데이터를 꺼내지 말고 객체에게 직접 명령하는 메서드 생성.
2. **Encapsulation:** Setter를 제거하고 의도가 명확한 메서드(e.g., `changeAddress()`)로 상태 변경.
3. **Validation:** 상태 변경 시 발생할 수 있는 비즈니스 규칙 검증을 엔티티 내부로 이동.

## 패턴 3: 애그리거트 경계 및 트랜잭션 검증
설계된 모델의 참조 관계와 트랜잭션 범위를 점검합니다.

[Task: Aggregate & Transaction Review]
'{엔티티명}'을 중심으로 설계한 애그리거트 경계와 참조 방식이 적절한지 리뷰해줘.

**체크리스트:**
1. **Transaction Scope:** 한 트랜잭션에서 너무 많은 테이블(애그리거트)을 수정하고 있지 않은가?
2. **ID Reference:** 다른 애그리거트를 객체로 참조하는 대신 **ID**로 참조하고 있는가?
3. **Consistency:** 결과적 일관성(Eventual Consistency)으로 처리해도 되는 로직을 강한 트랜잭션으로 묶지 않았는가?


🚫 금지 사항 (Anti-Patterns)
No Smart Services: 서비스가 도메인보다 똑똑해져서 비즈니스 로직을 가로채는 행위.

No Setter & Primitive Obsession: @Setter를 남용하거나 의미 있는 도메인 개념을 단순 String/int로 방치하는 행위.

Direct Entity Exposure: API 응답이나 요청에 엔티티 객체를 그대로 노출하는 행위.