# 🚀 Feedback API - Tech Challenge Fase 4

Bem-vindo ao repositório do **Tech Challenge Fase 4**. Este projeto consiste em uma plataforma de feedback de aulas, onde estudantes podem avaliar as sessões e administradores recebem notificações e relatórios automáticos.

Este README foi estruturado para apresentar a arquitetura, as decisões técnicas e as instruções de execução de forma alinhada aos **critérios de avaliação** do desafio.

---

## 🏗️ 1. Arquitetura da Solução e Modelo Cloud

O projeto foi construído focado em **Cloud Computing e Serverless**, dividindo as responsabilidades para garantir escalabilidade, resiliência e otimização de custos na nuvem da Microsoft Azure.

### Componentes Principais:
1. **API Principal (Quarkus + PostgreSQL):** 
   - Desenvolvida em Java com Quarkus.
   - Responsável por expor os endpoints HTTP RESTful para os estudantes enviarem avaliações.
   - Orquestra as regras de negócio, salva de forma persistente no banco relacional (PostgreSQL) e chama os gatilhos externos.
2. **Azure Function 1: Alerta de Urgência (`fn-alerta-urgencia`)**
   - Função Serverless invocada via HTTP POST.
   - Acionada em tempo real quando um feedback possui nota extremamente baixa (Urgência CRITICO).
   - Envia um e-mail imediato aos administradores (via SMTP/Mailtrap).
3. **Azure Function 2: Relatório Semanal (`fn-relatorio-semanal`)**
   - Função Serverless invocada via HTTP POST.
   - Acionada por um Scheduler (`@Scheduled`) na API Quarkus toda segunda-feira às 11:00.
   - Recebe um payload com dados agregados (por dia, por urgência e média das avaliações) e envia um relatório detalhado por e-mail.

### Por que Serverless e por que essa arquitetura?
Em vez de utilizar um *Message Broker* (como RabbitMQ) — o que aumentaria a complexidade e geraria custos fixos de infraestrutura na Azure —, optamos por integrações HTTP diretas usando o *Quarkus RestClient*.
Isso nos permitiu focar no modelo **Consumption Plan** das Azure Functions, onde o custo é estritamente proporcional ao uso (pagamento por milissegundo de execução) e escala a zero quando o sistema está ocioso. Além disso, criamos **dois projetos Serverless separados**, garantindo o Princípio da Responsabilidade Única (SRP) exigido no desafio.

---

## 🔒 2. Segurança dos Dados dos Clientes e Governança de Acesso

O ambiente em nuvem foi desenhado com rígidas configurações de segurança relacionadas aos dados dos clientes (estudantes) e governança de acesso administrativo:

- **Isolamento de Papéis (Alunos vs Administradores):** A arquitetura garante que a API de submissão (Quarkus) seja o único ponto de entrada para os **alunos** avaliarem as aulas. Os **administradores** não precisam de acesso ao banco de dados, pois recebem os relatórios formatados e os alertas críticos passivamente por e-mail, garantindo um fluxo unidirecional e seguro.
- **Governança de Acesso na Nuvem (RBAC):** O deploy automatizado e o gerenciamento das Azure Functions operam sob *Role-Based Access Control* (RBAC). O GitHub Actions utiliza um *Service Principal* com escopo restrito apenas aos recursos necessários, impedindo acessos não autorizados a outros componentes da assinatura Azure.
- **Segurança de Dados em Trânsito e Repouso:** Os feedbacks são transmitidos do Quarkus para as Azure Functions exclusivamente através de chamadas seguras (HTTPS / TLS 1.2+).
- **Gestão de Segredos:** As credenciais SMTP **nunca** são versionadas no repositório (`local.settings.json` está no `.gitignore`). As secrets são injetadas de forma dinâmica e segura através do Azure CLI, residindo criptografadas nas *App Settings* da Function App.

---

## 🤖 3. Deploy Automatizado (CI/CD)

O projeto possui esteiras de automação configuradas com o **GitHub Actions** (arquivos dentro de `.github/workflows/`).

1. A esteira é engatilhada a cada *push* na branch `main`.
2. Ela realiza o checkout, configura o Java 17 e compila a aplicação.
3. **Impedimento de Erros (Gatekeeper):** O deploy **só avança se os testes unitários passarem**. Se a compilação ou os testes falharem (ausência de `-DskipTests`), a esteira bloqueia o deploy, protegendo o ambiente de nuvem contra códigos com erro.
4. Após o sucesso do build, a pipeline loga na Azure e realiza o deploy da Serverless Function usando o Azure CLI.

---

## 📊 4. Monitoramento e Tolerância a Falhas

Um sistema na nuvem exige observabilidade e resiliência.
- **Health Checks:** A API Quarkus possui endpoints nativos de *liveness* e *readiness* (`/q/health`), que são utilizados ativamente pelo `docker-compose` para garantir a saúde do container.
- **Application Insights / Log Stream:** As Azure Functions estão configuradas debaixo do Azure Monitor. Toda a atividade de rede, erros e tempos de execução são rastreados nativamente pelo Application Insights.
- **Fallback Obrigatório:** Caso as senhas do SMTP estejam ausentes ou incorretas (seja local ou na Azure), o sistema **NÃO QUEBRA**. Implementamos um fallback defensivo (blocos try-catch) que captura a falha de rede/autenticação e imprime todo o conteúdo do e-mail no *Log Stream* da Azure (console log), permitindo que o administrador ainda tenha acesso ao alerta mesmo com indisponibilidade do provedor de e-mail.

## 🚀 5. Instruções de Deploy e Execução Local

Para testar a aplicação localmente e validar a comunicação com a nuvem, siga os passos abaixo:

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/gf-filipe/tech-challenge-fase-4.git
   cd tech-challenge-fase-4
   ```

2. **Suba os containers (Banco de Dados e API):**
   Certifique-se de ter o Docker instalado e execute:
   ```bash
   docker-compose up --build -d
   ```

3. **Acesse a Documentação (Swagger):**
   Com a aplicação rodando, acesse a interface interativa do Swagger OpenAPI em:
   👉 `http://localhost:8080/swagger`

4. **Teste o Alerta de Urgência:**
   Faça um POST criando um feedback com nota `2`. Verifique os logs do Docker para confirmar o envio do alerta para a nuvem da Azure.

5. **Teste o Relatório Semanal:**
   Para testar a função de relatório sem aguardar a rotina automática (cron), faça um POST na rota de disparo manual:
   👉 `http://localhost:8080/relatorio/disparar`

---

## 👨‍💻 Autores

* Filipe Gonçalves Ferreira - Rm367737
* Leandro da Silva Gonçalves - Rm367789
* Lucas Santos Escolástico do Nascimento - Rm367273