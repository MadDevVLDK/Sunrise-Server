document.addEventListener('DOMContentLoaded', function() {
    initializeFormHandlers();
});

function initializeFormHandlers() {
    const loginForm = document.getElementById('authLoginForm');
    const registerForm = document.getElementById('authRegisterForm');
    const forgotPasswordForm = document.getElementById('authForgotPasswordForm');

    if (loginForm) {
        loginForm.addEventListener('submit', handleLoginSubmit);
    }

    if (registerForm) {
        registerForm.addEventListener('submit', handleRegisterSubmit);
    }

    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener('submit', handleForgotPasswordSubmit);
    }
}

async function handleLoginSubmit(event) {
    event.preventDefault();

    const username = event.target.username.value.trim();
    const password = event.target.password.value.trim();

    // Очистка ошибок
    clearErrors(['loginUsernameError', 'loginPasswordError']);

    const loginMessageDiv = document.getElementById('loginMessage');
    if (loginMessageDiv) loginMessageDiv.style.display = 'none';

    // Валидация на клиенте
    const errors = validateLoginForm(username, password);
    if (Object.keys(errors).length > 0) {
        Object.entries(errors).forEach(([field, message]) => {
            showErrorMessage(field, message);
        });
        return;
    }

    showLoading('loginMessage', 'Вход в аккаунт...');

    try {
        console.log('[Auth] Sending login request to:', getApiPath('/auth/login'));
        const result = await API.login(username, password);

        // Проверяем статус ответа
        if (!result.success) {
            console.error('[Auth] Server returned error:', result.error);
            showError('loginMessage', `Ошибка. Проверьте консоль`);
            return;
        }

        showSuccess('loginMessage', 'Успешно! Перенаправление...');

        // Сохраняем токен
        localStorage.setItem('authToken', result.data.jwtToken);
        console.log('[Auth] Token saved:', result.data.jwtToken);

        setTimeout(() => {
            console.log('[Auth] Redirecting to /main');
            window.location.href = getFormsPath('/main');
        }, 500);

    } catch (error) {
        console.error('[Auth] Login error:', error);
        showError('loginMessage', 'Ошибка подключения. Попробуйте позже.');
    }
}
async function handleRegisterSubmit(event) {
    event.preventDefault();

    const username = event.target.username.value.trim();
    const name = event.target.name.value.trim();
    const email = event.target.email.value.trim();
    const password = event.target.password.value.trim();
    const termsCheckbox = event.target.termsCheckbox.checked;

    // Очистка ошибок
    clearErrors(['registerUsernameError', 'registerNameError', 'registerEmailError', 'registerPasswordError', 'registerTermsError']);

    const registerMessageDiv = document.getElementById('registerMessage');
    if (registerMessageDiv) registerMessageDiv.style.display = 'none';

    // Валидация
    const errors = validateRegisterForm(username, name, email, password, termsCheckbox);
    if (Object.keys(errors).length > 0) {
        Object.entries(errors).forEach(([field, message]) => {
            showErrorMessage(field, message);
        });
        return;
    }

    showLoading('registerMessage', 'Создание аккаунта...');

    try {
        console.log('[Auth] Sending register request to:', getApiPath('/auth/register'));
        const result = await API.register(username, name, email, password);

        // Проверяем статус ответа
        if (!result.success) {
            console.error('[Auth] Server returned error:', result.error);
            showError('registerMessage', `Ошибка. Проверьте консоль`);
            return;
        }

        showSuccess('registerMessage', 'Аккаунт создан! Проверьте email для подтверждения.');

        event.target.reset();
        setTimeout(() => switchForm('loginForm'), 2500);

    } catch (error) {
        console.error('Register error:', error);
        showError('registerMessage', 'Ошибка подключения. Попробуйте позже.');
    }
}
async function handleForgotPasswordSubmit(event) {
    event.preventDefault();

    const email = event.target.email.value.trim();

    clearErrors(['forgotPasswordEmailError']);

    const forgotPasswordMessageDiv = document.getElementById('forgotPasswordMessage');
    if (forgotPasswordMessageDiv) forgotPasswordMessageDiv.style.display = 'none';

    if (!email) {
        showErrorMessage('forgotPasswordEmailError', 'Укажите email');
        return;
    }

    if (!isValidEmail(email)) {
        showErrorMessage('forgotPasswordEmailError', 'Некорректный email адрес');
        return;
    }

    showLoading('forgotPasswordMessage', 'Отправка ссылки восстановления...');

    try {
        console.log('[Auth] Sending forgot password request to:', getApiPath('/auth/change-password'));
        const result = await API.forgotPassword(email);

        // Проверяем статус ответа
        if (!result.success) {
            console.error('[Auth] Server returned error:', result.error);
            showError('forgotPasswordMessage', `Ошибка. Проверьте консоль`);
            return;
        }

        showSuccess('forgotPasswordMessage', 'Ссылка отправлена! Проверьте почту для восстановления пароля.');
        event.target.reset();
        setTimeout(() => switchForm('loginForm'), 3000);

    } catch (error) {
        console.error('Forgot password error:', error);
        showError('forgotPasswordMessage', 'Ошибка подключения. Попробуйте позже.');
    }
}

function validateLoginForm(username, password) {
    const errors = {};

    if (!username) {
        errors.loginUsernameError = 'Укажите имя пользователя или email';
    } else if (containsCyrillic(username)) {
        errors.loginUsernameError = 'Имя пользователя/email не должно содержать русские символы';
    }

    if (!password) {
        errors.loginPasswordError = 'Укажите пароль';
    } else if (containsCyrillic(password)) {
        errors.loginPasswordError = 'Пароль не должен содержать русские символы';
    } else if (password.length < 8) {
        errors.loginPasswordError = 'Пароль должен быть минимум 8 символов';
    } else if (!hasLettersAndNumbers(password)) {
        errors.loginPasswordError = 'Пароль должен содержать буквы и цифры';
    }

    return errors;
}
function validateRegisterForm(username, name, email, password, termsCheckbox) {
    const errors = {};

    if (!username) {
        errors.registerUsernameError = 'Укажите имя пользователя';
    } else if (containsCyrillic(username)) {
        errors.registerUsernameError = 'Имя пользователя не должно содержать русские символы';
    } else if (username.length < 3) {
        errors.registerUsernameError = 'Имя пользователя: минимум 3 символа';
    } else if (username.length > 50) {
        errors.registerUsernameError = 'Имя пользователя: максимум 50 символов';
    }

    if (!name) {
        errors.registerNameError = 'Укажите ваше имя';
    } else if (name.length < 2) {
        errors.registerNameError = 'Имя: минимум 2 символа';
    } else if (name.length > 100) {
        errors.registerNameError = 'Имя: максимум 100 символов';
    }

    if (!isValidEmail(email)) {
        errors.registerEmailError = 'Некорректный email адрес';
    }

    if (!password) {
        errors.registerPasswordError = 'Укажите пароль';
    } else if (containsCyrillic(password)) {
        errors.registerPasswordError = 'Пароль не должен содержать русские символы';
    } else if (password.length < 8) {
        errors.registerPasswordError = 'Пароль: минимум 8 символов';
    } else if (password.length > 50) {
        errors.registerPasswordError = 'Пароль: максимум 50 символов';
    } else if (!hasLettersAndNumbers(password)) {
        errors.registerPasswordError = 'Пароль должен содержать буквы и цифры';
    }

    if (!termsCheckbox) {
        errors.registerTermsError = 'Необходимо согласиться с условиями';
    }

    return errors;
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}
function hasLettersAndNumbers(password) {
    const hasLetters = /[a-zA-Zа-яА-ЯёЁ]/.test(password);
    const hasNumbers = /\d/.test(password);
    return hasLetters && hasNumbers;
}
function containsCyrillic(text) {
    return /[а-яА-ЯёЁ]/.test(text);
}

function showErrorMessage(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.classList.add('show');
    }
}
function clearErrors(errorIds) {
    errorIds.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = '';
            element.classList.remove('show');
        }
    });
}

function showLoading(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.className = 'alert-message loading';
        element.style.display = 'block';
    }
}
function showSuccess(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = 'alert-message success';
        element.style.display = 'block';
    }
}
function showError(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = 'alert-message error';
        element.style.display = 'block';
    }
}

function togglePasswordVisibility(button) {
    // Найти input в родительском контейнере
    const inputWrapper = button.closest('.input-wrapper');
    const input = inputWrapper.querySelector('input[type="password"], input[type="text"]');
    const icon = button.querySelector('i');

    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('bi-eye-fill');
        icon.classList.add('bi-eye-slash-fill');
    } else {
        input.type = 'password';
        icon.classList.remove('bi-eye-slash-fill');
        icon.classList.add('bi-eye-fill');
    }
}
function switchForm(formId) {
    // Получить все панели форм
    const formPanels = document.querySelectorAll('.form-panel');

    // Скрыть все панели
    formPanels.forEach(panel => {
        panel.classList.remove('active');
    });

    // Показать указанную панель
    const targetForm = document.getElementById(formId);
    if (targetForm) {
        targetForm.classList.add('active');

        // Скролл к форме на мобильных устройствах
        if (window.innerWidth < 768) {
            targetForm.scrollIntoView({ behavior: 'smooth' });
        }

        // Очистить сообщения об ошибках при переключении
        const messageElements = targetForm.querySelectorAll('.alert-message');
        messageElements.forEach(el => {
            el.style.display = 'none';
            el.textContent = '';
            el.className = 'alert-message';
        });

        // Сфокусировать первый input
        const firstInput = targetForm.querySelector('input');
        if (firstInput) {
            setTimeout(() => firstInput.focus(), 100);
        }
    }
}