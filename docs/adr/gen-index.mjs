#!/usr/bin/env node
// 루트(전역) ADR index.md 생성기 — 의존성 없는 순수 Node.
//
// SSOT 는 각 `adr-<id>-*.md` 파일의 YAML frontmatter 다. 이 스크립트가 그것을
// 읽어 index.md 를 만든다. index.md 는 생성물이므로 직접 편집하지 않는다.
//
//   node docs/adr/gen-index.mjs          # index.md 갱신
//   node docs/adr/gen-index.mjs --check  # 최신이 아니면 비정상 종료 (CI 용)
//
// frontmatter 필수 키: id(이슈/PR 번호), title, status, scope, constraint.

import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const ADR_DIR = dirname(fileURLToPath(import.meta.url));
const INDEX_PATH = join(ADR_DIR, 'index.md');
const FIELDS = ['id', 'title', 'status', 'scope', 'constraint'];

// frontmatter(첫 `---` 블록)를 flat key: value 로 파싱한다. 중첩 YAML 은 쓰지 않는다.
function parseFrontmatter(text, file) {
  const m = text.match(/^---\n([\s\S]*?)\n---/);
  if (!m) throw new Error(`${file}: frontmatter(--- 블록)가 없습니다.`);
  const data = {};
  for (const line of m[1].split('\n')) {
    if (!line.trim() || line.trimStart().startsWith('#')) continue;
    const i = line.indexOf(':');
    if (i === -1) continue;
    data[line.slice(0, i).trim()] = line.slice(i + 1).trim();
  }
  for (const f of FIELDS) {
    if (!data[f]) throw new Error(`${file}: frontmatter 에 '${f}' 가 없습니다.`);
  }
  return data;
}

const cell = (v) => String(v).replace(/\|/g, '\\|');

function build() {
  const files = readdirSync(ADR_DIR)
    .filter((f) => /^adr-\d+-.+\.md$/.test(f))
    .sort();

  const rows = files
    .map((f) => ({ file: f, ...parseFrontmatter(readFileSync(join(ADR_DIR, f), 'utf8'), f) }))
    .sort((a, b) => Number(a.id) - Number(b.id))
    .map(
      (r) =>
        `| [#${cell(r.id)}](${r.file}) | ${cell(r.title)} | ${cell(r.status)} | ${cell(r.scope)} | ${cell(r.constraint)} |`,
    );

  return [
    '# ADR 인덱스 (전역)',
    '',
    '모노레포 전역·크로스커팅 의사결정 목록. FE/BE 국한 결정은 `frontend/docs/adr/`·`backend/docs/adr/` 를 본다.',
    '',
    '> 이 파일은 **생성물**이다. 직접 편집하지 말고 `node docs/adr/gen-index.mjs` 로 갱신한다.',
    '> 내용 검색은 qmd(`qmd query "..." --collection zzol-docs`)를 1차 경로로, 이 표는 상태·카탈로그 조회에 쓴다.',
    '',
    '| 번호 | 제목 | 상태 | 영향 범위 | 핵심 제약 |',
    '| --- | --- | --- | --- | --- |',
    ...(rows.length ? rows : ['| (없음) | | | | |']),
    '',
  ].join('\n');
}

const generated = build();

if (process.argv.includes('--check')) {
  let current = '';
  try {
    current = readFileSync(INDEX_PATH, 'utf8');
  } catch {
    /* 없으면 빈 문자열과 비교 → 불일치 처리 */
  }
  if (current !== generated) {
    console.error(
      "docs/adr/index.md 가 최신이 아닙니다. 'node docs/adr/gen-index.mjs' 실행 후 커밋하세요.",
    );
    process.exit(1);
  }
  console.log('docs/adr/index.md 최신 상태 확인.');
} else {
  writeFileSync(INDEX_PATH, generated);
  console.log(`docs/adr/index.md 생성 완료 (ADR ${generated.split('\n').filter((l) => l.startsWith('| [#')).length}건).`);
}
