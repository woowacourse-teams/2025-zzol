/**
 * 화면을 띄워 놓고 눈으로만 잡히던 것들을 기계로 잡는다.
 *
 * 세 가지를 센다.
 *   1. 죽은 여백  - 카드 안 내용이 카드를 못 채우는 자리. 가운데 정렬이면 양쪽이 같이
 *                  비므로 한쪽만 비는 것(비대칭)만 사고로 본다.
 *   2. 줄바꿈     - 배지나 짧은 이름처럼 접히면 안 되는 글자가 두 줄이 된 자리.
 *   3. 조각 색    - 한 그림 안의 조각 색이 서로 구분되지 않는 자리.
 *
 * 이 셋은 전부 <b>실제로 지적받은 뒤에</b> 추가됐다. 리뷰에서 "여백이 많다", "구분이 안
 * 간다"를 사람이 매번 짚어 주는 대신, 화면을 고칠 때마다 이 검사를 돌린다.
 *
 * 쓰는 법:
 *   0. playwright 가 필요하다. 의존성에는 넣지 않았다 - 브라우저 바이너리까지 받아야 해서
 *      이 검사를 안 쓰는 사람의 설치까지 무거워진다. `npx playwright install chromium`.
 *   1. 백엔드와 프론트를 로컬에 띄운다 (`npm run dev`).
 *   2. 관리자 토큰을 준비한다. 로그인 후 localStorage 의 `zzol-admin-token` 값이다.
 *   3. `ZZOL_ADMIN_TOKEN=... npm run ui-check`
 *
 * 환경 변수:
 *   ZZOL_ADMIN_TOKEN  필수. 관리자 JWT
 *   BASE_URL          기본 http://localhost:5173
 *   WIDTH             기본 1440. 좁은 폭에서도 한 번 더 돌려 본다
 *   GAP_LIMIT         기본 40(px). 이보다 크게 빈 자리를 사고로 본다
 */
// playwright 를 의존성에 넣지 않는다. 브라우저 바이너리까지 받아야 해서 이 검사를 안 쓰는
// 사람의 설치까지 무거워진다. 없으면 안내만 하고 조용히 빠진다.
let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch {
  console.error('playwright 가 없습니다. `npx playwright install chromium` 후 다시 실행하세요.');
  process.exit(2);
}

const TOKEN = process.env.ZZOL_ADMIN_TOKEN;
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:5173';
const WIDTH = Number(process.env.WIDTH ?? 1440);
const GAP_LIMIT = Number(process.env.GAP_LIMIT ?? 40);

if (!TOKEN) {
  console.error(
    'ZZOL_ADMIN_TOKEN 이 필요합니다. 로그인 후 localStorage 의 zzol-admin-token 값입니다.',
  );
  process.exit(2);
}

/** 패널과 탭까지 한 번씩 연다. 탭 안쪽은 열어 보지 않으면 검사에서 통째로 빠진다. */
const ROUTES = [
  '/',
  '/reports',
  '/profanity',
  '/ip-blocks',
  '/rooms',
  '/users',
  '/patch-notes',
  '/zzolbot?tab=chat',
  '/zzolbot?tab=monitor',
  '/zzolbot?tab=eval',
  '/admins',
  '/system',
  '/audit-logs',
];

function audit(gapLimit) {
  const out = [];

  for (const card of document.querySelectorAll('div.rounded-lg.border')) {
    // 표는 폭을 다 쓰는 것이 당연하다.
    if (card.querySelector('table')) continue;
    const box = card.getBoundingClientRect();
    if (box.height < 60) continue;

    // 머리글은 왼쪽 정렬이고 본문은 가운데 정렬인 카드가 있다. 둘을 한 상자로 묶으면
    // 멀쩡한 배치가 "오른쪽이 비었다"로 잡힌다. 본문만 잰다.
    const header = card.querySelector('h2')?.closest('div')?.parentElement;
    let bottom = -Infinity;
    let right = -Infinity;
    let left = Infinity;
    for (const el of card.querySelectorAll('*')) {
      if (header && header.contains(el)) continue;
      if (el.children.length && el.tagName !== 'svg') continue;
      const r = el.getBoundingClientRect();
      if (r.width === 0 || r.height === 0) continue;
      bottom = Math.max(bottom, r.bottom);
      right = Math.max(right, r.right);
      left = Math.min(left, r.left);
    }
    if (bottom === -Infinity) continue;

    const title = card.querySelector('h2')?.textContent?.trim() ?? '(제목없음)';
    const top = header ? header.getBoundingClientRect().bottom : box.top + 20;
    const bottomGap = Math.round(box.bottom - bottom - 20);
    const topGap = Math.round(top - box.top);
    const rightGap = Math.round(box.right - right - 20);
    const leftGap = Math.round(left - box.left - 20);

    if (rightGap > gapLimit && Math.abs(rightGap - leftGap) > gapLimit) {
      out.push(`${title} 오른쪽 ${rightGap}px 빔 (왼쪽 ${leftGap}px)`);
    }
    if (bottomGap > gapLimit && Math.abs(bottomGap - topGap) > gapLimit) {
      out.push(`${title} 아래 ${bottomGap}px 빔 (위 ${topGap}px)`);
    }
    // 가운데 정렬이면 위아래가 같이 빈다. 그건 배치이지 사고가 아니다 - 다만 <b>비는
    // 양이 크면</b> 배치가 아니다. 도넛 하나가 600px 짜리 카드 한가운데 떠 있던 적이 있고,
    // 위아래가 똑같이 비어 있어서 대칭 규칙만으로는 잡히지 않았다.
    if (bottomGap + Math.max(topGap - 20, 0) > gapLimit * 3) {
      out.push(`${title} 내용이 카드를 못 채움 (위 ${topGap}px, 아래 ${bottomGap}px)`);
    }
  }

  // 위아래로 쌓은 칸 안에서 목록이 폭을 못 쓰는 자리.
  //
  // 위의 여백 검사는 카드 안 모든 리프를 합쳐 상자를 만든다. 목록이 좁아도 그 아래 설명
  // 문단이 폭을 다 쓰고 있으면 상자는 꽉 찬 것으로 나온다. 실제로 도넛 범례가 좌우로
  // 100px 씩 비어 있는데 검사가 통과한 적이 있다.
  //
  // 세로로 쌓은 칸(flex-direction: column)만 본다. 그 안에서 가로는 남는 방향이라 목록이
  // 부모만큼 넓어야 맞다. 좌우로 놓은 칸에서는 목록이 남은 자리만 쓰는 것이 정상이고,
  // 격자 칸도 마찬가지다.
  for (const list of document.querySelectorAll('div.rounded-lg.border ul')) {
    const parent = list.parentElement;
    if (!parent) continue;
    const parentStyle = getComputedStyle(parent);
    if (parentStyle.display !== 'flex' || !parentStyle.flexDirection.startsWith('column')) continue;
    const short = parent.clientWidth - list.getBoundingClientRect().width;
    if (short > gapLimit) {
      const card = list.closest('div.rounded-lg.border');
      const title = card?.querySelector('h2')?.textContent?.trim() ?? '화면';
      out.push(`목록이 폭을 못 씀: ${title} (${Math.round(short)}px 모자람)`);
    }
  }

  // 높이로 재지 않는다. 배지와 버튼은 안쪽 여백이 있어 한 줄이어도 글자 높이의 두 배가
  // 된다. 글자에 Range 를 걸어 줄 상자가 몇 개인지 센다.
  for (const node of document.querySelectorAll('span, button, a, th, td')) {
    if (node.childNodes.length !== 1 || node.firstChild.nodeType !== Node.TEXT_NODE) continue;
    const text = node.textContent.trim();
    if (!text || text.length > 20) continue;
    const style = getComputedStyle(node);
    if (style.webkitLineClamp !== 'none') continue; // 일부러 여러 줄로 두는 자리
    if (style.whiteSpace === 'nowrap' || style.whiteSpace.startsWith('pre')) continue;
    const range = document.createRange();
    range.selectNodeContents(node);
    const lines = range.getClientRects().length;
    if (lines > 1) {
      const where =
        node.closest('div.rounded-lg.border')?.querySelector('h2')?.textContent?.trim() ?? '화면';
      out.push(`줄바꿈: "${text}" (${lines}줄, ${where})`);
    }
  }

  // 조각 색이 서로 구분되는지. 색 하나로 조각을 나누는 그림에서 값이 비슷한 두 조각이
  // 같은 색으로 보이면 그림이 거짓말을 한다.
  for (const card of document.querySelectorAll('div.rounded-lg.border')) {
    const colors = [...card.querySelectorAll('[data-swatch]')]
      .map((el) =>
        (getComputedStyle(el).backgroundColor.match(/[\d.]+/g) ?? []).slice(0, 3).map(Number),
      )
      .filter((rgb) => rgb.length === 3);
    for (let i = 1; i < colors.length; i++) {
      const distance = colors[i].reduce(
        (sum, value, axis) => sum + Math.abs(value - colors[i - 1][axis]),
        0,
      );
      if (distance < 60) {
        const title = card.querySelector('h2')?.textContent?.trim() ?? '화면';
        out.push(
          `조각 색이 붙음: ${title} (${i}번째와 ${i + 1}번째, 거리 ${Math.round(distance)})`,
        );
      }
    }
  }

  return out;
}

const browser = await chromium.launch();
const context = await browser.newContext({ viewport: { width: WIDTH, height: 1000 } });
await context.addInitScript((token) => localStorage.setItem('zzol-admin-token', token), TOKEN);
const page = await context.newPage();

let problems = 0;
for (const route of ROUTES) {
  await page.goto(BASE_URL + route, { waitUntil: 'networkidle' });
  // 차트가 부모 크기를 재고 다시 그릴 시간을 준다.
  await page.waitForTimeout(1200);
  // 검사가 빈 화면을 통과시키지 않게 한다.
  //
  // 토큰이 만료돼 로그인 화면이 떠 있는 것을 모르고 "모든 화면 통과"를 세 번 보고한 적이
  // 있다. 아무것도 안 그려진 화면에는 잡을 문제도 없다. 카드가 하나도 없으면 그건 통과가
  // 아니라 검사가 못 돈 것이다.
  const cards = await page.evaluate(
    () => document.querySelectorAll('div.rounded-lg.border').length,
  );
  if (cards === 0) {
    console.error(`\n${route}: 카드가 하나도 없습니다. 토큰이 만료됐거나 화면이 깨졌습니다.`);
    await browser.close();
    process.exit(2);
  }

  const rows = await page.evaluate(audit, GAP_LIMIT);
  if (rows.length > 0) {
    problems += rows.length;
    console.log(`\n== ${route}`);
    rows.forEach((row) => console.log('  ' + row));
  }
}
await browser.close();

console.log(
  problems === 0 ? `\n폭 ${WIDTH}px: 모든 화면 통과` : `\n폭 ${WIDTH}px: 문제 ${problems}건`,
);
process.exit(problems === 0 ? 0 : 1);
