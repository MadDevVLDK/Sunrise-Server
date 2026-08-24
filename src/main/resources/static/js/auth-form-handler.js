document.addEventListener('DOMContentLoaded', function () {
    initializeFormHandlers();
});

function initializeFormHandlers() {
    const loginForm = document.getElementById('authLoginForm');
    const registerForm = document.getElementById('authRegisterForm');
    const forgotPasswordForm = document.getElementById('authForgotPasswordForm');

    if (loginForm) loginForm.addEventListener('submit', handleLoginSubmit);
    if (registerForm) registerForm.addEventListener('submit', handleRegisterSubmit);
    if (forgotPasswordForm) forgotPasswordForm.addEventListener('submit', handleForgotPasswordSubmit);
}

async function handleLoginSubmit(event) {
    event.preventDefault();
    const username = event.target.username.value.trim();
    const password = event.target.password.value.trim();

    clearErrors(['loginUsernameError', 'loginPasswordError']);
    const errors = validateLoginForm(username, password);
    if (Object.keys(errors).length > 0) {
        Object.entries(errors).forEach(([f, m]) => showErrorMessage(f, m));
        return;
    }

    const toastId = Toast.loading('Вход в аккаунт...');

    try {
        const result = await API.login(username, password);
        Toast.dismiss(toastId);
        Toast.success('Успешно! Перенаправление...');
        localStorage.setItem('authToken', result.jwtToken);
        setTimeout(() => { window.location.href = getFormsPath('/main'); }, 500);
    } catch (error) {
        Toast.dismiss(toastId);
        if (error instanceof ApiError) {
            Toast.error(error.displayMessage);
        } else {
            Toast.error('Ошибка подключения. Попробуйте позже.');
        }
    }
}

async function handleRegisterSubmit(event) {
    event.preventDefault();
    const username = event.target.username.value.trim();
    const name = event.target.name.value.trim();
    const email = event.target.email.value.trim();
    const password = event.target.password.value.trim();
    const termsCheckbox = event.target.termsCheckbox.checked;

    clearErrors(['registerUsernameError', 'registerNameError', 'registerEmailError', 'registerPasswordError', 'registerTermsError']);
    const errors = validateRegisterForm(username, name, email, password, termsCheckbox);
    if (Object.keys(errors).length > 0) {
        Object.entries(errors).forEach(([f, m]) => showErrorMessage(f, m));
        return;
    }

    const toastId = Toast.loading('Создание аккаунта...');

    try {
        await API.register(username, name, email, password);
        Toast.dismiss(toastId);
        Toast.success('Аккаунт создан! Проверьте почту для подтверждения.');
        event.target.reset();
        setTimeout(() => switchForm('loginForm'), 2500);
    } catch (error) {
        Toast.dismiss(toastId);
        if (error instanceof ApiError) {
            const fieldMapping = {
                USERNAME_TAKEN: 'registerUsernameError',
                EMAIL_TAKEN: 'registerEmailError',
            };
            const targetField = fieldMapping[error.code];
            if (targetField) {
                showErrorMessage(targetField, error.displayMessage);
            } else {
                Toast.error(error.displayMessage);
            }
        } else {
            Toast.error('Ошибка подключения. Попробуйте позже.');
        }
    }
}

async function handleForgotPasswordSubmit(event) {
    event.preventDefault();
    const email = event.target.email.value.trim();

    clearErrors(['forgotPasswordEmailError']);
    if (!email) { showErrorMessage('forgotPasswordEmailError', 'Укажите email'); return; }
    if (!isValidEmail(email)) { showErrorMessage('forgotPasswordEmailError', 'Некорректный email адрес'); return; }

    const toastId = Toast.loading('Отправка ссылки восстановления...');

    try {
        await API.forgotPassword(email);
        Toast.dismiss(toastId);
        Toast.success('Если пользователь существует, ссылка отправлена на почту.');
        event.target.reset();
        setTimeout(() => switchForm('loginForm'), 3000);
    } catch (error) {
        Toast.dismiss(toastId);
        if (error instanceof ApiError) {
            Toast.error(error.displayMessage);
        } else {
            Toast.error('Ошибка подключения. Попробуйте позже.');
        }
    }
}

// ==================== ВАЛИДАЦИЯ ====================

function validateLoginForm(username, password) {
    const errors = {};
    if (!username) errors.loginUsernameError = 'Укажите имя пользователя или email';
    else if (containsCyrillic(username)) errors.loginUsernameError = 'Имя пользователя не должно содержать русские символы';
    if (!password) errors.loginPasswordError = 'Укажите пароль';
    else if (containsCyrillic(password)) errors.loginPasswordError = 'Пароль не должен содержать русские символы';
    else if (password.length < 8) errors.loginPasswordError = 'Пароль должен быть минимум 8 символов';
    else if (!hasLettersAndNumbers(password)) errors.loginPasswordError = 'Пароль должен содержать буквы и цифры';
    return errors;
}

function validateRegisterForm(username, name, email, password, termsCheckbox) {
    const errors = {};
    if (!username) errors.registerUsernameError = 'Укажите имя пользователя';
    else if (containsCyrillic(username)) errors.registerUsernameError = 'Имя пользователя не должно содержать русские символы';
    else if (username.length < 3) errors.registerUsernameError = 'Имя пользователя: минимум 3 символа';
    else if (username.length > 50) errors.registerUsernameError = 'Имя пользователя: максимум 50 символов';
    if (!name) errors.registerNameError = 'Укажите ваше имя';
    else if (name.length < 2) errors.registerNameError = 'Имя: минимум 2 символа';
    else if (name.length > 100) errors.registerNameError = 'Имя: максимум 100 символов';
    if (!isValidEmail(email)) errors.registerEmailError = 'Некорректный email адрес';
    if (!password) errors.registerPasswordError = 'Укажите пароль';
    else if (containsCyrillic(password)) errors.registerPasswordError = 'Пароль не должен содержать русские символы';
    else if (password.length < 8) errors.registerPasswordError = 'Пароль: минимум 8 символов';
    else if (password.length > 50) errors.registerPasswordError = 'Пароль: максимум 50 символов';
    else if (!hasLettersAndNumbers(password)) errors.registerPasswordError = 'Пароль должен содержать буквы и цифры';
    if (!termsCheckbox) errors.registerTermsError = 'Необходимо согласиться с условиями';
    return errors;
}

// ==================== УТИЛИТЫ ====================

function isValidEmail(email) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email); }
function hasLettersAndNumbers(p) { return /[a-zA-Zа-яА-ЯёЁ]/.test(p) && /\d/.test(p); }
function containsCyrillic(t) { return /[а-яА-ЯёЁ]/.test(t); }

function showErrorMessage(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) { el.textContent = message; el.classList.add('show'); }
}

function clearErrors(errorIds) {
    errorIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.textContent = ''; el.classList.remove('show'); }
    });
}

function togglePasswordVisibility(button) {
    const inputWrapper = button.closest('.input-wrapper');
    const input = inputWrapper.querySelector('input[type="password"], input[type="text"]');
    const icon = button.querySelector('i');
    if (!input) return;
    if (input.type === 'password') { input.type = 'text'; icon.classList.replace('bi-eye-fill', 'bi-eye-slash-fill'); }
    else { input.type = 'password'; icon.classList.replace('bi-eye-slash-fill', 'bi-eye-fill'); }
}

function switchForm(formId) {
    document.querySelectorAll('.form-panel').forEach(p => p.classList.remove('active'));
    const targetForm = document.getElementById(formId);
    if (targetForm) {
        targetForm.classList.add('active');
        if (window.innerWidth < 768) targetForm.scrollIntoView({ behavior: 'smooth' });
        const firstInput = targetForm.querySelector('input');
        if (firstInput) setTimeout(() => firstInput.focus(), 100);
    }
}