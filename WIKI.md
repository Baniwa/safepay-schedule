# SafePay Schedule API — Wiki Técnica

> **Nível:** Intermediário–Avançado  
> **Stack:** Java 17, Spring Boot 3.x, PostgreSQL, Docker  
> **Objetivo:** Referência arquitetural do projeto de portfólio SafePay Schedule API

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Estrutura de Pacotes](#2-estrutura-de-pacotes)
3. [Gitflow e Estratégia de Commits](#3-gitflow-e-estratégia-de-commits)
4. [Camada de Domínio](#4-camada-de-domínio)
   - 4.1 [Entidade JPA](#41-entidade-jpa)
   - 4.2 [BusinessException](#42-businessexception)
   - 4.3 [TaxPolicy](#43-taxpolicy)
5. [Camada de Repositório](#5-camada-de-repositório)
6. [Camada de DTO](#6-camada-de-dto)
7. [Camada de Serviço](#7-camada-de-serviço)
8. [Camada de Controller](#8-camada-de-controller)
9. [Tratamento Global de Erros](#9-tratamento-global-de-erros)
10. [Configuração de Infraestrutura](#10-configuração-de-infraestrutura)
11. [Testes](#11-testes)
12. [Como Executar](#12-como-executar)
13. [Referências](#13-referências)

---

## 1. Visão Geral

O **SafePay Schedule API** é um serviço RESTful que permite o agendamento de transferências financeiras futuras com cálculo dinâmico de tarifas. O projeto foi construído seguindo princípios de **arquitetura em camadas** com influências do **Domain-Driven Design (DDD) Lite** (Evans, 2003; Vernon, 2013).

### Regras de Negócio Centrais

| Regra | Detalhe |
|-------|---------|
| Data de agendamento | Deve ser estritamente no futuro |
| Valor da transferência | Deve ser maior que zero |
| Taxa (>10 dias) | 2% sobre o valor bruto |
| Taxa (≤10 dias) | 5% sobre o valor bruto |

### Decisões Arquiteturais

A separação entre **Entity**, **DTO (Record)** e **Policy** é deliberada e segue o princípio de **Separação de Responsabilidades** (SRP — Martin, 2017). Expor a entidade JPA diretamente na API criaria acoplamento entre o modelo de banco e o contrato HTTP — uma violação conhecida como "Leaky Abstraction" (Spolsky, 2002).

---

## 2. Estrutura de Pacotes

```
com.safepay.schedule
│
├── config/                        # Beans de configuração (OpenAPI, etc.)
│   └── OpenApiConfig.java
│
├── controller/                    # Camada de apresentação HTTP
│   ├── PaymentScheduleController.java
│   └── advice/
│       └── GlobalExceptionHandler.java
│
├── domain/                        # Núcleo do domínio (sem dependências externas)
│   ├── entity/
│   │   └── PaymentSchedule.java
│   ├── exception/
│   │   └── BusinessException.java
│   └── policy/
│       └── TaxPolicy.java
│
├── dto/                           # Objetos de transferência de dados (imutáveis)
│   ├── SchedulePaymentRequest.java
│   ├── SchedulePaymentResponse.java
│   └── ApiErrorResponse.java
│
├── repository/                    # Contratos de persistência (Spring Data JPA)
│   └── PaymentScheduleRepository.java
│
└── service/                       # Lógica de aplicação e orquestração
    └── PaymentScheduleService.java
```

> **Por que `domain/policy/` separado?**
> A `TaxPolicy` encapsula uma regra de negócio pura, sem estado e sem dependências de infraestrutura. Isolá-la como um **Domain Service** sem anotações Spring permite testá-la com zero dependências de framework — um requisito do princípio **Ports & Adapters** (Cockburn, 2005).

---

## 3. Gitflow e Estratégia de Commits

### Fluxo de Branches

```
main          ──────────────────────────────────────────── (produção)
                ↑ merge via PR
release/1.0.0 ──────────────────────────────────────────
                ↑ merge via PR
develop       ────────────────────────────────────────────
                ↑ feature branches
feature/payment-scheduling
```

### Conventional Commits (Especificação: conventionalcommits.org)

O formato adotado segue o padrão `<type>(<scope>): <description>` onde:
- **type**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`
- **scope**: módulo ou camada afetada
- **description**: frase imperativa em inglês, sem ponto final

#### Sequência de commits para a feature `payment-scheduling`

```bash
# 1. Setup inicial do projeto
git commit -m "chore(project): bootstrap Spring Boot 3 project with Maven"

# 2. Infraestrutura Docker
git commit -m "chore(infra): add PostgreSQL Docker Compose service with health check"

# 3. Migração do banco de dados
git commit -m "feat(db): create payment_schedule table via Flyway migration V1"

# 4. Domínio — Entidade
git commit -m "feat(domain): add PaymentSchedule JPA entity with Builder pattern"

# 5. Domínio — Exceção de negócio
git commit -m "feat(domain): introduce BusinessException as unchecked runtime exception"

# 6. Domínio — Política de taxa
git commit -m "feat(domain): implement regressive TaxPolicy with 2% and 5% rates"

# 7. Repositório
git commit -m "feat(repository): add PaymentScheduleRepository extending JpaRepository"

# 8. DTOs
git commit -m "feat(dto): define SchedulePaymentRequest and SchedulePaymentResponse records"

# 9. Serviço
git commit -m "feat(service): implement payment scheduling with tax computation and validation"

# 10. Controller
git commit -m "feat(controller): expose POST endpoint for payment scheduling with OpenAPI docs"

# 11. Error handler
git commit -m "feat(exception): add GlobalExceptionHandler with structured JSON error responses"

# 12. Testes
git commit -m "test(service): cover business rule violations and tax rate selection"
git commit -m "test(controller): verify HTTP status codes and error payload structure"
```

> **Referência:** A especificação Conventional Commits é compatível com o **Semantic Versioning (SemVer)** e permite geração automática de changelogs via ferramentas como `semantic-release` (Preston-Werner, 2013).

---

## 4. Camada de Domínio

### 4.1 Entidade JPA

**Arquivo:** [domain/entity/PaymentSchedule.java](src/main/java/com/safepay/schedule/domain/entity/PaymentSchedule.java)

A entidade utiliza o padrão **Builder** (Gamma et al., 1994 — GoF) em vez de um construtor com múltiplos parâmetros. Isso resolve o problema do "Telescoping Constructor Anti-Pattern" (Bloch, 2018 — Effective Java, Item 2).

```java
// Anti-pattern: construtor telescópico — frágil e ilegível
new PaymentSchedule("ACC-001", "ACC-002", amount, tax, date, now);

// Pattern Builder — legível, seguro e extensível
PaymentSchedule.builder()
    .originAccount("ACC-001")
    .destinationAccount("ACC-002")
    .amount(amount)
    .tax(tax)
    .scheduledDate(date)
    .build();
```

**Decisões técnicas:**
- `@GeneratedValue(strategy = GenerationType.UUID)` — delegação ao banco via `gen_random_uuid()` (PostgreSQL 13+). Garante unicidade sem sequência central.
- `protected PaymentSchedule()` — construtor sem argumentos exigido pelo JPA/Hibernate, mas com visibilidade reduzida para impedir instanciação acidental fora do pacote.
- Ausência de setters — a entidade é **imutável após persistência**, prevenindo estados inconsistentes.

### 4.2 BusinessException

**Arquivo:** [domain/exception/BusinessException.java](src/main/java/com/safepay/schedule/domain/exception/BusinessException.java)

`BusinessException` estende `RuntimeException` (unchecked), pois violações de regra de negócio são erros de programação do cliente da API — não condições recuperáveis que o chamador deva tratar no compilador (Bloch, 2018, Item 70).

Carrega o `HttpStatus` desejado para desacoplar a lógica de domínio da decisão de status HTTP, que é capturada pelo `GlobalExceptionHandler`.

### 4.3 TaxPolicy

**Arquivo:** [domain/policy/TaxPolicy.java](src/main/java/com/safepay/schedule/domain/policy/TaxPolicy.java)

`TaxPolicy` é um **Value Object de Comportamento** (Vernon, 2013). É uma classe `final` com construtor privado — um **Utility Class** com método estático puro. Isso garante:

1. **Testabilidade máxima:** sem mocks, sem Spring context.
2. **Sem efeitos colaterais:** entrada → saída determinística.
3. **Isolamento do domínio:** a regra vive no domínio, não no serviço.

```java
// Lógica de taxa — pura e auditável
long daysUntilExecution = ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate);
BigDecimal rate = daysUntilExecution > 10 ? new BigDecimal("0.02") : new BigDecimal("0.05");
return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
```

> **Por que `BigDecimal` e não `double`?**
> Aritmética de ponto flutuante IEEE 754 (`double`) introduz erros de representação em frações decimais (ex: `0.1 + 0.2 ≠ 0.3`). Em sistemas financeiros, isso é inaceitável. `BigDecimal` com `RoundingMode.HALF_UP` é o padrão da indústria (Bloch, 2018, Item 60).

---

## 5. Camada de Repositório

**Arquivo:** [repository/PaymentScheduleRepository.java](src/main/java/com/safepay/schedule/repository/PaymentScheduleRepository.java)

```java
@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, UUID> {}
```

Spring Data JPA implementa esta interface em tempo de execução via **Proxy Dinâmico** (Gamma et al., 1994 — Proxy Pattern). A anotação `@Repository` adiciona semântica ao componente e ativa a tradução de exceções JPA para `DataAccessException` do Spring.

A chave primária é `UUID` — resistente a ataques de enumeração sequencial de IDs (`/api/v1/payments/1`, `/api/v1/payments/2`...), um vetor de IDOR (*Insecure Direct Object Reference*) documentado no OWASP Top 10 (OWASP, 2021).

---

## 6. Camada de DTO

**Arquivos:** [dto/](src/main/java/com/safepay/schedule/dto/)

DTOs utilizam **Java Records** (JEP 395, Java 16+), que são classes imutáveis por design — sem setters, `equals`/`hashCode`/`toString` gerados automaticamente pelo compilador.

### SchedulePaymentRequest

```java
public record SchedulePaymentRequest(
    @NotBlank String originAccount,
    @NotBlank String destinationAccount,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull @Future LocalDate scheduledDate
) {}
```

As anotações `@NotBlank`, `@Future`, `@DecimalMin` são do **Bean Validation (JSR 380)** implementado pelo **Hibernate Validator**. A validação é disparada automaticamente pelo Spring MVC quando `@Valid` está presente no controller.

### SchedulePaymentResponse

O método estático de fábrica `from(PaymentSchedule entity)` segue o padrão **Static Factory Method** (Bloch, 2018, Item 1) e isola o mapeamento entidade→DTO dentro do próprio DTO — eliminando a necessidade de um mapper externo para casos simples.

### ApiErrorResponse

Estrutura padronizada inspirada no **RFC 7807 — Problem Details for HTTP APIs** (Nottingham & Wilde, 2016):

```json
{
  "timestamp": "2026-05-25T14:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Scheduled date must be strictly in the future.",
  "path": "/api/v1/payment-schedules",
  "violations": []
}
```

---

## 7. Camada de Serviço

**Arquivo:** [service/PaymentScheduleService.java](src/main/java/com/safepay/schedule/service/PaymentScheduleService.java)

A `PaymentScheduleService` é o **Application Service** (Evans, 2003) que orquestra o fluxo de criação de agendamento:

```
Request → validateBusinessRules() → TaxPolicy.compute() → builder().build() → repository.save() → Response
```

**Decisões técnicas:**

- **Constructor Injection** (não `@Autowired` em campo): favorece testabilidade (injeção manual sem Spring) e torna dependências explícitas — alinhado com os princípios SOLID (Martin, 2017).

- `@Transactional`: garante que a operação de escrita seja atômica. Se `repository.save()` lançar exceção, o contexto de transação é revertido automaticamente.

- **Validação dupla:** as anotações de Bean Validation no DTO capturam erros de formato/nulidade (HTTP 400). O método `validateBusinessRules()` captura invariantes de domínio (HTTP 422). Essa separação segue a distinção entre **erros de sintaxe** e **erros de semântica** (Fowler, 2002).

---

## 8. Camada de Controller

**Arquivo:** [controller/PaymentScheduleController.java](src/main/java/com/safepay/schedule/controller/PaymentScheduleController.java)

O controller é fino (*thin controller*) — delega 100% da lógica ao serviço. Sua única responsabilidade é: receber a requisição HTTP, validar a forma, delegar ao serviço e retornar o status HTTP correto.

```java
@PostMapping
public ResponseEntity<SchedulePaymentResponse> create(@Valid @RequestBody SchedulePaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.schedule(request));
}
```

| Verbo HTTP | Status de Sucesso | Semântica REST |
|------------|-------------------|----------------|
| POST       | 201 Created       | Recurso criado; URI do novo recurso pode ser retornada no header `Location` |

> **Referência:** O uso correto de HTTP verbs e status codes segue a especificação **RFC 7231** (Fielding & Reschke, 2014) e os princípios REST de Roy Fielding (2000).

---

## 9. Tratamento Global de Erros

**Arquivo:** [controller/advice/GlobalExceptionHandler.java](src/main/java/com/safepay/schedule/controller/advice/GlobalExceptionHandler.java)

`@RestControllerAdvice` é a composição de `@ControllerAdvice` + `@ResponseBody`. Atua como um interceptor **AOP (Aspect-Oriented Programming)** — captura exceções de qualquer controller sem poluir o código de negócio com `try/catch`.

### Mapeamento de Exceções

| Exceção | Status HTTP | Cenário |
|---------|-------------|---------|
| `MethodArgumentNotValidException` | 400 Bad Request | Falha nas anotações Bean Validation |
| `BusinessException` | 422 Unprocessable Entity | Violação de regra de negócio |
| `Exception` (fallback) | 500 Internal Server Error | Erros inesperados |

**Por que 422 e não 400 para regras de negócio?**

HTTP 400 indica que a requisição está mal formada sintáticamente. HTTP 422 (definido no RFC 4918 — WebDAV) indica que a entidade da requisição está semanticamente incorreta — o servidor entendeu a requisição, mas não pode processá-la. Essa distinção permite que clientes da API diferenciem erros de validação de erros de regra de negócio programaticamente.

---

## 10. Configuração de Infraestrutura

### Docker Compose

```yaml
services:
  postgres:
    image: postgres:16-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U safepay_user -d safepay_db"]
```

O `healthcheck` garante que a API só receba tráfego após o PostgreSQL estar pronto para aceitar conexões — prevenindo falhas de startup em ambientes orquestrados (Docker Compose `depends_on: condition: service_healthy`).

### Flyway (Migrations)

O Flyway gerencia o versionamento do schema de banco de dados. Arquivos de migração seguem a convenção `V{versão}__{descrição}.sql`. A propriedade `ddl-auto: validate` (não `create` ou `update`) garante que o Hibernate apenas **valide** o schema existente contra as entidades — sem alterações implícitas em produção, um risco crítico de segurança operacional.

### HikariCP Connection Pool

```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 2
  connection-timeout: 30000
```

HikariCP é o connection pool padrão do Spring Boot — reconhecido como o mais performático em benchmarks independentes (Brettmeyer, 2018). O `connection-timeout` de 30s previne o esgotamento silencioso de threads sob carga.

---

## 11. Testes

### Estratégia — Pirâmide de Testes

```
         /\
        /  \    E2E Tests (não implementados nesta iteração)
       /────\
      /  IT  \  Integration Tests (@SpringBootTest + Testcontainers)
     /────────\
    / Unit    \ Unit Tests (@ExtendWith(MockitoExtension.class))
   /──────────\
```

A **Pirâmide de Testes** (Cohn, 2009; Fowler, 2012) recomenda mais testes unitários (rápidos, baratos) e menos testes de integração (lentos, custosos).

### Testes Unitários — Service

**Arquivo:** [service/PaymentScheduleServiceTest.java](src/test/java/com/safepay/schedule/service/PaymentScheduleServiceTest.java)

Usa `@ExtendWith(MockitoExtension.class)` — inicializa mocks sem Spring context, executando em ~50ms.

- `ArgumentCaptor<PaymentSchedule>` verifica que a entidade persistida possui a taxa correta — testa o comportamento, não a implementação.
- Testes agrupados em `@Nested` classes seguem o padrão **Given-When-Then** implícito.

### Testes de Slice — Controller

**Arquivo:** [controller/PaymentScheduleControllerTest.java](src/test/java/com/safepay/schedule/controller/PaymentScheduleControllerTest.java)

`@WebMvcTest` inicializa apenas a camada web do Spring (controllers, filters, exception handlers) — sem banco de dados. É um **Slice Test** (Spring Boot Test, 2023), executando em ~1s.

`MockMvc` permite simular requisições HTTP reais sem iniciar um servidor, verificando status codes, headers e corpo JSON.

---

## 12. Como Executar

### Pré-requisitos

- Java 17+
- Maven 3.8+
- Docker e Docker Compose

### Passos

```bash
# 1. Subir o PostgreSQL
docker compose up -d

# 2. Aguardar o healthcheck (opcional — verificar)
docker compose ps

# 3. Compilar e executar a API
./mvnw spring-boot:run

# 4. Acessar o Swagger UI
# http://localhost:8080/swagger-ui.html

# 5. Executar os testes
./mvnw test
```

### Exemplo de Requisição

```bash
curl -X POST http://localhost:8080/api/v1/payment-schedules \
  -H "Content-Type: application/json" \
  -d '{
    "originAccount": "123456789",
    "destinationAccount": "987654321",
    "amount": 1500.00,
    "scheduledDate": "2026-08-01"
  }'
```

### Resposta Esperada (201 Created)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "originAccount": "123456789",
  "destinationAccount": "987654321",
  "amount": 1500.00,
  "tax": 30.00,
  "scheduledDate": "2026-08-01",
  "createdAt": "2026-05-25T14:30:00"
}
```

> Nota: agendamento para 2026-08-01 está a mais de 10 dias — taxa de 2% aplicada (R$1.500 × 2% = R$30,00).

---

## 13. Referências

### Livros

- **BLOCH, Joshua.** *Effective Java*, 3rd ed. Addison-Wesley, 2018.
- **EVANS, Eric.** *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley, 2003.
- **FOWLER, Martin.** *Patterns of Enterprise Application Architecture*. Addison-Wesley, 2002.
- **GAMMA, Erich; HELM, Richard; JOHNSON, Ralph; VLISSIDES, John.** *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley, 1994.
- **MARTIN, Robert C.** *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall, 2017.
- **MARTIN, Robert C.** *Clean Code: A Handbook of Agile Software Craftsmanship*. Prentice Hall, 2008.
- **VERNON, Vaughn.** *Implementing Domain-Driven Design*. Addison-Wesley, 2013.
- **COHN, Mike.** *Succeeding with Agile: Software Development Using Scrum*. Addison-Wesley, 2009.

### RFCs e Especificações

- **RFC 7231** — Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content. Fielding, R. & Reschke, J. (2014).
- **RFC 4918** — HTTP Extensions for Web Distributed Authoring and Versioning (WebDAV). Dusseault, L. (2007). *Define o status 422.*
- **RFC 7807** — Problem Details for HTTP APIs. Nottingham, M. & Wilde, E. (2016).
- **JEP 395** — Records. OpenJDK, Java 16 (2021).
- **JSR 380** — Bean Validation 2.0. Jakarta EE.
- **Conventional Commits v1.0.0** — conventionalcommits.org
- **Semantic Versioning 2.0.0** — semver.org (Preston-Werner, 2013)

### Artigos e Documentação

- **COCKBURN, Alistair.** *Hexagonal Architecture (Ports and Adapters)*. 2005. Disponível em: alistair.cockburn.us/hexagonal-architecture
- **FIELDING, Roy T.** *Architectural Styles and the Design of Network-based Software Architectures*. Dissertation, UC Irvine, 2000.
- **FOWLER, Martin.** *Test Pyramid*. martinfowler.com, 2012.
- **OWASP Top 10.** *A01:2021 – Broken Access Control (IDOR)*. owasp.org, 2021.
- **SPOLSKY, Joel.** *The Law of Leaky Abstractions*. Joel on Software, 2002.
- **Spring Boot Reference Documentation** — docs.spring.io/spring-boot/docs/3.2.x
- **Springdoc OpenAPI** — springdoc.org
