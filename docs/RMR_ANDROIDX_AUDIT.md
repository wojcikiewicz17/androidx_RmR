# RMR AndroidX Audit

Data da auditoria: 2026-04-30 (UTC)

## 1) Alterações locais auditadas

Arquivos detectados como alterados localmente:

- `kotlin-js-store/.pnp.cjs` (untracked)
- `kotlin-js-store/.yarn/` (untracked)
- `kotlin-js-store/package.json` (untracked)

`git diff --stat` não apresenta alterações em arquivos versionados neste estado atual. O impacto local está concentrado no cache/store de Kotlin/JS.

## 2) Foco em `kotlin-js-store/yarn.lock`

- `kotlin-js-store/yarn.lock` está presente no repositório e contém conteúdo mínimo (workspace root com metadata Yarn Berry).
- Não há diff local para `kotlin-js-store/yarn.lock` neste momento.
- Conteúdo indica lockfile gerado automaticamente por `yarn install` (mensagem no próprio arquivo).

### Classificação da redução/reescrita do lockfile

Com base no estado local atual:

- **Intencional (manual):** não há evidência.
- **Automática (ferramenta):** **provável**, pois o arquivo declara geração por `yarn install`.
- **Risco:** **moderado** se houver histórico anterior com dependências transitivas e o lockfile tiver sido reduzido fora de processo controlado; lockfile mínimo pode ocultar perda de resolução reprodutível para dependências futuras.

## 3) Reprodutibilidade

- Node: `v22.21.1`
- Yarn: `4.12.0`
- Comando de resolução associado ao lockfile (inferido do cabeçalho do lock): `yarn install`
- Hash SHA-256 de `kotlin-js-store/yarn.lock`: gerado no relatório em `reports/rmr_androidx_audit.txt`.

### Dependências críticas removidas/adicionadas

- Estado local atual não mostra diff do `yarn.lock`; portanto não há adições/remoções comparáveis no snapshot local.
- `kotlin-js-store/package.json` untracked está vazio (`{}`), consistente com lockfile sem dependências externas declaradas.

## 4) Build Kotlin/JS e impacto declarado

Sem benchmark comparativo antes/depois no estado auditado.

**impacto não medido**.

Não há base para alegar melhoria de performance nem redução de overhead AndroidX.

## 5) Conclusão objetiva

- Mudança local real: artefatos de Yarn Plug'n'Play e metadados de store Kotlin/JS não versionados.
- Lockfile: sem alteração local, mas com perfil mínimo/auto-gerado.
- Risco principal: drift de ambiente de resolução Yarn se geração não for padronizada em CI.
