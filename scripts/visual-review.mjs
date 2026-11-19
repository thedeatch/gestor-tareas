import { chromium } from '@playwright/test';
import { createServer } from 'node:http';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { resolve, extname, sep } from 'node:path';
import assert from 'node:assert/strict';

// Isolated production-build review. Every API call is intercepted with synthetic fixtures.
// No real accounts, passwords, comments or application records are used or modified.
const root = resolve('dist/gestor-tareas/browser');
const output = resolve('tmp/visual-review/results');
assert(existsSync(resolve(root, 'index.html')), 'Run npm run build first.');
await mkdir(output, { recursive: true });
const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.woff2': 'font/woff2', '.svg': 'image/svg+xml' };
const server = createServer(async (request, response) => {
  try {
    const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    let file = resolve(root, '.' + pathname);
    if (file !== root && !file.startsWith(root + sep)) { response.writeHead(403).end(); return; }
    if (!extname(file)) file = resolve(root, 'index.html');
    response.setHeader('Content-Type', types[extname(file)] ?? 'application/octet-stream');
    response.end(await readFile(file));
  } catch { response.writeHead(404).end(); }
});
await new Promise(done => server.listen(0, '127.0.0.1', done));
const base = `http://127.0.0.1:${server.address().port}`;
const now = '2026-11-19T12:00:00Z';
const future = '2027-06-12T18:00:00Z';
const name = 'Comunidad de recuerdos y proyectos compartidos';
const task = { id: 7, description: 'Organizar el encuentro anual de la comunidad', completed: false, status: 'PENDING', priority: 'NORMAL', dueDate: '2027-06-10', tags: ['comunidad'], assigneeId: 1, assigneeName: 'Ana María de la Comunidad', canChangeStatus: true };
const image = { id: 3, name: 'Encuentro de la comunidad 2016.png', commentCount: 1, likeCount: 2, liked: false };
const photo = '<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800"><rect width="1200" height="800" fill="#285a74"/><circle cx="930" cy="160" r="85" fill="#ffd68a"/><path d="M0 700L350 280L650 650L900 400L1200 750V800H0Z" fill="#588a70"/><text x="60" y="100" fill="white" font-size="44">Recuerdo de la comunidad · Datos de prueba</text></svg>';
const results = [];
const errors = [];
const unexpected = new Set();
const checks = [];
const executablePath = process.env.CHROME_PATH ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe';
let browser;
try {
  browser = await chromium.launch({ executablePath, headless: true });
} catch (error) {
  await new Promise(done => server.close(done));
  throw error;
}

async function setup(viewport, member = false, authenticated = true, role = 'USER') {
  const context = await browser.newContext({ viewport, locale: 'es-CL', timezoneId: 'America/Santiago' });
  const state = { tasks: [{ ...task }], comments: [{ id: 9, authorId: 2, authorName: 'Carlos de la Comunidad', authorHasAvatar: false, content: '¡Qué buen recuerdo de ese encuentro!', createdAt: now, likeCount: 1, liked: false }], saved: true, failGoals: false, theme: 'DARK', imageCount: 1, originals: 0, thumbnails: 0 };
  if (authenticated) await context.addInitScript(role => {
    sessionStorage.setItem('gestor-tareas.token', 'test.' + btoa(JSON.stringify({ exp: 4102444800 })) + '.fixture');
    sessionStorage.setItem('gestor-tareas.email', 'visual@example.test');
    sessionStorage.setItem('gestor-tareas.name', 'Ana María de la Comunidad');
    sessionStorage.setItem('gestor-tareas.role', role);
  }, role);
  await context.route('**/api/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();
    const json = value => route.fulfill({ contentType: 'application/json', body: JSON.stringify(value) });
    const empty = () => route.fulfill({ status: 204 });
    if (path === '/api/modules') return json({ tasks: true, communities: true, messaging: true, profile: true, users: true, audit: true });
    if (path.endsWith('/preferences')) return json({ theme: state.theme, reducedMotion: false });
    if (path === '/api/auth/login') return route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ message: 'Credenciales de prueba incorrectas' }) });
    if (path === '/api/users/me/avatar') return route.fulfill({ status: 404 });
    if (path === '/api/notifications/live') {
      if (url.searchParams.has('latestId')) return; // Keep the long poll pending until navigation/context close.
      return json({ latestId: 0, count: 0 });
    }
    if (path === '/api/tasks') {
      if (method === 'POST') { const created = { ...task, id: 8, description: request.postDataJSON().description }; state.tasks.push(created); return json(created); }
      return json(state.tasks);
    }
    if (path === '/api/task-organization') return json([]);
    if (path === '/api/saved-items/state') return json({ postIds: [], imageIds: [], eventIds: state.saved ? [4] : [] });
    if (/^\/api\/saved-items\/EVENT\//.test(path)) { state.saved = method === 'PUT'; return empty(); }
    if (path === '/api/saved-items') return json({ items: state.saved ? [{ kind: 'EVENT', targetId: 4, groupId: 2, groupName: name, albumId: null, title: 'Encuentro anual de la comunidad', excerpt: 'Organizamos una actividad para compartir nuestros recuerdos.', savedAt: now }] : [], total: state.saved ? 1 : 0, page: 0, size: 20 });
    if (/\/api\/communities\/\d+$/.test(path)) return json({ id: 2, name, description: 'Un espacio para compartir, organizar tareas y recordar nuestros encuentros.', slug: 'comunidad', visibility: 'PRIVATE', currentUserRole: member ? 'MEMBER' : 'OWNER', members: [{ userId: 1, name: 'Ana María de la Comunidad', email: 'visual@example.test', role: 'OWNER', joinedAt: now }, { userId: 2, name: 'Carlos de la Comunidad', email: 'carlos@example.test', role: 'MEMBER', joinedAt: now }], hasImage: false, extendedDescription: '', rules: 'Comparte con respeto y cuida los recuerdos del grupo.', createdAt: now });
    if (path === '/api/communities') return json([{ id: 2, name, description: 'Comunidad de prueba', slug: 'comunidad', visibility: 'PRIVATE', currentUserRole: 'OWNER', memberCount: 2, hasImage: false, createdAt: now }]);
    if (/\/(invitations|discover|join-requests|activity|channels|posts|polls)$/.test(path)) return json([]);
    if (path.endsWith('/metrics')) return json({ days: 30, from: now, until: future, counts: { members: 2, newMembers: 1, posts: 0, comments: 1, events: 1, registrations: 1, participants: 1 } });
    if (path.endsWith('/goals')) {
      if (state.failGoals) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Error temporal de prueba' }) });
      return json({ canManage: !member, tasks: [{ ...task, canChangeStatus: !member }], goals: [{ id: 1, title: 'Preparar el próximo encuentro de la comunidad', description: 'Coordinar las actividades y reunir nuestros mejores recuerdos.', taskIds: [7], dueDate: '2027-06-10', createdAt: now, authorName: 'Ana María', totalTasks: 1, completedTasks: 0, progress: 0 }] });
    }
    if (path.endsWith('/events')) return json([{ id: 4, title: 'Encuentro anual de la comunidad', description: 'Una actividad para volver a encontrarnos y compartir los recuerdos.', startsAt: future, participantCount: 12, registered: false }]);
    if (path.endsWith('/memories')) return json({ items: [], total: 0, page: 0, size: 20 });
    if (path.endsWith('/search')) return json({ items: [{ kind: 'TASK', id: 7, title: task.description, excerpt: 'Asignada a Ana María' }], total: 1, page: 0, size: 20, query: url.searchParams.get('q') });
    if (path.endsWith('/albums')) return json([{ id: 1, title: 'Encuentro de 2016', description: 'Nuestros recuerdos', imageCount: 1, coverImageId: 3 }]);
    if (path.endsWith('/images')) return json(Array.from({ length: state.imageCount }, (_, index) => ({ ...image, id: index + 3 })));
    if (/\/images\/\d+\/thumbnail$/.test(path)) { state.thumbnails++; return route.fulfill({ contentType: 'image/svg+xml', body: photo }); }
    if (/\/images\/\d+$/.test(path)) { state.originals++; return route.fulfill({ contentType: 'image/svg+xml', body: photo }); }
    if (path.endsWith('/images/3/comments')) {
      if (method === 'POST') { const comment = { ...state.comments[0], id: 10, authorId: 1, authorName: 'Ana María', content: request.postDataJSON().content }; state.comments.push(comment); return json(comment); }
      return json(state.comments);
    }
    if (path.endsWith('/registration')) return empty();
    if (path === '/api/users/2/profile') return json({ id: 2, name: 'Carlos de la Comunidad', bio: 'Compartiendo recuerdos desde 2010.', hasAvatar: false, joinedAt: now, communities: [] });
    if (path === '/api/users/me/blocks/2') return json({ canBlock: true, blocked: false });
    unexpected.add(method + ' ' + path);
    return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Unconfigured visual fixture: ' + path }) });
  });
  const page = await context.newPage();
  page.on('pageerror', error => errors.push(error.message));
  return { context, page, state };
}

async function capture(page, label) {
  await page.locator('app-navbar').waitFor();
  await page.evaluate(() => document.fonts.ready);
  const geometry = await page.evaluate(() => ({ viewport: innerWidth, documentWidth: document.documentElement.scrollWidth, overflow: [...document.querySelectorAll('main *')].filter(el => { const rect = el.getBoundingClientRect(); return rect.width > 0 && rect.right > innerWidth + 1 && getComputedStyle(el).position !== 'fixed'; }).slice(0, 8).map(el => ({ tag: el.tagName, class: el.className, text: el.textContent.slice(0, 70) })) }));
  results.push({ label, ...geometry });
  await page.screenshot({ path: resolve(output, label + '.png'), animations: 'disabled', fullPage: await page.locator('mat-dialog-container').count() === 0 });
}

try {
  const widths = (process.env.VISUAL_WIDTHS ?? '1440,1280,1024,390,320').split(',').map(Number);
  for (const viewport of widths.map(width => ({ width, height: width < 500 ? 844 : 1000 }))) {
    const { page, context, state } = await setup(viewport);
    for (const [label, path, ready] of [
      ['inicio', '/inicio', 'app-dashboard-page'],
      ['tareas', '/tareas', 'app-task-form'],
      ['grupos', '/grupos', '.community-card'],
      ['resumen', '/grupos/2', 'app-community-metrics dl'],
      ['objetivos', '/grupos/2?seccion=goals', 'app-community-goals article'],
      ['miembros', '/grupos/2?seccion=members', '.member-row'],
      ['eventos', '/grupos/2?seccion=events', '.event-item'],
      ['guardados', '/guardados', '.saved-grid mat-card'],
      ['perfil', '/usuarios/2', 'app-public-profile-page h1']
    ]) {
      await page.goto(base + path);
      await page.locator(ready).first().waitFor({ timeout: 12000 });
      await capture(page, `${viewport.width}-${label}`);
    }
    await page.goto(base + '/grupos/2?seccion=events&album=1');
    await page.locator('.image-tile .image-open').first().scrollIntoViewIfNeeded();
    await page.locator('.image-tile img').first().waitFor({ timeout: 12000 });
    assert.equal(state.originals, 0, 'Album opening must not download full-size images.');
    await page.locator('.image-tile').first().click();
    await page.getByRole('button', { name: 'Cerrar visor' }).waitFor();
    await capture(page, `${viewport.width}-visor`);
    const initialHeight = (await page.locator('.image-button').boundingBox()).height;
    await page.getByRole('button', { name: 'Ocultar comentarios', exact: true }).click();
    await page.locator('mat-dialog-content.image-only').waitFor();
    assert((await page.locator('.image-button').boundingBox()).height >= initialHeight);
    await capture(page, `${viewport.width}-foto-ampliada`);
    await page.getByRole('button', { name: 'Ver comentarios', exact: true }).click();
    checks.push(`${viewport.width}: ampliar foto ocultando comentarios y volver a mostrarlos`);
    const commentBox = page.locator('app-gallery-image-comments textarea');
    if (await commentBox.count() === 0) await page.getByRole('button', { name: 'Ver comentarios', exact: true }).click();
    await commentBox.fill('Comentario de prueba visual');
    await page.getByRole('button', { name: 'Comentar', exact: true }).click();
    await page.getByText('Comentario de prueba visual', { exact: true }).waitFor();
    assert.equal(state.originals, 1, 'Only the selected original should be downloaded, including after comment updates.');
    checks.push(`${viewport.width}: publicar comentario de foto y mostrarlo`);
    await capture(page, `${viewport.width}-comentarios`);
    await page.getByRole('button', { name: 'Cerrar visor' }).click();
    await page.locator('mat-dialog-container').waitFor({ state: 'hidden' });
    await page.goto(base + '/tareas');
    await page.getByRole('textbox', { name: 'Nueva tarea' }).fill('Preparar recuerdos para el encuentro');
    await page.getByRole('button', { name: 'Agregar', exact: true }).click();
    await page.getByText('Preparar recuerdos para el encuentro', { exact: true }).waitFor();
    checks.push(`${viewport.width}: crear tarea y mostrarla en lista`);
    for (const [view, selector] of [['Kanban', 'app-task-kanban'], ['Calendario', 'app-task-calendar'], ['Estadísticas', 'app-task-productivity']]) {
      await page.getByRole('button', { name: 'Vista ' + view, exact: true }).click();
      await page.locator(selector).waitFor();
      await capture(page, `${viewport.width}-${view.toLowerCase()}`);
    }
    checks.push(`${viewport.width}: cambiar entre Kanban, calendario y estadísticas`);
    await page.goto(base + '/grupos/2?seccion=goals');
    await page.getByRole('button', { name: 'Nuevo objetivo', exact: true }).click();
    await page.locator('app-community-goals form').waitFor();
    await capture(page, `${viewport.width}-formulario-objetivo`);
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
    await page.goto(base + '/grupos/2?seccion=search');
    await page.getByRole('textbox', { name: 'Buscar en este grupo' }).fill('encuentro');
    await page.locator('app-community-search button[type=submit]').click();
    await page.locator('.search-results a').waitFor();
    assert((await page.locator('.search-results a').getAttribute('href')).includes('tarea=7'));
    await capture(page, `${viewport.width}-busqueda`);
    checks.push(`${viewport.width}: buscar dentro del grupo y enlazar una tarea`);
    await page.goto(base + '/guardados');
    await page.getByRole('button', { name: 'Quitar de mis guardados' }).click();
    await page.getByText('Aún no tienes guardados', { exact: true }).waitFor();
    checks.push(`${viewport.width}: quitar evento de guardados y mostrar estado vacío`);
    if (viewport.width <= 1360) {
      await page.getByRole('button', { name: 'Abrir menú de navegación' }).click();
      await page.getByRole('menu').waitFor();
      await capture(page, `${viewport.width}-menu`);
      await page.getByRole('menuitem', { name: 'Grupos', exact: false }).click();
      await page.locator('.community-card').first().waitFor();
      checks.push(`${viewport.width}: navegar con el menú compacto`);
    }
    await context.close();
  }
  for (const width of [1440, 390, 320]) {
    const viewport = { width, height: 900 };
    const { context, page } = await setup(viewport, true);
    await page.goto(base + '/grupos/2?seccion=goals');
    await page.locator('app-community-goals article').waitFor();
    assert.equal(await page.getByRole('button', { name: 'Nuevo objetivo', exact: true }).count(), 0);
    assert.equal(await page.locator('app-community-goals article select').count(), 0);
    checks.push(`${width}: miembro sin controles de administración de objetivos`);
    await capture(page, `${width}-miembro`);
    await context.close();
    const guest = await setup(viewport, false, false);
    for (const [label, path] of [['login', '/login'], ['registro', '/registro'], ['recuperacion', '/olvide-contrasena']]) {
      await guest.page.goto(base + path);
      await guest.page.locator('.login-card').waitFor();
      await capture(guest.page, `${width}-${label}`);
    }
    await guest.page.goto(base + '/login');
    await guest.page.locator('input[name=email]').fill('visual@example.test');
    await guest.page.locator('input[name=password]').fill('SyntheticTest123');
    await guest.page.locator('button[type=submit]').click();
    await guest.page.locator('[role=alert]').waitFor();
    checks.push(`${width}: mostrar error de credenciales sin navegar`);
    await capture(guest.page, `${width}-error-login`);
    await guest.context.close();
  }
  const recovery = await setup({ width: 390, height: 844 });
  recovery.state.failGoals = true;
  await recovery.page.goto(base + '/grupos/2?seccion=goals');
  await recovery.page.locator('app-community-goals [role=alert]').waitFor();
  await capture(recovery.page, '390-error-objetivos');
  recovery.state.failGoals = false;
  await recovery.page.getByRole('button', { name: 'Actualizar', exact: true }).click();
  await recovery.page.locator('app-community-goals article').waitFor();
  checks.push('390: recuperar objetivos tras un error 503');
  recovery.state.theme = 'LIGHT';
  await recovery.page.goto(base + '/grupos/2?seccion=goals');
  await recovery.page.locator('app-community-goals article').waitFor();
  await capture(recovery.page, '390-tema-claro');
  await recovery.context.close();
  const large = await setup({ width: 390, height: 844 });
  large.state.imageCount = 120;
  await large.page.goto(base + '/grupos/2?seccion=events&album=1');
  await large.page.locator('.image-tile').first().waitFor();
  assert.equal(await large.page.locator('.image-tile').count(), 24);
  assert.equal(large.state.originals, 0);
  await large.page.getByRole('button', { name: 'Ver más fotos', exact: true }).click();
  assert.equal(await large.page.locator('.image-tile').count(), 48);
  await capture(large.page, '390-album-120-fotos');
  checks.push('390: álbum de 120 fotos muestra bloques de 24 sin descargar originales');
  await large.context.close();
  for (const width of [1440, 1280]) {
    const admin = await setup({ width, height: 900 }, false, true, 'ADMIN');
    await admin.page.goto(base + '/inicio');
    await admin.page.locator('app-dashboard-page').waitFor();
    await capture(admin.page, `${width}-administrador`);
    await admin.context.close();
  }
} finally {
  await writeFile(resolve(output, 'report.json'), JSON.stringify({ results, checks, errors, unexpected: [...unexpected] }, null, 2));
  await browser.close();
  await new Promise(done => server.close(done));
}
console.log(JSON.stringify({ captures: results.length, checks, overflow: results.filter(item => item.documentWidth > item.viewport + 1), errors, unexpected: [...unexpected] }, null, 2));
assert.equal(errors.length, 0, 'Browser runtime errors occurred.');
assert.equal(unexpected.size, 0, 'Some API fixtures were not configured.');
assert.equal(results.filter(item => item.documentWidth > item.viewport + 1).length, 0, 'Some views overflow the viewport.');
