import { addPlayer, savePreset, NetworkError } from '../api/client';
import { getData, subscribe, mergePlayer, mergePresetLocal } from '../store';
import { getCurrentRoomId } from '../room';
import { navigate } from '../router';
import { setLiveMatch } from '../match/liveMatch';
import { createLiveMatch } from '../match/rules';
import type { MatchMode, Player, Rules } from '../types';

const DEFAULT_RULES: Rules = {
  pointsToWin: 11,
  winBy: 2,
  bestOf: 3,
  serveEvery: 2,
  deuceServeEvery: 1,
};

export function render(root: HTMLElement): () => void {
  let mode: MatchMode = 'singles';
  let player1 = '';
  let player2 = '';
  let partner1 = '';
  let partner2 = '';
  let presetId: string | 'custom' = '';
  let customRules: Rules = { ...DEFAULT_RULES };
  let firstServer = 0;
  let newPlayerName = '';
  let presetNameToSave = '';
  let error = '';

  const draw = () => {
    const data = getData();
    const players = data?.players ?? [];
    const presets = data?.presets ?? [];
    if (!presetId && presets.length > 0) presetId = presets[0].id;

    const selectedPreset = presets.find((p) => p.id === presetId);
    const rules = presetId === 'custom' ? customRules : (selectedPreset?.rules ?? DEFAULT_RULES);
    const chosen = mode === 'doubles' ? [player1, player2, partner1, partner2] : [player1, player2];
    const rotation = mode === 'doubles' ? [player1, player2, partner1, partner2] : [player1, player2];
    if (firstServer >= rotation.length) firstServer = 0;

    root.innerHTML = `
      <div class="screen">
        <div class="topbar">
          <md-icon-button id="back-btn"><md-icon>arrow_back</md-icon></md-icon-button>
          <div class="topbar-title md-typescale-headline-small">Новий матч</div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Режим</div>
          <div class="row">
            <label class="row"><md-radio name="mode" value="singles" ${mode === 'singles' ? 'checked' : ''}></md-radio>Одиночна</label>
            <label class="row"><md-radio name="mode" value="doubles" ${mode === 'doubles' ? 'checked' : ''}></md-radio>Парна 2х2</label>
          </div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Гравці</div>
          ${
            mode === 'doubles'
              ? `
            <div class="md-typescale-label-large">Команда А</div>
            ${playerSelect('pA1', 'Гравець 1 (А)', players, chosen, player1)}
            ${playerSelect('pA2', 'Гравець 2 (А)', players, chosen, partner1)}
            <div class="md-typescale-label-large">Команда Б</div>
            ${playerSelect('pB1', 'Гравець 1 (Б)', players, chosen, player2)}
            ${playerSelect('pB2', 'Гравець 2 (Б)', players, chosen, partner2)}
          `
              : `
            ${playerSelect('pA1', 'Гравець 1', players, chosen, player1)}
            ${playerSelect('pB1', 'Гравець 2', players, chosen, player2)}
          `
          }
          <div class="row">
            <md-outlined-text-field class="fill" id="new-player-name" label="Новий гравець" value="${escapeAttr(newPlayerName)}"></md-outlined-text-field>
            <md-filled-tonal-button id="add-player-btn">Додати</md-filled-tonal-button>
          </div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Правила</div>
          <md-outlined-select id="preset" label="Пресет">
            ${presets.map((p) => `<md-select-option value="${p.id}" ${p.id === presetId ? 'selected' : ''}><div slot="headline">${escapeHtml(p.name)}</div></md-select-option>`).join('')}
            <md-select-option value="custom" ${presetId === 'custom' ? 'selected' : ''}><div slot="headline">Кастомні правила</div></md-select-option>
          </md-outlined-select>

          ${presetId === 'custom' ? customRulesForm(customRules) : ''}
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Перша подача</div>
          <div class="row">
            ${rotation
              .map(
                (id, idx) =>
                  `<label class="row"><md-radio name="server" value="${idx}" ${firstServer === idx ? 'checked' : ''}></md-radio>${playerLabel(players, id, `Гравець ${idx + 1}`)}</label>`,
              )
              .join('')}
          </div>
        </div>

        ${error ? `<div class="md-typescale-body-medium" style="color:var(--md-sys-color-error)">${error}</div>` : ''}

        <md-filled-button id="start-btn" style="align-self:stretch;">
          <md-icon slot="icon">play_arrow</md-icon>
          Почати матч
        </md-filled-button>
      </div>
    `;

    wire(rules);
  };

  function customRulesForm(r: Rules): string {
    return `
      <div class="card" id="custom-rules">
        <div class="row">
          <md-outlined-text-field class="fill" id="pointsToWin" label="Очок для перемоги" type="number" value="${r.pointsToWin}"></md-outlined-text-field>
          <md-outlined-text-field class="fill" id="bestOf" label="Сетів (нечітне)" type="number" value="${r.bestOf}"></md-outlined-text-field>
        </div>
        <div class="row">
          <label class="row"><md-radio name="winBy" value="1" ${r.winBy === 1 ? 'checked' : ''}></md-radio>Перевага 1</label>
          <label class="row"><md-radio name="winBy" value="2" ${r.winBy === 2 ? 'checked' : ''}></md-radio>Перевага 2</label>
        </div>
        <div class="row">
          <md-outlined-text-field class="fill" id="serveEvery" label="Подача кожні" type="number" value="${r.serveEvery}"></md-outlined-text-field>
          <md-outlined-text-field class="fill" id="deuceServeEvery" label="Подача при рівному" type="number" value="${r.deuceServeEvery}"></md-outlined-text-field>
        </div>
        <div class="row">
          <md-outlined-text-field class="fill" id="preset-name" label="Назва пресету (щоб зберегти)" value="${escapeAttr(presetNameToSave)}"></md-outlined-text-field>
          <md-filled-tonal-button id="save-preset-btn">Зберегти</md-filled-tonal-button>
        </div>
      </div>
    `;
  }

  function playerLabel(players: Player[], id: string, fallback: string): string {
    return players.find((p) => p.id === id)?.name ?? fallback;
  }

  function playerSelect(id: string, label: string, players: Player[], chosen: string[], value: string): string {
    const options = players.filter((p) => p.id === value || !chosen.includes(p.id));
    return `
      <md-outlined-select id="${id}" label="${label}">
        ${options.map((p) => `<md-select-option value="${p.id}" ${p.id === value ? 'selected' : ''}><div slot="headline">${escapeHtml(p.name)}</div></md-select-option>`).join('')}
      </md-outlined-select>
    `;
  }

  function wire(rules: Rules) {
    root.querySelector('#back-btn')?.addEventListener('click', () => navigate('home'));

    root.querySelectorAll('md-radio[name="mode"]').forEach((el) => {
      el.addEventListener('change', () => {
        mode = (el as HTMLInputElement).value as MatchMode;
        draw();
      });
    });

    bindPlayerSelect('pA1', (v) => (player1 = v));
    bindPlayerSelect('pB1', (v) => (player2 = v));
    if (mode === 'doubles') {
      bindPlayerSelect('pA2', (v) => (partner1 = v));
      bindPlayerSelect('pB2', (v) => (partner2 = v));
    }

    const newPlayerField = root.querySelector('#new-player-name') as HTMLInputElement & { value: string };
    newPlayerField.addEventListener('input', () => {
      newPlayerName = newPlayerField.value;
    });
    root.querySelector('#add-player-btn')?.addEventListener('click', () => void onAddPlayer());

    const presetSelect = root.querySelector('#preset') as HTMLElement & { value: string };
    presetSelect.addEventListener('change', () => {
      presetId = presetSelect.value;
      draw();
    });

    if (presetId === 'custom') {
      bindNumberField('pointsToWin', (v) => (customRules.pointsToWin = v));
      bindNumberField('bestOf', (v) => (customRules.bestOf = v));
      bindNumberField('serveEvery', (v) => (customRules.serveEvery = v));
      bindNumberField('deuceServeEvery', (v) => (customRules.deuceServeEvery = v));
      root.querySelectorAll('md-radio[name="winBy"]').forEach((el) => {
        el.addEventListener('change', () => {
          customRules.winBy = Number((el as HTMLInputElement).value) as 1 | 2;
        });
      });
      const presetNameField = root.querySelector('#preset-name') as HTMLInputElement & { value: string };
      presetNameField?.addEventListener('input', () => {
        presetNameToSave = presetNameField.value;
      });
      root.querySelector('#save-preset-btn')?.addEventListener('click', () => void onSavePreset());
    }

    root.querySelectorAll('md-radio[name="server"]').forEach((el) => {
      el.addEventListener('change', () => {
        firstServer = Number((el as HTMLInputElement).value);
      });
    });

    root.querySelector('#start-btn')?.addEventListener('click', () => onStart(rules));
  }

  function bindPlayerSelect(id: string, setter: (v: string) => void) {
    const el = root.querySelector(`#${id}`) as (HTMLElement & { value: string }) | null;
    el?.addEventListener('change', () => {
      setter(el.value);
      draw();
    });
  }

  function bindNumberField(id: string, setter: (v: number) => void) {
    const el = root.querySelector(`#${id}`) as HTMLInputElement & { value: string };
    el?.addEventListener('input', () => {
      const n = Number(el.value);
      if (Number.isFinite(n)) setter(n);
    });
  }

  async function onAddPlayer() {
    const name = newPlayerName.trim();
    const roomId = getCurrentRoomId();
    if (!name || !roomId) return;
    try {
      const player = await addPlayer(roomId, name);
      mergePlayer(player);
      newPlayerName = '';
      if (!player1) player1 = player.id;
      else if (!player2) player2 = player.id;
      error = '';
      draw();
    } catch (e) {
      error = e instanceof NetworkError ? 'Немає інтернету - неможливо додати гравця.' : 'Не вдалося додати гравця.';
      draw();
    }
  }

  async function onSavePreset() {
    const name = presetNameToSave.trim();
    const roomId = getCurrentRoomId();
    if (!name || !roomId) return;
    try {
      const preset = await savePreset(roomId, { name, rules: customRules });
      mergePresetLocal(preset);
      presetNameToSave = '';
      presetId = preset.id;
      error = '';
      draw();
    } catch (e) {
      error = e instanceof NetworkError ? 'Немає інтернету - неможливо зберегти пресет.' : 'Не вдалося зберегти пресет.';
      draw();
    }
  }

  function onStart(rules: Rules) {
    const chosen = mode === 'doubles' ? [player1, player2, partner1, partner2] : [player1, player2];
    if (chosen.some((id) => !id) || new Set(chosen).size !== chosen.length) {
      error = mode === 'doubles' ? 'Оберіть чотирьох різних гравців.' : 'Оберіть двох різних гравців.';
      draw();
      return;
    }
    if (rules.bestOf % 2 === 0 || rules.bestOf < 1 || rules.pointsToWin < 1) {
      error = 'Перевірте правила матчу.';
      draw();
      return;
    }
    const data = getData();
    const presetName = presetId === 'custom' ? '' : (data?.presets.find((p) => p.id === presetId)?.name ?? '');
    const live = createLiveMatch(
      player1,
      player2,
      rules,
      presetName,
      firstServer,
      mode,
      mode === 'doubles' ? partner1 : null,
      mode === 'doubles' ? partner2 : null,
    );
    setLiveMatch(live);
    navigate('scoreboard');
  }

  draw();
  const unsub = subscribe(draw);
  return unsub;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}

function escapeAttr(s: string): string {
  return escapeHtml(s);
}
