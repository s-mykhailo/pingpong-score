/**
 * Ping Pong Score - backend на Google Apps Script (v2)
 * Групи, авторизація по коду, кастомні матчі та пресети.
 * MASTER_KEY лежить у властивостях скрипту, не в коді.
 * Усі запити: POST з Content-Type: text/plain, тіло = JSON.
 */

const SHEET_GROUPS = 'Groups';
const SHEET_SESSIONS = 'Sessions';

const GROUP_HEADERS = ['id', 'name', 'salt', 'joinHash', 'adminHash', 'createdAt'];
const SESSION_HEADERS = ['token', 'groupId', 'role', 'createdAt', 'lastUsed'];
const PLAYER_HEADERS = ['id', 'name', 'createdAt', 'deleted'];
const MATCH_HEADERS = ['id', 'playedAt', 'player1', 'player2', 'sets', 'winner', 'rules', 'presetName', 'createdAt', 'deleted'];
const PRESET_HEADERS = ['id', 'name', 'rules', 'createdAt', 'deleted'];

const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const JOIN_FAIL_LIMIT = 20;
const JOIN_FAIL_WINDOW = 600;

function setup() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  ensureSheet_(ss, SHEET_GROUPS, GROUP_HEADERS);
  ensureSheet_(ss, SHEET_SESSIONS, SESSION_HEADERS);
}

function createGroupManual(name) {
  const key = PropertiesService.getScriptProperties().getProperty('MASTER_KEY');
  Logger.log(JSON.stringify(createGroup_(key, name || 'Офіс'), null, 2));
}

function doGet() {
  return json_({ ok: true, service: 'pingpong', version: 2 });
}

function doPost(e) {
  let body;
  try {
    body = JSON.parse(e.postData.contents);
  } catch (_) {
    return json_({ ok: false, error: 'bad json' });
  }

  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    if (body.action === 'createGroup') return json_({ ok: true, ...createGroup_(body.masterKey, body.name) });
    if (body.action === 'join') return json_({ ok: true, ...join_(body.code) });

    const s = auth_(body.token);
    const g = s.groupId;

    switch (body.action) {
      case 'logout':
        deleteSession_(body.token);
        return json_({ ok: true });
      case 'getAll':
        return json_({
          ok: true,
          group: { id: g, name: groupById_(g).name },
          role: s.role,
          players: readPlayers_(g),
          matches: readMatches_(g),
          presets: readPresets_(g),
        });
      case 'addPlayer':
        return json_({ ok: true, player: addPlayer_(g, body.name, body.id) });
      case 'deletePlayer':
        requireAdmin_(s);
        softDelete_(tab_(g, 'players'), body.id, PLAYER_HEADERS);
        return json_({ ok: true });
      case 'addMatch':
        return json_({ ok: true, match: addMatch_(g, body.match) });
      case 'deleteMatch':
        requireAdmin_(s);
        softDelete_(tab_(g, 'matches'), body.id, MATCH_HEADERS);
        return json_({ ok: true });
      case 'savePreset':
        return json_({ ok: true, preset: savePreset_(g, body.preset) });
      case 'deletePreset':
        softDelete_(tab_(g, 'presets'), body.id, PRESET_HEADERS);
        return json_({ ok: true });
      default:
        throw new Error('unknown action');
    }
  } catch (err) {
    return json_({ ok: false, error: String(err.message || err) });
  } finally {
    try { lock.releaseLock(); } catch (_) {}
  }
}

function createGroup_(masterKey, name) {
  const expected = PropertiesService.getScriptProperties().getProperty('MASTER_KEY');
  if (!expected) throw new Error('MASTER_KEY not set');
  if (masterKey !== expected) throw new Error('unauthorized');

  name = cleanName_(name);
  const id = randomCode_(6).toLowerCase();
  const salt = Utilities.getUuid();
  const joinCode = randomCode_(10);
  const adminCode = randomCode_(14);

  sheet_(SHEET_GROUPS).appendRow([id, name, salt, hash_(salt, joinCode), hash_(salt, adminCode), now_()]);

  const ss = SpreadsheetApp.getActiveSpreadsheet();
  ensureSheet_(ss, tabName_(id, 'players'), PLAYER_HEADERS);
  ensureSheet_(ss, tabName_(id, 'matches'), MATCH_HEADERS);
  ensureSheet_(ss, tabName_(id, 'presets'), PRESET_HEADERS);
  seedPresets_(id);

  return { groupId: id, name, joinCode: fmtCode_(joinCode), adminCode: fmtCode_(adminCode) };
}

function join_(rawCode) {
  const cache = CacheService.getScriptCache();
  const fails = Number(cache.get('joinFails') || 0);
  if (fails >= JOIN_FAIL_LIMIT) throw new Error('too many attempts, try later');

  const code = String(rawCode || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (code.length < 10) {
    cache.put('joinFails', String(fails + 1), JOIN_FAIL_WINDOW);
    throw new Error('invalid code');
  }

  const groups = readRows_(SHEET_GROUPS, GROUP_HEADERS);
  for (const gr of groups) {
    const h = hash_(gr.salt, code);
    let role = null;
    if (h === gr.adminHash) role = 'admin';
    else if (h === gr.joinHash) role = 'member';
    if (role) {
      const token = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '');
      sheet_(SHEET_SESSIONS).appendRow([token, String(gr.id), role, now_(), now_()]);
      return { token, role, group: { id: String(gr.id), name: String(gr.name) } };
    }
  }

  cache.put('joinFails', String(fails + 1), JOIN_FAIL_WINDOW);
  throw new Error('invalid code');
}

function auth_(token) {
  if (!token || String(token).length !== 64) throw new Error('unauthorized');
  const cache = CacheService.getScriptCache();
  const cached = cache.get('s_' + token);
  if (cached) return JSON.parse(cached);

  const sh = sheet_(SHEET_SESSIONS);
  const values = sh.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (values[i][0] === token) {
      sh.getRange(i + 1, 5).setValue(now_());
      const s = { groupId: String(values[i][1]), role: String(values[i][2]) };
      cache.put('s_' + token, JSON.stringify(s), 600);
      return s;
    }
  }
  throw new Error('unauthorized');
}

function deleteSession_(token) {
  const sh = sheet_(SHEET_SESSIONS);
  const values = sh.getDataRange().getValues();
  for (let i = values.length - 1; i >= 1; i--) {
    if (values[i][0] === token) sh.deleteRow(i + 1);
  }
  CacheService.getScriptCache().remove('s_' + token);
}

function requireAdmin_(s) {
  if (s.role !== 'admin') throw new Error('admin only');
}

function groupById_(id) {
  const gr = readRows_(SHEET_GROUPS, GROUP_HEADERS).find(x => String(x.id) === id);
  if (!gr) throw new Error('group not found');
  return gr;
}

function readPlayers_(g) {
  return readRows_(tabName_(g, 'players'), PLAYER_HEADERS)
    .filter(notDeleted_)
    .map(p => ({ id: String(p.id), name: String(p.name), createdAt: p.createdAt }));
}

function addPlayer_(g, name, id) {
  name = cleanName_(name);
  const pid = String(id || Utilities.getUuid());
  const rows = readRows_(tabName_(g, 'players'), PLAYER_HEADERS);
  const dup = rows.find(p => String(p.id) === pid);
  if (dup) return { id: pid, name: String(dup.name) };
  if (rows.filter(notDeleted_).some(p => String(p.name).toLowerCase() === name.toLowerCase())) {
    throw new Error('player exists');
  }
  tab_(g, 'players').appendRow([pid, name, now_(), false]);
  return { id: pid, name };
}

function validateRules_(r) {
  r = r || {};
  const out = {
    pointsToWin: int_(r.pointsToWin, 11),
    winBy: int_(r.winBy, 2),
    bestOf: int_(r.bestOf, 3),
    serveEvery: int_(r.serveEvery, 2),
    deuceServeEvery: int_(r.deuceServeEvery, 1),
  };
  if (out.pointsToWin < 1 || out.pointsToWin > 99) throw new Error('bad pointsToWin');
  if (out.winBy !== 1 && out.winBy !== 2) throw new Error('bad winBy');
  if (out.bestOf < 1 || out.bestOf > 9 || out.bestOf % 2 === 0) throw new Error('bad bestOf');
  if (out.serveEvery < 1 || out.serveEvery > 10) throw new Error('bad serveEvery');
  if (out.deuceServeEvery < 1 || out.deuceServeEvery > 10) throw new Error('bad deuceServeEvery');
  return out;
}

function seedPresets_(g) {
  const sh = tab_(g, 'presets');
  const t = now_();
  sh.appendRow([Utilities.getUuid(), 'Класика до 11, BO3', JSON.stringify(validateRules_({ pointsToWin: 11, bestOf: 3 })), t, false]);
  sh.appendRow([Utilities.getUuid(), 'Класика до 11, BO5', JSON.stringify(validateRules_({ pointsToWin: 11, bestOf: 5 })), t, false]);
  sh.appendRow([Utilities.getUuid(), 'Старий до 21', JSON.stringify(validateRules_({ pointsToWin: 21, bestOf: 3, serveEvery: 5 })), t, false]);
  sh.appendRow([Utilities.getUuid(), 'Швидкий сет', JSON.stringify(validateRules_({ pointsToWin: 11, bestOf: 1 })), t, false]);
}

function readPresets_(g) {
  return readRows_(tabName_(g, 'presets'), PRESET_HEADERS)
    .filter(notDeleted_)
    .map(p => ({ id: String(p.id), name: String(p.name), rules: safeParse_(p.rules, null) }));
}

function savePreset_(g, p) {
  if (!p) throw new Error('preset required');
  const name = cleanName_(p.name);
  const rules = validateRules_(p.rules);
  const id = String(p.id || Utilities.getUuid());

  const sh = tab_(g, 'presets');
  const values = sh.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (String(values[i][0]) === id) {
      sh.getRange(i + 1, 2, 1, 2).setValues([[name, JSON.stringify(rules)]]);
      return { id, name, rules };
    }
  }
  sh.appendRow([id, name, JSON.stringify(rules), now_(), false]);
  return { id, name, rules };
}

function readMatches_(g) {
  return readRows_(tabName_(g, 'matches'), MATCH_HEADERS)
    .filter(notDeleted_)
    .map(m => ({
      id: String(m.id),
      playedAt: m.playedAt,
      player1: String(m.player1),
      player2: String(m.player2),
      sets: safeParse_(m.sets, []),
      winner: String(m.winner),
      rules: safeParse_(m.rules, null),
      presetName: String(m.presetName || ''),
    }));
}

function addMatch_(g, m) {
  if (!m || !m.id) throw new Error('match.id required');
  if (!m.player1 || !m.player2 || m.player1 === m.player2) throw new Error('bad players');
  if (!Array.isArray(m.sets) || m.sets.length === 0) throw new Error('sets required');

  const players = readPlayers_(g).map(p => p.id);
  if (players.indexOf(String(m.player1)) === -1 || players.indexOf(String(m.player2)) === -1) {
    throw new Error('unknown player');
  }

  const rules = validateRules_(m.rules);
  const need = Math.ceil(rules.bestOf / 2);
  let w1 = 0, w2 = 0;
  m.sets.forEach((s, i) => {
    if (Math.max(w1, w2) === need) throw new Error('extra set after match end');
    const a = Number(s[0]), b = Number(s[1]);
    if (!validSet_(a, b, rules)) throw new Error('bad set ' + (i + 1) + ': ' + a + ':' + b);
    if (a > b) w1++; else w2++;
  });
  if (Math.max(w1, w2) !== need) throw new Error('match not finished');
  const winner = w1 > w2 ? String(m.player1) : String(m.player2);

  const rows = readRows_(tabName_(g, 'matches'), MATCH_HEADERS);
  if (rows.some(x => String(x.id) === String(m.id))) return { id: String(m.id), duplicate: true };

  tab_(g, 'matches').appendRow([
    String(m.id),
    m.playedAt || now_(),
    String(m.player1),
    String(m.player2),
    JSON.stringify(m.sets),
    winner,
    JSON.stringify(rules),
    String(m.presetName || '').slice(0, 40),
    now_(),
    false,
  ]);
  return { id: String(m.id), winner };
}

function validSet_(a, b, r) {
  if (!Number.isInteger(a) || !Number.isInteger(b) || a < 0 || b < 0 || a === b) return false;
  const hi = Math.max(a, b), lo = Math.min(a, b), P = r.pointsToWin;
  if (hi < P) return false;
  if (r.winBy === 1) return hi === P;
  if (hi === P) return lo <= P - 2;
  return hi - lo === 2 && lo >= P - 1;
}

function tabName_(g, kind) { return 'g_' + g + '_' + kind; }

function tab_(g, kind) { return sheet_(tabName_(g, kind)); }

function ensureSheet_(ss, name, headers) {
  let sh = ss.getSheetByName(name);
  if (!sh) sh = ss.insertSheet(name);
  if (sh.getLastRow() === 0) {
    sh.appendRow(headers);
    sh.setFrozenRows(1);
    sh.getRange(1, 1, 1, headers.length).setFontWeight('bold');
  }
  return sh;
}

function sheet_(name) {
  const sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(name);
  if (!sh) throw new Error('sheet missing: ' + name);
  return sh;
}

function readRows_(name, headers) {
  const values = sheet_(name).getDataRange().getValues();
  const out = [];
  for (let i = 1; i < values.length; i++) {
    if (values[i][0] === '') continue;
    const row = {};
    headers.forEach((h, j) => { row[h] = values[i][j]; });
    out.push(row);
  }
  return out;
}

function softDelete_(sh, id, headers) {
  const col = headers.indexOf('deleted') + 1;
  const values = sh.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (String(values[i][0]) === String(id)) {
      sh.getRange(i + 1, col).setValue(true);
      return;
    }
  }
  throw new Error('not found');
}

function notDeleted_(x) { return x.deleted !== true && x.deleted !== 'TRUE'; }

function cleanName_(name) {
  name = String(name || '').trim();
  if (!name) throw new Error('name required');
  if (name.length > 40) throw new Error('name too long');
  return name;
}

function randomCode_(len) {
  const bytes = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, Utilities.getUuid() + Utilities.getUuid());
  let s = '';
  for (let i = 0; i < len; i++) s += CODE_ALPHABET[(bytes[i] + 256) % CODE_ALPHABET.length];
  return s;
}

function fmtCode_(c) { return c.match(/.{1,5}/g).join('-'); }

function hash_(salt, code) {
  const raw = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, salt + ':' + code);
  return raw.map(b => ('0' + ((b + 256) % 256).toString(16)).slice(-2)).join('');
}

function int_(v, def) {
  const n = v === undefined || v === null || v === '' ? def : Number(v);
  if (!Number.isInteger(n)) throw new Error('not integer: ' + v);
  return n;
}

function now_() { return new Date().toISOString(); }

function safeParse_(s, fallback) {
  try { return JSON.parse(s); } catch (_) { return fallback; }
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
