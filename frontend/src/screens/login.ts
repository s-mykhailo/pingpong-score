import { register, recoverAccount, getRecoveryQuestion, loginByQuestion, ApiError, NetworkError } from '../api/client';
import { navigate } from '../router';
import { loadInitial } from '../store';

type Mode = 'register' | 'recover' | 'login';

const NICKNAME_MAX = 100;
const QUESTION_MAX = 100;
const ANSWER_MAX = 60;

export function render(root: HTMLElement): void {
  let mode: Mode = 'register';
  let nickname = '';
  let question = '';
  let answer = '';
  let code = '';

  // "Вхід" ("log in") is two steps: first look up the nickname's question, then answer it.
  let loginNickname = '';
  let loginQuestion: string | null = null;
  let loginAnswer = '';

  let loading = false;
  let error = '';
  let recoveryCode: string | null = null;

  const draw = () => {
    if (recoveryCode) {
      drawRecoveryCode();
      return;
    }

    root.innerHTML = `
      <div class="center-screen">
        <div class="col" style="align-items:center; text-align:center;">
          <md-icon style="font-size:48px; width:48px; height:48px;">sports_tennis</md-icon>
          <div class="md-typescale-headline-medium">Ping Pong Score</div>
          <div class="muted md-typescale-body-medium">Персональний акаунт, а не код групи</div>
        </div>

        <div class="row" style="justify-content:center; flex-wrap:wrap;">
          <md-filled-tonal-button id="tab-register" style="${mode === 'register' ? '' : 'opacity:0.6;'}">Новий профіль</md-filled-tonal-button>
          <md-filled-tonal-button id="tab-recover" style="${mode === 'recover' ? '' : 'opacity:0.6;'}">Відновити</md-filled-tonal-button>
          <md-filled-tonal-button id="tab-login" style="${mode === 'login' ? '' : 'opacity:0.6;'}">Вхід</md-filled-tonal-button>
        </div>

        ${mode === 'register' ? registerForm() : mode === 'recover' ? recoverForm() : loginForm()}

        <div id="error" class="md-typescale-body-medium" style="color:var(--md-sys-color-error); display:${error ? 'block' : 'none'};">${error}</div>

        <md-filled-button id="submit" ${loading ? 'disabled' : ''}>
          <md-icon slot="icon">login</md-icon>
          ${submitLabel()}
        </md-filled-button>
      </div>
    `;

    wire();
  };

  function submitLabel(): string {
    if (mode === 'register') return 'Створити профіль';
    if (mode === 'recover') return 'Увійти';
    return loginQuestion === null ? 'Далі' : 'Увійти';
  }

  function registerForm(): string {
    return `
      <md-outlined-text-field id="nickname" label="Нікнейм" value="${escapeAttr(nickname)}" maxlength="${NICKNAME_MAX}"></md-outlined-text-field>
      <div class="muted md-typescale-body-small" style="align-self:flex-end;">${nickname.length}/${NICKNAME_MAX}</div>

      <div class="muted md-typescale-body-small">Контрольне питання (опційно, для входу без коду)</div>
      <md-outlined-text-field id="question" label="Питання" value="${escapeAttr(question)}" maxlength="${QUESTION_MAX}"></md-outlined-text-field>
      <div class="muted md-typescale-body-small" style="align-self:flex-end;">${question.length}/${QUESTION_MAX}</div>
      <md-outlined-text-field id="answer" label="Відповідь" value="${escapeAttr(answer)}" maxlength="${ANSWER_MAX}"></md-outlined-text-field>
      <div class="muted md-typescale-body-small" style="align-self:flex-end;">${answer.length}/${ANSWER_MAX}</div>
    `;
  }

  function recoverForm(): string {
    return `
      <div class="muted md-typescale-body-small">14-символьний код відновлення</div>
      <md-outlined-text-field id="code" label="Код відновлення" placeholder="XXXXXXXXXXXXXX" value="${escapeAttr(code)}"></md-outlined-text-field>
    `;
  }

  function loginForm(): string {
    if (loginQuestion === null) {
      return `
        <md-outlined-text-field id="login-nickname" label="Нікнейм" value="${escapeAttr(loginNickname)}" maxlength="${NICKNAME_MAX}"></md-outlined-text-field>
      `;
    }
    return `
      <div class="card">
        <div class="muted md-typescale-label-medium">Контрольне питання</div>
        <div class="md-typescale-title-medium">${escapeHtml(loginQuestion)}</div>
      </div>
      <md-outlined-text-field id="login-answer" label="Відповідь" value="${escapeAttr(loginAnswer)}" maxlength="${ANSWER_MAX}"></md-outlined-text-field>
    `;
  }

  function drawRecoveryCode() {
    root.innerHTML = `
      <div class="center-screen">
        <div class="col" style="align-items:center; text-align:center;">
          <md-icon style="font-size:48px; width:48px; height:48px;">key</md-icon>
          <div class="md-typescale-headline-medium">Збережіть код відновлення</div>
          <div class="muted md-typescale-body-medium">
            Цей код показується лише один раз. Він потрібен, щоб повернути акаунт на іншому
            пристрої або після перевстановлення - запишіть або зробіть скріншот.
          </div>
        </div>
        <div class="card" style="align-self:stretch; text-align:center;">
          <div class="md-typescale-headline-small" style="letter-spacing:2px;">${escapeHtml(recoveryCode!)}</div>
        </div>
        <md-filled-button id="done-btn">
          <md-icon slot="icon">check</md-icon>
          Я зберіг код
        </md-filled-button>
      </div>
    `;
    root.querySelector('#done-btn')?.addEventListener('click', () => void onAuthed());
  }

  function wire() {
    root.querySelector('#tab-register')?.addEventListener('click', () => switchMode('register'));
    root.querySelector('#tab-recover')?.addEventListener('click', () => switchMode('recover'));
    root.querySelector('#tab-login')?.addEventListener('click', () => switchMode('login'));

    bindText('#nickname', NICKNAME_MAX, (v) => (nickname = v));
    bindText('#question', QUESTION_MAX, (v) => (question = v));
    bindText('#answer', ANSWER_MAX, (v) => (answer = v));
    bindText('#code', 20, (v) => (code = v));
    bindText('#login-nickname', NICKNAME_MAX, (v) => (loginNickname = v));
    bindText('#login-answer', ANSWER_MAX, (v) => (loginAnswer = v));

    root.querySelector('#submit')?.addEventListener('click', () => void submit());
  }

  function switchMode(next: Mode) {
    mode = next;
    error = '';
    loginQuestion = null;
    draw();
  }

  function bindText(selector: string, max: number, setter: (v: string) => void) {
    const field = root.querySelector(selector) as (HTMLInputElement & { value: string }) | null;
    field?.addEventListener('input', () => setter(field.value.slice(0, max)));
    field?.addEventListener('keydown', (e) => {
      if ((e as KeyboardEvent).key === 'Enter') void submit();
    });
  }

  async function submit() {
    if (loading) return;
    error = '';

    if (mode === 'register') {
      if (!nickname.trim()) return;
      if (!!question.trim() !== !!answer.trim()) {
        error = 'Заповніть і питання, і відповідь (або лишіть обидва порожніми).';
        draw();
        return;
      }
      loading = true;
      draw();
      try {
        const { recoveryCode: freshCode } = await register(nickname.trim(), '', question.trim() || undefined, answer.trim() || undefined);
        recoveryCode = freshCode;
      } catch (e) {
        error = friendlyError(e, mode);
        loading = false;
        draw();
        return;
      }
      draw();
      return;
    }

    if (mode === 'recover') {
      if (!code.trim()) return;
      loading = true;
      draw();
      try {
        await recoverAccount(code.trim());
        await onAuthed();
      } catch (e) {
        error = friendlyError(e, mode);
        loading = false;
        draw();
      }
      return;
    }

    // mode === 'login'
    if (loginQuestion === null) {
      if (!loginNickname.trim()) return;
      loading = true;
      draw();
      try {
        loginQuestion = await getRecoveryQuestion(loginNickname.trim());
      } catch (e) {
        error = friendlyError(e, mode);
      } finally {
        loading = false;
        draw();
      }
      return;
    }

    if (!loginAnswer.trim()) return;
    loading = true;
    draw();
    try {
      await loginByQuestion(loginNickname.trim(), loginAnswer.trim());
      await onAuthed();
    } catch (e) {
      error = friendlyError(e, mode);
      loading = false;
      draw();
    }
  }

  async function onAuthed() {
    await loadInitial();
    navigate('rooms');
  }

  draw();
}

function friendlyError(e: unknown, mode: Mode): string {
  if (e instanceof NetworkError) return 'Немає інтернету. Перевірте з’єднання і спробуйте ще раз.';
  if (e instanceof ApiError) {
    if (e.message === 'nickname taken') return 'Такий нікнейм вже зайнятий, виберіть інший.';
    if (e.message === 'name required') return 'Введіть нікнейм.';
    if (e.message === 'name too long') return 'Нікнейм задовгий (до 100 символів).';
    if (e.message === 'question too long') return 'Питання задовге (до 100 символів).';
    if (e.message === 'answer too long') return 'Відповідь задовга (до 60 символів).';
    if (e.message === 'question and answer must be set together') return 'Заповніть і питання, і відповідь (або лишіть обидва порожніми).';
    if (e.message === 'no recovery question set') return mode === 'login' ? 'Для цього нікнейму не задано контрольне питання.' : 'Невірний нікнейм.';
    if (e.message === 'wrong answer') return 'Невірна відповідь.';
    if (e.message === 'invalid code') return mode === 'recover' ? 'Невірний код відновлення.' : 'Невірний код.';
    return 'Сервер тимчасово недоступний, спробуйте ще раз за кілька секунд.';
  }
  return 'Щось пішло не так.';
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}

function escapeAttr(s: string): string {
  return escapeHtml(s);
}
