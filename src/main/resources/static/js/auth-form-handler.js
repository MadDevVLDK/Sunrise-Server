// Получить context-path из текущего URL
const BASE_URL = document.location.pathname.split('/').slice(0, -1).join('/');

// API endpoints
const API_ENDPOINTS = {
    login: BASE_URL + '/auth/login',
    register: BASE_URL + '/auth/register',
    forgotPassword: BASE_URL + '/auth/change-password'
};

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
        const response = await fetch(API_ENDPOINTS.login, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showSuccess('loginMessage', 'Успешно! Перенаправление...');
            // Сохраняем токен если есть
            if (data.result && data.result.token) {
                localStorage.setItem('authToken', data.result.token);
            }
            // Перенаправляем на главную или профиль
            setTimeout(() => window.location.href = '/', 1500);
        } else {
            // Обработка ошибок с сервера
            handleServerErrors(data, 'login');
        }
    } catch (error) {
        console.error('Login error:', error);
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
        const response = await fetch(API_ENDPOINTS.register, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                name: name,
                email: email,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showSuccess('registerMessage', 'Аккаунт создан! Проверьте email для подтверждения.');
            event.target.reset();
            setTimeout(() => switchForm('loginForm'), 2000);
        } else {
            // Обработка ошибок с сервера
            handleServerErrors(data, 'register');
        }
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
        const response = await fetch(API_ENDPOINTS.forgotPassword, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `email=${encodeURIComponent(email)}`
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showSuccess('forgotPasswordMessage', 'Ссылка отправлена! Проверьте почту для восстановления пароля.');
            event.target.reset();
            setTimeout(() => switchForm('loginForm'), 3000);
        } else {
            // API отправляет одинаковое сообщение для безопасности
            showSuccess('forgotPasswordMessage', 'Если аккаунт существует, ссылка отправлена на email.');
            event.target.reset();
            setTimeout(() => switchForm('loginForm'), 3000);
        }
    } catch (error) {
        console.error('Forgot password error:', error);
        showError('forgotPasswordMessage', 'Ошибка подключения. Попробуйте позже.');
    }
}
function handleServerErrors(data, formType) {
    const messageElementId = formType === 'login' ? 'loginMessage' : 'registerMessage';

    // Если есть поле error - это общая ошибка
    if (data.error) {
        showError(messageElementId, data.error);
        return;
    }

    // Если есть поле message - это тоже общая ошибка
    if (data.message) {
        showError(messageElementId, data.message);
        return;
    }

    // Если есть поле errors (массив или объект с подробными ошибками)
    if (data.errors) {
        const errors = data.errors;

        if (Array.isArray(errors)) {
            // Если это массив строк
            const errorMessage = errors.join('; ');
            showError(messageElementId, errorMessage);
        } else if (typeof errors === 'object') {
            // Если это объект с названиями полей
            const fieldErrors = {};

            if (formType === 'login') {
                if (errors.username) fieldErrors.loginUsernameError = errors.username;
                if (errors.password) fieldErrors.loginPasswordError = errors.password;
            } else if (formType === 'register') {
                if (errors.username) fieldErrors.registerUsernameError = errors.username;
                if (errors.name) fieldErrors.registerNameError = errors.name;
                if (errors.email) fieldErrors.registerEmailError = errors.email;
                if (errors.password) fieldErrors.registerPasswordError = errors.password;
            }

            // Если есть ошибки полей - показываем их
            if (Object.keys(fieldErrors).length > 0) {
                Object.entries(fieldErrors).forEach(([field, message]) => {
                    showErrorMessage(field, message);
                });
            } else {
                // Если нет специфичных ошибок полей - показываем общую ошибку
                showError(messageElementId, 'Попробуйте позже');
            }
        } else {
            showError(messageElementId, 'Попробуйте позже');
        }
        return;
    }

    // По умолчанию
    const defaultMessages = {
        login: 'Ошибка входа. Проверьте данные.',
        register: 'Ошибка регистрации. Попробуйте другой username/email.'
    };
    showError(messageElementId, defaultMessages[formType] || 'Произошла ошибка. Попробуйте позже.');
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