import { join, ApiError, NetworkError } from '../api/client';
import { navigate } from '../router';
import { loadInitial } from '../store';

export function render(root: HTMLElement): void {
  root.innerHTML = `
    <div class="center-screen">
      <div class="col" style="align-items:center; text-align:center;">
        <md-icon style="font-size:48px; width:48px; height:48px;">sports_tennis</md-icon>
        <div class="md-typescale-headline-medium">Ping Pong Score</div>
        <div class="muted md-typescale-body-medium">Введіть код групи, щоб продовжити</div>
      </div>
      <md-outlined-text-field id="code" label="Код групи" placeholder="XXXXX-XXXXX"></md-outlined-text-field>
      <div id="error" class="md-typescale-body-medium" style="color:var(--md-sys-color-error); display:none;"></div>
      <md-filled-button id="submit">
        <md-icon slot="icon">login</md-icon>
        Увійти
      </md-filled-button>
    </div>
  `;

  const codeField = root.querySelector('#code') as HTMLInputElement & { value: string };
  const errorEl = root.querySelector('#error') as HTMLElement;
  const submitBtn = root.querySelector('#submit') as HTMLElement & { disabled: boolean };

  async function submit() {
    const code = codeField.value.trim();
    if (!code) return;
    errorEl.style.display = 'none';
    submitBtn.disabled = true;
    try {
      await join(code);
      await loadInitial();
      navigate('home');
    } catch (e) {
      errorEl.textContent =
        e instanceof NetworkError
          ? 'Немає інтернету. Перевірте з’єднання і спробуйте ще раз.'
          : e instanceof ApiError
            ? e.message === 'invalid code'
              ? 'Невірний код групи.'
              : e.message === 'too many attempts, try later'
                ? 'Забагато спроб. Зачекайте кілька хвилин і спробуйте ще раз.'
                : 'Сервер тимчасово недоступний, спробуйте ще раз за кілька секунд.'
            : 'Щось пішло не так.';
      errorEl.style.display = 'block';
    } finally {
      submitBtn.disabled = false;
    }
  }

  submitBtn.addEventListener('click', submit);
  codeField.addEventListener('keydown', (e) => {
    if ((e as KeyboardEvent).key === 'Enter') void submit();
  });
}
