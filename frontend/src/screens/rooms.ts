import { myRooms, createRoom, joinRoom, getSession, logout, ApiError, NetworkError } from '../api/client';
import { setCurrentRoom, clearCurrentRoom } from '../room';
import { navigate } from '../router';
import { loadInitial, clearData } from '../store';
import { loadLiveMatch } from '../match/liveMatch';
import type { RoomSummary } from '../types';

export function render(root: HTMLElement): void {
  let rooms: RoomSummary[] = [];
  let loading = true;
  let error = '';
  let showCreate = false;
  let showJoin = false;
  let newRoomName = '';
  let joinCode = '';
  let busy = false;

  const session = getSession();
  const canCreate = session?.user.role === 'admin' || session?.user.role === 'masterkey';

  const draw = () => {
    root.innerHTML = `
      <div class="screen">
        <div class="topbar">
          <div class="topbar-title md-typescale-headline-small">Мої кімнати</div>
          <md-icon-button id="logout-btn"><md-icon>logout</md-icon></md-icon-button>
        </div>
        <div class="muted md-typescale-body-medium">${escapeHtml(session?.user.nickname ?? '')}</div>

        ${error ? `<div class="md-typescale-body-medium" style="color:var(--md-sys-color-error)">${error}</div>` : ''}

        ${
          loading
            ? `<div class="center-screen"><md-circular-progress indeterminate></md-circular-progress></div>`
            : rooms.length === 0
              ? `<div class="empty-state"><md-icon>sports_tennis</md-icon><div>Ще немає кімнат. Приєднайтесь за кодом.</div></div>`
              : `<div class="col" id="room-list">${rooms.map(roomRow).join('')}</div>`
        }

        <div class="row" style="align-self:stretch;">
          ${canCreate ? `<md-filled-button class="fill" id="create-btn"><md-icon slot="icon">add</md-icon>Створити кімнату</md-filled-button>` : ''}
          <md-outlined-button class="fill" id="join-btn"><md-icon slot="icon">group_add</md-icon>Приєднатись за кодом</md-outlined-button>
        </div>
      </div>

      ${showCreate ? createDialog() : ''}
      ${showJoin ? joinDialog() : ''}
    `;

    wire();
  };

  function roomRow(r: RoomSummary): string {
    return `
      <div class="card room-row" data-id="${r.id}">
        <div class="md-typescale-title-medium">${escapeHtml(r.name)}</div>
        ${r.isAdmin ? `<div class="muted md-typescale-body-small">Адмін кімнати</div>` : ''}
      </div>
    `;
  }

  function createDialog(): string {
    return `
      <md-dialog id="create-dialog" open>
        <div slot="headline">Нова кімната</div>
        <form slot="content" id="create-form" method="dialog">
          <md-outlined-text-field id="room-name" label="Назва кімнати" value="${escapeAttr(newRoomName)}"></md-outlined-text-field>
        </form>
        <div slot="actions">
          <md-text-button id="create-cancel">Скасувати</md-text-button>
          <md-filled-button id="create-confirm" ${busy ? 'disabled' : ''}>Створити</md-filled-button>
        </div>
      </md-dialog>
    `;
  }

  function joinDialog(): string {
    return `
      <md-dialog id="join-dialog" open>
        <div slot="headline">Приєднатись до кімнати</div>
        <form slot="content" id="join-form" method="dialog">
          <md-outlined-text-field id="join-code" label="Код запрошення" value="${escapeAttr(joinCode)}"></md-outlined-text-field>
        </form>
        <div slot="actions">
          <md-text-button id="join-cancel">Скасувати</md-text-button>
          <md-filled-button id="join-confirm" ${busy ? 'disabled' : ''}>Приєднатись</md-filled-button>
        </div>
      </md-dialog>
    `;
  }

  function wire() {
    root.querySelector('#logout-btn')?.addEventListener('click', () => void onLogout());

    root.querySelectorAll('.room-row').forEach((el) => {
      el.addEventListener('click', () => void onOpenRoom((el as HTMLElement).dataset.id!));
    });

    root.querySelector('#create-btn')?.addEventListener('click', () => {
      showCreate = true;
      draw();
    });
    root.querySelector('#join-btn')?.addEventListener('click', () => {
      showJoin = true;
      draw();
    });

    const nameField = root.querySelector('#room-name') as (HTMLInputElement & { value: string }) | null;
    nameField?.addEventListener('input', () => {
      newRoomName = nameField.value;
    });
    root.querySelector('#create-cancel')?.addEventListener('click', () => {
      showCreate = false;
      draw();
    });
    root.querySelector('#create-confirm')?.addEventListener('click', () => void onCreate());

    const codeField = root.querySelector('#join-code') as (HTMLInputElement & { value: string }) | null;
    codeField?.addEventListener('input', () => {
      joinCode = codeField.value;
    });
    root.querySelector('#join-cancel')?.addEventListener('click', () => {
      showJoin = false;
      draw();
    });
    root.querySelector('#join-confirm')?.addEventListener('click', () => void onJoin());
  }

  async function load() {
    loading = true;
    draw();
    try {
      rooms = await myRooms();
      error = '';
    } catch (e) {
      error = e instanceof NetworkError ? 'Немає інтернету.' : 'Не вдалося завантажити кімнати.';
    } finally {
      loading = false;
      draw();
    }
  }

  async function onCreate() {
    const name = newRoomName.trim();
    if (!name || busy) return;
    busy = true;
    draw();
    try {
      const { room } = await createRoom(name);
      rooms = [...rooms, room];
      newRoomName = '';
      showCreate = false;
      error = '';
    } catch (e) {
      error = e instanceof NetworkError ? 'Немає інтернету.' : 'Не вдалося створити кімнату.';
    } finally {
      busy = false;
      draw();
    }
  }

  async function onJoin() {
    const code = joinCode.trim();
    if (!code || busy) return;
    busy = true;
    draw();
    try {
      const room = await joinRoom(code);
      rooms = rooms.some((r) => r.id === room.id) ? rooms : [...rooms, room];
      joinCode = '';
      showJoin = false;
      error = '';
    } catch (e) {
      error = e instanceof NetworkError ? 'Немає інтернету.' : e instanceof ApiError && e.message === 'invalid code' ? 'Невірний код запрошення.' : 'Не вдалося приєднатись.';
    } finally {
      busy = false;
      draw();
    }
  }

  async function onOpenRoom(id: string) {
    const room = rooms.find((r) => r.id === id);
    if (!room) return;
    setCurrentRoom(room);
    await loadLiveMatch();
    await loadInitial();
    navigate('home');
  }

  async function onLogout() {
    await logout();
    clearCurrentRoom();
    clearData();
    navigate('login');
  }

  draw();
  void load();
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}

function escapeAttr(s: string): string {
  return escapeHtml(s);
}
