# Limite entre Vectras e AndroidX_RmR

## Escopo arquitetural

- **AndroidX_RmR é infraestrutura/framework**: camada de bibliotecas, integração e compatibilidade.
- **AndroidX_RmR não é core de produto Vectras**: não deve concentrar lógica de negócio principal, regras centrais de domínio ou orquestração final de produto.

## Fronteira obrigatória

Mudanças neste fork devem priorizar:

1. Compatibilidade de build/toolchain (Gradle, Kotlin, Node/Yarn para Kotlin/JS).
2. Correções de ciclo de vida/integração de componentes AndroidX.
3. Ajustes de infra para CI/release reproduzível.
4. Pesquisa controlada com isolamento e documentação de risco.

Não devem ser introduzidos como padrão neste repositório:

- Lógica principal do Vectras.
- Acoplamento direto de fluxos de negócio do app ao framework base.
- Alterações orientadas a feature de produto sem justificativa de compatibilidade/framework.

## Regra operacional

- Toda alteração deve declarar explicitamente se é: `compatibilidade`, `build`, `lifecycle` ou `pesquisa controlada`.
- Mudanças de lockfile/dependências devem ter trilha de reprodutibilidade (versões, comando, hash, diff de dependências).
- Sem benchmark comparativo, não declarar ganho de performance.
