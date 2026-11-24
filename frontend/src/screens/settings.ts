import { renderShell } from './shell';
import {
  getSession,
  logout,
  updateProfile,
  claimMasterkey,
  claimAdmin,
  createAdminInvite,
  setRecoveryQuestion,
  ApiError,
  NetworkError,
} from '../api/client';
import { getCurrentRoom, clearCurrentRoom } from '../room';
import { navigate } from '../router';
import { ACCENT_PRESETS, getAccentColor, getThemeMode, setAccentColor, setThemeMode, type ThemeMode } from '../theme/theme';
import { clearData } from '../store';
import type { UserRole } from '../types';

const NICKNAME_MAX = 100;
const QUESTION_MAX = 100;
const ANSWER_MAX = 60;

export function render(root: HTMLElement): () => void {
  let nickname = '';
  let nicknameInitialized = false;
  let masterKeyInput = '';
  let adminCodeInput = '';
  let busy = false;
  let error = '';
  let notice = '';
  let codeToShow: { title: string; code: string } | null = null;
  let editingQuestion = false;
  let newQuestion = '';
  let newAnswer = '';

  return renderShell(root, 'settings', (content) => {
    const draw = () => {
      const session = getSession();
      const room = getCurrentRoom();
      const mode = getThemeMode();
      const accent = getAccentColor();
      if (!nicknameInitialized && session) {
        nickname = session.user.nickname;
        nicknameInitialized = true;
      }
      const role: UserRole = session?.user.role ?? 'user';

      content.innerHTML = `
        <div class="md-typescale-headline-small">Налаштування</div>

        <div class="card">
          <div class="muted md-typescale-label-medium">Кімната</div>
          <div class="md-typescale-title-medium">${escapeHtml(room?.name ?? '')}</div>
          <div class="muted md-typescale-body-small">Роль у кімнаті: ${room?.isAdmin ? 'адміністратор' : 'учасник'}</div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Профіль</div>
          <div class="muted md-typescale-body-small">Роль акаунта: ${roleLabel(role)}</div>
          <md-outlined-text-field id="nickname" label="Нікнейм" value="${escapeAttr(nickname)}" maxlength="${NICKNAME_MAX}"></md-outlined-text-field>
          <md-filled-tonal-button id="save-nickname-btn" ${busy ? 'disabled' : ''}>Зберегти нікнейм</md-filled-tonal-button>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Контрольне питання</div>
          <div class="muted md-typescale-body-small">Дозволяє увійти без коду відновлення - за ніком і відповіддю.</div>
          ${
            editingQuestion
              ? `
            <md-outlined-text-field id="question" label="Питання" value="${escapeAttr(newQuestion)}" maxlength="${QUESTION_MAX}"></md-outlined-text-field>
            <md-outlined-text-field id="answer" label="Відповідь" value="${escapeAttr(newAnswer)}" maxlength="${ANSWER_MAX}"></md-outlined-text-field>
            <div class="row">
              <md-filled-tonal-button id="save-question-btn" ${busy ? 'disabled' : ''}>Зберегти</md-filled-tonal-button>
              <md-text-button id="cancel-question-btn">Скасувати</md-text-button>
            </div>
          `
              : session?.user.hasRecoveryQuestion
                ? `
            <div class="muted md-typescale-body-medium">${escapeHtml(session.user.recoveryQuestion)}</div>
            <div class="row">
              <md-outlined-button id="change-question-btn">Змінити</md-outlined-button>
              <md-text-button id="remove-question-btn" ${busy ? 'disabled' : ''}>Прибрати</md-text-button>
            </div>
          `
                : `<md-outlined-button id="add-question-btn">Додати питання</md-outlined-button>`
          }
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Тема</div>
          <div class="row">
            ${themeOption('light', 'Світла', mode)}
            ${themeOption('dark', 'Темна', mode)}
            ${themeOption('system', 'Системна', mode)}
          </div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Акцентний колір</div>
          <div class="row" style="flex-wrap:wrap;">
            ${ACCENT_PRESETS.map(
              (c) =>
                `<div class="color-swatch ${c.hex === accent ? 'selected' : ''}" style="background:${c.hex}" data-hex="${c.hex}" title="${c.name}"></div>`,
            ).join('')}
          </div>
        </div>

        ${
          role !== 'masterkey'
            ? `
          <div class="col">
            <div class="md-typescale-title-medium">Активувати мастер-ключ</div>
            <div class="muted md-typescale-body-small">Дає право створювати кімнати і видавати адмінку.</div>
            <md-outlined-text-field id="master-key" label="Master key" value="${escapeAttr(masterKeyInput)}"></md-outlined-text-field>
            <md-filled-tonal-button id="claim-masterkey-btn" ${busy ? 'disabled' : ''}>Активувати</md-filled-tonal-button>
          </div>
        `
            : ''
        }

        ${
          role === 'user'
            ? `
          <div class="col">
            <div class="md-typescale-title-medium">Активувати адмінку</div>
            <div class="muted md-typescale-body-small">Код від власника мастер-ключа.</div>
            <md-outlined-text-field id="admin-code" label="Код" value="${escapeAttr(adminCodeInput)}"></md-outlined-text-field>
            <md-filled-tonal-button id="claim-admin-btn" ${busy ? 'disabled' : ''}>Активувати</md-filled-tonal-button>
          </div>
        `
            : ''
        }

        ${
          role === 'masterkey'
            ? `
          <md-outlined-button id="create-admin-invite-btn" style="align-self:flex-start;">Видати адмінку</md-outlined-button>
        `
            : ''
        }

        ${error ? `<div class="md-typescale-body-medium" style="color:var(--md-sys-color-error)">${escapeHtml(error)}</div>` : ''}
        ${notice ? `<div class="md-typescale-body-medium" style="color:var(--md-sys-color-primary)">${escapeHtml(notice)}</div>` : ''}

        <div class="row">
          <md-outlined-button id="rooms-btn"><md-icon slot="icon">meeting_room</md-icon>Інші кімнати</md-outlined-button>
          <md-outlined-button id="logout-btn"><md-icon slot="icon">logout</md-icon>Вийти</md-outlined-button>
        </div>

        ${
          codeToShow
            ? `<md-dialog open id="code-dialog">
                <div slot="headline">${escapeHtml(codeToShow.title)}</div>
                <div slot="content" class="md-typescale-headline-small" style="letter-spacing:2px;">${escapeHtml(codeToShow.code)}</div>
                <div slot="actions"><md-filled-button id="code-dialog-done">Готово</md-filled-button></div>
              </md-dialog>`
            : ''
        }
      `;

      content.querySelectorAll('[data-mode]').forEach((btn) => {
        btn.addEventListener('click', () => {
          setThemeMode((btn as HTMLElement).dataset.mode as ThemeMode);
          draw();
        });
      });
      content.querySelectorAll('.color-swatch').forEach((el) => {
        el.addEventListener('click', () => {
          setAccentColor((el as HTMLElement).dataset.hex!);
          draw();
        });
      });

      const nicknameField = content.querySelector('#nickname') as (HTMLInputElement & { value: string }) | null;
      nicknameField?.addEventListener('input', () => {
        nickname = nicknameField.value.slice(0, NICKNAME_MAX);
      });
      content.querySelector('#save-nickname-btn')?.addEventListener('click', () => void onSaveNickname());

      content.querySelector('#add-question-btn')?.addEventListener('click', () => {
        editingQuestion = true;
        newQuestion = '';
        newAnswer = '';
        draw();
      });
      content.querySelector('#change-question-btn')?.addEventListener('click', () => {
        editingQuestion = true;
        newQuestion = session?.user.recoveryQuestion ?? '';
        newAnswer = '';
        draw();
      });
      content.querySelector('#cancel-question-btn')?.addEventListener('click', () => {
        editingQuestion = false;
        draw();
      });
      content.querySelector('#remove-question-btn')?.addEventListener('click', () => void onSaveQuestion('', ''));
      content.querySelector('#save-question-btn')?.addEventListener('click', () => void onSaveQuestion(newQuestion.trim(), newAnswer.trim()));

      const questionField = content.querySelector('#question') as (HTMLInputElement & { value: string }) | null;
      questionField?.addEventListener('input', () => {
        newQuestion = questionField.value.slice(0, QUESTION_MAX);
      });
      const answerField = content.querySelector('#answer') as (HTMLInputElement & { value: string }) | null;
      answerField?.addEventListener('input', () => {
        newAnswer = answerField.value.slice(0, ANSWER_MAX);
      });

      const masterKeyField = content.querySelector('#master-key') as (HTMLInputElement & { value: string }) | null;
      masterKeyField?.addEventListener('input', () => {
        masterKeyInput = masterKeyField.value;
      });
      content.querySelector('#claim-masterkey-btn')?.addEventListener('click', () => void onClaimMasterkey());

      const adminCodeField = content.querySelector('#admin-code') as (HTMLInputElement & { value: string }) | null;
      adminCodeField?.addEventListener('input', () => {
        adminCodeInput = adminCodeField.value;
      });
      content.querySelector('#claim-admin-btn')?.addEventListener('click', () => void onClaimAdmin());

      content.querySelector('#create-admin-invite-btn')?.addEventListener('click', () => void onCreateAdminInvite());
      content.querySelector('#code-dialog-done')?.addEventListener('click', () => {
        codeToShow = null;
        draw();
      });

      content.querySelector('#rooms-btn')?.addEventListener('click', () => navigate('rooms'));
      content.querySelector('#logout-btn')?.addEventListener('click', () => void onLogout());
    };

    function themeOption(value: ThemeMode, label: string, current: ThemeMode): string {
      const selected = value === current;
      return `<md-filled-tonal-button data-mode="${value}" style="${selected ? '' : 'opacity:0.6;'}">${label}</md-filled-tonal-button>`;
    }

    async function onSaveNickname() {
      const name = nickname.trim();
      const session = getSession();
      if (!name || busy || !session) return;
      busy = true;
      error = '';
      notice = '';
      draw();
      try {
        await updateProfile(name, session.user.avatar);
        notice = 'Нікнейм оновлено.';
      } catch (e) {
        error = friendlyError(e);
      } finally {
        busy = false;
        draw();
      }
    }

    async function onSaveQuestion(q: string, a: string) {
      if (busy) return;
      if (!!q !== !!a) {
        error = 'Заповніть і питання, і відповідь (або лишіть обидва порожніми).';
        draw();
        return;
      }
      busy = true;
      error = '';
      notice = '';
      draw();
      try {
        await setRecoveryQuestion(q, a);
        editingQuestion = false;
        notice = q ? 'Контрольне питання збережено.' : 'Контрольне питання прибрано.';
      } catch (e) {
        error = friendlyError(e);
      } finally {
        busy = false;
        draw();
      }
    }

    async function onClaimMasterkey() {
      const key = masterKeyInput.trim();
      if (!key || busy) return;
      busy = true;
      error = '';
      notice = '';
      draw();
      try {
        await claimMasterkey(key);
        masterKeyInput = '';
        notice = 'Мастер-ключ активовано.';
      } catch (e) {
        error = friendlyError(e);
      } finally {
        busy = false;
        draw();
      }
    }

    async function onClaimAdmin() {
      const code = adminCodeInput.trim();
      if (!code || busy) return;
      busy = true;
      error = '';
      notice = '';
      draw();
      try {
        await claimAdmin(code);
        adminCodeInput = '';
        notice = 'Адмінку активовано.';
      } catch (e) {
        error = friendlyError(e);
      } finally {
        busy = false;
        draw();
      }
    }

    async function onCreateAdminInvite() {
      error = '';
      try {
        const code = await createAdminInvite();
        codeToShow = { title: 'Код для видачі адмінки', code };
      } catch (e) {
        error = friendlyError(e);
      }
      draw();
    }

    async function onLogout() {
      await logout();
      clearCurrentRoom();
      clearData();
      navigate('login');
    }

    draw();
    return () => {};
  });
}

function friendlyError(e: unknown): string {
  if (e instanceof NetworkError) return 'Немає інтернету.';
  if (e instanceof ApiError) {
    if (e.message === 'unauthorized') return 'Невірний master key.';
    if (e.message === 'invalid code') return 'Невірний код.';
    if (e.message === 'nickname taken') return 'Такий нікнейм вже зайнятий.';
    if (e.message === 'question too long') return 'Питання задовге (до 100 символів).';
    if (e.message === 'answer too long') return 'Відповідь задовга (до 60 символів).';
    if (e.message === 'question and answer must be set together') return 'Заповніть і питання, і відповідь (або лишіть обидва порожніми).';
    return 'Сервер відхилив запит.';
  }
  return 'Щось пішло не так.';
}

function roleLabel(role: UserRole): string {
  if (role === 'masterkey') return 'Master';
  if (role === 'admin') return 'Admin';
  return 'User';
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}

function escapeAttr(s: string): string {
  return escapeHtml(s);
}
