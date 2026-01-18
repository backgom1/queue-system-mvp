---
name: api-spec
description: REST API 설계 표준 및 DTO 전략을 정의합니다. 수석 엔지니어 관점에서 명확하고 유지보수가 용이한 인터페이스 설계를 가이드합니다.
---

# 🌐 REST API Design Specification

## 🎭 Role: API Designer & Protocol Guardian (Staff Level)
이 가이드를 참조할 때, 에이전트는 **클라이언트가 사용하기 편하면서도 서버의 도메인을 안전하게 보호하는 인터페이스**를 설계하는 가디언 역할을 수행합니다. **80:20 원칙**에 따라, API의 리소스 설계와 데이터 노출 범위는 사용자가 깊이 고민하게 유도하고, 반복적인 DTO 구조나 Swagger 설정은 에이전트가 완벽히 처리합니다.

---

## 💎 핵심 설계 원칙 (Staff Standard)

1. **Resource Oriented Design (리소스 중심 설계):** URI는 행위(동사)가 아닌 **대상(명사)** 중심으로 설계합니다. 리차드슨 성숙도 모델 2단계를 기본 목표로 하며, 적절한 HTTP Method와 상태 코드를 사용하여 인터페이스만 보고도 의도를 파악할 수 있게 합니다.
2. **Entity Isolation (엔티티 격리):** 엔티티는 절대로 외부 API로 직접 노출하지 않습니다. 모든 외부 통신은 **Java Record** 기반의 불변 DTO를 통해 이루어져야 하며, 이는 API 스펙 변화가 도메인에 영향을 주는 것을 방지하는 최소한의 방어선입니다.
3. **Explicit Contract (명확한 계약):** Request DTO는 입력값에 대한 책임을 지며(`Validation`), Response DTO는 클라이언트가 필요로 하는 데이터만 선별하여 전달합니다.

---

## 🛠️ 실무 API 프롬프트 패턴 (Staff Edition)

### 패턴 1: RESTful 엔드포인트 설계 (Contract Design)
단순한 URL 생성을 넘어 리소스 간의 관계와 의미를 정의할 때 사용합니다.

```text
[Task: REST API Design]
'{도메인/기능}'에 대한 API 명세를 설계해줘.

**가이드라인:**
1. **Hierarchy:** 리소스 간의 관계를 `/users/{id}/orders`와 같이 계층적으로 표현해줘.
2. **HTTP Semantics:** - 조회(GET), 생성(POST/201), 수정(PATCH/PUT), 삭제(DELETE)를 정확히 구분.
   - 성공 시 200/201, 실패 시 400(입력오류), 404(자원없음), 409(상태충돌) 등 사용.
3. **Clean URI:** URI에 `get`, `create`, `update` 같은 동사를 넣지 말고 리소스와 Method로 의도를 표현해줘.

---

패턴 2: Record 기반 DTO 설계 (Data Transfer)
안전하고 효율적인 데이터 전송 객체를 설계할 때 사용합니다.

[Task: DTO Design]
'{API 기능}'을 위한 Request/Response DTO를 설계해줘.

**요구사항:**
1. **Modern Java:** 모든 DTO는 `Java Record`로 구현해줘.
2. **Validation:** Request DTO 필드에 `jakarta.validation`(@NotNull, @Size 등)을 적용해줘.
3. **Mapping Strategy:** - Request: `toEntity()` 메서드를 포함하여 도메인 변환 로직 제공.
   - Response: `from(Entity)` 정적 팩토리 메서드를 통해 변환.

---

🚀 API 응답 및 예외 표준 (The Wrapper)

1. 공통 응답 포맷 (Common Response)
성공과 실패에 관계없이 일관된 응답 구조를 유지하여 클라이언트의 파싱 부담을 줄입니다.
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {}

제공해주신 API_MASTER.md를 수석 엔지니어(Staff Engineer)의 실무적 통찰을 담아 api-spec.md로 개정했습니다. 불필요하게 복잡하거나 형식적인 전략(예: 지나치게 엄격한 HATEOAS 등)은 배제하고, Java 21 Record와 도메인 보호를 중심으로 한 실용적인 가이드로 구성했습니다.

이 내용을 .gemini/skills/spring-api-master/references/api-spec.md에 저장하여 사용하세요.

Markdown

---
name: api-spec
description: REST API 설계 표준 및 DTO 전략을 정의합니다. 수석 엔지니어 관점에서 명확하고 유지보수가 용이한 인터페이스 설계를 가이드합니다.
---

# 🌐 REST API Design Specification

## 🎭 Role: API Designer & Protocol Guardian (Staff Level)
이 가이드를 참조할 때, 에이전트는 **클라이언트가 사용하기 편하면서도 서버의 도메인을 안전하게 보호하는 인터페이스**를 설계하는 가디언 역할을 수행합니다. **80:20 원칙**에 따라, API의 리소스 설계와 데이터 노출 범위는 사용자가 깊이 고민하게 유도하고, 반복적인 DTO 구조나 Swagger 설정은 에이전트가 완벽히 처리합니다.

---

## 💎 핵심 설계 원칙 (Staff Standard)

1. **Resource Oriented Design (리소스 중심 설계):** URI는 행위(동사)가 아닌 **대상(명사)** 중심으로 설계합니다. 리차드슨 성숙도 모델 2단계를 기본 목표로 하며, 적절한 HTTP Method와 상태 코드를 사용하여 인터페이스만 보고도 의도를 파악할 수 있게 합니다.
2. **Entity Isolation (엔티티 격리):** 엔티티는 절대로 외부 API로 직접 노출하지 않습니다. 모든 외부 통신은 **Java Record** 기반의 불변 DTO를 통해 이루어져야 하며, 이는 API 스펙 변화가 도메인에 영향을 주는 것을 방지하는 최소한의 방어선입니다.
3. **Explicit Contract (명확한 계약):** Request DTO는 입력값에 대한 책임을 지며(`Validation`), Response DTO는 클라이언트가 필요로 하는 데이터만 선별하여 전달합니다.

---

## 🛠️ 실무 API 프롬프트 패턴 (Staff Edition)

### 패턴 1: RESTful 엔드포인트 설계 (Contract Design)
단순한 URL 생성을 넘어 리소스 간의 관계와 의미를 정의할 때 사용합니다.

```text
[Task: REST API Design]
'{도메인/기능}'에 대한 API 명세를 설계해줘.

**가이드라인:**
1. **Hierarchy:** 리소스 간의 관계를 `/users/{id}/orders`와 같이 계층적으로 표현해줘.
2. **HTTP Semantics:** - 조회(GET), 생성(POST/201), 수정(PATCH/PUT), 삭제(DELETE)를 정확히 구분.
   - 성공 시 200/201, 실패 시 400(입력오류), 404(자원없음), 409(상태충돌) 등 사용.
3. **Clean URI:** URI에 `get`, `create`, `update` 같은 동사를 넣지 말고 리소스와 Method로 의도를 표현해줘.
패턴 2: Record 기반 DTO 설계 (Data Transfer)
안전하고 효율적인 데이터 전송 객체를 설계할 때 사용합니다.

Plaintext

[Task: DTO Design]
'{API 기능}'을 위한 Request/Response DTO를 설계해줘.

**요구사항:**
1. **Modern Java:** 모든 DTO는 `Java Record`로 구현해줘.
2. **Validation:** Request DTO 필드에 `jakarta.validation`(@NotNull, @Size 등)을 적용해줘.
3. **Mapping Strategy:** - Request: `toEntity()` 메서드를 포함하여 도메인 변환 로직 제공.
   - Response: `from(Entity)` 정적 팩토리 메서드를 통해 변환.
🚀 API 응답 및 예외 표준 (The Wrapper)
1. 공통 응답 포맷 (Common Response)
성공과 실패에 관계없이 일관된 응답 구조를 유지하여 클라이언트의 파싱 부담을 줄입니다.

Java

public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {}
2. 글로벌 예외 처리
@RestControllerAdvice를 활용하여 비즈니스 예외를 표준화된 HTTP 응답으로 변환합니다.

Domain Exception: 비즈니스 예외(예: UserNotFoundException) 발생 시 커스텀 에러 코드와 함께 적절한 상태 코드(404)를 반환하도록 설계합니다.

🚫 금지 사항 (Anti-Patterns)
No Logic in Controller: 컨트롤러는 오직 입력 검증과 응답 변환만 담당합니다. 비즈니스 로직은 도메인과 서비스로 위임하세요.

No Primitive Response: 데이터를 전달할 때 단순 문자열이나 숫자가 아닌, 확장성을 고려하여 항상 JSON 객체(DTO)로 감싸서 반환하세요.

No Verbs in URI: /user/updatePassword와 같은 설계는 지양합니다. /users/{id}/password에 PATCH를 사용하는 것이 더 RESTful합니다.